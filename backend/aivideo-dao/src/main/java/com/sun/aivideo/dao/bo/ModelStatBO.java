package com.sun.aivideo.dao.bo;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 业务对象：按模型分组的统计结果。
 */
@Data
public class ModelStatBO {

    private String modelCode;
    private Long totalCount;
    private Long succeededCount;
    private BigDecimal totalCost;
    private Long totalVideoSeconds;
    private BigDecimal avgElapsedMs;
}
