package com.sun.aivideo.model.vo;

import com.sun.aivideo.common.base.BaseResponse;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 出参：文生图任务详情（含耗时与费用）。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class ImageTaskVO extends BaseResponse {

    private Long id;
    /** 实际调用的厂商编码（显式开发模拟时为 MOCK） */
    private String provider;
    private String providerName;
    private Boolean mock;
    private String modelCode;
    private String modelName;
    private String status;
    private String statusText;
    private Boolean finished;

    private String prompt;
    private String finalPrompt;
    private Boolean enhancePrompt;
    private String enhanceWarning;
    private List<String> referenceImages;
    private String aspectRatio;
    private String imageSize;
    private Integer imageCount;

    /** 生成的图片（本地访问路径） */
    private List<String> imageUrls;
    private String errorCode;
    private String errorMessage;

    private CostVO cost;
    private TimingVO timing;

    private LocalDateTime createdAt;
    private LocalDateTime finishedAt;

    @Data
    public static class CostVO {
        /** 单价的计价币种：CNY / USD */
        private String currency;
        /** 每张单价（原币种） */
        private BigDecimal unitPrice;
        /** 每张单价（人民币） */
        private BigDecimal unitPriceCny;
        /** 图片费用（人民币） */
        private BigDecimal imageCost;
        private Integer llmInputTokens;
        private Integer llmOutputTokens;
        private BigDecimal llmCost;
        /** 总费用（人民币） */
        private BigDecimal totalCost;
        /** 未完成时的预估费用（人民币） */
        private BigDecimal estimatedCost;
    }

    @Data
    public static class TimingVO {
        private Long enhanceMs;
        private Long generateMs;
        /** 已完成：总耗时；未完成：已等待时长 */
        private Long totalElapsedMs;
    }
}
