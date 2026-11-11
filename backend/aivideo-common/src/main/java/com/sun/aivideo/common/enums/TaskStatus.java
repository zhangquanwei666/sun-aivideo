package com.sun.aivideo.common.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.List;

@Getter
@RequiredArgsConstructor
public enum TaskStatus {
    SUBMITTING("提交中"),
    PENDING("排队中"),
    RUNNING("生成中"),
    SUCCEEDED("已完成"),
    FAILED("失败"),
    CANCELED("已取消"),
    TIMEOUT("超时");

    private final String label;

    /** 需要继续轮询厂商的状态 */
    public static final List<String> POLLING = List.of(PENDING.name(), RUNNING.name());

    public boolean isFinished() {
        return this == SUCCEEDED || this == FAILED || this == CANCELED || this == TIMEOUT;
    }

    public static TaskStatus of(String name) {
        for (TaskStatus s : values()) {
            if (s.name().equalsIgnoreCase(name)) {
                return s;
            }
        }
        return null;
    }
}
