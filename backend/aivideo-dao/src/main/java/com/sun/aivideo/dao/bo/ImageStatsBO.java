package com.sun.aivideo.dao.bo;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 文生图汇总统计的原始结果。
 */
@Data
public class ImageStatsBO {

    private Long totalCount;
    private Long succeededCount;
    private Long failedCount;
    private Long inProgressCount;
    /** 成功生成的图片张数 */
    private Long totalImages;
    private BigDecimal totalCost;
    private BigDecimal totalImageCost;
    private BigDecimal totalLlmCost;
    private BigDecimal avgCost;
    private BigDecimal avgElapsedMs;
}
