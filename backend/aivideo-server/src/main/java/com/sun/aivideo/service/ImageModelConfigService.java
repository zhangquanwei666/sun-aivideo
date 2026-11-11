package com.sun.aivideo.service;

import com.sun.aivideo.entity.ImageModelConfig;
import com.sun.aivideo.model.vo.ImageModelListVO;

public interface ImageModelConfigService {

    /** 前端可选的文生图厂商与模型（只含已启用、且厂商已实现策略的模型） */
    ImageModelListVO listForSelection();

    /** 按编码获取已启用的模型，不存在则抛业务异常 */
    ImageModelConfig getEnabledByCode(String modelCode);

    /** 按编码获取模型（含已停用），不存在返回 null */
    ImageModelConfig findByCode(String modelCode);
}
