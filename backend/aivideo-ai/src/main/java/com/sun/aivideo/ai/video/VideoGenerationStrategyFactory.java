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

    /**
     * 新任务实际使用的策略：
     * <ol>
     *     <li>所选厂商可以真实调用（已配置 Key）→ 该厂商</li>
     *     <li>不可用但允许 Mock（aivideo.mock.when-no-key=true）→ Mock</li>
     *     <li>否则报错</li>
     * </ol>
     *
     * @param providerName 模型配置中的厂商名，如 DASHSCOPE
     */
    public VideoGenerationStrategy resolveForNewTask(String providerName) {
        VideoGenerationStrategy strategy = getStrategy(providerName);
        if (strategy.available()) {
            return strategy;
        }
        if (allowMockFallback()) {
            return getStrategy(VideoProvider.MOCK);
        }
        throw new AiClientException("PROVIDER_NOT_CONFIGURED",
                strategy.provider().getLabel() + " 未配置 API Key，当前环境不允许使用 Mock");
    }

    /** 厂商是否已经实现了策略（未实现的厂商不在前端展示） */
    public boolean isSupported(String providerName) {
        VideoProvider provider = VideoProvider.of(providerName);
        return provider != null && provider != VideoProvider.MOCK && strategies.containsKey(provider);
    }

    /** 厂商当前能否真实调用 */
    public boolean isAvailable(String providerName) {
        return isSupported(providerName) && getStrategy(providerName).available();
    }

    /** 厂商不可用时是否会退回 Mock */
    public boolean allowMockFallback() {
        return mockProperties.isWhenNoKey() && strategies.containsKey(VideoProvider.MOCK);
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
