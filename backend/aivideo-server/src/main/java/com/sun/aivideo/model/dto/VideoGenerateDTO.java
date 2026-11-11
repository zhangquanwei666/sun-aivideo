package com.sun.aivideo.model.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import com.sun.aivideo.common.base.BaseRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

/**
 * 入参：创建视频生成任务。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class VideoGenerateDTO extends BaseRequest {

    @NotBlank(message = "请选择模型")
    private String modelCode;

    @Size(max = 5000, message = "提示词不能超过 5000 字")
    private String prompt;

    @Size(max = 500, message = "反向提示词不能超过 500 字")
    private String negativePrompt;

    /** 图生视频的首帧：公网 http(s) 地址，或上传接口返回的 /files/... 路径 */
    @Size(max = 1024, message = "图片地址过长")
    private String imageUrl;

    @NotBlank(message = "请选择分辨率")
    @Pattern(regexp = "480P|720P|1080P", message = "分辨率只支持 480P / 720P / 1080P")
    private String resolution;

    /** 画幅比例，仅文生视频有效，如 16:9 */
    private String ratio;

    @NotNull(message = "请选择时长")
    @Min(value = 1, message = "时长至少 1 秒")
    @Max(value = 30, message = "时长最多 30 秒")
    private Integer duration;

    /** 是否先用千问润色提示词 */
    private Boolean enhancePrompt = Boolean.FALSE;

    /** 是否开启百炼自带的智能改写 */
    private Boolean promptExtend = Boolean.TRUE;

    private Boolean watermark = Boolean.FALSE;

    /** 是否生成音频（仅部分模型支持） */
    private Boolean audio;

    @Min(value = 0, message = "seed 不能小于 0")
    @Max(value = 2147483647L, message = "seed 超出范围")
    private Long seed;
}
