package com.sun.aivideo.model.dto;

import com.sun.aivideo.common.base.BaseRequest;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.util.List;

/**
 * 入参：创建文生图任务。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class ImageGenerateDTO extends BaseRequest {

    @NotBlank(message = "请选择模型")
    private String modelCode;

    @NotBlank(message = "请填写提示词")
    @Size(max = 5000, message = "提示词不能超过 5000 字")
    private String prompt;

    /** 画幅比例，如 16:9；为空时用模型支持的第一个 */
    @Pattern(regexp = "^$|\\d{1,2}:\\d{1,2}", message = "画幅比例格式不正确")
    private String aspectRatio;

    @NotBlank(message = "请选择清晰度")
    @Pattern(regexp = "1K|2K|4K", message = "清晰度只支持 1K / 2K / 4K")
    private String imageSize;

    /** 参考图：上传接口返回的 /files/... 路径 */
    @Size(max = 14, message = "参考图最多 14 张")
    private List<String> referenceImages;

    /** 是否先用大模型润色提示词 */
    private Boolean enhancePrompt = Boolean.FALSE;
}
