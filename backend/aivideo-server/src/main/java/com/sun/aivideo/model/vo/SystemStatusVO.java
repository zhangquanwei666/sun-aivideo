package com.sun.aivideo.model.vo;

import com.sun.aivideo.common.base.BaseResponse;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class SystemStatusVO extends BaseResponse {

    /** 当前环境：uat / pro */
    private String env;

    /** 是否显式开启开发 Mock；不包含个人凭证状态 */
    private Boolean mockMode;
    /** 千问润色是否可用 */
    private Boolean llmAvailable;
    private String llmModel;
    private Long pollingIntervalMs;
}
