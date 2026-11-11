package com.sun.aivideo.converter;

import com.sun.aivideo.common.enums.ImageProvider;
import com.sun.aivideo.common.enums.TaskStatus;
import com.sun.aivideo.entity.ImageModelConfig;
import com.sun.aivideo.entity.ImageTask;
import com.sun.aivideo.model.vo.ImageTaskVO;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;

public final class ImageTaskConverter {

    private ImageTaskConverter() {
    }

    /**
     * @param model         任务对应的模型配置，可为 null
     * @param estimatedCost 未完成任务的预估费用（人民币），可为 null
     */
    public static ImageTaskVO toVO(ImageTask t, ImageModelConfig model, BigDecimal estimatedCost) {
        TaskStatus status = TaskStatus.of(t.getStatus());
        boolean finished = status != null && status.isFinished();

        ImageTaskVO vo = new ImageTaskVO();
        vo.setId(t.getId());
        vo.setProvider(t.getProvider());
        vo.setProviderName(ImageProvider.labelOf(t.getProvider()));
        vo.setMock(ImageProvider.MOCK.name().equals(t.getProvider()));
        vo.setModelCode(t.getModelCode());
        vo.setModelName(model != null ? model.getDisplayName() : t.getModelCode());
        vo.setStatus(t.getStatus());
        vo.setStatusText(status != null ? status.getLabel() : t.getStatus());
        vo.setFinished(finished);

        vo.setPrompt(t.getPrompt());
        vo.setFinalPrompt(t.getFinalPrompt());
        vo.setEnhancePrompt(t.getEnhancePrompt());
        vo.setReferenceImages(ModelConfigConverter.split(t.getReferenceImages()));
        vo.setAspectRatio(t.getAspectRatio());
        vo.setImageSize(t.getImageSize());
        vo.setImageCount(t.getImageCount());
        vo.setImageUrls(ModelConfigConverter.split(t.getImagePaths()));
        vo.setErrorCode(t.getErrorCode());
        vo.setErrorMessage(t.getErrorMessage());

        ImageTaskVO.CostVO cost = new ImageTaskVO.CostVO();
        cost.setCurrency(t.getCurrency());
        cost.setUnitPrice(t.getUnitPrice());
        cost.setImageCost(t.getImageCost());
        if (t.getImageCost() != null && t.getImageCount() != null && t.getImageCount() > 0) {
            cost.setUnitPriceCny(t.getImageCost().divide(BigDecimal.valueOf(t.getImageCount()), 4, java.math.RoundingMode.HALF_UP));
        }
        cost.setLlmInputTokens(t.getLlmInputTokens());
        cost.setLlmOutputTokens(t.getLlmOutputTokens());
        cost.setLlmCost(t.getLlmCost());
        cost.setTotalCost(t.getTotalCost());
        cost.setEstimatedCost(finished ? null : estimatedCost);
        vo.setCost(cost);

        ImageTaskVO.TimingVO timing = new ImageTaskVO.TimingVO();
        timing.setEnhanceMs(t.getEnhanceMs());
        timing.setGenerateMs(t.getGenerateMs());
        if (finished || t.getCreatedAt() == null) {
            timing.setTotalElapsedMs(t.getTotalElapsedMs());
        } else {
            timing.setTotalElapsedMs(Duration.between(t.getCreatedAt(), LocalDateTime.now()).toMillis());
        }
        vo.setTiming(timing);

        vo.setCreatedAt(t.getCreatedAt());
        vo.setFinishedAt(t.getFinishedAt());
        return vo;
    }
}
