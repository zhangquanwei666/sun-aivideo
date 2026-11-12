package com.sun.aivideo.common.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/** 账号状态（用户、管理员共用） */
@Getter
@RequiredArgsConstructor
public enum UserStatus {
    ENABLED("正常"),
    DISABLED("已禁用");

    private final String label;

    public static String labelOf(String name) {
        for (UserStatus s : values()) {
            if (s.name().equals(name)) {
                return s.label;
            }
        }
        return name;
    }
}
