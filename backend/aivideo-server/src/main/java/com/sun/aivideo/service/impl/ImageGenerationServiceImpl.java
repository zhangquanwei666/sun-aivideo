package com.sun.aivideo.service.impl;

import com.sun.aivideo.common.enums.ErrorCode;
import com.sun.aivideo.ai.image.ImageGenerationStrategyFactory;
import com.sun.aivideo.ai.llm.PromptEnhancer;
import com.sun.aivideo.common.enums.ImageProvider;
import com.sun.aivideo.common.enums.TaskStatus;
import com.sun.aivideo.common.exception.BizException;
import com.sun.aivideo.converter.ImageTaskConverter;
import com.sun.aivideo.converter.ModelConfigConverter;
import com.sun.aivideo.dao.ImageModelConfigMapper;
import com.sun.aivideo.dao.ImageTaskMapper;
import com.sun.aivideo.dao.bo.ImageModelStatBO;
import com.sun.aivideo.dao.bo.ImageStatsBO;
import com.sun.aivideo.entity.ImageModelConfig;
import com.sun.aivideo.entity.ImageTask;
import com.sun.aivideo.model.dto.ImageGenerateDTO;
import com.sun.aivideo.model.vo.ImageStatsVO;
import com.sun.aivideo.model.vo.ImageTaskVO;
import com.sun.aivideo.service.CostService;
import com.sun.aivideo.service.ImageGenerationService;
import com.sun.aivideo.service.ImageModelConfigService;
import com.sun.aivideo.service.ImageTaskWorker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import cn.dev33.satoken.stp.StpUtil;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ImageGenerationServiceImpl implements ImageGenerationService {

    private static final String FILES_PREFIX = "/files/";

    private final ImageTaskMapper imageTaskMapper;
    private final ImageModelConfigMapper imageModelConfigMapper;
    private final ImageModelConfigService imageModelConfigService;
    private final ImageGenerationStrategyFactory imageStrategyFactory;
    private final PromptEnhancer promptEnhancer;
    private final CostService costService;
    private final ImageTaskWorker imageTaskWorker;

    @Override
    public ImageTaskVO create(ImageGenerateDTO dto) {
        ImageModelConfig model = imageModelConfigService.getEnabledByCode(dto.getModelCode());
        if (!imageStrategyFactory.isSupported(model.getProvider())) {
            throw new BizException(ErrorCode.IMAGE_PROVIDER_UNSUPPORTED, ImageProvider.labelOf(model.getProvider()));
        }
        String ratio = validate(dto, model);
        List<String> references = dto.getReferenceImages() == null ? List.of()
                : dto.getReferenceImages().stream().map(String::trim).filter(StringUtils::hasText).toList();

        ImageTask task = new ImageTask();
        task.setUserId(StpUtil.getLoginIdAsLong());
        task.setProvider(imageStrategyFactory.resolveForNewTask(model.getProvider()).provider().name());
        task.setModelCode(model.getModelCode());
        task.setStatus(TaskStatus.SUBMITTING.name());
        task.setPrompt(dto.getPrompt().trim());
        task.setEnhancePrompt(Boolean.TRUE.equals(dto.getEnhancePrompt()) && promptEnhancer.available());
        task.setReferenceImages(references.isEmpty() ? null : String.join(",", references));
        task.setAspectRatio(ratio);
        task.setImageSize(dto.getImageSize());
        task.setImageCount(1);
        task.setCurrency(model.getCurrency());
        task.setImageCost(BigDecimal.ZERO);
        task.setLlmInputTokens(0);
        task.setLlmOutputTokens(0);
        task.setLlmCost(BigDecimal.ZERO);
        task.setTotalCost(BigDecimal.ZERO);
        task.setCreatedAt(LocalDateTime.now());
        imageTaskMapper.insert(task);
        log.info("创建文生图任务 id={} model={} provider={}", task.getId(), task.getModelCode(), task.getProvider());

        imageTaskWorker.generateAsync(task.getId());
        return ImageTaskConverter.toVO(task, model, estimate(task, model));
    }

    @Override
    public ImageTaskVO detail(Long id) {
        ImageTask task = imageTaskMapper.selectById(id);
        if (task == null || !java.util.Objects.equals(task.getUserId(), StpUtil.getLoginIdAsLong())) {
            throw new BizException(ErrorCode.IMAGE_NOT_FOUND, id);
        }
        ImageModelConfig model = imageModelConfigService.findByCode(task.getModelCode());
        return ImageTaskConverter.toVO(task, model, estimate(task, model));
    }

    @Override
    public Map<Long, ImageTaskVO> listByIds(Collection<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return Map.of();
        }
        Map<String, ImageModelConfig> models = modelMap();
        Map<Long, ImageTaskVO> result = new LinkedHashMap<>();
        for (ImageTask t : imageTaskMapper.selectBatchIds(ids)) {
            if (!java.util.Objects.equals(t.getUserId(), StpUtil.getLoginIdAsLong())) continue;
            ImageModelConfig model = models.get(t.getModelCode());
            result.put(t.getId(), ImageTaskConverter.toVO(t, model, estimate(t, model)));
        }
        return result;
    }

    @Override
    public void delete(Long id) {
        ImageTask task = imageTaskMapper.selectById(id);
        if (task == null || !java.util.Objects.equals(task.getUserId(), StpUtil.getLoginIdAsLong())) {
            throw new BizException(ErrorCode.IMAGE_DELETED, id);
        }
        TaskStatus status = TaskStatus.of(task.getStatus());
        if (status == null || !status.isFinished()) {
            throw new BizException(ErrorCode.IMAGE_IN_PROGRESS);
        }
        if (imageTaskMapper.softDeleteById(id, StpUtil.getLoginIdAsLong()) == 0) {
            throw new BizException(ErrorCode.IMAGE_DELETED, id);
        }
        log.info("图片任务已删除（逻辑删除）id={}", id);
    }

    @Override
    public ImageStatsVO stats() {
        ImageStatsBO bo = imageTaskMapper.selectStats(StpUtil.getLoginIdAsLong());
        Map<String, ImageModelConfig> models = modelMap();

        ImageStatsVO vo = new ImageStatsVO();
        vo.setTotalCount(nz(bo.getTotalCount()));
        vo.setSucceededCount(nz(bo.getSucceededCount()));
        vo.setFailedCount(nz(bo.getFailedCount()));
        vo.setInProgressCount(nz(bo.getInProgressCount()));
        vo.setTotalImages(nz(bo.getTotalImages()));
        vo.setTotalCost(money(bo.getTotalCost()));
        vo.setTotalImageCost(money(bo.getTotalImageCost()));
        vo.setTotalLlmCost(bo.getTotalLlmCost() == null ? BigDecimal.ZERO : bo.getTotalLlmCost().setScale(6, RoundingMode.HALF_UP));
        vo.setAvgCost(money(bo.getAvgCost()));
        vo.setAvgElapsedMs(bo.getAvgElapsedMs() == null ? null : bo.getAvgElapsedMs().longValue());
        vo.setByModel(imageTaskMapper.selectStatsByModel(StpUtil.getLoginIdAsLong()).stream()
                .map(m -> toModelStat(m, models.get(m.getModelCode())))
                .toList());
        return vo;
    }

    // ------------------------------------------------------------------

    /** 校验参数是否符合模型能力，返回最终使用的画幅比例 */
    private String validate(ImageGenerateDTO dto, ImageModelConfig model) {
        List<String> sizes = ModelConfigConverter.split(model.getSizes());
        if (!sizes.contains(dto.getImageSize())) {
            throw new BizException(ErrorCode.IMAGE_SIZE_UNSUPPORTED, model.getDisplayName(), String.join(" / ", sizes));
        }
        if (costService.imageUnitPrice(model, dto.getImageSize()) == null) {
            throw new BizException(ErrorCode.IMAGE_PRICE_MISSING, dto.getImageSize());
        }

        List<String> references = dto.getReferenceImages() == null ? List.of() : dto.getReferenceImages();
        int max = model.getMaxReferenceImages() == null ? 0 : model.getMaxReferenceImages();
        if (references.size() > max) {
            throw new BizException(max == 0 ? ErrorCode.REFERENCE_UNSUPPORTED : ErrorCode.TOO_MANY_REFERENCES, model.getDisplayName(), max);
        }
        for (String ref : references) {
            if (!ref.startsWith(FILES_PREFIX) || ref.contains("..")) {
                throw new BizException(ErrorCode.REFERENCE_UPLOAD_REQUIRED);
            }
        }
        if (!references.isEmpty() && "4K".equals(dto.getImageSize())
                && ImageProvider.DASHSCOPE.name().equals(model.getProvider())) {
            throw new BizException(ErrorCode.REFERENCE_SIZE_UNSUPPORTED);
        }

        List<String> ratios = ModelConfigConverter.split(model.getRatios());
        if (!StringUtils.hasText(dto.getAspectRatio())) {
            return ratios.isEmpty() ? "1:1" : ratios.get(0);
        }
        if (!ratios.isEmpty() && !ratios.contains(dto.getAspectRatio())) {
            throw new BizException(ErrorCode.RATIO_UNSUPPORTED, model.getDisplayName(), String.join(" / ", ratios));
        }
        return dto.getAspectRatio();
    }

    /** 未完成任务的预估费用：每张单价（人民币）× 张数 + 已产生的润色费用 */
    private BigDecimal estimate(ImageTask task, ImageModelConfig model) {
        if (model == null) {
            return null;
        }
        int count = task.getImageCount() == null ? 1 : task.getImageCount();
        BigDecimal image = costService.calcImageCost(model, task.getImageSize(), count).getCostCny();
        BigDecimal llm = task.getLlmCost() == null ? BigDecimal.ZERO : task.getLlmCost();
        return image.add(llm).setScale(4, RoundingMode.HALF_UP);
    }

    private ImageStatsVO.ModelStat toModelStat(ImageModelStatBO m, ImageModelConfig model) {
        ImageStatsVO.ModelStat s = new ImageStatsVO.ModelStat();
        s.setModelCode(m.getModelCode());
        s.setModelName(model != null ? model.getDisplayName() : m.getModelCode());
        s.setTotalCount(nz(m.getTotalCount()));
        s.setSucceededCount(nz(m.getSucceededCount()));
        s.setTotalImages(nz(m.getTotalImages()));
        s.setTotalCost(money(m.getTotalCost()));
        s.setAvgElapsedMs(m.getAvgElapsedMs() == null ? null : m.getAvgElapsedMs().longValue());
        return s;
    }

    private Map<String, ImageModelConfig> modelMap() {
        return imageModelConfigMapper.selectList(null).stream()
                .collect(Collectors.toMap(ImageModelConfig::getModelCode, Function.identity(), (a, b) -> a));
    }

    private static long nz(Long v) {
        return v == null ? 0L : v;
    }

    private static BigDecimal money(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v.setScale(4, RoundingMode.HALF_UP);
    }
}
