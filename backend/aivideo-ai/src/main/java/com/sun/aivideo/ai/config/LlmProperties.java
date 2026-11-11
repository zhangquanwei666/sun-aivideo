package com.sun.aivideo.ai.config;

import com.sun.aivideo.common.enums.LlmProvider;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.math.BigDecimal;

/**
 * 千问（提示词润色）配置（aivideo.llm.*）。
 */
@Data
@ConfigurationProperties(prefix = "aivideo.llm")
public class LlmProperties {

    private boolean enabled = true;
    /** 使用的大模型厂商，由 LlmStrategyFactory 选择对应策略 */
    private LlmProvider provider = LlmProvider.QWEN;
    private String baseUrl = "https://dashscope.aliyuncs.com/compatible-mode/v1";
    private String modelName = "qwen-plus";
    private int timeoutSeconds = 60;
    /** 元 / 百万 tokens */
    private BigDecimal inputPricePerMillion = new BigDecimal("0.8");
    private BigDecimal outputPricePerMillion = new BigDecimal("2.0");
}
