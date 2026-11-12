package com.sun.aivideo.admin.config;

import cn.dev33.satoken.interceptor.SaInterceptor;
import com.sun.aivideo.admin.auth.AdminStpUtil;
import com.sun.aivideo.common.enums.ErrorCode;
import com.sun.aivideo.common.enums.UserStatus;
import com.sun.aivideo.common.exception.BizException;
import com.sun.aivideo.dao.AdminMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@RequiredArgsConstructor
public class AdminWebConfig implements WebMvcConfigurer {

    private final AdminMapper adminMapper;

    @Value("${aivideo.admin.cors-origins:http://localhost:*,http://127.0.0.1:*}")
    private String corsOrigins;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new SaInterceptor(handler -> {
                    AdminStpUtil.checkLogin();
                    var admin = adminMapper.selectById(AdminStpUtil.getLoginIdAsLong());
                    if (admin == null || !UserStatus.ENABLED.name().equals(admin.getStatus())) {
                        AdminStpUtil.logout();
                        throw new BizException(ErrorCode.ADMIN_UNAVAILABLE);
                    }
                }))
                .addPathPatterns("/api/admin/**")
                .excludePathPatterns("/api/admin/auth/login");
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        String[] origins = java.util.Arrays.stream(corsOrigins.split(","))
                .map(String::trim)
                .filter(StringUtils::hasText)
                .toArray(String[]::new);
        if (origins.length > 0) {
            registry.addMapping("/**")
                    .allowedOriginPatterns(origins)
                    .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                    .allowedHeaders("*");
        }
    }
}
