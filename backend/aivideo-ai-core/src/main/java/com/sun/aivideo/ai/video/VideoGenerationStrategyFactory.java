package com.sun.aivideo.ai.video;

import com.sun.aivideo.ai.config.MockVideoProperties;
import com.sun.aivideo.ai.exception.AiClientException;
import com.sun.aivideo.ai.video.strategy.VideoGenerationStrategy;
import com.sun.aivideo.common.enums.VideoProvider;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * 视频生成策略工厂：启动时收集所有 {@link VideoGenerationStrategy} 实现，按厂商取用。
 */
@Slf4j
@Component
public class VideoGenerationStrategyFactory {

    private final Map<VideoProvider, VideoGenerationStrategy> strategies = new EnumMap<>(VideoProvider.class);
    private final MockVideoProperties mockProperties;

    public VideoGenerationStrategyFactory(List<VideoGenerationStrategy> strategyList, MockVideoProperties mockProperties) {
        for (VideoGenerationStrategy s : strategyList) {
            VideoGenerationStrategy old = strategies.put(s.provider(), s);
            if (old != null) {
                throw new IllegalStateException("视频厂商 " + s.provider() + " 存在多个策略实现："
                        + old.getClass().getSimpleName() + "、" + s.getClass().getSimpleName());
            }
        }
        this.mockProperties = mockProperties;
        log.info("已注册视频生成策略：{}", strategies.keySet());
    }

    /** 厂商是否已经实现了策略（未实现的厂商不在前端展示） */
    public boolean isSupported(String providerName) {
        VideoProvider provider = VideoProvider.of(providerName);
        return provider != null && provider != VideoProvider.MOCK && strategies.containsKey(provider);
    }


    /** 是否显式开启开发 Mock 模式 */
    public boolean isMockMode() {
        return mockProperties.isEnabled() && strategies.containsKey(VideoProvider.MOCK);
    }

    /** 按任务记录的厂商名获取（轮询时使用） */
    public VideoGenerationStrategy getStrategy(String providerName) {
        VideoProvider provider = VideoProvider.of(providerName);
        if (provider == null) {
            throw new AiClientException("UNSUPPORTED_PROVIDER", "不支持的视频厂商：" + providerName);
        }
        return getStrategy(provider);
    }

    public VideoGenerationStrategy getStrategy(VideoProvider provider) {
        VideoGenerationStrategy strategy = strategies.get(provider);
        if (strategy == null) {
            throw new AiClientException("UNSUPPORTED_PROVIDER", "视频厂商未实现：" + provider.getLabel());
        }
        return strategy;
    }
}
