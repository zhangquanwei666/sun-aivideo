package com.sun.aivideo.config;

import com.sun.aivideo.ai.config.DashScopeProperties;
import com.sun.aivideo.ai.llm.PromptEnhancer;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Arrays;

/**
 * 启动时检查当前环境的关键配置，配置不全时尽早失败，并打印环境摘要。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class EnvironmentChecker {

    private final Environment environment;
    private final DashScopeProperties dashScopeProperties;
    private final PromptEnhancer promptEnhancer;

    /** 当前环境名：local / uat / pro */
    public String currentEnv() {
        String[] active = environment.getActiveProfiles();
        return active.length == 0 ? "default" : String.join(",", active);
    }

    public boolean isProduction() {
        return Arrays.asList(environment.getActiveProfiles()).contains("pro");
    }

    @PostConstruct
    public void check() {
        boolean hasKey = dashScopeProperties.hasApiKey();
        if (!hasKey && !dashScopeProperties.isMockWhenNoKey()) {
            throw new IllegalStateException("当前环境 [" + currentEnv() + "] 禁止 Mock，请配置环境变量 DASHSCOPE_API_KEY");
        }
        String password = environment.getProperty("spring.datasource.password", "");
        if (isProduction() && ("XXXX".equals(password) || !StringUtils.hasText(password))) {
            throw new IllegalStateException("生产环境数据库密码未配置，请设置环境变量 MYSQL_PASSWORD");
        }
        log.info("当前环境：{} | 数据库：{} | 视频生成：{} | 提示词润色：{}",
                currentEnv(),
                maskUrl(environment.getProperty("spring.datasource.url", "")),
                dashScopeProperties.isMockMode() ? "Mock" : "百炼",
                promptEnhancer.available() ? promptEnhancer.currentModelName() : "未启用");
    }

    /** 只打印 jdbc 地址的 host/库名部分 */
    private static String maskUrl(String url) {
        int q = url.indexOf('?');
        return q > 0 ? url.substring(0, q) : url;
    }
}
