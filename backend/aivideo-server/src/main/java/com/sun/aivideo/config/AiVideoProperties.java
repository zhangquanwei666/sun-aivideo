package com.sun.aivideo.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

/**
 * Web 服务自身的配置。AI 相关配置（aivideo.dashscope / llm / mock）在 aivideo-ai 模块中。
 */
@Data
@ConfigurationProperties(prefix = "aivideo")
public class AiVideoProperties {

    private Cors cors = new Cors();
    private Storage storage = new Storage();
    private Polling polling = new Polling();

    @Data
    public static class Cors {
        /** 允许跨域的前端地址（支持通配），为空则不开启跨域 */
        private List<String> allowedOriginPatterns = new ArrayList<>();
    }

    @Data
    public static class Storage {
        private String baseDir = "./data";
        private boolean downloadVideo = true;
    }

    @Data
    public static class Polling {
        private long intervalMs = 10000;
        private int timeoutMinutes = 30;
    }
}
