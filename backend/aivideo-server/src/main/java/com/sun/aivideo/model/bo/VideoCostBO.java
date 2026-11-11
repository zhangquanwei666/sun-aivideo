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

    /** 元/秒 */
    private BigDecimal unitPrice;
    private int billedSeconds;
    private BigDecimal cost;
}
