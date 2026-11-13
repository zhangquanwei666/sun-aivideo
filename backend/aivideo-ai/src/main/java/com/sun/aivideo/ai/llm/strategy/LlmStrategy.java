package com.sun.aivideo.ai.llm.strategy;

import com.sun.aivideo.ai.auth.ChannelCredentials;
import com.sun.aivideo.ai.llm.model.LlmChatRequest;
import com.sun.aivideo.ai.llm.model.LlmChatResponse;
import com.sun.aivideo.common.enums.LlmProvider;

/**
 * 大模型调用策略：每个厂商一个实现。
 * 新增厂商：实现本接口并声明为 Spring Bean，{@link com.sun.aivideo.ai.llm.LlmStrategyFactory} 会自动收录，
 * 再把 aivideo.llm.provider 配置成对应的厂商即可。
 */
public interface LlmStrategy {

    /** 策略对应的厂商（工厂按此注册） */
    LlmProvider provider();

/** 实际调用的模型名 */
    String modelName();

    LlmChatResponse chat(LlmChatRequest request, ChannelCredentials credentials);
}
