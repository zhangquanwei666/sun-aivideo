package com.sun.aivideo.controller;

import com.sun.aivideo.common.base.BaseResponse;
import com.sun.aivideo.model.dto.VideoDeleteDTO;
import com.sun.aivideo.model.dto.VideoDetailDTO;
import com.sun.aivideo.model.dto.VideoGenerateDTO;
import com.sun.aivideo.model.dto.VideoTaskQueryDTO;
import com.sun.aivideo.model.vo.PageVO;
import com.sun.aivideo.model.vo.VideoStatsVO;
import com.sun.aivideo.model.vo.VideoTaskVO;
import com.sun.aivideo.service.VideoGenerationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 视频生成接口。入参继承 BaseRequest，出参继承 BaseResponse。
 */
@RestController
@RequestMapping("/api/videos")
@RequiredArgsConstructor
public class VideoController {

    private final VideoGenerationService videoGenerationService;

    /** 创建视频生成任务（异步），返回任务信息，之后轮询详情接口获取结果 */
    @PostMapping
    public VideoTaskVO generate(@Valid @RequestBody VideoGenerateDTO dto) {
        return BaseResponse.ok(videoGenerationService.create(dto));
    }

    /** 任务详情：状态、视频地址、耗时、费用 */
    @GetMapping("/{id}")
    public VideoTaskVO detail(@Valid VideoDetailDTO dto) {
        return BaseResponse.ok(videoGenerationService.detail(dto.getId()));
    }

    /** 分页查询历史任务 */
    @GetMapping
    public PageVO<VideoTaskVO> page(@Valid VideoTaskQueryDTO query) {
        return BaseResponse.ok(videoGenerationService.page(query));
    }

    /** 费用与耗时汇总 */
    @GetMapping("/stats")
    public VideoStatsVO stats() {
        return BaseResponse.ok(videoGenerationService.stats());
    }

    /** 删除视频（逻辑删除，只隐藏不物理删除；生成中的任务不能删除） */
    @DeleteMapping("/{id}")
    public BaseResponse delete(@Valid VideoDeleteDTO dto) {
        videoGenerationService.delete(dto.getId());
        return BaseResponse.ok();
    }
}
