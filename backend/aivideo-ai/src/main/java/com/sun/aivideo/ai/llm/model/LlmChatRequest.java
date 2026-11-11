package com.sun.aivideo.ai.llm.model;

import lombok.Builder;
import lombok.Data;

/**
 * 大模型对话请求（单轮：系统提示词 + 用户输入）。
 */
@Data
@Builder
public class LlmChatRequest {

    private String systemPrompt;
    private String userPrompt;
    /** 为空时使用策略默认值 */
    private Double temperature;
}
