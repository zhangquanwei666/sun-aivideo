package com.sun.aivideo.ai.gemini.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.StringUtils;

/**
 * Google Gemini 配置（aivideo.gemini.*），用于 Nano Banana 文生图。
 */
@Data
@ConfigurationProperties(prefix = "aivideo.gemini")
public class GeminiProperties {

    /** Gemini API Key，通过环境变量 GEMINI_API_KEY 注入 */
    private String apiKey;
    /** 接口地址（含版本号）；国内可换成自己的中转地址 */
    private String baseUrl = "https://generativelanguage.googleapis.com/v1";
    /** 视频生成（Interactions API）的接口地址，目前在 v1beta 下 */
    private String videoBaseUrl = "https://generativelanguage.googleapis.com/v1beta";
    private int connectTimeoutSeconds = 15;
    /** 图片生成较慢，读取超时单独放宽 */
    private int readTimeoutSeconds = 180;
    /** 视频生成（Omni Flash 同步接口）更慢，读取超时再放宽 */
    private int videoReadTimeoutSeconds = 600;
    /** 可选：HTTP 代理（国内直连 Google 不通时使用），例如 127.0.0.1 / 7890 */
    private String proxyHost;
    private Integer proxyPort;

    public boolean hasApiKey() {
        return StringUtils.hasText(apiKey);
    }

    public boolean hasProxy() {
        return StringUtils.hasText(proxyHost) && proxyPort != null && proxyPort > 0;
    }
}
