package com.sun.aivideo.service.impl;

import com.sun.aivideo.common.enums.ErrorCode;
import com.sun.aivideo.ai.image.ImageGenerationStrategyFactory;
import com.sun.aivideo.common.enums.ImageProvider;
import com.sun.aivideo.common.exception.BizException;
import com.sun.aivideo.converter.ImageModelConfigConverter;
import com.sun.aivideo.dao.ImageModelConfigMapper;
import com.sun.aivideo.entity.ImageModelConfig;
import com.sun.aivideo.model.vo.ImageModelConfigVO;
import com.sun.aivideo.model.vo.ImageModelListVO;
import com.sun.aivideo.model.vo.ProviderVO;
import com.sun.aivideo.service.CostService;
import com.sun.aivideo.service.ImageModelConfigService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class ImageModelConfigServiceImpl implements ImageModelConfigService {

    private final ImageModelConfigMapper imageModelConfigMapper;
    private final ImageGenerationStrategyFactory imageStrategyFactory;
    private final CostService costService;

    @Override
    public ImageModelListVO listForSelection() {
        List<ImageModelConfigVO> models = imageModelConfigMapper.selectEnabledList().stream()
                .filter(m -> {
                    boolean supported = imageStrategyFactory.isSupported(m.getProvider());
                    if (!supported) {
                        log.warn("文生图模型 {} 的厂商 {} 尚未实现调用策略，已从可选列表中隐藏", m.getModelCode(), m.getProvider());
                    }
                    return supported;
                })
                .map(m -> ImageModelConfigConverter.toVO(m, costService::toCny))
                .toList();

        Map<String, ProviderVO> providers = new LinkedHashMap<>();
        for (ImageModelConfigVO m : models) {
            ProviderVO p = providers.computeIfAbsent(m.getProvider(), code -> {
                ProviderVO vo = new ProviderVO();
                vo.setCode(code);
                vo.setName(ImageProvider.labelOf(code));
                vo.setAvailable(imageStrategyFactory.isAvailable(code));
                vo.setMockFallback(imageStrategyFactory.allowMockFallback());
                vo.setModelCount(0);
                return vo;
            });
            p.setModelCount(p.getModelCount() + 1);
        }
        return new ImageModelListVO(List.copyOf(providers.values()), models);
    }

    @Override
    public ImageModelConfig getEnabledByCode(String modelCode) {
        ImageModelConfig model = findByCode(modelCode);
        if (model == null || !Boolean.TRUE.equals(model.getEnabled())) {
            throw new BizException(ErrorCode.IMAGE_MODEL_NOT_FOUND, modelCode);
        }
        return model;
    }

    @Override
    public ImageModelConfig findByCode(String modelCode) {
        return imageModelConfigMapper.selectByModelCode(modelCode);
    }
}
