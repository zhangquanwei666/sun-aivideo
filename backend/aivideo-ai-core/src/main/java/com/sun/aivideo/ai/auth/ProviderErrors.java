package com.sun.aivideo.ai.auth;

import com.sun.aivideo.ai.exception.AiClientException;

/** 不透传可能回显用户 Key 的厂商响应、请求 URL 或底层异常。 */
public final class ProviderErrors {
    private ProviderErrors() {}
    public static AiClientException http(int status) {
        return switch (status) {
            case 401 -> new AiClientException("KEY_INVALID", "厂商密钥无效或已撤销，请更新模型密钥");
            case 402 -> new AiClientException("QUOTA_EXHAUSTED", "厂商账户额度不足，请检查厂商账单");
            case 403 -> new AiClientException("MODEL_FORBIDDEN", "厂商拒绝访问，请检查密钥、项目及模型权限");
            case 429 -> new AiClientException("RATE_LIMITED", "厂商限流或配额不足，请稍后重试");
            default -> new AiClientException("HTTP_" + status, "厂商接口调用失败（HTTP " + status + "）");
        };
    }
    public static AiClientException network() {
        return new AiClientException("NETWORK_ERROR", "调用厂商失败，请检查网络或稍后重试；未自动重复提交任务");
    }
    public static AiClientException rejected(String code) {
        String normalized = code == null ? "" : code.toUpperCase(java.util.Locale.ROOT);
        if (normalized.contains("KEY") || normalized.contains("UNAUTHENTICATED")) return http(401);
        if (normalized.contains("ACCESSDENIED") || normalized.contains("FORBIDDEN") || normalized.contains("PERMISSION")) return http(403);
        if (normalized.contains("QUOTA") || normalized.contains("BALANCE")) return http(402);
        if (normalized.contains("LIMIT") || normalized.contains("THROTTL")) return http(429);
        return new AiClientException("GENERATION_REJECTED", "厂商生成失败，请检查模型权限、配额或提示词");
    }
}
