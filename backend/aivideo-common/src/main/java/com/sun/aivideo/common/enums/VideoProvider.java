package com.sun.aivideo.common.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 视频生成厂商。新增厂商（可灵、火山引擎 Seedance、Google Veo 等）时：
 * 在此加一个枚举值，并在 aivideo-ai 中实现 VideoGenerationStrategy，前端的厂商下拉框会自动出现。
 */
@Getter
@RequiredArgsConstructor
public enum VideoProvider {
    /** 阿里云百炼（通义万相） */
    DASHSCOPE("阿里云百炼"),
    /** 本地模拟，不产生费用 */
    MOCK("本地模拟");

    /** 前端展示的厂商名称 */
    private final String label;

    /** 按名称查找，找不到返回 null */
    public static VideoProvider of(String name) {
        for (VideoProvider p : values()) {
            if (p.name().equalsIgnoreCase(name)) {
                return p;
            }
        }
        return null;
    }

    /** 厂商展示名称，未知厂商原样返回 */
    public static String labelOf(String name) {
        VideoProvider p = of(name);
        return p != null ? p.getLabel() : name;
    }
}
