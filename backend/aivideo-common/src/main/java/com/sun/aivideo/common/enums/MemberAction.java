package com.sun.aivideo.common.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/** 会员变更动作（记录在 t_member_record） */
@Getter
@RequiredArgsConstructor
public enum MemberAction {
    OPEN("开通 / 续期"),
    REVOKE("取消会员");

    private final String label;

    public static String labelOf(String name) {
        for (MemberAction a : values()) {
            if (a.name().equals(name)) {
                return a.label;
            }
        }
        return name;
    }
}
