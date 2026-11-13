package com.sun.aivideo.ai.video.model;

import com.sun.aivideo.common.enums.ApiProtocol;
import lombok.Builder;
import lombok.Data;

/**
 * 视频生成请求（已完成参数校验、提示词润色、图片解析）。
 */
@Data
@Builder
public class VideoGenerateRequest {

    /** 业务任务 ID，仅用于日志 */
    private Long bizTaskId;
    private String modelCode;
    private ApiProtocol protocol;
    /** 模型是否支持 audio 参数 */
    private boolean supportsAudio;

    /** 最终提交的提示词 */
    private String prompt;
    private String negativePrompt;
    /** 首帧图片：公网地址或 data:image/...;base64,... */
    private String imageSource;
    private String resolution;
    private String ratio;
    private Integer duration;
    private Boolean promptExtend;
    private Boolean watermark;
    private Boolean audio;
    private Long seed;
}
