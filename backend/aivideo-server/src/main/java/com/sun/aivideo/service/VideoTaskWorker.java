package com.sun.aivideo.service;

import com.sun.aivideo.ai.exception.AiClientException;
import com.sun.aivideo.common.enums.ApiProtocol;
import com.sun.aivideo.common.enums.VideoMode;
import com.sun.aivideo.ai.llm.PromptEnhancer;
import com.sun.aivideo.ai.llm.model.PromptEnhanceResult;
import com.sun.aivideo.ai.video.strategy.VideoGenerationStrategy;
import com.sun.aivideo.ai.video.VideoGenerationStrategyFactory;
import com.sun.aivideo.ai.video.model.VideoGenerateRequest;
import com.sun.aivideo.ai.video.model.VideoSubmitResult;
import com.sun.aivideo.ai.video.model.VideoTaskResult;
import com.sun.aivideo.config.AiVideoProperties;
import com.sun.aivideo.config.AsyncConfig;
import com.sun.aivideo.dao.VideoTaskMapper;
import com.sun.aivideo.entity.ModelConfig;
import com.sun.aivideo.entity.VideoTask;
import com.sun.aivideo.common.enums.TaskStatus;
import com.sun.aivideo.model.bo.VideoCostBO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 视频任务的后台执行者（调用 aivideo-ai 模块）：
 * <ol>
 *     <li>submitAsync：千问润色提示词 → 调用厂商创建任务（记录润色耗时与 tokens 费用）</li>
 *     <li>pollActiveTasks：由定时任务调用，轮询厂商状态；成功后计算视频费用、各阶段耗时并下载视频</li>
 * </ol>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class VideoTaskWorker {

    private final VideoTaskMapper videoTaskMapper;
    private final ModelConfigService modelConfigService;
    private final PromptEnhancer promptEnhancer;
    private final CostService costService;
    private final FileStorageService fileStorageService;
    private final VideoGenerationStrategyFactory videoStrategyFactory;
    private final AiVideoProperties properties;

    @Async(AsyncConfig.VIDEO_EXECUTOR)
    public void submitAsync(Long taskId) {
        VideoTask task = videoTaskMapper.selectById(taskId);
        if (task == null) {
            return;
        }
        VideoTask update = new VideoTask();
        update.setId(taskId);
        try {
            ModelConfig model = modelConfigService.getEnabledByCode(task.getModelCode());
            VideoMode mode = VideoMode.valueOf(task.getMode());

            String finalPrompt = enhanceIfNeeded(task, mode, update);
            update.setFinalPrompt(finalPrompt);

            VideoGenerateRequest request = VideoGenerateRequest.builder()
                    .bizTaskId(taskId)
                    .modelCode(model.getModelCode())
                    .protocol(ApiProtocol.valueOf(model.getProtocol()))
                    .supportsAudio(Boolean.TRUE.equals(model.getSupportsAudio()))
                    .prompt(finalPrompt)
                    .negativePrompt(task.getNegativePrompt())
                    .imageSource(mode == VideoMode.I2V ? fileStorageService.toProviderImageSource(task.getImageUrl()) : null)
                    .resolution(task.getResolution())
                    .ratio(task.getRatio())
                    .duration(task.getDuration())
                    .promptExtend(task.getPromptExtend())
                    .watermark(task.getWatermark())
                    .audio(task.getAudio())
                    .seed(task.getSeed())
                    .build();

            VideoGenerationStrategy strategy = videoStrategyFactory.getStrategy(task.getProvider());
            VideoSubmitResult result = strategy.submit(request);
            update.setProviderTaskId(result.getProviderTaskId());
            update.setStatus(toTaskStatus(result.getState()).name());
            videoTaskMapper.updateById(update);
        } catch (Exception e) {
            log.warn("提交视频任务失败 id={}：{}", taskId, e.getMessage());
            update.setErrorCode(e instanceof AiClientException ace ? ace.getErrorCode() : "SUBMIT_FAILED");
            finishAsFailed(task, update, TaskStatus.FAILED, e.getMessage());
        }
    }

    /** 轮询所有进行中的任务 */
    public void pollActiveTasks() {
        List<VideoTask> tasks = videoTaskMapper.selectActiveTasks(TaskStatus.POLLING);
        for (VideoTask task : tasks) {
            try {
                pollOne(task);
            } catch (Exception e) {
                log.warn("轮询任务失败 id={}：{}", task.getId(), e.getMessage());
            }
        }
    }

    // ------------------------------------------------------------------

    private String enhanceIfNeeded(VideoTask task, VideoMode mode, VideoTask update) {
        if (!Boolean.TRUE.equals(task.getEnhancePrompt())) {
            return task.getPrompt();
        }
        try {
            PromptEnhanceResult r = promptEnhancer.enhance(task.getPrompt(), mode);
            if (r.isEnhanced()) {
                BigDecimal llmCost = costService.calcLlmCost(r.getInputTokens(), r.getOutputTokens());
                update.setEnhanceMs(r.getElapsedMs());
                update.setLlmInputTokens(r.getInputTokens());
                update.setLlmOutputTokens(r.getOutputTokens());
                update.setLlmCost(llmCost);
                update.setTotalCost(llmCost.setScale(4, RoundingMode.HALF_UP));
            }
            return r.getPrompt();
        } catch (Exception e) {
            // 润色失败不影响生成，使用原始提示词
            log.warn("千问润色失败，使用原始提示词 id={}：{}", task.getId(), e.getMessage());
            return task.getPrompt();
        }
    }

    private void pollOne(VideoTask task) {
        LocalDateTime now = LocalDateTime.now();
        if (task.getCreatedAt() != null
                && task.getCreatedAt().plusMinutes(properties.getPolling().getTimeoutMinutes()).isBefore(now)) {
            VideoTask update = new VideoTask();
            update.setId(task.getId());
            update.setErrorCode("TIMEOUT");
            finishAsFailed(task, update, TaskStatus.TIMEOUT,
                    "超过 " + properties.getPolling().getTimeoutMinutes() + " 分钟未完成");
            return;
        }

        VideoTaskResult r = videoStrategyFactory.getStrategy(task.getProvider()).query(task.getProviderTaskId());
        TaskStatus status = toTaskStatus(r.getState());

        VideoTask update = new VideoTask();
        update.setId(task.getId());
        update.setProviderSubmitTime(r.getSubmitTime());
        update.setProviderScheduledTime(r.getScheduledTime());
        update.setProviderEndTime(r.getEndTime());
        update.setQueueMs(between(r.getSubmitTime(), r.getScheduledTime()));

        switch (status) {
            case PENDING, RUNNING -> {
                update.setStatus(status.name());
                videoTaskMapper.updateById(update);
            }
            case SUCCEEDED -> onSucceeded(task, update, r, now);
            default -> {
                update.setErrorCode(r.getErrorCode());
                update.setGenerateMs(between(r.getScheduledTime(), r.getEndTime()));
                finishAsFailed(task, update, status, r.getErrorMessage());
            }
        }
    }

    private void onSucceeded(VideoTask task, VideoTask update, VideoTaskResult r, LocalDateTime now) {
        ModelConfig model = modelConfigService.findByCode(task.getModelCode());
        int billedSeconds = r.getBilledDuration() != null ? r.getBilledDuration() : task.getDuration();
        VideoCostBO videoCost = costService.calcVideoCost(model, task.getResolution(), billedSeconds);
        BigDecimal llmCost = task.getLlmCost() == null ? BigDecimal.ZERO : task.getLlmCost();

        // 厂商直接返回视频内容（如 Gemini Omni）时，立即保存到本地
        if (r.getVideoData() != null) {
            update.setLocalVideoPath(fileStorageService.saveVideoBytes(task.getId(), r.getVideoData()));
        }

        update.setStatus(TaskStatus.SUCCEEDED.name());
        update.setRemoteVideoUrl(r.getVideoUrl());
        update.setBilledDuration(billedSeconds);
        update.setCurrency(videoCost.getCurrency());
        update.setUnitPrice(videoCost.getUnitPrice());
        update.setVideoCost(videoCost.getCost());
        update.setTotalCost(videoCost.getCost().add(llmCost).setScale(4, RoundingMode.HALF_UP));
        update.setGenerateMs(between(r.getScheduledTime(), r.getEndTime()));
        update.setFinishedAt(now);
        update.setTotalElapsedMs(totalElapsed(task.getCreatedAt(), r.getEndTime(), now));
        videoTaskMapper.updateById(update);
        log.info("视频生成完成 id={} 耗时={}ms 费用={}元", task.getId(), update.getTotalElapsedMs(), update.getTotalCost());

        if (properties.getStorage().isDownloadVideo() && r.getVideoData() == null) {
            fileStorageService.downloadVideoAsync(task.getId(), r.getVideoUrl()).thenAccept(path -> {
                if (path != null) {
                    VideoTask u = new VideoTask();
                    u.setId(task.getId());
                    u.setLocalVideoPath(path);
                    videoTaskMapper.updateById(u);
                }
            });
        }
    }

    /** 失败/取消/超时：视频不计费，只保留已产生的润色费用 */
    private void finishAsFailed(VideoTask task, VideoTask update, TaskStatus status, String message) {
        LocalDateTime now = LocalDateTime.now();
        BigDecimal llmCost = update.getLlmCost() != null ? update.getLlmCost()
                : (task.getLlmCost() == null ? BigDecimal.ZERO : task.getLlmCost());
        update.setStatus(status.name());
        update.setErrorMessage(message == null ? null : message.substring(0, Math.min(message.length(), 1000)));
        update.setVideoCost(BigDecimal.ZERO);
        update.setTotalCost(llmCost.setScale(4, RoundingMode.HALF_UP));
        update.setFinishedAt(now);
        update.setTotalElapsedMs(task.getCreatedAt() == null ? null : Duration.between(task.getCreatedAt(), now).toMillis());
        videoTaskMapper.updateById(update);
    }

    /** AI 模块的厂商状态 → 业务任务状态 */
    private static TaskStatus toTaskStatus(com.sun.aivideo.common.enums.VideoTaskState state) {
        return switch (state) {
            case PENDING -> TaskStatus.PENDING;
            case RUNNING -> TaskStatus.RUNNING;
            case SUCCEEDED -> TaskStatus.SUCCEEDED;
            case CANCELED -> TaskStatus.CANCELED;
            case FAILED -> TaskStatus.FAILED;
        };
    }

    /**
     * 总耗时 = 厂商完成时间 - 用户创建时间；取不到厂商时间时用当前时间（会包含轮询间隔误差）。
     */
    private static Long totalElapsed(LocalDateTime createdAt, LocalDateTime providerEnd, LocalDateTime now) {
        if (createdAt == null) {
            return null;
        }
        LocalDateTime end = providerEnd != null && providerEnd.isAfter(createdAt) ? providerEnd : now;
        return Duration.between(createdAt, end).toMillis();
    }

    private static Long between(LocalDateTime start, LocalDateTime end) {
        if (start == null || end == null) {
            return null;
        }
        return Math.max(0, Duration.between(start, end).toMillis());
    }
}
