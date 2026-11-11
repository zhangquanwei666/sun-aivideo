package com.sun.aivideo.common.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 文生图厂商。新增厂商时在此加一个枚举值，并在 aivideo-ai 中实现 ImageGenerationStrategy，
 * 前端「文生图」模式下的厂商下拉框会自动出现。
 */
@Getter
@RequiredArgsConstructor
public enum ImageProvider {
    /** 阿里云百炼（通义万相 2.7 图像） */
    DASHSCOPE("阿里云百炼"),
    /** Google Gemini（Nano Banana） */
    GEMINI("Google Gemini"),
    /** 本地模拟，不产生费用 */
    MOCK("本地模拟");

    /** 前端展示的厂商名称 */
    private final String label;

    /** 按名称查找，找不到返回 null */
    public static ImageProvider of(String name) {
        for (ImageProvider p : values()) {
            if (p.name().equalsIgnoreCase(name)) {
                return p;
            }
        }
        return null;
    }

    /** 厂商展示名称，未知厂商原样返回 */
    public static String labelOf(String name) {
        ImageProvider p = of(name);
        return p != null ? p.getLabel() : name;
    }
}
