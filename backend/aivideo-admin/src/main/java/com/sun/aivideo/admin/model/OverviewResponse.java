package com.sun.aivideo.admin.model;

import com.sun.aivideo.common.base.BaseResponse;
import com.sun.aivideo.dao.bo.UserOverviewBO;
import lombok.Getter;

@Getter
public class OverviewResponse extends BaseResponse {

    private final long totalUsers;
    private final long todayNewUsers;
    private final long disabledUsers;

    private OverviewResponse(UserOverviewBO source) {
        this.totalUsers = value(source.getTotalUsers());
        this.todayNewUsers = value(source.getTodayNewUsers());
        this.disabledUsers = value(source.getDisabledUsers());
    }

    public static OverviewResponse of(UserOverviewBO source) {
        return BaseResponse.ok(new OverviewResponse(source));
    }

    private static long value(Long value) {
        return value == null ? 0 : value;
    }
}
