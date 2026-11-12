package com.sun.aivideo.model.vo;

import com.sun.aivideo.common.base.BaseResponse;
import lombok.Data;
import lombok.EqualsAndHashCode;

/** 注册或登录成功后返回的用户会话。 */
@Data
@EqualsAndHashCode(callSuper = true)
public class AuthVO extends BaseResponse {
    private String token;
    private String username;
}
