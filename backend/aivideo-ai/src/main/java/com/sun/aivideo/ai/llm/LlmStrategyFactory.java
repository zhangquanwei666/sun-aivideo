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

    /**
     * 当前应使用的策略：
     * <ol>
     *     <li>润色关闭（aivideo.llm.enabled=false）→ 无</li>
     *     <li>配置的厂商可用 → 该厂商</li>
     *     <li>不可用但允许 Mock（aivideo.mock.when-no-key=true）→ Mock</li>
     *     <li>否则 → 无</li>
     * </ol>
     */
    public Optional<LlmStrategy> resolveDefault() {
        if (!llmProperties.isEnabled()) {
            return Optional.empty();
        }
        LlmStrategy configured = strategies.get(llmProperties.getProvider());
        if (configured != null && configured.available()) {
            return Optional.of(configured);
        }
        if (mockProperties.isWhenNoKey()) {
            return Optional.of(getStrategy(LlmProvider.MOCK));
        }
        return Optional.empty();
    }
}
