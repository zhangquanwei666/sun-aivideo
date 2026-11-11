package com.sun.aivideo.converter;

import com.sun.aivideo.common.enums.ApiProtocol;
import com.sun.aivideo.common.enums.VideoProvider;
import com.sun.aivideo.entity.ModelConfig;
import com.sun.aivideo.model.vo.ModelConfigVO;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;

public final class ModelConfigConverter {

    private ModelConfigConverter() {
    }

    /**
     * @param toCny 原币种金额 → 人民币（金额, 币种）
     */
    public static ModelConfigVO toVO(ModelConfig m, BiFunction<BigDecimal, String, BigDecimal> toCny) {
        String currency = StringUtils.hasText(m.getCurrency()) ? m.getCurrency() : "CNY";
        ModelConfigVO vo = new ModelConfigVO();
        vo.setModelCode(m.getModelCode());
        vo.setDisplayName(m.getDisplayName());
        vo.setProvider(m.getProvider());
        vo.setProviderName(VideoProvider.labelOf(m.getProvider()));
        vo.setMode(m.getMode());
        vo.setModes(split(m.getMode()));
        vo.setResolutions(split(m.getResolutions()));
        vo.setRatios(split(m.getRatios()));
        vo.setMinDuration(m.getMinDuration());
        vo.setMaxDuration(m.getMaxDuration());
        if (ApiProtocol.GEMINI_OMNI.name().equals(m.getProtocol()) && m.getMaxDuration() != null && m.getMaxDuration() > 10) {
            vo.setMaxDurationI2v(10);
        }
        vo.setDefaultDuration(m.getDefaultDuration());
        vo.setCurrency(currency);
        Map<String, BigDecimal> prices = new LinkedHashMap<>();
        Map<String, BigDecimal> pricesCny = new LinkedHashMap<>();
        put(prices, pricesCny, "480P", m.getPrice480p(), currency, toCny);
        put(prices, pricesCny, "720P", m.getPrice720p(), currency, toCny);
        put(prices, pricesCny, "1080P", m.getPrice1080p(), currency, toCny);
        vo.setPrices(prices);
        vo.setPricesCny(pricesCny);
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

    private static void put(Map<String, BigDecimal> prices, Map<String, BigDecimal> pricesCny, String key,
                            BigDecimal value, String currency, BiFunction<BigDecimal, String, BigDecimal> toCny) {
        if (value != null) {
            prices.put(key, value);
            pricesCny.put(key, toCny.apply(value, currency));
        }
    }
}
