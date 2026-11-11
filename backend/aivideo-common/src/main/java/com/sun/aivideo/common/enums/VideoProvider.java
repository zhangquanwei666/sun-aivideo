package com.sun.aivideo.common.enums;

/** 视频生成厂商。新增厂商（可灵、火山引擎 Seedance 等）时在此扩展，并在 aivideo-ai 中实现 VideoGenerationStrategy */
public enum VideoProvider {
    /** 阿里云百炼（通义万相） */
    DASHSCOPE,
    /** 本地模拟，不产生费用 */
    MOCK
}
