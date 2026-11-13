package com.sun.aivideo.ai.auth;

import com.sun.aivideo.ai.exception.AiClientException;
import com.sun.aivideo.common.enums.AiChannel;

/** 仅供后端逐次调用使用；不序列化、不记录明文。 */
public final class ChannelCredentials {
    private final AiChannel channel;
    private final String apiKey;

    public ChannelCredentials(AiChannel channel, String apiKey) {
        this.channel = channel;
        this.apiKey = apiKey;
    }

    public String keyFor(AiChannel expected) {
        if (channel != expected || apiKey == null || apiKey.isBlank()) {
            throw new AiClientException("CREDENTIAL_UNAVAILABLE", "任务凭证不可用，请检查模型密钥配置");
        }
        return apiKey;
    }

    /** 厂商任务缓存也必须按凭证隔离，不能只按厂商任务 ID。 */
    public String cacheKey(String taskId) {
        try {
            byte[] hash = java.security.MessageDigest.getInstance("SHA-256")
                    .digest(keyFor(channel).getBytes(java.nio.charset.StandardCharsets.UTF_8));
            return java.util.HexFormat.of().formatHex(hash) + ":" + taskId;
        } catch (java.security.NoSuchAlgorithmException e) { throw new IllegalStateException(); }
    }

    @Override public String toString() { return "ChannelCredentials[redacted]"; }
}
