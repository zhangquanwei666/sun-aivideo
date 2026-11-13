package com.sun.aivideo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 启动类。扫描 com.sun.aivideo 下的所有组件，包括：
 * <ul>
 *     <li>aivideo-ai-core 模块：com.sun.aivideo.ai（策略接口、工厂、Mock、提示词润色编排）</li>
 *     <li>厂商模块：com.sun.aivideo.ai.dashscope / gemini（客户端配置与模型调用实现）</li>
 *     <li>aivideo-dao 模块：com.sun.aivideo.dao / entity（Mapper 由 MybatisPlusConfig 注册）</li>
 * </ul>
 */
@SpringBootApplication
@EnableAsync
@EnableScheduling
@ConfigurationPropertiesScan
public class AiVideoApplication {

    public static void main(String[] args) {
        SpringApplication.run(AiVideoApplication.class, args);
    }
}
