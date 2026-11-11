package com.sun.aivideo.model.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * 视频模型及单价。作为 {@link ModelListVO} 的列表元素返回，本身不是独立响应，因此不继承 BaseResponse。
 */
@Data
public class ModelConfigVO {

    private String modelCode;
    private String displayName;
    private String provider;
    /** 厂商展示名称 */
    private String providerName;
    /** 原始配置值，如 T2V 或 T2V,I2V */
    private String mode;
    /** 支持的生成方式列表：T2V / I2V */
    private List<String> modes;
    private List<String> resolutions;
    private List<String> ratios;
    private Integer minDuration;
    private Integer maxDuration;
    /** 图生视频时的最长秒数（与 maxDuration 不同时才有值，如 Gemini Omni 带首帧最长 10 秒） */
    private Integer maxDurationI2v;
    private Integer defaultDuration;
    /** 计价币种：CNY / USD */
    private String currency;
    /** 分辨率 -> 每秒单价（原币种） */
    private Map<String, BigDecimal> prices;
    /** 分辨率 -> 每秒单价（折算成人民币） */
    private Map<String, BigDecimal> pricesCny;
    private Boolean supportsAudio;
    private String description;
    private String priceNote;
}
