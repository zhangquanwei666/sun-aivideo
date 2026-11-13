package com.sun.aivideo.ai.dashscope.video;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.sun.aivideo.ai.dashscope.config.DashScopeClientConfig;
import com.sun.aivideo.ai.dashscope.config.DashScopeProperties;
import com.sun.aivideo.ai.video.strategy.VideoGenerationStrategy;
import org.springframework.beans.factory.annotation.Qualifier;
import com.sun.aivideo.ai.exception.AiClientException;
import com.sun.aivideo.ai.video.model.VideoGenerateRequest;
import com.sun.aivideo.ai.video.model.VideoSubmitResult;
import com.sun.aivideo.ai.video.model.VideoTaskResult;
import com.sun.aivideo.common.enums.ApiProtocol;
import com.sun.aivideo.common.enums.VideoProvider;
import com.sun.aivideo.common.enums.VideoTaskState;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoField;
import java.util.Map;
import java.util.function.Supplier;

/**
 * 视频生成策略：阿里云百炼 · 通义万相（纯 HTTP 调用，异步任务）。
 * <ul>
 *     <li>创建：POST /api/v1/services/aigc/video-generation/video-synthesis（Header: X-DashScope-Async: enable）</li>
 *     <li>查询：GET /api/v1/tasks/{task_id}</li>
 * </ul>
 */
@Slf4j
@Component
public class DashScopeWanVideoStrategy implements VideoGenerationStrategy {

    private static final String CREATE_PATH = "/api/v1/services/aigc/video-generation/video-synthesis";
    private static final String QUERY_PATH = "/api/v1/tasks/{taskId}";

    /** 百炼时间格式：2025-09-29 14:18:52.331 */
    private static final DateTimeFormatter TIME_FORMAT = new DateTimeFormatterBuilder()
            .appendPattern("yyyy-MM-dd HH:mm:ss")
            .optionalStart().appendFraction(ChronoField.NANO_OF_SECOND, 0, 9, true).optionalEnd()
            .toFormatter();

    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final DashScopeProperties dashScopeProperties;

    public DashScopeWanVideoStrategy(@Qualifier(DashScopeClientConfig.VIDEO_REST_CLIENT) RestClient dashScopeRestClient,
                                     ObjectMapper objectMapper,
                                     DashScopeProperties dashScopeProperties) {
        this.restClient = dashScopeRestClient;
        this.objectMapper = objectMapper;
        this.dashScopeProperties = dashScopeProperties;
    }

    @Override
    public VideoProvider provider() {
        return VideoProvider.DASHSCOPE;
    }

    /** 配置了百炼 API Key 才能真实调用 */
    @Override
    public boolean available() {
        return dashScopeProperties.hasApiKey();
    }

    @Override
    public VideoSubmitResult submit(VideoGenerateRequest request) {
        ObjectNode body = buildRequestBody(request);
        JsonNode resp = call(() -> restClient.post()
                .uri(CREATE_PATH)
                .header("X-DashScope-Async", "enable")
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .body(JsonNode.class));
        JsonNode output = resp.path("output");
        String taskId = output.path("task_id").asText(null);
        if (!StringUtils.hasText(taskId)) {
            throw new AiClientException("NO_TASK_ID", "百炼未返回 task_id：" + resp);
        }
        log.info("百炼任务已创建 taskId={} model={} bizTaskId={}", taskId, request.getModelCode(), request.getBizTaskId());
        VideoTaskState state = "RUNNING".equals(output.path("task_status").asText()) ? VideoTaskState.RUNNING : VideoTaskState.PENDING;
        return new VideoSubmitResult(taskId, state);
    }

    @Override
    public VideoTaskResult query(String providerTaskId) {
        JsonNode resp = call(() -> restClient.get()
                .uri(QUERY_PATH, providerTaskId)
                .retrieve()
                .body(JsonNode.class));
        JsonNode output = resp.path("output");
        JsonNode usage = resp.path("usage");
        String rawStatus = output.path("task_status").asText("UNKNOWN");

        VideoTaskResult.VideoTaskResultBuilder result = VideoTaskResult.builder()
                .submitTime(parseTime(output.path("submit_time").asText(null)))
                .scheduledTime(parseTime(output.path("scheduled_time").asText(null)))
                .endTime(parseTime(output.path("end_time").asText(null)));

        switch (rawStatus) {
            case "PENDING" -> result.state(VideoTaskState.PENDING);
            case "RUNNING" -> result.state(VideoTaskState.RUNNING);
            case "SUCCEEDED" -> result.state(VideoTaskState.SUCCEEDED)
                    .videoUrl(output.path("video_url").asText(null))
                    .billedDuration(readBilledDuration(usage));
            case "CANCELED" -> result.state(VideoTaskState.CANCELED);
            case "UNKNOWN" -> result.state(VideoTaskState.FAILED)
                    .errorCode("UNKNOWN")
                    .errorMessage("任务不存在或已超过 24 小时有效期");
            default -> result.state(VideoTaskState.FAILED)
                    .errorCode(output.path("code").asText(rawStatus))
                    .errorMessage(output.path("message").asText("生成失败"));
        }
        return result.build();
    }

    /** 按模型协议组装请求体，参数格式见百炼万相 API 参考 */
    private ObjectNode buildRequestBody(VideoGenerateRequest req) {
        ObjectNode root = objectMapper.createObjectNode();
        root.put("model", req.getModelCode());

        ObjectNode input = root.putObject("input");
        if (StringUtils.hasText(req.getPrompt())) {
            input.put("prompt", req.getPrompt());
        }
        if (StringUtils.hasText(req.getNegativePrompt())) {
            input.put("negative_prompt", req.getNegativePrompt());
        }

        ObjectNode params = root.putObject("parameters");
        if (req.getProtocol() == ApiProtocol.WAN_LEGACY_T2V) {
            // 旧版文生视频不认 resolution / ratio，只认具体像素
            String size = legacyT2vSize(req.getResolution(), req.getRatio());
            if (size == null) {
                throw new AiClientException("UNSUPPORTED_SIZE",
                        req.getModelCode() + " 不支持 " + req.getResolution() + " " + req.getRatio());
            }
            params.put("size", size);
        } else {
            params.put("resolution", req.getResolution());
        }
        params.put("duration", req.getDuration());
        params.put("prompt_extend", Boolean.TRUE.equals(req.getPromptExtend()));
        params.put("watermark", Boolean.TRUE.equals(req.getWatermark()));
        if (req.getSeed() != null) {
            params.put("seed", req.getSeed());
        }

        switch (req.getProtocol()) {
            case WAN27_T2V -> {
                if (StringUtils.hasText(req.getRatio())) {
                    params.put("ratio", req.getRatio());
                }
            }
            case WAN27_I2V -> {
                ArrayNode media = input.putArray("media");
                media.addObject().put("type", "first_frame").put("url", req.getImageSource());
            }
            case WAN_LEGACY_I2V -> {
                input.put("img_url", req.getImageSource());
                if (req.isSupportsAudio() && req.getAudio() != null) {
                    params.put("audio", req.getAudio());
                }
            }
            default -> {
                // WAN_LEGACY_T2V 的 size 已在上面处理
            }
        }
        return root;
    }

    /**
     * 旧版文生视频（wan2.2-t2v-plus 等）的 size：分辨率 + 画幅 → 具体像素，取自百炼「万相-文生视频 API 参考（2.1～2.6）」。
     * 480P 只有 16:9 / 9:16 / 1:1；1080P 另有 4:3 / 3:4。不支持的组合返回 null。
     */
    public static String legacyT2vSize(String resolution, String ratio) {
        String r = StringUtils.hasText(ratio) ? ratio : "16:9";
        return LEGACY_T2V_SIZES.getOrDefault(resolution, Map.of()).get(r);
    }

    private static final Map<String, Map<String, String>> LEGACY_T2V_SIZES = Map.of(
            "480P", Map.of("16:9", "832*480", "9:16", "480*832", "1:1", "624*624"),
            "1080P", Map.of("16:9", "1920*1080", "9:16", "1080*1920", "1:1", "1440*1440",
                    "4:3", "1632*1248", "3:4", "1248*1632"));

    /** 计费秒数：优先 usage.duration，其次 output_video_duration / video_duration */
    private Integer readBilledDuration(JsonNode usage) {
        for (String field : new String[]{"duration", "output_video_duration", "video_duration"}) {
            JsonNode node = usage.path(field);
            if (node.isNumber() && node.asInt() > 0) {
                return node.asInt();
            }
        }
        return null;
    }

    private LocalDateTime parseTime(String text) {
        if (!StringUtils.hasText(text)) {
            return null;
        }
        try {
            return LocalDateTime.parse(text, TIME_FORMAT);
        } catch (DateTimeParseException e) {
            log.warn("无法解析百炼时间：{}", text);
            return null;
        }
    }

    private JsonNode call(Supplier<JsonNode> request) {
        try {
            JsonNode resp = request.get();
            if (resp == null) {
                throw new AiClientException("EMPTY_RESPONSE", "百炼返回为空");
            }
            return resp;
        } catch (RestClientResponseException e) {
            String body = e.getResponseBodyAsString();
            String code = "HTTP_" + e.getStatusCode().value();
            String message = body;
            try {
                JsonNode err = objectMapper.readTree(body);
                code = err.path("code").asText(code);
                message = err.path("message").asText(body);
            } catch (Exception ignore) {
                // 非 JSON 错误体，直接透传
            }
            throw new AiClientException(code, "百炼接口错误(HTTP " + e.getStatusCode().value() + ")：" + message, e);
        } catch (RestClientException e) {
            throw new AiClientException("NETWORK_ERROR", "调用百炼失败：" + e.getMessage(), e);
        }
    }
}
