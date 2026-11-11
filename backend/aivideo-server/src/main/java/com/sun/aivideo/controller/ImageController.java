package com.sun.aivideo.controller;

import com.sun.aivideo.common.base.BaseResponse;
import com.sun.aivideo.model.dto.ImageGenerateDTO;
import com.sun.aivideo.model.dto.ImageIdDTO;
import com.sun.aivideo.model.vo.ImageStatsVO;
import com.sun.aivideo.model.vo.ImageTaskVO;
import com.sun.aivideo.service.ImageGenerationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 文生图接口。入参继承 BaseRequest，出参继承 BaseResponse。
 */
@RestController
@RequestMapping("/api/images")
@RequiredArgsConstructor
public class ImageController {

    private final ImageGenerationService imageGenerationService;

    /** 创建文生图任务（异步），返回任务信息，之后轮询详情接口获取结果 */
    @PostMapping
    public ImageTaskVO generate(@Valid @RequestBody ImageGenerateDTO dto) {
        return BaseResponse.ok(imageGenerationService.create(dto));
    }

    /** 费用与耗时汇总 */
    @GetMapping("/stats")
    public ImageStatsVO stats() {
        return BaseResponse.ok(imageGenerationService.stats());
    }

    /** 任务详情：状态、图片地址、耗时、费用 */
    @GetMapping("/{id}")
    public ImageTaskVO detail(@Valid ImageIdDTO dto) {
        return BaseResponse.ok(imageGenerationService.detail(dto.getId()));
    }

    /** 删除图片（逻辑删除；生成中的任务不能删除） */
    @DeleteMapping("/{id}")
    public BaseResponse delete(@Valid ImageIdDTO dto) {
        imageGenerationService.delete(dto.getId());
        return BaseResponse.ok();
    }
}
