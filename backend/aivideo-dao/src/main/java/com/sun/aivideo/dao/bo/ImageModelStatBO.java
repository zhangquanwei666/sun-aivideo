package com.sun.aivideo.dao.bo;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 文生图按模型分组的统计结果。
 */
@Data
public class ImageModelStatBO {

    private String modelCode;
    private Long totalCount;
    private Long succeededCount;
    private Long totalImages;
    private BigDecimal totalCost;
    private BigDecimal avgElapsedMs;
}
