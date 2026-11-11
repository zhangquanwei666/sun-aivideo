package com.sun.aivideo.ai.video;

import com.sun.aivideo.ai.config.DashScopeProperties;
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
    private final DashScopeProperties dashScopeProperties;

    public VideoGenerationStrategyFactory(List<VideoGenerationStrategy> strategyList, DashScopeProperties dashScopeProperties) {
        for (VideoGenerationStrategy s : strategyList) {
            VideoGenerationStrategy old = strategies.put(s.provider(), s);
            if (old != null) {
                throw new IllegalStateException("视频厂商 " + s.provider() + " 存在多个策略实现："
                        + old.getClass().getSimpleName() + "、" + s.getClass().getSimpleName());
            }
        }
        this.dashScopeProperties = dashScopeProperties;
        log.info("已注册视频生成策略：{}", strategies.keySet());
    }

    /**
     * 新任务使用的策略：没有配置 API Key 且允许 Mock 时返回 Mock，否则按模型配置的厂商。
     *
     * @param configuredProvider 模型配置中的厂商名，如 DASHSCOPE
     */
    public VideoGenerationStrategy resolveForNewTask(String configuredProvider) {
        if (dashScopeProperties.isMockMode()) {
            return getStrategy(VideoProvider.MOCK);
        }
        return getStrategy(configuredProvider);
    }

    /** 按任务记录的厂商名获取（轮询时使用） */
    public VideoGenerationStrategy getStrategy(String providerName) {
        VideoProvider provider;
        try {
            provider = VideoProvider.valueOf(providerName);
        } catch (IllegalArgumentException | NullPointerException e) {
            throw new AiClientException("UNSUPPORTED_PROVIDER", "不支持的视频厂商：" + providerName);
        }
        return getStrategy(provider);
    }

    public VideoGenerationStrategy getStrategy(VideoProvider provider) {
        VideoGenerationStrategy strategy = strategies.get(provider);
        if (strategy == null) {
            throw new AiClientException("UNSUPPORTED_PROVIDER", "视频厂商未实现：" + provider);
        }
        return strategy;
    }
}
