package com.sun.aivideo.controller;

import com.sun.aivideo.common.base.BaseResponse;
import com.sun.aivideo.model.vo.ModelListVO;
import com.sun.aivideo.service.ModelConfigService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/models")
@RequiredArgsConstructor
public class ModelController {

    private final ModelConfigService modelConfigService;

    /** 可选的视频模型、支持的参数与单价 */
    @GetMapping
    public ModelListVO list() {
        return BaseResponse.ok(new ModelListVO(modelConfigService.listEnabled()));
    }
}
