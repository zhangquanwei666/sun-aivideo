package com.sun.aivideo.ai.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.net.InetSocketAddress;
import java.net.ProxySelector;
import java.net.http.HttpClient;
import java.time.Duration;

/**
 * AI 模块的公共 HTTP 客户端。大模型客户端由各 LlmStrategy 自行创建。
 */
@Configuration
@EnableConfigurationProperties({DashScopeProperties.class, LlmProperties.class, MockVideoProperties.class, GeminiProperties.class})
public class AiClientConfig {

    public static final String DASHSCOPE_REST_CLIENT = "dashScopeRestClient";
    public static final String DASHSCOPE_IMAGE_REST_CLIENT = "dashScopeImageRestClient";
    public static final String GEMINI_REST_CLIENT = "geminiRestClient";
    public static final String GEMINI_VIDEO_REST_CLIENT = "geminiVideoRestClient";

    /** 图片同步生成可能需要几十秒，读取超时至少 180 秒 */
    private static final int IMAGE_MIN_READ_TIMEOUT_SECONDS = 180;

    /** 百炼视频生成（异步任务，单次请求很快） */
    @Bean(DASHSCOPE_REST_CLIENT)
    public RestClient dashScopeRestClient(DashScopeProperties cfg) {
        return dashScopeClient(cfg, cfg.getReadTimeoutSeconds());
    }

    /** 百炼文生图（同步接口，等待图片生成完成才返回） */
    @Bean(DASHSCOPE_IMAGE_REST_CLIENT)
    public RestClient dashScopeImageRestClient(DashScopeProperties cfg) {
        return dashScopeClient(cfg, Math.max(cfg.getReadTimeoutSeconds(), IMAGE_MIN_READ_TIMEOUT_SECONDS));
    }

    /** Google Gemini（Nano Banana 文生图），支持可选的 HTTP 代理 */
    @Bean(GEMINI_REST_CLIENT)
    public RestClient geminiRestClient(GeminiProperties cfg) {
        return geminiClient(cfg, cfg.getBaseUrl(), cfg.getReadTimeoutSeconds());
    }

    /** Google Gemini（Omni Flash 视频，Interactions API，同步接口，等待时间更长） */
    @Bean(GEMINI_VIDEO_REST_CLIENT)
    public RestClient geminiVideoRestClient(GeminiProperties cfg) {
        return geminiClient(cfg, cfg.getVideoBaseUrl(), cfg.getVideoReadTimeoutSeconds());
    }

    private static RestClient geminiClient(GeminiProperties cfg, String baseUrl, int readTimeoutSeconds) {
        HttpClient.Builder builder = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(cfg.getConnectTimeoutSeconds()))
                .followRedirects(HttpClient.Redirect.NORMAL);
        if (cfg.hasProxy()) {
            builder.proxy(ProxySelector.of(new InetSocketAddress(cfg.getProxyHost(), cfg.getProxyPort())));
        }
        JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(builder.build());
        factory.setReadTimeout(Duration.ofSeconds(readTimeoutSeconds));
        return RestClient.builder()
                .baseUrl(baseUrl)
                .requestFactory(factory)
                .defaultHeader("x-goog-api-key", cfg.getApiKey() == null ? "" : cfg.getApiKey())
                .build();
    }

    private static RestClient dashScopeClient(DashScopeProperties cfg, int readTimeoutSeconds) {
        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(cfg.getConnectTimeoutSeconds()))
                .build();
        JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(httpClient);
        factory.setReadTimeout(Duration.ofSeconds(readTimeoutSeconds));
        return RestClient.builder()
                .baseUrl(cfg.getBaseUrl())
                .requestFactory(factory)
                .defaultHeader("Authorization", "Bearer " + (cfg.getApiKey() == null ? "" : cfg.getApiKey()))
                .build();
    }
}
