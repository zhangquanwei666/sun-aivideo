package com.sun.aivideo.service.impl;

import com.sun.aivideo.ai.video.VideoGenerationStrategyFactory;
import com.sun.aivideo.common.enums.VideoProvider;
import com.sun.aivideo.common.exception.BizException;
import com.sun.aivideo.converter.ModelConfigConverter;
import com.sun.aivideo.dao.ModelConfigMapper;
import com.sun.aivideo.entity.ModelConfig;
import com.sun.aivideo.model.vo.ModelConfigVO;
import com.sun.aivideo.model.vo.ModelListVO;
import com.sun.aivideo.model.vo.ProviderVO;
import com.sun.aivideo.service.ModelConfigService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class ModelConfigServiceImpl implements ModelConfigService {

    private final ModelConfigMapper modelConfigMapper;
    private final VideoGenerationStrategyFactory videoStrategyFactory;

    @Override
    public ModelListVO listForSelection() {
        List<ModelConfigVO> models = modelConfigMapper.selectEnabledList().stream()
                .filter(m -> {
                    boolean supported = videoStrategyFactory.isSupported(m.getProvider());
                    if (!supported) {
                        log.warn("模型 {} 的厂商 {} 尚未实现调用策略，已从可选列表中隐藏", m.getModelCode(), m.getProvider());
                    }
                    return supported;
                })
                .map(ModelConfigConverter::toVO)
                .toList();

        // 按模型出现顺序汇总厂商，保证排序与 sort_order 一致
        Map<String, ProviderVO> providers = new LinkedHashMap<>();
        for (ModelConfigVO m : models) {
            ProviderVO p = providers.computeIfAbsent(m.getProvider(), code -> {
                ProviderVO vo = new ProviderVO();
                vo.setCode(code);
                vo.setName(VideoProvider.labelOf(code));
                vo.setAvailable(videoStrategyFactory.isAvailable(code));
                vo.setMockFallback(videoStrategyFactory.allowMockFallback());
                vo.setModelCount(0);
                return vo;
            });
            p.setModelCount(p.getModelCount() + 1);
        }
        return new ModelListVO(List.copyOf(providers.values()), models);
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
