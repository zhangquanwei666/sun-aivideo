package com.sun.aivideo.dao.bo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** 管理后台用户列表的一行：用户信息 + 使用量汇总 */
@Data
public class UserListBO {

    private Long id;
    private String username;
    private String nickname;
    private String status;
    private String registerIp;
    private LocalDateTime lastLoginAt;
    private String lastLoginIp;
    private LocalDateTime createdAt;

    /** 视频任务数（含已删除） */
    private Long videoCount;
    /** 图片任务数（含已删除） */
    private Long imageCount;
    /** 累计费用（元）= 视频总费用 + 图片总费用 */
    private BigDecimal totalCost;
}
