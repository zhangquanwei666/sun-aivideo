package com.sun.aivideo.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** 图片与视频模型配置及单价（t_model_config）。 */
@Data
@TableName("t_model_config")
public class ModelConfig {

    @TableId(type = IdType.AUTO)
    private Long id;
    private ModelType modelType;
    private String modelCode;
    private String displayName;
    private String provider;
    /** 支持的生成方式：T2V / I2V，同时支持时逗号分隔，如 T2V,I2V */
    private String mode;
    /** 请求协议：WAN27_T2V / WAN27_I2V / WAN_LEGACY_I2V */
    private String protocol;
    /** 逗号分隔，如 720P,1080P */
    private String resolutions;
    /** 逗号分隔，如 16:9,9:16，图片和视频共用。 */
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
    /** 图片清晰度，逗号分隔，如 1K,2K,4K。 */
    private String sizes;
    private Integer maxReferenceImages;
    /** 图片每张单价（按 currency 计价）。 */
    @TableField("price_1k")
    private BigDecimal price1k;
    @TableField("price_2k")
    private BigDecimal price2k;
    @TableField("price_4k")
    private BigDecimal price4k;
    private String description;
    private String priceNote;
    private Boolean enabled;
    private Integer sortOrder;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
