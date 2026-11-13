package com.sun.aivideo.common.enums;

/**
 * 大模型厂商。新增厂商（DeepSeek、文心等）时在此扩展，并在对应 aivideo-ai-channel-* 模块中实现 LlmStrategy。
 */
public enum LlmProvider {
    /** 通义千问（阿里云百炼 OpenAI 兼容接口） */
    QWEN,
    /** 本地模拟，不产生费用 */
    MOCK
}
