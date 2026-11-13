package com.sun.aivideo.ai.dashscope.llm;

import com.sun.aivideo.ai.auth.ChannelCredentials;
import com.sun.aivideo.common.enums.AiChannel;
import com.sun.aivideo.ai.llm.strategy.LlmStrategy;
import com.sun.aivideo.ai.config.LlmProperties;
import com.sun.aivideo.ai.exception.AiClientException;
import com.sun.aivideo.ai.llm.model.LlmChatRequest;
import com.sun.aivideo.ai.llm.model.LlmChatResponse;
import com.sun.aivideo.common.enums.LlmProvider;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.output.TokenUsage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.time.Duration;

/**
 * 大模型策略：通义千问。百炼提供 OpenAI 兼容接口，通过 LangChain4j 的 OpenAiChatModel 调用。
 */
@Slf4j
@Component
public class QwenLlmStrategy implements LlmStrategy {

    private static final double DEFAULT_TEMPERATURE = 0.7;

    private final LlmProperties llmProperties;
    public QwenLlmStrategy(LlmProperties llmProperties) {
        this.llmProperties = llmProperties;
    }

    @Override
    public LlmProvider provider() {
        return LlmProvider.QWEN;
    }

    @Override
    public String modelName() {
        return llmProperties.getModelName();
    }

    @Override
    public LlmChatResponse chat(LlmChatRequest request, ChannelCredentials credentials) {
        ChatModel chatModel = OpenAiChatModel.builder()
                .baseUrl(llmProperties.getBaseUrl())
                .apiKey(credentials.keyFor(AiChannel.DASHSCOPE))
                .modelName(llmProperties.getModelName())
                .temperature(DEFAULT_TEMPERATURE)
                .timeout(Duration.ofSeconds(llmProperties.getTimeoutSeconds()))
                .maxRetries(0)
                .build();
        ChatRequest.Builder builder = ChatRequest.builder();
        if (StringUtils.hasText(request.getSystemPrompt())) {
            builder.messages(SystemMessage.from(request.getSystemPrompt()), UserMessage.from(request.getUserPrompt()));
        } else {
            builder.messages(UserMessage.from(request.getUserPrompt()));
        }
        if (request.getTemperature() != null) {
            builder.temperature(request.getTemperature());
        }

        long start = System.currentTimeMillis();
        ChatResponse response;
        try {
            response = chatModel.chat(builder.build());
        } catch (RuntimeException e) {
            throw new AiClientException("LLM_ERROR", "调用千问失败，请检查个人百炼密钥、模型权限、额度或网络");
        }
        long elapsed = System.currentTimeMillis() - start;

        TokenUsage usage = response.tokenUsage();
        int in = usage != null && usage.inputTokenCount() != null ? usage.inputTokenCount() : 0;
        int out = usage != null && usage.outputTokenCount() != null ? usage.outputTokenCount() : 0;
        log.info("千问调用完成 model={} {}ms tokens in={} out={}", modelName(), elapsed, in, out);

        return LlmChatResponse.builder()
                .provider(provider())
                .modelName(modelName())
                .content(response.aiMessage().text())
                .inputTokens(in)
                .outputTokens(out)
                .elapsedMs(elapsed)
                .build();
    }
}
