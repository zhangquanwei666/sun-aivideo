package com.sun.aivideo.ai.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.net.http.HttpClient;
import java.time.Duration;

/**
 * AI 模块的公共客户端配置。大模型客户端由各 LlmStrategy 自行创建。
 */
@Configuration
@EnableConfigurationProperties({DashScopeProperties.class, LlmProperties.class, MockVideoProperties.class})
public class AiClientConfig {

    /** 调用百炼视频生成接口的 RestClient */
    @Bean
    public RestClient dashScopeRestClient(DashScopeProperties cfg) {
        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(cfg.getConnectTimeoutSeconds()))
                .build();
        JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(httpClient);
        factory.setReadTimeout(Duration.ofSeconds(cfg.getReadTimeoutSeconds()));
        return RestClient.builder()
                .baseUrl(cfg.getBaseUrl())
                .requestFactory(factory)
                .defaultHeader("Authorization", "Bearer " + (cfg.getApiKey() == null ? "" : cfg.getApiKey()))
                .build();
    }
}
