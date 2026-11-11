package com.sun.aivideo.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** 文生图任务，记录参数、结果、耗时和费用（t_image_task） */
@Data
@TableName("t_image_task")
public class ImageTask {

    @TableId(type = IdType.AUTO)
    private Long id;
    /** 实际调用的厂商：DASHSCOPE / GEMINI / MOCK */
    private String provider;
    private String modelCode;
    /** SUBMITTING / RUNNING / SUCCEEDED / FAILED */
    private String status;

    // ---------- 请求参数 ----------
    private String prompt;
    /** 实际提交给模型的提示词（润色后） */
    private String finalPrompt;
    private Boolean enhancePrompt;
    /** 参考图，逗号分隔的 /files/... 路径 */
    private String referenceImages;
    private String aspectRatio;
    private String imageSize;
    private Integer imageCount;

    // ---------- 结果 ----------
    /** 生成的图片，逗号分隔的本地访问路径 /files/generated/... */
    private String imagePaths;
    private String errorCode;
    private String errorMessage;

    // ---------- 费用 ----------
    /** 单价的计价币种：CNY / USD */
    private String currency;
    /** 每张单价（按 currency） */
    private BigDecimal unitPrice;
    /** 图片费用（已折算为人民币） */
    private BigDecimal imageCost;
    private Integer llmInputTokens;
    private Integer llmOutputTokens;
    private BigDecimal llmCost;
    /** 总费用（人民币）= 图片费用 + 润色费用 */
    private BigDecimal totalCost;
    /** 厂商返回的 tokens，仅供参考 */
    private Integer providerInputTokens;
    private Integer providerOutputTokens;

    // ---------- 耗时（毫秒） ----------
    private Long enhanceMs;
    private Long generateMs;
    private Long totalElapsedMs;
    private LocalDateTime finishedAt;

    // ---------- 逻辑删除 ----------
    @TableLogic(value = "0", delval = "1")
    private Boolean deleted;
    private LocalDateTime deletedAt;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
