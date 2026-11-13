package com.sun.aivideo.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Web 服务自身的配置。llm / mock 配置在 aivideo-ai-core 模块中，厂商连接配置在对应 channel 模块中。
 */
@Data
@ConfigurationProperties(prefix = "aivideo")
public class AiVideoProperties {

    private Cors cors = new Cors();
    private Storage storage = new Storage();
    private Polling polling = new Polling();
    private Cost cost = new Cost();

    @Data
    public static class Cost {
        /** 美元兑人民币汇率：Gemini 等按美元计价的模型，费用按此折算成人民币后再统计 */
        private BigDecimal usdToCny = new BigDecimal("7.2");
    }

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
