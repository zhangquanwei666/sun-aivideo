package com.sun.aivideo.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.sun.aivideo.common.exception.BizException;
import com.sun.aivideo.converter.ModelConfigConverter;
import com.sun.aivideo.converter.VideoTaskConverter;
import com.sun.aivideo.dao.ModelConfigMapper;
import com.sun.aivideo.dao.VideoTaskMapper;
import com.sun.aivideo.entity.ModelConfig;
import com.sun.aivideo.entity.VideoTask;
import com.sun.aivideo.common.enums.TaskStatus;
import com.sun.aivideo.common.enums.VideoMode;
import com.sun.aivideo.dao.bo.ModelStatBO;
import com.sun.aivideo.dao.bo.VideoStatsBO;
import com.sun.aivideo.model.dto.VideoGenerateDTO;
import com.sun.aivideo.model.dto.VideoTaskQueryDTO;
import com.sun.aivideo.model.vo.PageVO;
import com.sun.aivideo.model.vo.VideoStatsVO;
import com.sun.aivideo.model.vo.VideoTaskVO;
import com.sun.aivideo.ai.video.VideoGenerationStrategyFactory;
import com.sun.aivideo.service.CostService;
import com.sun.aivideo.service.ModelConfigService;
import com.sun.aivideo.ai.llm.PromptEnhancer;
import com.sun.aivideo.service.VideoGenerationService;
import com.sun.aivideo.service.VideoTaskWorker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class VideoGenerationServiceImpl implements VideoGenerationService {

    private final VideoTaskMapper videoTaskMapper;
    private final ModelConfigMapper modelConfigMapper;
    private final ModelConfigService modelConfigService;
    private final PromptEnhancer promptEnhancer;
    private final CostService costService;
    private final VideoGenerationStrategyFactory videoStrategyFactory;
    private final VideoTaskWorker videoTaskWorker;

    @Override
    public VideoTaskVO create(VideoGenerateDTO dto) {
        ModelConfig model = modelConfigService.getEnabledByCode(dto.getModelCode());
        VideoMode mode = VideoMode.valueOf(model.getMode());
        String ratio = validate(dto, model, mode);

        VideoTask task = new VideoTask();
        task.setProvider(videoStrategyFactory.resolveForNewTask(model.getProvider()).provider().name());
        task.setModelCode(model.getModelCode());
        task.setMode(mode.name());
        task.setStatus(TaskStatus.SUBMITTING.name());
        task.setPrompt(trimToNull(dto.getPrompt()));
        task.setNegativePrompt(trimToNull(dto.getNegativePrompt()));
        task.setImageUrl(mode == VideoMode.I2V ? dto.getImageUrl().trim() : null);
        task.setResolution(dto.getResolution());
        task.setRatio(ratio);
        task.setDuration(dto.getDuration());
        task.setEnhancePrompt(Boolean.TRUE.equals(dto.getEnhancePrompt()) && promptEnhancer.available());
        task.setPromptExtend(!Boolean.FALSE.equals(dto.getPromptExtend()));
        task.setWatermark(Boolean.TRUE.equals(dto.getWatermark()));
        task.setAudio(Boolean.TRUE.equals(model.getSupportsAudio()) ? dto.getAudio() : null);
        task.setSeed(dto.getSeed());
        task.setVideoCost(BigDecimal.ZERO);
        task.setLlmInputTokens(0);
        task.setLlmOutputTokens(0);
        task.setLlmCost(BigDecimal.ZERO);
        task.setTotalCost(BigDecimal.ZERO);
        task.setCreatedAt(LocalDateTime.now());
        videoTaskMapper.insert(task);
        log.info("创建视频任务 id={} model={} provider={}", task.getId(), task.getModelCode(), task.getProvider());

        videoTaskWorker.submitAsync(task.getId());
        return VideoTaskConverter.toVO(task, model, estimate(task, model));
    }

    @Override
    public VideoTaskVO detail(Long id) {
        VideoTask task = videoTaskMapper.selectById(id);
        if (task == null) {
            throw new BizException(BizException.NOT_FOUND, "任务不存在：" + id);
        }
        ModelConfig model = modelConfigService.findByCode(task.getModelCode());
        return VideoTaskConverter.toVO(task, model, estimate(task, model));
    }

    @Override
    public PageVO<VideoTaskVO> page(VideoTaskQueryDTO query) {
        IPage<VideoTask> page = videoTaskMapper.selectTaskPage(
                Page.of(query.getPageNum(), query.getPageSize()),
                trimToNull(query.getStatus()),
                trimToNull(query.getModelCode()),
                trimToNull(query.getKeyword()));

        Map<String, ModelConfig> models = modelMap();
        List<VideoTaskVO> records = page.getRecords().stream()
                .map(t -> {
                    ModelConfig model = models.get(t.getModelCode());
                    return VideoTaskConverter.toVO(t, model, estimate(t, model));
                })
                .toList();
        return new PageVO<>(page.getTotal(), page.getCurrent(), page.getSize(), records);
    }

    @Override
    public VideoStatsVO stats() {
        VideoStatsBO bo = videoTaskMapper.selectStats();
        Map<String, ModelConfig> models = modelMap();

        VideoStatsVO vo = new VideoStatsVO();
        vo.setTotalCount(nz(bo.getTotalCount()));
        vo.setSucceededCount(nz(bo.getSucceededCount()));
        vo.setFailedCount(nz(bo.getFailedCount()));
        vo.setInProgressCount(nz(bo.getInProgressCount()));
        vo.setTotalCost(money(bo.getTotalCost()));
        vo.setTotalVideoCost(money(bo.getTotalVideoCost()));
        vo.setTotalLlmCost(bo.getTotalLlmCost() == null ? BigDecimal.ZERO : bo.getTotalLlmCost().setScale(6, RoundingMode.HALF_UP));
        vo.setTotalVideoSeconds(nz(bo.getTotalVideoSeconds()));
        vo.setAvgCost(money(bo.getAvgCost()));
        vo.setAvgElapsedMs(bo.getAvgElapsedMs() == null ? null : bo.getAvgElapsedMs().longValue());

        List<VideoStatsVO.ModelStat> byModel = videoTaskMapper.selectStatsByModel().stream()
                .map(m -> toModelStat(m, models.get(m.getModelCode())))
                .toList();
        vo.setByModel(byModel);
        return vo;
    }

    // ------------------------------------------------------------------

    /** 校验参数是否符合模型能力，返回最终使用的画幅比例 */
    private String validate(VideoGenerateDTO dto, ModelConfig model, VideoMode mode) {
        if (mode == VideoMode.T2V && !StringUtils.hasText(dto.getPrompt())) {
            throw new BizException("文生视频需要填写提示词");
        }
        if (mode == VideoMode.I2V && !StringUtils.hasText(dto.getImageUrl())) {
            throw new BizException("图生视频需要上传首帧图片");
        }
        if (mode == VideoMode.I2V && !isAcceptedImage(dto.getImageUrl().trim())) {
            throw new BizException("首帧图片需为 http(s) 公网地址或上传后的 /files/ 路径");
        }
        List<String> resolutions = ModelConfigConverter.split(model.getResolutions());
        if (!resolutions.contains(dto.getResolution())) {
            throw new BizException(model.getDisplayName() + " 只支持分辨率：" + String.join(" / ", resolutions));
        }
        if (costService.unitPrice(model, dto.getResolution()) == null) {
            throw new BizException("未配置 " + dto.getResolution() + " 的单价，请先在 t_model_config 中补充");
        }
        if (dto.getDuration() < model.getMinDuration() || dto.getDuration() > model.getMaxDuration()) {
            throw new BizException(String.format("%s 时长范围为 %d~%d 秒",
                    model.getDisplayName(), model.getMinDuration(), model.getMaxDuration()));
        }
        if (mode == VideoMode.I2V) {
            // 图生视频的画幅跟随首帧图片
            return null;
        }
        List<String> ratios = ModelConfigConverter.split(model.getRatios());
        if (!StringUtils.hasText(dto.getRatio())) {
            return ratios.isEmpty() ? null : ratios.get(0);
        }
        if (!ratios.isEmpty() && !ratios.contains(dto.getRatio())) {
            throw new BizException(model.getDisplayName() + " 只支持画幅：" + String.join(" / ", ratios));
        }
        return dto.getRatio();
    }

    private static boolean isAcceptedImage(String url) {
        if (url.startsWith("/files/")) {
            return !url.contains("..");
        }
        return url.startsWith("http://") || url.startsWith("https://");
    }

    /** 未完成任务的预估费用：单价 × 请求时长 + 已产生的润色费用 */
    private BigDecimal estimate(VideoTask task, ModelConfig model) {
        if (model == null) {
            return null;
        }
        BigDecimal video = costService.calcVideoCost(model, task.getResolution(), task.getDuration()).getCost();
        BigDecimal llm = task.getLlmCost() == null ? BigDecimal.ZERO : task.getLlmCost();
        return video.add(llm).setScale(4, RoundingMode.HALF_UP);
    }

    private VideoStatsVO.ModelStat toModelStat(ModelStatBO m, ModelConfig model) {
        VideoStatsVO.ModelStat s = new VideoStatsVO.ModelStat();
        s.setModelCode(m.getModelCode());
        s.setModelName(model != null ? model.getDisplayName() : m.getModelCode());
        s.setTotalCount(nz(m.getTotalCount()));
        s.setSucceededCount(nz(m.getSucceededCount()));
        s.setTotalCost(money(m.getTotalCost()));
        s.setTotalVideoSeconds(nz(m.getTotalVideoSeconds()));
        s.setAvgElapsedMs(m.getAvgElapsedMs() == null ? null : m.getAvgElapsedMs().longValue());
        return s;
    }

    private Map<String, ModelConfig> modelMap() {
        return modelConfigMapper.selectList(null).stream()
                .collect(Collectors.toMap(ModelConfig::getModelCode, Function.identity(), (a, b) -> a));
    }

    private static long nz(Long v) {
        return v == null ? 0L : v;
    }

    private static BigDecimal money(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v.setScale(4, RoundingMode.HALF_UP);
    }

    private static String trimToNull(String s) {
        return StringUtils.hasText(s) ? s.trim() : null;
    }
}
