package com.sun.aivideo.service;

import com.sun.aivideo.entity.ModelConfig;
import com.sun.aivideo.model.vo.ModelListVO;

public interface ModelConfigService {

    /**
     * 前端可选的厂商与模型：只包含已启用、且厂商已实现调用策略的模型；
     * 厂商按模型配置的顺序排列，并带上是否已配置 Key。
     */
    ModelListVO listForSelection();

    /** 按编码获取已启用的模型，不存在则抛业务异常 */
    ModelConfig getEnabledByCode(String modelCode);

    /** 按编码获取模型（含已停用），不存在返回 null */
    ModelConfig findByCode(String modelCode);
}
