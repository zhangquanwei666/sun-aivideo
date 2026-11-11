package com.sun.aivideo.ai.image.model;

import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * 文生图结果。费用由调用方按模型单价 × 张数计算，tokens 仅供参考。
 */
@Data
@Builder
public class ImageGenerateResult {

    private List<GeneratedImage> images;
    /** 厂商返回的输入 / 输出 tokens（部分厂商不返回） */
    private Integer inputTokens;
    private Integer outputTokens;
}
