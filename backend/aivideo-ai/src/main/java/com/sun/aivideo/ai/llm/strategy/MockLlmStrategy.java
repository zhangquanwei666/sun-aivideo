package com.sun.aivideo.ai.llm.strategy;

import com.sun.aivideo.ai.llm.model.LlmChatRequest;
import com.sun.aivideo.ai.llm.model.LlmChatResponse;
import com.sun.aivideo.common.enums.LlmProvider;
import org.springframework.stereotype.Component;

/**
 * 大模型策略：本地模拟。没有 API Key 时用于联调润色流程，不调用任何模型、不产生费用（tokens 记 0）。
 * 返回内容 = 用户输入 + 固定的画面描述后缀。
 */
@Component
public class MockLlmStrategy implements LlmStrategy {

    private static final String MOCK_SUFFIX = "。画面细节丰富，电影质感，镜头缓慢推进，柔和自然光。（Mock 润色）";

    @Override
    public LlmProvider provider() {
        return LlmProvider.MOCK;
    }

    @Override
    public boolean available() {
        return true;
    }

    @Override
    public String modelName() {
        return "mock";
    }

    @Override
    public LlmChatResponse chat(LlmChatRequest request) {
        String input = request.getUserPrompt() == null ? "" : request.getUserPrompt().trim();
        return LlmChatResponse.builder()
                .provider(provider())
                .modelName(modelName())
                .content(input + MOCK_SUFFIX)
                .inputTokens(0)
                .outputTokens(0)
                .elapsedMs(0)
                .build();
    }
}
