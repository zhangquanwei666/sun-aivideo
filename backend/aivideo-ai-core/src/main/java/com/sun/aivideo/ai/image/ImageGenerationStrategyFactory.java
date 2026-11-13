package com.sun.aivideo.ai.image;

import com.sun.aivideo.ai.config.MockVideoProperties;
import com.sun.aivideo.ai.exception.AiClientException;
import com.sun.aivideo.ai.image.strategy.ImageGenerationStrategy;
import com.sun.aivideo.common.enums.ImageProvider;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * 文生图策略工厂：启动时收集所有 {@link ImageGenerationStrategy} 实现，按厂商取用。
 */
@Slf4j
@Component
public class ImageGenerationStrategyFactory {

    private final Map<ImageProvider, ImageGenerationStrategy> strategies = new EnumMap<>(ImageProvider.class);
    private final MockVideoProperties mockProperties;

    public ImageGenerationStrategyFactory(List<ImageGenerationStrategy> strategyList, MockVideoProperties mockProperties) {
        for (ImageGenerationStrategy s : strategyList) {
            ImageGenerationStrategy old = strategies.put(s.provider(), s);
            if (old != null) {
                throw new IllegalStateException("文生图厂商 " + s.provider() + " 存在多个策略实现："
                        + old.getClass().getSimpleName() + "、" + s.getClass().getSimpleName());
            }
        }
        this.mockProperties = mockProperties;
        log.info("已注册文生图策略：{}", strategies.keySet());
    }

    /** 厂商是否已经实现了策略（未实现的厂商不在前端展示） */
    public boolean isSupported(String providerName) {
        ImageProvider provider = ImageProvider.of(providerName);
        return provider != null && provider != ImageProvider.MOCK && strategies.containsKey(provider);
    }


    /** 与视频共用显式开关：aivideo.mock.enabled */
    public boolean isMockMode() {
        return mockProperties.isEnabled() && strategies.containsKey(ImageProvider.MOCK);
    }

    public ImageGenerationStrategy getStrategy(String providerName) {
        ImageProvider provider = ImageProvider.of(providerName);
        if (provider == null) {
            throw new AiClientException("UNSUPPORTED_PROVIDER", "不支持的文生图厂商：" + providerName);
        }
        return getStrategy(provider);
    }

    public ImageGenerationStrategy getStrategy(ImageProvider provider) {
        ImageGenerationStrategy strategy = strategies.get(provider);
        if (strategy == null) {
            throw new AiClientException("UNSUPPORTED_PROVIDER", "文生图厂商未实现：" + provider.getLabel());
        }
        return strategy;
    }
}
