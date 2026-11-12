package com.sun.aivideo.dao.bo;

import lombok.Data;

/** 用户概况：总数、有效会员数、今日新增、已禁用数 */
@Data
public class UserOverviewBO {

    private Long totalUsers;
    private Long vipUsers;
    private Long todayNewUsers;
    private Long disabledUsers;
}
