package com.sun.aivideo.admin.auth;

import cn.dev33.satoken.stp.StpLogic;

/** 管理员使用独立 loginType=admin，与前台用户登录态完全隔离。 */
public final class AdminStpUtil {

    private static final StpLogic LOGIC = new StpLogic("admin");

    private AdminStpUtil() {
    }

    public static void login(long adminId) {
        LOGIC.login(adminId);
    }

    public static void checkLogin() {
        LOGIC.checkLogin();
    }

    public static long getLoginIdAsLong() {
        return LOGIC.getLoginIdAsLong();
    }

    public static String getTokenValue() {
        return LOGIC.getTokenValue();
    }

    public static void logout() {
        LOGIC.logout();
    }
}
