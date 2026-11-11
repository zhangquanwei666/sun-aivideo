package com.sun.aivideo.controller;

import com.sun.aivideo.common.base.BaseResponse;
import com.sun.aivideo.model.vo.ImageModelListVO;
import com.sun.aivideo.service.ImageModelConfigService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/image-models")
@RequiredArgsConstructor
public class ImageModelController {

    private final ImageModelConfigService imageModelConfigService;

    /** 可选的文生图厂商与模型（含清晰度、画幅、参考图张数、单价） */
    @GetMapping
    public ImageModelListVO list() {
        return BaseResponse.ok(imageModelConfigService.listForSelection());
    }
}
