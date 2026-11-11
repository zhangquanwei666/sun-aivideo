package com.sun.aivideo.converter;

import com.sun.aivideo.entity.ModelConfig;
import com.sun.aivideo.entity.VideoTask;
import com.sun.aivideo.common.enums.VideoProvider;
import com.sun.aivideo.common.enums.TaskStatus;
import com.sun.aivideo.model.vo.VideoTaskVO;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;

public final class VideoTaskConverter {

    private VideoTaskConverter() {
    }

    /**
     * @param model         任务对应的模型配置，可为 null
     * @param estimatedCost 未完成任务的预估费用，可为 null
     */
    public static VideoTaskVO toVO(VideoTask t, ModelConfig model, BigDecimal estimatedCost) {
        TaskStatus status = TaskStatus.of(t.getStatus());
        boolean finished = status != null && status.isFinished();

        VideoTaskVO vo = new VideoTaskVO();
        vo.setId(t.getId());
        vo.setProvider(t.getProvider());
        vo.setMock(VideoProvider.MOCK.name().equals(t.getProvider()));
        vo.setModelCode(t.getModelCode());
        vo.setModelName(model != null ? model.getDisplayName() : t.getModelCode());
        vo.setMode(t.getMode());
        vo.setStatus(t.getStatus());
        vo.setStatusText(status != null ? status.getLabel() : t.getStatus());
        vo.setFinished(finished);

        vo.setPrompt(t.getPrompt());
        vo.setFinalPrompt(t.getFinalPrompt());
        vo.setNegativePrompt(t.getNegativePrompt());
        vo.setImageUrl(t.getImageUrl());
        vo.setResolution(t.getResolution());
        vo.setRatio(t.getRatio());
        vo.setDuration(t.getDuration());
        vo.setEnhancePrompt(t.getEnhancePrompt());
        vo.setPromptExtend(t.getPromptExtend());
        vo.setSeed(t.getSeed());

        vo.setVideoUrl(StringUtils.hasText(t.getLocalVideoPath()) ? t.getLocalVideoPath() : t.getRemoteVideoUrl());
        vo.setRemoteVideoUrl(t.getRemoteVideoUrl());
        vo.setErrorCode(t.getErrorCode());
        vo.setErrorMessage(t.getErrorMessage());

        VideoTaskVO.CostVO cost = new VideoTaskVO.CostVO();
        cost.setBilledDuration(t.getBilledDuration());
        cost.setUnitPrice(t.getUnitPrice());
        cost.setVideoCost(t.getVideoCost());
        cost.setLlmInputTokens(t.getLlmInputTokens());
        cost.setLlmOutputTokens(t.getLlmOutputTokens());
        cost.setLlmCost(t.getLlmCost());
        cost.setTotalCost(t.getTotalCost());
        cost.setEstimatedCost(finished ? null : estimatedCost);
        vo.setCost(cost);

        VideoTaskVO.TimingVO timing = new VideoTaskVO.TimingVO();
        timing.setEnhanceMs(t.getEnhanceMs());
        timing.setQueueMs(t.getQueueMs());
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
