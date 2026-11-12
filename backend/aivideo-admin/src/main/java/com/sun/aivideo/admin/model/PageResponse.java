package com.sun.aivideo.admin.model;

import com.sun.aivideo.common.base.BaseResponse;
import lombok.Getter;

import java.util.List;

@Getter
public class PageResponse<T> extends BaseResponse {

    private final long total;
    private final long pageNum;
    private final long pageSize;
    private final List<T> records;

    public PageResponse(long total, long pageNum, long pageSize, List<T> records) {
        this.total = total;
        this.pageNum = pageNum;
        this.pageSize = pageSize;
        this.records = records;
    }

    public static <T> PageResponse<T> of(long total, long pageNum, long pageSize, List<T> records) {
        return BaseResponse.ok(new PageResponse<>(total, pageNum, pageSize, records));
    }
}
