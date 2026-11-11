package com.sun.aivideo.service;

import com.sun.aivideo.entity.ModelConfig;
import com.sun.aivideo.model.bo.VideoCostBO;

import java.math.BigDecimal;

/**
 * 费用计算。视频：单价(元/秒) × 计费秒数；提示词润色：tokens × 单价。
 */
public interface CostService {

    /** 模型在某分辨率下的单价（元/秒），未配置返回 null */
    BigDecimal unitPrice(ModelConfig model, String resolution);

    VideoCostBO calcVideoCost(ModelConfig model, String resolution, int billedSeconds);

    BigDecimal calcLlmCost(int inputTokens, int outputTokens);
}
