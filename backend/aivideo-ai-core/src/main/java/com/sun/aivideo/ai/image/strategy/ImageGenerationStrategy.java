package com.sun.aivideo.ai.image.strategy;

import com.sun.aivideo.ai.auth.ChannelCredentials;
import com.sun.aivideo.ai.image.model.ImageGenerateRequest;
import com.sun.aivideo.ai.image.model.ImageGenerateResult;
import com.sun.aivideo.common.enums.ImageProvider;

/**
 * 文生图策略：每个厂商一个实现。
 * 图片生成都是同步调用（请求返回时图片已生成），由调用方在后台线程中执行。
 * 新增厂商：实现本接口并声明为 Spring Bean，{@link com.sun.aivideo.ai.image.ImageGenerationStrategyFactory} 会自动收录。
 */
public interface ImageGenerationStrategy {

    /** 策略对应的厂商（工厂按此注册） */
    ImageProvider provider();

    /** 生成图片（阻塞直到生成完成） */
    ImageGenerateResult generate(ImageGenerateRequest request, ChannelCredentials credentials);
}
