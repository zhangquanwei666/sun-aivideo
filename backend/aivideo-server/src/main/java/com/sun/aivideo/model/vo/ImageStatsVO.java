package com.sun.aivideo.model.vo;

import com.sun.aivideo.common.base.BaseResponse;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.math.BigDecimal;
import java.util.List;

/**
 * 出参：文生图的费用与耗时汇总（人民币，包含已删除的任务）。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class ImageStatsVO extends BaseResponse {

    private Long totalCount;
    private Long succeededCount;
    private Long failedCount;
    private Long inProgressCount;
    private Long totalImages;
    private BigDecimal totalCost;
    private BigDecimal totalImageCost;
    private BigDecimal totalLlmCost;
    private BigDecimal avgCost;
    private Long avgElapsedMs;
    private List<ModelStat> byModel;

    @Data
    public static class ModelStat {
        private String modelCode;
        private String modelName;
        private Long totalCount;
        private Long succeededCount;
        private Long totalImages;
        private BigDecimal totalCost;
        private Long avgElapsedMs;
    }
}
