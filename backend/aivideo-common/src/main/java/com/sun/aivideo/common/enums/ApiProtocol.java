package com.sun.aivideo.common.enums;

/**
 * 视频模型的请求协议：不同厂商、不同版本的请求体格式不同。
 */
public enum ApiProtocol {
    /** 百炼 wan2.7 文生视频：input.prompt + parameters.resolution/ratio/duration */
    WAN27_T2V,
    /** 百炼 wan2.7 图生视频：input.media[{type:first_frame,url}] */
    WAN27_I2V,
    /** 百炼 wan2.6 及以前的图生视频：input.img_url */
    WAN_LEGACY_I2V,
    /** 百炼 wan2.6 及以前的文生视频（如 wan2.2-t2v-plus）：parameters.size 填具体像素（如 832*480），不传 resolution/ratio */
    WAN_LEGACY_T2V,
    /** Google Gemini Omni Flash（Interactions API）：input[] + response_format，文生视频与首帧图生视频共用 */
    GEMINI_OMNI
}
