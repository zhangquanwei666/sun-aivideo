package com.sun.aivideo.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/** 会员变更记录（t_member_record）：每次开通、续期、取消会员都记一条，便于追溯 */
@Data
@TableName("t_member_record")
public class MemberRecord {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    /** OPEN 开通 / 续期，REVOKE 取消 */
    private String action;
    /** 变更后的会员等级 */
    private String memberLevel;
    /** 本次开通 / 续期的天数；为空表示永久 */
    private Integer days;
    /** 变更前的到期时间 */
    private LocalDateTime expireBefore;
    /** 变更后的到期时间；为空表示永久（或已取消） */
    private LocalDateTime expireAfter;
    /** 操作的管理员 */
    private Long operatorId;
    private String operatorName;
    private String remark;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
