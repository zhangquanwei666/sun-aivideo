package com.sun.aivideo.ai.image.model;

import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * 文生图请求（已完成参数校验、提示词润色、参考图解析）。
 */
@Data
@Builder
public class ImageGenerateRequest {

    /** 业务任务 ID，仅用于日志 */
    private Long bizTaskId;
    private String modelCode;
    private String prompt;
    /** 画幅比例，如 16:9 */
    private String aspectRatio;
    /** 清晰度档位：1K / 2K / 4K */
    private String imageSize;
    /** 参考图，统一为 data:image/...;base64,... 格式；为空表示纯文生图 */
    private List<String> referenceImages;
}
