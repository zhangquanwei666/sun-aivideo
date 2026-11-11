package com.sun.aivideo.controller;

import com.sun.aivideo.common.base.BaseResponse;
import com.sun.aivideo.model.dto.ImageUploadDTO;
import com.sun.aivideo.model.vo.UploadVO;
import com.sun.aivideo.service.FileStorageService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/files")
@RequiredArgsConstructor
public class FileController {

    private final FileStorageService fileStorageService;

    /** 上传图生视频的首帧图片（multipart/form-data，字段名 file） */
    @PostMapping("/images")
    public UploadVO uploadImage(@Valid ImageUploadDTO dto) {
        return BaseResponse.ok(fileStorageService.saveImage(dto.getFile()));
    }
}
