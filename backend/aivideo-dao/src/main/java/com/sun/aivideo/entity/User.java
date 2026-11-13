package com.sun.aivideo.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/** 前台用户（t_user）：启用的账号配置个人厂商密钥后可生成图片和视频 */
@Data
@TableName("t_user")
public class User {

    @TableId(type = IdType.AUTO)
    private Long id;
    /** 登录名，唯一 */
    private String username;
    /** BCrypt 加密后的密码 */
    private String passwordHash;
    private String nickname;
    private String avatar;

    /** ENABLED 正常 / DISABLED 已禁用 */
    private String status;
    private String registerIp;
    private LocalDateTime lastLoginAt;
    private String lastLoginIp;

    @TableLogic(value = "0", delval = "1")
    private Boolean deleted;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
