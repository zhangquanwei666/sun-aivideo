package com.sun.aivideo.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** 文生图模型配置与单价（t_image_model_config），按张计价 */
@Data
@TableName("t_image_model_config")
public class ImageModelConfig {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String modelCode;
    private String displayName;
    /** 厂商：DASHSCOPE / GEMINI */
    private String provider;
    /** 支持的清晰度，逗号分隔，如 1K,2K,4K */
    private String sizes;
    /** 支持的画幅比例，逗号分隔，如 1:1,16:9,9:16 */
    private String ratios;
    /** 最多可上传的参考图张数，0 表示不支持 */
    private Integer maxReferenceImages;
    /** 计价币种：CNY / USD */
    private String currency;
    /** 每张单价（按 currency 计价），为空表示不支持该清晰度 */
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
