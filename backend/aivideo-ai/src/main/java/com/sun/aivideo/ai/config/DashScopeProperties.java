package com.sun.aivideo.ai.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.StringUtils;

/**
 * 阿里云百炼配置（aivideo.dashscope.*）。
 */
@Data
@ConfigurationProperties(prefix = "aivideo.dashscope")
public class DashScopeProperties {

    private String apiKey;
    private String baseUrl = "https://dashscope.aliyuncs.com";
    private int connectTimeoutSeconds = 10;
    private int readTimeoutSeconds = 60;
    /** 未配置 API Key 时是否允许走 Mock */
    private boolean mockWhenNoKey = true;

    public boolean hasApiKey() {
        return StringUtils.hasText(apiKey);
    }

    /** 没有配置 API Key 且允许 Mock 时，视频生成走 Mock 模型 */
    public boolean isMockMode() {
        return !hasApiKey() && mockWhenNoKey;
    }
}
