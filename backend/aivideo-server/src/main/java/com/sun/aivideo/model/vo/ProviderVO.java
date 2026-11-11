package com.sun.aivideo.model.vo;

import lombok.Data;

/**
 * 视频厂商。作为 {@link ModelListVO} 的列表元素返回，本身不是独立响应，因此不继承 BaseResponse。
 */
@Data
public class ProviderVO {

    /** 厂商编码，如 DASHSCOPE */
    private String code;
    /** 展示名称，如 阿里云百炼 */
    private String name;
    /** 是否可以真实调用（已配置该厂商的 API Key） */
    private Boolean available;
    /** 不可用时是否会退回 Mock（不扣费） */
    private Boolean mockFallback;
    /** 该厂商下已启用的模型数量 */
    private Integer modelCount;
}
