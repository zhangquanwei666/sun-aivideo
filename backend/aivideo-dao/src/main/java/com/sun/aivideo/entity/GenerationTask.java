package com.sun.aivideo.entity;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** 图片与视频任务。持久化时使用任务类型和 ID 定位记录。 */
@Data
public class GenerationTask {
    private Long id;
    private Long userId;
    private Long credentialVersionId;
    private Long llmCredentialVersionId;
    private String provider;
    private String modelCode;
    private String mode;
    private String status;
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
    private Boolean watermark;
    private Boolean audio;
    private Long seed;
    private String providerTaskId;
    private String remoteVideoUrl;
    private String localVideoPath;
    private String errorCode;
    private String errorMessage;
    private Integer billedDuration;
    private String currency;
    private BigDecimal unitPrice;
    private BigDecimal videoCost;
    private Integer llmInputTokens;
    private Integer llmOutputTokens;
    private BigDecimal llmCost;
    private BigDecimal totalCost;
    private Long enhanceMs;
    private Long queueMs;
    private Long generateMs;
    private Long totalElapsedMs;
    private LocalDateTime providerSubmitTime;
    private LocalDateTime providerScheduledTime;
    private LocalDateTime providerEndTime;
    private LocalDateTime finishedAt;
    private Boolean deleted;
    private LocalDateTime deletedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String referenceImages;
    private String aspectRatio;
    private String imageSize;
    private Integer imageCount;
    private String imagePaths;
    private BigDecimal imageCost;
    private Integer providerInputTokens;
    private Integer providerOutputTokens;
}
