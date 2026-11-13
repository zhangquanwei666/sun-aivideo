package com.sun.aivideo.ai.dashscope.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.net.http.HttpClient;
import java.time.Duration;

/** 阿里云百炼模块 HTTP 客户端配置。 */
@Configuration
@EnableConfigurationProperties(DashScopeProperties.class)
public class DashScopeClientConfig {

    public static final String VIDEO_REST_CLIENT = "dashScopeRestClient";
    public static final String IMAGE_REST_CLIENT = "dashScopeImageRestClient";
    private static final int IMAGE_MIN_READ_TIMEOUT_SECONDS = 180;

    @Bean(VIDEO_REST_CLIENT)
    public RestClient videoRestClient(DashScopeProperties cfg) {
        return createClient(cfg, cfg.getReadTimeoutSeconds());
    }

    @Bean(IMAGE_REST_CLIENT)
    public RestClient imageRestClient(DashScopeProperties cfg) {
        return createClient(cfg, Math.max(cfg.getReadTimeoutSeconds(), IMAGE_MIN_READ_TIMEOUT_SECONDS));
    }

    private static RestClient createClient(DashScopeProperties cfg, int readTimeoutSeconds) {
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
