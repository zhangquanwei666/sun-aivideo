package com.sun.aivideo.admin.config;

import com.sun.aivideo.common.enums.UserStatus;
import com.sun.aivideo.dao.AdminMapper;
import com.sun.aivideo.entity.Admin;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Slf4j
@Component
@RequiredArgsConstructor
public class InitialAdminInitializer implements ApplicationRunner {

    private final AdminMapper adminMapper;

    @Value("${aivideo.admin.initial-username:admin}")
    private String username;

    @Value("${aivideo.admin.initial-password:}")
    private String password;

    @Override
    public void run(ApplicationArguments args) {
        if (adminMapper.countAll() > 0) {
            return;
        }
        if (!StringUtils.hasText(password) || "XXXX".equals(password)) {
            log.warn("t_admin 为空，尚未创建初始管理员；请设置 AIVIDEO_ADMIN_PASSWORD 后重启管理服务");
            return;
        }
        Admin admin = new Admin();
        admin.setUsername(username.trim());
        admin.setNickname("系统管理员");
        admin.setPasswordHash(new BCryptPasswordEncoder().encode(password));
        admin.setStatus(UserStatus.ENABLED.name());
        adminMapper.insert(admin);
        log.info("已创建初始管理员 username={}", admin.getUsername());
    }
}
