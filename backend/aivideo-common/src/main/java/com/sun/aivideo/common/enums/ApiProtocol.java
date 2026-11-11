package com.sun.aivideo.common.enums;

/**
 * 百炼万相不同版本的请求体格式不同。
 */
public enum ApiProtocol {
    /** wan2.7 文生视频：input.prompt + parameters.resolution/ratio/duration */
    WAN27_T2V,
    /** wan2.7 图生视频：input.media[{type:first_frame,url}] */
    WAN27_I2V,
    /** wan2.6 及以前的图生视频：input.img_url */
    WAN_LEGACY_I2V
}
