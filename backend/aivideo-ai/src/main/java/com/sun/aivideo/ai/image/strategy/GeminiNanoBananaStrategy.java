package com.sun.aivideo.ai.image.strategy;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.sun.aivideo.ai.config.AiClientConfig;
import com.sun.aivideo.ai.config.GeminiProperties;
import com.sun.aivideo.ai.exception.AiClientException;
import com.sun.aivideo.ai.image.model.GeneratedImage;
import com.sun.aivideo.ai.image.model.ImageGenerateRequest;
import com.sun.aivideo.ai.image.model.ImageGenerateResult;
import com.sun.aivideo.common.enums.ImageProvider;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

/**
 * 文生图策略：Google Gemini · Nano Banana（gemini-3-pro-image、gemini-3.1-flash-image 等），同步接口。
 * <ul>
 *     <li>POST {base-url}/models/{model}:generateContent，Header: x-goog-api-key</li>
 *     <li>图片在 candidates[0].content.parts[].inlineData.data（base64）</li>
 * </ul>
 */
@Slf4j
@Component
public class GeminiNanoBananaStrategy implements ImageGenerationStrategy {

    private static final String GENERATE_PATH = "/models/{model}:generateContent";

    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final GeminiProperties geminiProperties;

    public GeminiNanoBananaStrategy(@Qualifier(AiClientConfig.GEMINI_REST_CLIENT) RestClient restClient,
                                    ObjectMapper objectMapper,
                                    GeminiProperties geminiProperties) {
        this.restClient = restClient;
        this.objectMapper = objectMapper;
        this.geminiProperties = geminiProperties;
    }

    @Override
    public ImageProvider provider() {
        return ImageProvider.GEMINI;
    }

    @Override
    public boolean available() {
        return geminiProperties.hasApiKey();
    }

    @Override
    public ImageGenerateResult generate(ImageGenerateRequest req) {
        JsonNode resp = call(req.getModelCode(), buildBody(req));

        List<GeneratedImage> images = new ArrayList<>();
        StringBuilder texts = new StringBuilder();
        JsonNode candidates = resp.path("candidates");
        for (JsonNode candidate : candidates) {
            for (JsonNode part : candidate.path("content").path("parts")) {
                if (part.path("thought").asBoolean(false)) {
                    // 思考过程中的草图不计费，也不作为结果
                    continue;
                }
                JsonNode inline = part.has("inlineData") ? part.path("inlineData") : part.path("inline_data");
                String data = inline.path("data").asText(null);
                if (StringUtils.hasText(data)) {
                    String mime = inline.has("mimeType") ? inline.path("mimeType").asText() : inline.path("mime_type").asText("image/png");
                    images.add(GeneratedImage.ofData(Base64.getDecoder().decode(data), mime));
                } else if (part.hasNonNull("text")) {
                    texts.append(part.path("text").asText()).append(' ');
                }
            }
        }
        if (images.isEmpty()) {
            throw new AiClientException(failureCode(resp), "Gemini 未返回图片：" + failureReason(resp, texts.toString()));
        }

        JsonNode usage = resp.path("usageMetadata");
        log.info("Gemini 文生图完成 model={} bizTaskId={} 张数={}", req.getModelCode(), req.getBizTaskId(), images.size());
        return ImageGenerateResult.builder()
                .images(images)
                .inputTokens(usage.has("promptTokenCount") ? usage.path("promptTokenCount").asInt() : null)
                .outputTokens(usage.has("candidatesTokenCount") ? usage.path("candidatesTokenCount").asInt() : null)
                .build();
    }

    private ObjectNode buildBody(ImageGenerateRequest req) {
        ObjectNode root = objectMapper.createObjectNode();
        ArrayNode parts = root.putArray("contents").addObject().putArray("parts");
        if (req.getReferenceImages() != null) {
            for (String img : req.getReferenceImages()) {
                DataUri uri = DataUri.parse(img);
                parts.addObject().putObject("inline_data")
                        .put("mime_type", uri.mimeType())
                        .put("data", uri.base64());
            }
        }
        parts.addObject().put("text", req.getPrompt());

        ObjectNode config = root.putObject("generationConfig");
        config.putArray("responseModalities").add("TEXT").add("IMAGE");
        ObjectNode imageConfig = config.putObject("imageConfig");
        if (StringUtils.hasText(req.getAspectRatio())) {
            imageConfig.put("aspectRatio", req.getAspectRatio());
        }
        if (StringUtils.hasText(req.getImageSize())) {
            imageConfig.put("imageSize", req.getImageSize());
        }
        return root;
    }

    private JsonNode call(String model, ObjectNode body) {
        try {
            JsonNode resp = restClient.post()
                    .uri(GENERATE_PATH, model)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(JsonNode.class);
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
            throw new AiClientException(code, "Gemini 接口错误(HTTP " + e.getStatusCode().value() + ")：" + message, e);
        } catch (RestClientException e) {
            throw new AiClientException("NETWORK_ERROR",
                    "调用 Gemini 失败：" + e.getMessage() + "（国内网络请配置 aivideo.gemini.proxy-host / proxy-port 或中转地址）", e);
        }
    }

    private static String failureCode(JsonNode resp) {
        String block = resp.path("promptFeedback").path("blockReason").asText(null);
        if (StringUtils.hasText(block)) {
            return block;
        }
        return resp.path("candidates").path(0).path("finishReason").asText("NO_IMAGE");
    }

    private static String failureReason(JsonNode resp, String texts) {
        String block = resp.path("promptFeedback").path("blockReason").asText(null);
        if (StringUtils.hasText(block)) {
            return "提示词被安全策略拦截（" + block + "）";
        }
        String finish = resp.path("candidates").path(0).path("finishReason").asText(null);
        String detail = StringUtils.hasText(texts) ? texts.trim() : "";
        return (StringUtils.hasText(finish) ? "finishReason=" + finish + " " : "") + detail;
    }

    /** 解析 data:image/png;base64,xxxx */
    private record DataUri(String mimeType, String base64) {
        static DataUri parse(String uri) {
            if (uri == null || !uri.startsWith("data:") || !uri.contains(";base64,")) {
                throw new AiClientException("INVALID_REFERENCE", "参考图必须是 data URI");
            }
            int comma = uri.indexOf(";base64,");
            return new DataUri(uri.substring(5, comma), uri.substring(comma + 8));
        }
    }
}
