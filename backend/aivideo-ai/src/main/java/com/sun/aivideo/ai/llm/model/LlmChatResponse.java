package com.sun.aivideo.ai.llm.model;

import com.sun.aivideo.common.enums.LlmProvider;
import lombok.Builder;
import lombok.Data;

/**
 * 大模型对话结果，含 tokens 用量与耗时（费用由调用方按单价计算）。
 */
@Data
@Builder
public class LlmChatResponse {

    private LlmProvider provider;
    private String modelName;
    private String content;
    private int inputTokens;
    private int outputTokens;
    private long elapsedMs;
}
