package com.sun.aivideo.ai.llm;

import com.sun.aivideo.ai.config.LlmProperties;
import com.sun.aivideo.ai.config.MockVideoProperties;
import com.sun.aivideo.ai.exception.AiClientException;
import com.sun.aivideo.ai.llm.strategy.LlmStrategy;
import com.sun.aivideo.common.enums.LlmProvider;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 大模型策略工厂：启动时收集所有 {@link LlmStrategy} 实现，按厂商取用。
 */
@Slf4j
@Component
public class LlmStrategyFactory {

    private final Map<LlmProvider, LlmStrategy> strategies = new EnumMap<>(LlmProvider.class);
    private final LlmProperties llmProperties;
    private final MockVideoProperties mockProperties;

    public LlmStrategyFactory(List<LlmStrategy> strategyList, LlmProperties llmProperties,
                              MockVideoProperties mockProperties) {
        for (LlmStrategy s : strategyList) {
            LlmStrategy old = strategies.put(s.provider(), s);
            if (old != null) {
                throw new IllegalStateException("大模型厂商 " + s.provider() + " 存在多个策略实现："
                        + old.getClass().getSimpleName() + "、" + s.getClass().getSimpleName());
            }
        }
        this.llmProperties = llmProperties;
        this.mockProperties = mockProperties;
        log.info("已注册大模型策略：{}，当前配置：{}", strategies.keySet(), llmProperties.getProvider());
    }

    public LlmStrategy getStrategy(LlmProvider provider) {
        LlmStrategy strategy = strategies.get(provider);
        if (strategy == null) {
            throw new AiClientException("UNSUPPORTED_LLM", "大模型厂商未实现：" + provider);
        }
        return strategy;
    }

    /** 只选择策略；个人凭证由上层校验并逐次传入，Mock 需显式开启。 */
    public Optional<LlmStrategy> resolveDefault() {
        if (!llmProperties.isEnabled()) {
            return Optional.empty();
        }
        if (mockProperties.isEnabled()) {
            return Optional.of(getStrategy(LlmProvider.MOCK));
        }
        LlmStrategy configured = strategies.get(llmProperties.getProvider());
        if (configured != null && llmProperties.getProvider() != LlmProvider.MOCK) {
            return Optional.of(configured);
        }

        return Optional.empty();
    }
}
