package com.sun.aivideo.ai.dashscope.image;

import com.sun.aivideo.ai.auth.ChannelCredentials;
import com.sun.aivideo.ai.auth.ProviderErrors;
import com.sun.aivideo.common.enums.AiChannel;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.sun.aivideo.ai.dashscope.config.DashScopeClientConfig;
import com.sun.aivideo.ai.image.strategy.ImageGenerationStrategy;
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
import java.util.List;
import java.util.Map;

/**
 * 文生图策略：阿里云百炼 · 通义万相 2.7 图像（wan2.7-image / wan2.7-image-pro），同步接口。
 * <ul>
 *     <li>POST /api/v1/services/aigc/multimodal-generation/generation</li>
 *     <li>返回 output.choices[].message.content[].image（图片地址 24 小时有效）</li>
 * </ul>
 */
@Slf4j
@Component
public class DashScopeWanImageStrategy implements ImageGenerationStrategy {

    private static final String GENERATE_PATH = "/api/v1/services/aigc/multimodal-generation/generation";

    /** 清晰度 × 画幅 → 像素尺寸（总像素与官方推荐值一致） */
    private static final Map<String, Map<String, String>> SIZE_TABLE = Map.of(
            "1K", Map.of("1:1", "1024*1024", "16:9", "1344*768", "9:16", "768*1344", "4:3", "1184*864", "3:4", "864*1184"),
            "2K", Map.of("1:1", "2048*2048", "16:9", "2688*1536", "9:16", "1536*2688", "4:3", "2368*1728", "3:4", "1728*2368"),
            "4K", Map.of("1:1", "4096*4096", "16:9", "5376*3072", "9:16", "3072*5376", "4:3", "4736*3456", "3:4", "3456*4736"));

    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    public DashScopeWanImageStrategy(@Qualifier(DashScopeClientConfig.IMAGE_REST_CLIENT) RestClient restClient,
                                     ObjectMapper objectMapper) {
        this.restClient = restClient;
        this.objectMapper = objectMapper;
    }

    @Override
    public ImageProvider provider() {
        return ImageProvider.DASHSCOPE;
    }

    @Override
    public ImageGenerateResult generate(ImageGenerateRequest req, ChannelCredentials credentials) {
        JsonNode resp = call(buildBody(req), credentials);
        List<GeneratedImage> images = new ArrayList<>();
        for (JsonNode choice : resp.path("output").path("choices")) {
            for (JsonNode content : choice.path("message").path("content")) {
                String url = content.path("image").asText(null);
                if (StringUtils.hasText(url)) {
                    images.add(GeneratedImage.ofUrl(url));
                }
            }
        }
        if (images.isEmpty()) {
            throw new AiClientException("NO_IMAGE", "百炼未返回图片");
        }
        JsonNode usage = resp.path("usage");
        log.info("百炼文生图完成 model={} bizTaskId={} 张数={}", req.getModelCode(), req.getBizTaskId(), images.size());
        return ImageGenerateResult.builder()
                .images(images)
                .inputTokens(usage.has("input_tokens") ? usage.path("input_tokens").asInt() : null)
                .outputTokens(usage.has("output_tokens") ? usage.path("output_tokens").asInt() : null)
                .build();
    }

    private ObjectNode buildBody(ImageGenerateRequest req) {
        ObjectNode root = objectMapper.createObjectNode();
        root.put("model", req.getModelCode());
        ArrayNode content = root.putObject("input").putArray("messages")
                .addObject().put("role", "user").putArray("content");
        boolean hasReference = req.getReferenceImages() != null && !req.getReferenceImages().isEmpty();
        if (hasReference) {
            // 参考图放在文字前面，输出画幅跟随最后一张参考图
            req.getReferenceImages().forEach(img -> content.addObject().put("image", img));
        }
        content.addObject().put("text", req.getPrompt());

        ObjectNode params = root.putObject("parameters");
        params.put("n", 1);
        params.put("watermark", false);
        if (hasReference) {
            // 有参考图时只能用 1K / 2K 简写，4K 仅支持纯文生图
            params.put("size", "4K".equals(req.getImageSize()) ? "2K" : req.getImageSize());
        } else {
            params.put("size", pixelSize(req.getImageSize(), req.getAspectRatio()));
        }
        return root;
    }

    private static String pixelSize(String imageSize, String ratio) {
        Map<String, String> byRatio = SIZE_TABLE.getOrDefault(imageSize, SIZE_TABLE.get("2K"));
        String size = byRatio.get(ratio);
        return size != null ? size : byRatio.get("1:1");
    }

    private JsonNode call(ObjectNode body, ChannelCredentials credentials) {
        try {
            JsonNode resp = restClient.post()
                    .uri(GENERATE_PATH)
                    .header("Authorization", "Bearer " + credentials.keyFor(AiChannel.DASHSCOPE))
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(JsonNode.class);
            if (resp == null) {
                throw new AiClientException("EMPTY_RESPONSE", "百炼返回为空");
            }
            if (StringUtils.hasText(resp.path("code").asText(null))) {
                throw ProviderErrors.rejected(resp.path("code").asText());
            }
            return resp;
        } catch (RestClientResponseException e) {
            throw ProviderErrors.http(e.getStatusCode().value());
        } catch (RestClientException e) {
            throw ProviderErrors.network();
        }
    }

}
