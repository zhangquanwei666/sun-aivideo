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
    /** 当前用户是否已配置并启用此厂商 Key；不保证权限和余额 */
    private Boolean available;
    /** 平台是否已接入，与用户是否已配置凭证分开。 */
    private Boolean supported;
    /** 兼容字段：个人密钥模式不自动回退，始终为 false */
    private Boolean mockFallback;
    /** 该厂商下已启用的模型数量 */
    private Integer modelCount;
}
