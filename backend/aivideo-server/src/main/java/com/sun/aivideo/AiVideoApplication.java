package com.sun.aivideo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 启动类。扫描 com.sun.aivideo 下的所有组件，包括：
 * <ul>
 *     <li>aivideo-ai 模块：com.sun.aivideo.ai（视频厂商适配、千问润色）</li>
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
