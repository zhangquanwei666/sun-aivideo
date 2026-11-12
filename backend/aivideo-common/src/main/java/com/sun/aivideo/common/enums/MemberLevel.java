package com.sun.aivideo.common.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.time.LocalDateTime;

/**
 * 会员等级。注册后为普通用户，开通会员后才能生成视频。
 * <p>会员是否有效：等级为 VIP，且到期时间为空（永久）或晚于当前时间。</p>
 */
@Getter
@RequiredArgsConstructor
public enum MemberLevel {
    NORMAL("普通用户"),
    VIP("会员");

    private final String label;

    /** 判断会员当前是否有效 */
    public static boolean isActive(String level, LocalDateTime expireAt) {
        return VIP.name().equals(level) && (expireAt == null || expireAt.isAfter(LocalDateTime.now()));
    }

    public static String labelOf(String name) {
        for (MemberLevel l : values()) {
            if (l.name().equals(name)) {
                return l.label;
            }
        }
        return name;
    }
}
