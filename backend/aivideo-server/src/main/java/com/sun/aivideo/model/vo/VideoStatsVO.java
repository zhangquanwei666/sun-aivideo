package com.sun.aivideo.model.vo;

import com.sun.aivideo.common.base.BaseResponse;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.math.BigDecimal;
import java.util.List;

/**
 * 出参：视频生成的费用与耗时汇总。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class VideoStatsVO extends BaseResponse {

    private Long totalCount;
    private Long succeededCount;
    private Long failedCount;
    private Long inProgressCount;
    private BigDecimal totalCost;
    private BigDecimal totalVideoCost;
    private BigDecimal totalLlmCost;
    private Long totalVideoSeconds;
    /** 成功视频的平均费用 */
    private BigDecimal avgCost;
    /** 成功视频的平均总耗时（毫秒） */
    private Long avgElapsedMs;
    private List<ModelStat> byModel;

    @Data
    public static class ModelStat {
        private String modelCode;
        private String modelName;
        private Long totalCount;
        private Long succeededCount;
        private BigDecimal totalCost;
        private Long totalVideoSeconds;
        private Long avgElapsedMs;
    }
}
