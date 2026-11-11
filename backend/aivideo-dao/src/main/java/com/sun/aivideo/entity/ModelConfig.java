package com.sun.aivideo.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** 视频模型配置与单价（t_model_config） */
@Data
@TableName("t_model_config")
public class ModelConfig {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String modelCode;
    private String displayName;
    /** 厂商：DASHSCOPE / MOCK */
    private String provider;
    /** 支持的生成方式：T2V / I2V，同时支持时逗号分隔，如 T2V,I2V */
    private String mode;
    /** 请求协议：WAN27_T2V / WAN27_I2V / WAN_LEGACY_I2V */
    private String protocol;
    /** 逗号分隔，如 720P,1080P */
    private String resolutions;
    /** 逗号分隔，如 16:9,9:16 */
    private String ratios;
    private Integer minDuration;
    private Integer maxDuration;
    private Integer defaultDuration;
    /** 单价的计价币种：CNY / USD */
    private String currency;
    /** 每秒单价（按 currency 计价），为空表示不支持该分辨率 */
    @TableField("price_480p")
    private BigDecimal price480p;
    @TableField("price_720p")
    private BigDecimal price720p;
    @TableField("price_1080p")
    private BigDecimal price1080p;
    private Boolean supportsAudio;
    private String description;
    private String priceNote;
    private Boolean enabled;
    private Integer sortOrder;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
