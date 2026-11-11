package com.sun.aivideo.ai.video.strategy;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.sun.aivideo.ai.config.AiClientConfig;
import com.sun.aivideo.ai.config.GeminiProperties;
import com.sun.aivideo.ai.exception.AiClientException;
import com.sun.aivideo.ai.video.model.VideoGenerateRequest;
import com.sun.aivideo.ai.video.model.VideoSubmitResult;
import com.sun.aivideo.ai.video.model.VideoTaskResult;
import com.sun.aivideo.common.enums.VideoProvider;
import com.sun.aivideo.common.enums.VideoTaskState;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

/**
 * 视频生成策略：Google Gemini Omni Flash（gemini-omni-1.2-flash，Interactions API）。
 * <p>Gemini API 中的 Veo 已于 2026-10-22 下线，官方替代为 Omni Flash。</p>
 * <ul>
 *     <li>POST {video-base-url}/interactions：同步生成，响应里直接带视频（steps[].content[] 中 type=video 的 data 或 uri）</li>
 *     <li>为了复用"提交 + 轮询"的流程：submit 阻塞到生成完成并缓存结果，下一次 query 直接返回缓存；
 *         服务重启导致缓存丢失时，query 通过 GET /interactions/{id} 重新取回</li>
 *     <li>原生带音频；时长 3–15 秒（带首帧时 3–10 秒）；画幅 16:9 / 9:16</li>
 * </ul>
 */
@Slf4j
@Component
public class GeminiOmniVideoStrategy implements VideoGenerationStrategy {

    private static final String CREATE_PATH = "/interactions";
    private static final String QUERY_PATH = "/interactions/{id}";
    private static final String DEFAULT_I2V_PROMPT = "让画面自然地动起来，保持画面主体和风格不变";

    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final GeminiProperties geminiProperties;
    private final HttpClient imageFetcher = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(15))
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();

    /** 已生成、等待下一次 query 取走的结果 */
    private final Map<String, Completed> completed = new ConcurrentHashMap<>();

    private record Completed(byte[] video, int duration, LocalDateTime start, LocalDateTime end) {
    }

    public GeminiOmniVideoStrategy(@Qualifier(AiClientConfig.GEMINI_VIDEO_REST_CLIENT) RestClient restClient,
                                   ObjectMapper objectMapper,
                                   GeminiProperties geminiProperties) {
        this.restClient = restClient;
        this.objectMapper = objectMapper;
        this.geminiProperties = geminiProperties;
    }

    @Override
    public VideoProvider provider() {
        return VideoProvider.GEMINI;
    }

    @Override
    public boolean available() {
        return geminiProperties.hasApiKey();
    }

    @Override
    public VideoSubmitResult submit(VideoGenerateRequest req) {
        LocalDateTime start = LocalDateTime.now();
        JsonNode interaction = call(() -> restClient.post()
                .uri(CREATE_PATH)
                .contentType(MediaType.APPLICATION_JSON)
                .body(buildBody(req))
                .retrieve()
                .body(JsonNode.class));
        LocalDateTime end = LocalDateTime.now();

        String id = interaction.path("id").asText(null);
        String status = interaction.path("status").asText("");
        byte[] video = extractVideo(interaction);
        if (video == null) {
            throw new AiClientException(errorCode(interaction, status), "Gemini 未返回视频：" + errorMessage(interaction, status));
        }
        if (!StringUtils.hasText(id)) {
            id = "omni-" + UUID.randomUUID();
        }
        completed.put(id, new Completed(video, req.getDuration(), start, end));
        log.info("Gemini Omni 视频生成完成 id={} model={} bizTaskId={} {}KB 耗时{}ms", id, req.getModelCode(),
                req.getBizTaskId(), video.length / 1024, Duration.between(start, end).toMillis());
        // 视频已生成，交给下一次轮询入库
        return new VideoSubmitResult(id, VideoTaskState.RUNNING);
    }

    @Override
    public VideoTaskResult query(String providerTaskId) {
        Completed c = completed.remove(providerTaskId);
        if (c != null) {
            return VideoTaskResult.builder()
                    .state(VideoTaskState.SUCCEEDED)
                    .videoData(c.video())
                    .billedDuration(c.duration())
                    .submitTime(c.start())
                    .scheduledTime(c.start())
                    .endTime(c.end())
                    .build();
        }
        // 缓存丢失（如服务重启），从 Gemini 重新取回
        JsonNode interaction = call(() -> restClient.get()
                .uri(QUERY_PATH, providerTaskId)
                .retrieve()
                .body(JsonNode.class));
        String status = interaction.path("status").asText("").toLowerCase(Locale.ROOT);
        return switch (status) {
            case "completed" -> {
                byte[] video = extractVideo(interaction);
                yield video != null
                        ? VideoTaskResult.builder().state(VideoTaskState.SUCCEEDED).videoData(video).build()
                        : VideoTaskResult.builder().state(VideoTaskState.FAILED)
                            .errorCode("NO_VIDEO").errorMessage(errorMessage(interaction, status)).build();
            }
            case "failed", "error" -> VideoTaskResult.builder().state(VideoTaskState.FAILED)
                    .errorCode(errorCode(interaction, status)).errorMessage(errorMessage(interaction, status)).build();
            case "cancelled", "canceled" -> VideoTaskResult.builder().state(VideoTaskState.CANCELED).build();
            default -> VideoTaskResult.builder().state(VideoTaskState.RUNNING).build();
        };
    }

    private ObjectNode buildBody(VideoGenerateRequest req) {
        ObjectNode root = objectMapper.createObjectNode();
        root.put("model", req.getModelCode());

        ArrayNode input = root.putArray("input");
        if (StringUtils.hasText(req.getImageSource())) {
            ImageData image = toImageData(req.getImageSource());
            input.addObject().put("type", "image").put("data", image.base64()).put("mime_type", image.mimeType());
        }
        String prompt = StringUtils.hasText(req.getPrompt()) ? req.getPrompt() : DEFAULT_I2V_PROMPT;
        if (StringUtils.hasText(req.getNegativePrompt())) {
            // Omni 没有单独的反向提示词参数，拼到提示词里
            prompt = prompt + "\n避免出现：" + req.getNegativePrompt();
        }
        input.addObject().put("type", "text").put("text", prompt);

        ObjectNode format = root.putObject("response_format");
        format.put("type", "video");
        format.put("duration", req.getDuration() + "s");
        format.put("resolution", req.getResolution().toLowerCase(Locale.ROOT));
        if (StringUtils.hasText(req.getRatio())) {
            format.put("aspect_ratio", req.getRatio());
        }
        return root;
    }

    /** 从 interaction 中取出视频：steps[].content[] 里 type=video 的 data（base64）或 uri */
    private byte[] extractVideo(JsonNode interaction) {
        for (JsonNode step : interaction.path("steps")) {
            for (JsonNode content : step.path("content")) {
                if (!"video".equals(content.path("type").asText())) {
                    continue;
                }
                String data = content.path("data").asText(null);
                if (StringUtils.hasText(data)) {
                    return Base64.getDecoder().decode(data);
                }
                String uri = content.path("uri").asText(null);
                if (StringUtils.hasText(uri)) {
                    return download(uri);
                }
            }
        }
        return null;
    }

    /** 视频以文件形式返回时，带上 API Key 下载（与接口走同一代理） */
    private byte[] download(String uri) {
        String url = uri.startsWith("http") ? uri : geminiProperties.getVideoBaseUrl() + "/" + uri;
        byte[] bytes = call(() -> restClient.get()
                .uri(URI.create(url))
                .retrieve()
                .body(byte[].class));
        if (bytes.length == 0) {
            throw new AiClientException("EMPTY_VIDEO", "下载 Gemini 视频失败：内容为空");
        }
        return bytes;
    }

    private record ImageData(String mimeType, String base64) {
    }

    /** 首帧：data URI 直接解析；公网地址先下载再转 base64 */
    private ImageData toImageData(String source) {
        if (source.startsWith("data:") && source.contains(";base64,")) {
            int comma = source.indexOf(";base64,");
            return new ImageData(source.substring(5, comma), source.substring(comma + 8));
        }
        try {
            HttpResponse<byte[]> resp = imageFetcher.send(
                    HttpRequest.newBuilder(URI.create(source)).timeout(Duration.ofSeconds(60)).GET().build(),
                    HttpResponse.BodyHandlers.ofByteArray());
            if (resp.statusCode() / 100 != 2) {
                throw new AiClientException("IMAGE_FETCH_FAILED", "下载首帧图片失败：HTTP " + resp.statusCode());
            }
            String mime = resp.headers().firstValue("Content-Type").orElse("image/png");
            return new ImageData(mime, Base64.getEncoder().encodeToString(resp.body()));
        } catch (IOException e) {
            throw new AiClientException("IMAGE_FETCH_FAILED", "下载首帧图片失败：" + e.getMessage(), e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new AiClientException("IMAGE_FETCH_FAILED", "下载首帧图片被中断", e);
        }
    }

    private static String errorCode(JsonNode interaction, String status) {
        String code = interaction.path("error").path("status").asText(null);
        if (!StringUtils.hasText(code)) {
            code = interaction.path("error").path("code").asText(null);
        }
        return StringUtils.hasText(code) ? code : (StringUtils.hasText(status) ? status.toUpperCase(Locale.ROOT) : "NO_VIDEO");
    }

    private static String errorMessage(JsonNode interaction, String status) {
        String msg = interaction.path("error").path("message").asText(null);
        if (StringUtils.hasText(msg)) {
            return msg;
        }
        // 没有视频时把模型输出的文字一并返回，便于排查（如安全策略拦截的说明）
        StringBuilder texts = new StringBuilder();
        for (JsonNode step : interaction.path("steps")) {
            for (JsonNode content : step.path("content")) {
                if ("text".equals(content.path("type").asText()) && "model_output".equals(step.path("type").asText())) {
                    texts.append(content.path("text").asText()).append(' ');
                }
            }
        }
        String detail = texts.toString().trim();
        return "status=" + status + (detail.isEmpty() ? "" : "，" + (detail.length() > 300 ? detail.substring(0, 300) + "…" : detail));
    }

    private <T> T call(Supplier<T> request) {
        try {
            T resp = request.get();
            if (resp == null) {
                throw new AiClientException("EMPTY_RESPONSE", "Gemini 返回为空");
            }
            return resp;
        } catch (RestClientResponseException e) {
            String message = e.getResponseBodyAsString();
            String code = "HTTP_" + e.getStatusCode().value();
            try {
                JsonNode err = objectMapper.readTree(message).path("error");
                code = err.path("status").asText(code);
                message = err.path("message").asText(message);
            } catch (Exception ignore) {
                // 非 JSON 错误体，直接透传
            }
            throw new AiClientException(code, "Gemini 视频接口错误(HTTP " + e.getStatusCode().value() + ")：" + message, e);
        } catch (RestClientException e) {
            throw new AiClientException("NETWORK_ERROR",
                    "调用 Gemini 失败：" + e.getMessage() + "（国内网络请配置 aivideo.gemini.proxy-host / proxy-port 或中转地址）", e);
        }
    }
}
