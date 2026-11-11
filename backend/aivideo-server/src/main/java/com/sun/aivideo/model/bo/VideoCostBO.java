package com.sun.aivideo.model.bo;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 业务对象：视频生成费用。
 */
@Data
@AllArgsConstructor
public class VideoCostBO {

    /** 单价的计价币种：CNY / USD */
    private String currency;
    /** 每秒单价（按 currency） */
    private BigDecimal unitPrice;
    private int billedSeconds;
    /** 视频费用（人民币）= 单价折算人民币 × 计费秒数 */
    private BigDecimal cost;
}
