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
    private String mode;
    private List<String> resolutions;
    private List<String> ratios;
    private Integer minDuration;
    private Integer maxDuration;
    private Integer defaultDuration;
    /** 分辨率 -> 单价（元/秒） */
    private Map<String, BigDecimal> prices;
    private Boolean supportsAudio;
    private String description;
    private String priceNote;
}
