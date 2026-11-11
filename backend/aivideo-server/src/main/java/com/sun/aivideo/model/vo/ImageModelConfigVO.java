package com.sun.aivideo.model.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * 文生图模型及单价。作为 {@link ImageModelListVO} 的列表元素返回，不单独返回。
 */
@Data
public class ImageModelConfigVO {

    private String modelCode;
    private String displayName;
    private String provider;
    private String providerName;
    /** 支持的清晰度：1K / 2K / 4K */
    private List<String> sizes;
    /** 支持的画幅比例 */
    private List<String> ratios;
    /** 最多参考图张数，0 表示不支持 */
    private Integer maxReferenceImages;
    /** 计价币种：CNY / USD */
    private String currency;
    /** 清晰度 -> 每张单价（原币种） */
    private Map<String, BigDecimal> prices;
    /** 清晰度 -> 每张单价（折算成人民币） */
    private Map<String, BigDecimal> pricesCny;
    private String description;
    private String priceNote;
}
