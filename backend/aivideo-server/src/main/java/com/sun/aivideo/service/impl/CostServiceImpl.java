package com.sun.aivideo.service.impl;

import com.sun.aivideo.ai.config.LlmProperties;
import com.sun.aivideo.entity.ModelConfig;
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

    private final LlmProperties llm;

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
        BigDecimal cost = price.multiply(BigDecimal.valueOf(billedSeconds)).setScale(4, RoundingMode.HALF_UP);
        return new VideoCostBO(price, billedSeconds, cost);
    }

    @Override
    public BigDecimal calcLlmCost(int inputTokens, int outputTokens) {
        BigDecimal in = llm.getInputPricePerMillion().multiply(BigDecimal.valueOf(inputTokens));
        BigDecimal out = llm.getOutputPricePerMillion().multiply(BigDecimal.valueOf(outputTokens));
        return in.add(out).divide(MILLION, 6, RoundingMode.HALF_UP);
    }
}
