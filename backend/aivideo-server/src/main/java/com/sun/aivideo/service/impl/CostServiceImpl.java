package com.sun.aivideo.service.impl;

import com.sun.aivideo.ai.config.LlmProperties;
import com.sun.aivideo.config.AiVideoProperties;
import com.sun.aivideo.entity.ImageModelConfig;
import com.sun.aivideo.entity.ModelConfig;
import com.sun.aivideo.model.bo.ImageCostBO;
import com.sun.aivideo.model.bo.VideoCostBO;
import com.sun.aivideo.service.CostService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Service
@RequiredArgsConstructor
public class CostServiceImpl implements CostService {

    private static final BigDecimal MILLION = new BigDecimal("1000000");
    private static final String USD = "USD";

    private final LlmProperties llm;
    private final AiVideoProperties properties;

    @Override
    public BigDecimal unitPrice(ModelConfig model, String resolution) {
        return switch (resolution) {
            case "480P" -> model.getPrice480p();
            case "720P" -> model.getPrice720p();
            case "1080P" -> model.getPrice1080p();
            default -> null;
        };
    }

    @Override
    public VideoCostBO calcVideoCost(ModelConfig model, String resolution, int billedSeconds) {
        BigDecimal price = unitPrice(model, resolution);
        if (price == null) {
            price = BigDecimal.ZERO;
        }
        String currency = model.getCurrency() == null ? "CNY" : model.getCurrency();
        BigDecimal cost = toCny(price, currency).multiply(BigDecimal.valueOf(billedSeconds)).setScale(4, RoundingMode.HALF_UP);
        return new VideoCostBO(currency, price, billedSeconds, cost);
    }

    @Override
    public BigDecimal calcLlmCost(int inputTokens, int outputTokens) {
        BigDecimal in = llm.getInputPricePerMillion().multiply(BigDecimal.valueOf(inputTokens));
        BigDecimal out = llm.getOutputPricePerMillion().multiply(BigDecimal.valueOf(outputTokens));
        return in.add(out).divide(MILLION, 6, RoundingMode.HALF_UP);
    }

    @Override
    public BigDecimal imageUnitPrice(ImageModelConfig model, String imageSize) {
        return switch (imageSize) {
            case "1K" -> model.getPrice1k();
            case "2K" -> model.getPrice2k();
            case "4K" -> model.getPrice4k();
            default -> null;
        };
    }

    @Override
    public ImageCostBO calcImageCost(ImageModelConfig model, String imageSize, int imageCount) {
        BigDecimal price = imageUnitPrice(model, imageSize);
        if (price == null) {
            price = BigDecimal.ZERO;
        }
        BigDecimal priceCny = toCny(price, model.getCurrency());
        BigDecimal cost = priceCny.multiply(BigDecimal.valueOf(imageCount)).setScale(4, RoundingMode.HALF_UP);
        return new ImageCostBO(model.getCurrency(), price, priceCny, imageCount, cost);
    }

    @Override
    public BigDecimal toCny(BigDecimal amount, String currency) {
        if (amount == null) {
            return null;
        }
        if (USD.equalsIgnoreCase(currency)) {
            return amount.multiply(properties.getCost().getUsdToCny()).setScale(4, RoundingMode.HALF_UP);
        }
        return amount.setScale(4, RoundingMode.HALF_UP);
    }
}
