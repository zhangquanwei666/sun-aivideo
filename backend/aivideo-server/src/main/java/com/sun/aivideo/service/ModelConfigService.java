package com.sun.aivideo.service;

import com.sun.aivideo.entity.ModelConfig;
import com.sun.aivideo.model.vo.ModelConfigVO;

import java.util.List;

public interface ModelConfigService {

    /** 前端可选的模型列表 */
    List<ModelConfigVO> listEnabled();

    /** 按编码获取已启用的模型，不存在则抛业务异常 */
    ModelConfig getEnabledByCode(String modelCode);

    /** 按编码获取模型（含已停用），不存在返回 null */
    ModelConfig findByCode(String modelCode);
}
