package com.sun.aivideo.ai.llm.model;

import com.sun.aivideo.common.enums.LlmProvider;
import lombok.Builder;
import lombok.Data;

/**
 * 提示词润色结果（费用由调用方按 tokens 计算）。
 */
@Data
@Builder
public class PromptEnhanceResult {

    private String prompt;
    /** 是否真正经过了大模型（含 Mock） */
    private boolean enhanced;
    private LlmProvider provider;
    private String modelName;
    private int inputTokens;
    private int outputTokens;
    private long elapsedMs;
}
