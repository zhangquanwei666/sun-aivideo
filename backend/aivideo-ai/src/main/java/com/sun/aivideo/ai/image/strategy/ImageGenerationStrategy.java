package com.sun.aivideo.ai.image.strategy;

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

    /** 当前能否真实调用（通常是是否配置了 API Key）；不可用且允许 Mock 时会退回 Mock */
    default boolean available() {
        return true;
    }

    /** 生成图片（阻塞直到生成完成） */
    ImageGenerateResult generate(ImageGenerateRequest request);
}
