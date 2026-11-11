package com.sun.aivideo.converter;

import com.sun.aivideo.common.enums.ImageProvider;
import com.sun.aivideo.entity.ImageModelConfig;
import com.sun.aivideo.model.vo.ImageModelConfigVO;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.BiFunction;

public final class ImageModelConfigConverter {

    private ImageModelConfigConverter() {
    }

    /**
     * @param toCny 原币种金额 → 人民币（金额, 币种）
     */
    public static ImageModelConfigVO toVO(ImageModelConfig m, BiFunction<BigDecimal, String, BigDecimal> toCny) {
        ImageModelConfigVO vo = new ImageModelConfigVO();
        vo.setModelCode(m.getModelCode());
        vo.setDisplayName(m.getDisplayName());
        vo.setProvider(m.getProvider());
        vo.setProviderName(ImageProvider.labelOf(m.getProvider()));
        vo.setSizes(ModelConfigConverter.split(m.getSizes()));
        vo.setRatios(ModelConfigConverter.split(m.getRatios()));
        vo.setMaxReferenceImages(m.getMaxReferenceImages() == null ? 0 : m.getMaxReferenceImages());
        vo.setCurrency(m.getCurrency());

        Map<String, BigDecimal> prices = new LinkedHashMap<>();
        Map<String, BigDecimal> pricesCny = new LinkedHashMap<>();
        put(prices, pricesCny, "1K", m.getPrice1k(), m.getCurrency(), toCny);
        put(prices, pricesCny, "2K", m.getPrice2k(), m.getCurrency(), toCny);
        put(prices, pricesCny, "4K", m.getPrice4k(), m.getCurrency(), toCny);
        vo.setPrices(prices);
        vo.setPricesCny(pricesCny);
        vo.setDescription(m.getDescription());
        vo.setPriceNote(m.getPriceNote());
        return vo;
    }

    private static void put(Map<String, BigDecimal> prices, Map<String, BigDecimal> pricesCny, String size,
                            BigDecimal price, String currency, BiFunction<BigDecimal, String, BigDecimal> toCny) {
        if (price != null) {
            prices.put(size, price);
            pricesCny.put(size, toCny.apply(price, currency));
        }
    }
}
