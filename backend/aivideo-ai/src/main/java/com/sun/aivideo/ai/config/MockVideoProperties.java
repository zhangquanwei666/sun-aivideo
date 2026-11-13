package com.sun.aivideo.ai.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Mock 视频模型配置（aivideo.mock.*）。
 */
@Data
@ConfigurationProperties(prefix = "aivideo.mock")
public class MockVideoProperties {

    /** 厂商未配置 API Key 时是否允许回退到 Mock。 */
    private boolean whenNoKey = true;
    /** 返回的示例视频地址 */
    private String videoUrl;
    /** 模拟生成耗时（秒） */
    private int generateSeconds = 12;
}
