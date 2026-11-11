package com.sun.aivideo.model.bo;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 业务对象：文生图费用。
 */
@Data
@AllArgsConstructor
public class ImageCostBO {

    /** 单价的计价币种：CNY / USD */
    private String currency;
    /** 每张单价（按 currency） */
    private BigDecimal unitPrice;
    /** 每张单价折算成人民币 */
    private BigDecimal unitPriceCny;
    private int imageCount;
    /** 图片费用（人民币） */
    private BigDecimal costCny;
}
