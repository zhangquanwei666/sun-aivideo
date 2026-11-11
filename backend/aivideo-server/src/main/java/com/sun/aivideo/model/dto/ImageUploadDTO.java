package com.sun.aivideo.model.dto;

import com.sun.aivideo.common.base.BaseRequest;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.springframework.web.multipart.MultipartFile;

/**
 * 入参：上传图生视频的首帧图片（multipart/form-data，字段名 file）。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class ImageUploadDTO extends BaseRequest {

    @NotNull(message = "请选择图片")
    private MultipartFile file;
}
