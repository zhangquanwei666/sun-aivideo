package com.sun.aivideo.model.vo;

import com.sun.aivideo.common.base.BaseResponse;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class SystemStatusVO extends BaseResponse {

    /** 当前环境：local / uat / pro */
    private String env;

    /** 未配置 API Key，视频生成走 Mock */
    private Boolean mockMode;
    /** 千问润色是否可用 */
    private Boolean llmAvailable;
    private String llmModel;
    private Long pollingIntervalMs;
}
