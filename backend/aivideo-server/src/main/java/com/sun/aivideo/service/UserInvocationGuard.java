package com.sun.aivideo.service;

import com.sun.aivideo.ai.exception.AiClientException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import java.util.concurrent.ConcurrentHashMap;

/** 用户维度的并发上限，空闲即移除计数；不同实例仍受各自上限约束。 */
@Component
public class UserInvocationGuard {
    private final ConcurrentHashMap<Long, Integer> active = new ConcurrentHashMap<>();
    private final int limit;
    public UserInvocationGuard(@Value("${aivideo.credentials.max-concurrent-per-user:4}") int limit) {
        if (limit < 1) throw new IllegalArgumentException("用户并发上限必须大于 0");
        this.limit = limit;
    }
    public Permit acquire(Long userId) {
        if (userId == null) throw new AiClientException("CREDENTIAL_UNAVAILABLE", "历史任务没有用户归属，不能调用个人密钥");
        active.compute(userId, (id, count) -> {
            int n = count == null ? 0 : count;
            if (n >= limit) throw new AiClientException("USER_BUSY", "当前生成任务较多，请等待其他任务完成后重试");
            return n + 1;
        });
        return new Permit(userId);
    }
    public final class Permit implements AutoCloseable {
        private final Long userId;
        private boolean closed;
        private Permit(Long userId) { this.userId = userId; }
        @Override public void close() {
            if (!closed) { active.compute(userId, (id, n) -> n == null || n <= 1 ? null : n - 1); closed = true; }
        }
    }
}
