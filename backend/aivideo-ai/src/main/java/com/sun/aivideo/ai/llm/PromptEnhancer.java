package com.sun.aivideo.ai.llm;

import com.sun.aivideo.ai.llm.model.LlmChatRequest;
import com.sun.aivideo.ai.llm.model.LlmChatResponse;
import com.sun.aivideo.ai.llm.model.PromptEnhanceResult;
import com.sun.aivideo.ai.llm.strategy.LlmStrategy;
import com.sun.aivideo.common.enums.VideoMode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Optional;

/**
 * 提示词润色：把用户的简单描述扩写成适合视频模型的提示词。
 * 具体调用哪个大模型由 {@link LlmStrategyFactory} 决定。
 */
@Component
@RequiredArgsConstructor
public class PromptEnhancer {

    private static final String T2V_SYSTEM = """
            你是专业的 AI 视频提示词工程师。把用户的描述改写成一段适合文生视频模型的中文提示词：
            1. 明确主体、动作、场景、光线、色调、镜头语言（景别、运镜）和画面风格；
            2. 保留用户的核心意图，不要添加与原意冲突的内容；
            3. 控制在 300 字以内，只输出提示词本身，不要解释、不要加引号或标题。
            """;

    private static final String I2V_SYSTEM = """
            你是专业的 AI 视频提示词工程师。用户会提供一张首帧图片（你看不到图片）以及对画面运动的描述。
            请把描述改写成一段适合图生视频模型的中文提示词：
            1. 重点描述主体的动作、表情变化、环境动态和镜头运动，不要重新描述图片里已有的静态内容；
            2. 保留用户的核心意图；
            3. 控制在 200 字以内，只输出提示词本身，不要解释、不要加引号或标题。
            """;

    private static final String T2I_SYSTEM = """
            你是专业的 AI 绘画提示词工程师。把用户的描述改写成一段适合文生图模型的中文提示词：
            1. 明确主体、外观细节、姿态、场景、构图、光线、色调和画面风格；
            2. 静态画面，不要描述镜头运动或随时间变化的动作；
            3. 保留用户的核心意图，控制在 300 字以内，只输出提示词本身，不要解释、不要加引号或标题。
            """;

    private final LlmStrategyFactory llmStrategyFactory;

    /** 是否有可用的大模型（真实或 Mock） */
    public boolean available() {
        return llmStrategyFactory.resolveDefault().isPresent();
    }

    /** 当前使用的模型名，没有可用模型时返回 null */
    public String currentModelName() {
        return llmStrategyFactory.resolveDefault().map(LlmStrategy::modelName).orElse(null);
    }

    public PromptEnhanceResult enhance(String prompt, VideoMode mode) {
        Optional<LlmStrategy> strategy = llmStrategyFactory.resolveDefault();
        if (strategy.isEmpty() || !StringUtils.hasText(prompt)) {
            return PromptEnhanceResult.builder().prompt(prompt).enhanced(false).build();
        }
        LlmChatResponse resp = strategy.get().chat(LlmChatRequest.builder()
                .systemPrompt(switch (mode) {
                    case I2V -> I2V_SYSTEM;
                    case T2I -> T2I_SYSTEM;
                    default -> T2V_SYSTEM;
                })
                .userPrompt(prompt)
                .build());
        String text = resp.getContent();
        return PromptEnhanceResult.builder()
                .prompt(StringUtils.hasText(text) ? text.trim() : prompt)
                .enhanced(true)
                .provider(resp.getProvider())
                .modelName(resp.getModelName())
                .inputTokens(resp.getInputTokens())
                .outputTokens(resp.getOutputTokens())
                .elapsedMs(resp.getElapsedMs())
                .build();
    }
}
