package com.sun.aivideo.model.vo;

import com.sun.aivideo.common.base.BaseResponse;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 出参：视频任务详情（含耗时与费用）。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class VideoTaskVO extends BaseResponse {

    private Long id;
    /** 实际调用的厂商编码（显式开发模拟时为 MOCK） */
    private String provider;
    /** 实际调用的厂商展示名称 */
    private String providerName;
    /** 是否为 Mock 结果（显式开发模拟模式） */
    private Boolean mock;
    private String modelCode;
    private String modelName;
    private String mode;
    private String status;
    private String statusText;
    private Boolean finished;

    private String prompt;
    private String finalPrompt;
    private String negativePrompt;
    private String imageUrl;
    private String resolution;
    private String ratio;
    private Integer duration;
    private Boolean enhancePrompt;
    private String enhanceWarning;
    private Boolean promptExtend;
    private Long seed;

    /** 优先返回本地地址，没有则返回厂商地址 */
    private String videoUrl;
    private String remoteVideoUrl;
    private String errorCode;
    private String errorMessage;

    private CostVO cost;
    private TimingVO timing;

    private LocalDateTime createdAt;
    private LocalDateTime finishedAt;

    @Data
    public static class CostVO {
        /** 计费时长（秒） */
        private Integer billedDuration;
        /** 单价币种：CNY / USD（videoCost、totalCost 始终为人民币） */
        private String currency;
        /** 单价（每秒，币种见 currency） */
        private BigDecimal unitPrice;
        private BigDecimal videoCost;
        private Integer llmInputTokens;
        private Integer llmOutputTokens;
        private BigDecimal llmCost;
        private BigDecimal totalCost;
        /** 未完成时给出的预估费用 */
        private BigDecimal estimatedCost;
    }

    @Data
    public static class TimingVO {
        private Long enhanceMs;
        private Long queueMs;
        private Long generateMs;
        /** 已完成：总耗时；未完成：已等待时长 */
        private Long totalElapsedMs;
    }
}
