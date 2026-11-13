package com.sun.aivideo.common.enums;

/** 凭证按接入渠道保存，而不是按模型（如千问、万相）保存。 */
public enum AiChannel {
    DASHSCOPE("阿里云百炼"), GEMINI("Google Gemini");

    private final String label;
    AiChannel(String label) { this.label = label; }
    public String getLabel() { return label; }
}
