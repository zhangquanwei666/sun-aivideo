package com.sun.aivideo.ai.gemini.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.net.InetSocketAddress;
import java.net.ProxySelector;
import java.net.http.HttpClient;
import java.time.Duration;

/** Google Gemini 模块 HTTP 客户端配置。 */
@Configuration
@EnableConfigurationProperties(GeminiProperties.class)
public class GeminiClientConfig {

    public static final String IMAGE_REST_CLIENT = "geminiRestClient";
    public static final String VIDEO_REST_CLIENT = "geminiVideoRestClient";

    @Bean(IMAGE_REST_CLIENT)
    public RestClient imageRestClient(GeminiProperties cfg) {
        return createClient(cfg, cfg.getBaseUrl(), cfg.getReadTimeoutSeconds());
    }

    @Bean(VIDEO_REST_CLIENT)
    public RestClient videoRestClient(GeminiProperties cfg) {
        return createClient(cfg, cfg.getVideoBaseUrl(), cfg.getVideoReadTimeoutSeconds());
    }

    private static RestClient createClient(GeminiProperties cfg, String baseUrl, int readTimeoutSeconds) {
        HttpClient.Builder builder = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(cfg.getConnectTimeoutSeconds()))
                .followRedirects(HttpClient.Redirect.NEVER);
        if (cfg.hasProxy()) {
            builder.proxy(ProxySelector.of(new InetSocketAddress(cfg.getProxyHost(), cfg.getProxyPort())));
        }
        JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(builder.build());
        factory.setReadTimeout(Duration.ofSeconds(readTimeoutSeconds));
        return RestClient.builder()
                .baseUrl(baseUrl)
                .requestFactory(factory)
                .build();
    }
}
