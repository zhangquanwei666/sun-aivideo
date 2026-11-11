package com.sun.aivideo.converter;

import com.sun.aivideo.entity.ModelConfig;
import com.sun.aivideo.model.vo.ModelConfigVO;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class ModelConfigConverter {

    private ModelConfigConverter() {
    }

    public static ModelConfigVO toVO(ModelConfig m) {
        ModelConfigVO vo = new ModelConfigVO();
        vo.setModelCode(m.getModelCode());
        vo.setDisplayName(m.getDisplayName());
        vo.setProvider(m.getProvider());
        vo.setMode(m.getMode());
        vo.setResolutions(split(m.getResolutions()));
        vo.setRatios(split(m.getRatios()));
        vo.setMinDuration(m.getMinDuration());
        vo.setMaxDuration(m.getMaxDuration());
        vo.setDefaultDuration(m.getDefaultDuration());
        Map<String, BigDecimal> prices = new LinkedHashMap<>();
        putIfPresent(prices, "480P", m.getPrice480p());
        putIfPresent(prices, "720P", m.getPrice720p());
        putIfPresent(prices, "1080P", m.getPrice1080p());
        vo.setPrices(prices);
        vo.setSupportsAudio(Boolean.TRUE.equals(m.getSupportsAudio()));
        vo.setDescription(m.getDescription());
        vo.setPriceNote(m.getPriceNote());
        return vo;
    }

    public static List<String> split(String csv) {
        if (!StringUtils.hasText(csv)) {
            return List.of();
        }
        return Arrays.stream(csv.split(",")).map(String::trim).filter(StringUtils::hasText).toList();
    }

    private static void putIfPresent(Map<String, BigDecimal> map, String key, BigDecimal value) {
        if (value != null) {
            map.put(key, value);
        }
    }
}
