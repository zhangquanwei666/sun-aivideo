package com.sun.aivideo.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Path;

@Configuration
@RequiredArgsConstructor
public class WebConfig implements WebMvcConfigurer {

    private final AiVideoProperties properties;

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        String[] origins = properties.getCors().getAllowedOriginPatterns().stream()
                .filter(StringUtils::hasText)
                .map(String::trim)
                .toArray(String[]::new);
        if (origins.length == 0) {
            return;
        }
        registry.addMapping("/**")
                .allowedOriginPatterns(origins)
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*");
    }

    /** 本地文件（上传的图片、下载的视频）以 /files/** 对外访问 */
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String location = Path.of(properties.getStorage().getBaseDir(), "files")
                .toAbsolutePath().normalize().toUri().toString();
        registry.addResourceHandler("/files/**").addResourceLocations(location);
    }
}
