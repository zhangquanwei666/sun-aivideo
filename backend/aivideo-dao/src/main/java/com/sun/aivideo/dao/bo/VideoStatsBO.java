package com.sun.aivideo.dao.bo;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 业务对象：DAO 层汇总统计的原始结果。
 */
@Data
public class VideoStatsBO {

    private Long totalCount;
    private Long succeededCount;
    private Long failedCount;
    private Long inProgressCount;
    private BigDecimal totalCost;
    private BigDecimal totalVideoCost;
    private BigDecimal totalLlmCost;
    private Long totalVideoSeconds;
    private BigDecimal avgCost;
    private BigDecimal avgElapsedMs;
}
