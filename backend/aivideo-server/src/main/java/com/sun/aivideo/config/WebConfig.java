package com.sun.aivideo.config;

import com.sun.aivideo.common.enums.ErrorCode;
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
    private final com.sun.aivideo.dao.UserMapper users;
    @Override
    public void addInterceptors(org.springframework.web.servlet.config.annotation.InterceptorRegistry registry) {
        registry.addInterceptor(new cn.dev33.satoken.interceptor.SaInterceptor(handler -> {
            cn.dev33.satoken.stp.StpUtil.checkLogin();
            var user = users.selectById(cn.dev33.satoken.stp.StpUtil.getLoginIdAsLong());
            if (user == null || !"ENABLED".equals(user.getStatus())) {
                cn.dev33.satoken.stp.StpUtil.logout();
                throw new com.sun.aivideo.common.exception.BizException(ErrorCode.ACCOUNT_UNAVAILABLE);
            }
        })).addPathPatterns("/api/**").excludePathPatterns("/api/auth/login", "/api/auth/register", "/api/system/status");
    }


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
