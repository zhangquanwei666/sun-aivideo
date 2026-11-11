package com.sun.aivideo.ai.image.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 生成的一张图片：厂商返回图片地址（如百炼，24 小时有效）或直接返回图片字节（如 Gemini），二选一。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class GeneratedImage {

    /** 厂商返回的图片地址 */
    private String url;
    /** 厂商直接返回的图片内容 */
    private byte[] data;
    /** 如 image/png */
    private String mimeType;

    public static GeneratedImage ofUrl(String url) {
        return new GeneratedImage(url, null, null);
    }

    public static GeneratedImage ofData(byte[] data, String mimeType) {
        return new GeneratedImage(null, data, mimeType);
    }
}
