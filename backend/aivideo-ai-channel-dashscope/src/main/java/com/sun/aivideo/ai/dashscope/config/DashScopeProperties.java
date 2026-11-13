package com.sun.aivideo.ai.dashscope.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.StringUtils;

/**
 * 阿里云百炼配置（aivideo.dashscope.*）。
 */
@Data
@ConfigurationProperties(prefix = "aivideo.dashscope")
public class DashScopeProperties {

    private String baseUrl = "https://dashscope.aliyuncs.com";
    private int connectTimeoutSeconds = 10;
    private int readTimeoutSeconds = 60;
}
