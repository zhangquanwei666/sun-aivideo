package com.sun.aivideo.dao.bo;

import lombok.Data;

import java.time.LocalDateTime;

/** 会员变更记录 + 用户登录名 / 昵称 */
@Data
public class MemberRecordBO {

    private Long id;
    private Long userId;
    private String username;
    private String nickname;
    private String action;
    private String memberLevel;
    private Integer days;
    private LocalDateTime expireBefore;
    private LocalDateTime expireAfter;
    private Long operatorId;
    private String operatorName;
    private String remark;
    private LocalDateTime createdAt;
}
