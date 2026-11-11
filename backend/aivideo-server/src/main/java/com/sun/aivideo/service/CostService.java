package com.sun.aivideo.service;

import com.sun.aivideo.entity.ImageModelConfig;
import com.sun.aivideo.entity.ModelConfig;
import com.sun.aivideo.model.bo.ImageCostBO;
import com.sun.aivideo.model.bo.VideoCostBO;

import java.math.BigDecimal;

/**
 * 费用计算。视频：单价(元/秒) × 计费秒数；图片：单价(每张) × 张数；提示词润色：tokens × 单价。
 * 所有费用最终统一折算为人民币。
 */
public interface CostService {

    /** 视频模型在某分辨率下的单价（元/秒），未配置返回 null */
    BigDecimal unitPrice(ModelConfig model, String resolution);

    VideoCostBO calcVideoCost(ModelConfig model, String resolution, int billedSeconds);

    BigDecimal calcLlmCost(int inputTokens, int outputTokens);

    /** 图片模型在某清晰度下的每张单价（按模型的计价币种），未配置返回 null */
    BigDecimal imageUnitPrice(ImageModelConfig model, String imageSize);

    /** 图片费用：每张单价折算成人民币 × 张数 */
    ImageCostBO calcImageCost(ImageModelConfig model, String imageSize, int imageCount);

    /** 按币种折算成人民币（USD 使用 aivideo.cost.usd-to-cny） */
    BigDecimal toCny(BigDecimal amount, String currency);
}
