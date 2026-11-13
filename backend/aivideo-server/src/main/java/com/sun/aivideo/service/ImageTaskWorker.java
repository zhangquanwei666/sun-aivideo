package com.sun.aivideo.service;

import com.sun.aivideo.ai.exception.AiClientException;
import com.sun.aivideo.ai.image.ImageGenerationStrategyFactory;
import com.sun.aivideo.ai.image.model.GeneratedImage;
import com.sun.aivideo.ai.image.model.ImageGenerateRequest;
import com.sun.aivideo.ai.image.model.ImageGenerateResult;
import com.sun.aivideo.ai.llm.PromptEnhancer;
import com.sun.aivideo.ai.llm.model.PromptEnhanceResult;
import com.sun.aivideo.common.enums.TaskStatus;
import com.sun.aivideo.common.enums.VideoMode;
import com.sun.aivideo.common.exception.BizException;
import com.sun.aivideo.config.AsyncConfig;
import com.sun.aivideo.converter.ModelConfigConverter;
import com.sun.aivideo.dao.ImageTaskMapper;
import com.sun.aivideo.entity.ImageModelConfig;
import com.sun.aivideo.entity.ImageTask;
import com.sun.aivideo.model.bo.ImageCostBO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 文生图任务的后台执行者：润色提示词 → 调用厂商同步生成 → 保存图片到本地 → 计算费用和耗时。
 * 图片生成是同步调用，不需要定时轮询。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ImageTaskWorker {

    private final ImageTaskMapper imageTaskMapper;
    private final ImageModelConfigService imageModelConfigService;
    private final ImageGenerationStrategyFactory imageStrategyFactory;
    private final PromptEnhancer promptEnhancer;
    private final CostService costService;
    private final FileStorageService fileStorageService;
    private final ChannelCredentialService credentials;
    private final UserInvocationGuard invocationGuard;

    /**
     * 服务启动时，把上次运行中断的任务标记为失败。
     * 图片任务在内存线程中执行，服务重启后不会继续，不处理的话会一直显示"生成中"。
     */
    @EventListener(ApplicationReadyEvent.class)
    public void failInterruptedTasks() {
        try {
            int n = imageTaskMapper.failUnfinished("服务重启，任务中断，请重新生成");
            if (n > 0) {
                log.warn("已将 {} 个中断的图片任务标记为失败", n);
            }
        } catch (Exception e) {
            // 数据库未就绪（如未建表）时不影响启动
            log.warn("检查中断的图片任务失败：{}", e.getMessage());
        }
    }

    @Async(AsyncConfig.VIDEO_EXECUTOR)
    public void generateAsync(Long taskId) {
        ImageTask task = imageTaskMapper.selectById(taskId);
        if (task == null) {
            return;
        }
        ImageTask update = new ImageTask();
        update.setId(taskId);
        update.setStatus(TaskStatus.RUNNING.name());
        imageTaskMapper.updateById(update);

        update = new ImageTask();
        update.setId(taskId);
        try (var permit = invocationGuard.acquire(task.getUserId())) {
            ImageModelConfig model = imageModelConfigService.getEnabledByCode(task.getModelCode());
            String finalPrompt = enhanceIfNeeded(task, update);
            update.setFinalPrompt(finalPrompt);

            List<String> references = ModelConfigConverter.split(task.getReferenceImages()).stream()
                    .map(fileStorageService::toProviderImageSource)
                    .toList();
            ImageGenerateRequest request = ImageGenerateRequest.builder()
                    .bizTaskId(taskId)
                    .modelCode(model.getModelCode())
                    .prompt(finalPrompt)
                    .aspectRatio(task.getAspectRatio())
                    .imageSize(task.getImageSize())
                    .referenceImages(references)
                    .build();

            long start = System.currentTimeMillis();
            ImageGenerateResult result = imageStrategyFactory.getStrategy(task.getProvider()).generate(request, "MOCK".equals(task.getProvider()) ? null
                    : credentials.forTask(task.getUserId(), task.getProvider(), task.getCredentialVersionId()));
            update.setGenerateMs(System.currentTimeMillis() - start);

            List<String> paths = new ArrayList<>();
            for (int i = 0; i < result.getImages().size(); i++) {
                GeneratedImage image = result.getImages().get(i);
                paths.add(fileStorageService.saveGeneratedImage(taskId, i, image));
            }

            ImageCostBO cost = "MOCK".equals(task.getProvider())
                    ? new ImageCostBO("CNY", BigDecimal.ZERO, BigDecimal.ZERO, paths.size(), BigDecimal.ZERO)
                    : costService.calcImageCost(model, task.getImageSize(), paths.size());
            BigDecimal llmCost = update.getLlmCost() != null ? update.getLlmCost() : BigDecimal.ZERO;
            LocalDateTime now = LocalDateTime.now();
            update.setStatus(TaskStatus.SUCCEEDED.name());
            update.setImagePaths(String.join(",", paths));
            update.setImageCount(paths.size());
            update.setCurrency(cost.getCurrency());
            update.setUnitPrice(cost.getUnitPrice());
            update.setImageCost(cost.getCostCny());
            update.setTotalCost(cost.getCostCny().add(llmCost).setScale(4, RoundingMode.HALF_UP));
            update.setProviderInputTokens(result.getInputTokens());
            update.setProviderOutputTokens(result.getOutputTokens());
            update.setFinishedAt(now);
            update.setTotalElapsedMs(elapsed(task, now));
            imageTaskMapper.updateById(update);
            log.info("文生图完成 id={} 耗时={}ms 费用={}元", taskId, update.getTotalElapsedMs(), update.getTotalCost());
        } catch (Exception e) {
            log.warn("文生图失败 id={}：{}", taskId, e.getMessage());
            LocalDateTime now = LocalDateTime.now();
            BigDecimal llmCost = update.getLlmCost() != null ? update.getLlmCost() : BigDecimal.ZERO;
            update.setStatus(TaskStatus.FAILED.name());
            update.setErrorCode(e instanceof AiClientException ace ? ace.getErrorCode()
                    : e instanceof BizException ? "BIZ_ERROR" : e.getClass().getSimpleName());
            String message = e.getMessage() == null ? "生成失败" : e.getMessage();
            update.setErrorMessage(message.substring(0, Math.min(message.length(), 1000)));
            update.setImageCost(BigDecimal.ZERO);
            update.setTotalCost(llmCost.setScale(4, RoundingMode.HALF_UP));
            update.setFinishedAt(now);
            update.setTotalElapsedMs(elapsed(task, now));
            imageTaskMapper.updateById(update);
        }
    }

    private String enhanceIfNeeded(ImageTask task, ImageTask update) {
        if (!Boolean.TRUE.equals(task.getEnhancePrompt())) {
            return task.getPrompt();
        }
        try {
            PromptEnhanceResult r = promptEnhancer.enhance(task.getPrompt(), VideoMode.T2I, "MOCK".equals(task.getProvider()) ? null
                    : credentials.forTask(task.getUserId(), "DASHSCOPE", task.getLlmCredentialVersionId()));
            if (r.isEnhanced()) {
                BigDecimal llmCost = costService.calcLlmCost(r.getInputTokens(), r.getOutputTokens());
                update.setEnhanceMs(r.getElapsedMs());
                update.setLlmInputTokens(r.getInputTokens());
                update.setLlmOutputTokens(r.getOutputTokens());
                update.setLlmCost(llmCost);
            }
            return r.getPrompt();
        } catch (Exception e) {
            update.setEnhanceWarning("润色失败，已使用原始提示词。请检查百炼密钥、模型权限或额度。");
            log.warn("提示词润色失败，使用原始提示词 id={}：{}", task.getId(), e.getMessage());
            return task.getPrompt();
        }
    }

    private static Long elapsed(ImageTask task, LocalDateTime end) {
        return task.getCreatedAt() == null ? null : Duration.between(task.getCreatedAt(), end).toMillis();
    }
}
