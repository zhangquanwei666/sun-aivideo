package com.sun.aivideo.service.impl;

import com.sun.aivideo.common.exception.BizException;
import com.sun.aivideo.converter.ModelConfigConverter;
import com.sun.aivideo.dao.ModelConfigMapper;
import com.sun.aivideo.entity.ModelConfig;
import com.sun.aivideo.model.vo.ModelConfigVO;
import com.sun.aivideo.service.ModelConfigService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ModelConfigServiceImpl implements ModelConfigService {

    private final ModelConfigMapper modelConfigMapper;

    @Override
    public List<ModelConfigVO> listEnabled() {
        return modelConfigMapper.selectEnabledList().stream()
                .map(ModelConfigConverter::toVO)
                .toList();
    }

    @Override
    public ModelConfig getEnabledByCode(String modelCode) {
        ModelConfig model = findByCode(modelCode);
        if (model == null || !Boolean.TRUE.equals(model.getEnabled())) {
            throw new BizException("模型不存在或未启用：" + modelCode);
        }
        return model;
    }

    @Override
    public ModelConfig findByCode(String modelCode) {
        return modelConfigMapper.selectByModelCode(modelCode);
    }
}
