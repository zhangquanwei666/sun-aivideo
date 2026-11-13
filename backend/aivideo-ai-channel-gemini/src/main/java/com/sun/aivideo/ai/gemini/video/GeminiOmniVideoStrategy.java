package com.sun.aivideo.ai.gemini.video;

import com.sun.aivideo.ai.auth.ChannelCredentials;
import com.sun.aivideo.ai.auth.ProviderErrors;
import com.sun.aivideo.common.enums.AiChannel;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.sun.aivideo.ai.gemini.config.GeminiClientConfig;
import com.sun.aivideo.ai.gemini.config.GeminiProperties;
import com.sun.aivideo.ai.video.strategy.VideoGenerationStrategy;
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

    public GeminiOmniVideoStrategy(@Qualifier(GeminiClientConfig.VIDEO_REST_CLIENT) RestClient restClient,
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
    public VideoSubmitResult submit(VideoGenerateRequest req, ChannelCredentials credentials) {
        LocalDateTime start = LocalDateTime.now();
        JsonNode interaction = call(() -> restClient.post()
                .uri(CREATE_PATH)
                .header("x-goog-api-key", credentials.keyFor(AiChannel.GEMINI))
                .contentType(MediaType.APPLICATION_JSON)
                .body(buildBody(req))
                .retrieve()
                .body(JsonNode.class));
        LocalDateTime end = LocalDateTime.now();

        String id = interaction.path("id").asText(null);
        byte[] video = extractVideo(interaction, credentials);
        if (video == null) {
            throw new AiClientException("NO_VIDEO", "Gemini 未返回视频，请检查提示词、模型权限或配额");
        }
        if (!StringUtils.hasText(id)) {
            id = "omni-" + UUID.randomUUID();
        }
        completed.put(credentials.cacheKey(id), new Completed(video, req.getDuration(), start, end));
        log.info("Gemini Omni 视频生成完成 id={} model={} bizTaskId={} {}KB 耗时{}ms", id, req.getModelCode(),
                req.getBizTaskId(), video.length / 1024, Duration.between(start, end).toMillis());
        // 视频已生成，交给下一次轮询入库
        return new VideoSubmitResult(id, VideoTaskState.RUNNING);
    }

    @Override
    public VideoTaskResult query(String providerTaskId, ChannelCredentials credentials) {
        Completed c = completed.remove(credentials.cacheKey(providerTaskId));
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
                .header("x-goog-api-key", credentials.keyFor(AiChannel.GEMINI))
                .retrieve()
                .body(JsonNode.class));
        String status = interaction.path("status").asText("").toLowerCase(Locale.ROOT);
        return switch (status) {
            case "completed" -> {
                byte[] video = extractVideo(interaction, credentials);
                yield video != null
                        ? VideoTaskResult.builder().state(VideoTaskState.SUCCEEDED).videoData(video).build()
                        : VideoTaskResult.builder().state(VideoTaskState.FAILED)
                            .errorCode("NO_VIDEO").errorMessage("Gemini 生成失败，请检查厂商控制台").build();
            }
            case "failed", "error" -> VideoTaskResult.builder().state(VideoTaskState.FAILED)
                    .errorCode("GENERATION_REJECTED").errorMessage("Gemini 生成失败，请检查厂商控制台").build();
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
    private byte[] extractVideo(JsonNode interaction, ChannelCredentials credentials) {
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
                    return download(uri, credentials);
                }
            }
        }
        return null;
    }

    /** 视频以文件形式返回时，带上 API Key 下载（与接口走同一代理） */
    private byte[] download(String uri, ChannelCredentials credentials) {
        URI base = URI.create(geminiProperties.getVideoBaseUrl() + "/");
        URI url = base.resolve(uri);
        if (!java.util.Objects.equals(base.getScheme(), url.getScheme())
                || !java.util.Objects.equals(base.getHost(), url.getHost()) || base.getPort() != url.getPort()
                || url.getUserInfo() != null || url.getFragment() != null) {
            throw new AiClientException("UNTRUSTED_DOWNLOAD", "厂商返回的下载地址不在已配置的 API 域名内，未发送密钥");
        }
        byte[] bytes = call(() -> restClient.get()
                .uri(url)
                .header("x-goog-api-key", credentials.keyFor(AiChannel.GEMINI))
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
            throw new AiClientException("IMAGE_FETCH_FAILED", "下载首帧图片失败，请检查图片地址");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new AiClientException("IMAGE_FETCH_FAILED", "下载首帧图片被中断", e);
        }
    }

    private <T> T call(Supplier<T> request) {
        try {
            T resp = request.get();
            if (resp == null) {
                throw new AiClientException("EMPTY_RESPONSE", "Gemini 返回为空");
            }
            return resp;
        } catch (RestClientResponseException e) {
            throw ProviderErrors.http(e.getStatusCode().value());
        } catch (RestClientException e) {
            throw ProviderErrors.network();
        }
    }
}
