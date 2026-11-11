package com.sun.aivideo.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** 视频生成任务，记录参数、结果、耗时和费用（t_video_task） */
@Data
@TableName("t_video_task")
public class VideoTask {

    @TableId(type = IdType.AUTO)
    private Long id;
    /** 实际调用的厂商：DASHSCOPE / MOCK */
    private String provider;
    private String modelCode;
    private String mode;
    /** SUBMITTING / PENDING / RUNNING / SUCCEEDED / FAILED / CANCELED / TIMEOUT */
    private String status;

    // ---------- 请求参数 ----------
    private String prompt;
    /** 实际提交给视频模型的提示词（经千问润色后） */
    private String finalPrompt;
    private String negativePrompt;
    private String imageUrl;
    private String resolution;
    private String ratio;
    private Integer duration;
    private Boolean enhancePrompt;
    private Boolean promptExtend;
    private Boolean watermark;
    private Boolean audio;
    private Long seed;

    // ---------- 结果 ----------
    private String providerTaskId;
    private String remoteVideoUrl;
    private String localVideoPath;
    private String errorCode;
    private String errorMessage;

    // ---------- 费用（元） ----------
    /** 计费时长（秒） */
    private Integer billedDuration;
    /** 视频单价（元/秒） */
    private BigDecimal unitPrice;
    private BigDecimal videoCost;
    private Integer llmInputTokens;
    private Integer llmOutputTokens;
    private BigDecimal llmCost;
    private BigDecimal totalCost;

    // ---------- 耗时（毫秒） ----------
    /** 千问润色提示词耗时 */
    private Long enhanceMs;
    /** 厂商排队耗时：scheduled_time - submit_time */
    private Long queueMs;
    /** 厂商生成耗时：end_time - scheduled_time */
    private Long generateMs;
    /** 从用户点击生成到视频完成的总耗时 */
    private Long totalElapsedMs;
    private LocalDateTime providerSubmitTime;
    private LocalDateTime providerScheduledTime;
    private LocalDateTime providerEndTime;
    private LocalDateTime finishedAt;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
