package com.sun.aivideo.ai.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/** AI 核心模块配置，只装配与厂商无关的配置项。 */
@Configuration
@EnableConfigurationProperties({LlmProperties.class, MockVideoProperties.class})
public class AiCoreConfig {
}
