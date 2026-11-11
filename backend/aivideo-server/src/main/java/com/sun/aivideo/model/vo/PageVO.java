package com.sun.aivideo.model.vo;

import com.sun.aivideo.common.base.BaseResponse;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.util.List;

/**
 * 出参：分页结果。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class PageVO<T> extends BaseResponse {

    private long total;
    private long pageNum;
    private long pageSize;
    private List<T> records;
}
