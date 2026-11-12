package com.sun.aivideo.admin.controller;

import com.sun.aivideo.admin.auth.AdminStpUtil;
import com.sun.aivideo.common.base.BaseResponse;
import com.sun.aivideo.common.enums.ErrorCode;
import com.sun.aivideo.common.enums.UserStatus;
import com.sun.aivideo.common.exception.BizException;
import com.sun.aivideo.dao.AdminMapper;
import com.sun.aivideo.entity.Admin;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/auth")
@RequiredArgsConstructor
public class AdminAuthController {

    private final AdminMapper adminMapper;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public record LoginRequest(@NotBlank String username, @NotBlank String password) {
    }

    @PostMapping("/login")
    public Map<String, Object> login(@Valid @RequestBody LoginRequest request, HttpServletRequest servletRequest) {
        Admin admin = adminMapper.selectByUsername(request.username().trim());
        if (admin == null || !passwordEncoder.matches(request.password(), admin.getPasswordHash())) {
            throw new BizException(ErrorCode.ADMIN_INVALID_CREDENTIALS);
        }
        if (!UserStatus.ENABLED.name().equals(admin.getStatus())) {
            throw new BizException(ErrorCode.ADMIN_DISABLED);
        }

        Admin update = new Admin();
        update.setId(admin.getId());
        update.setLastLoginAt(LocalDateTime.now());
        update.setLastLoginIp(servletRequest.getRemoteAddr());
        adminMapper.updateById(update);

        AdminStpUtil.login(admin.getId());
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("code", 0);
        result.put("message", "success");
        result.put("token", AdminStpUtil.getTokenValue());
        result.put("username", admin.getUsername());
        result.put("nickname", admin.getNickname());
        return result;
    }

    @GetMapping("/me")
    public Map<String, Object> me() {
        Admin admin = adminMapper.selectById(AdminStpUtil.getLoginIdAsLong());
        if (admin == null) {
            throw new BizException(ErrorCode.ADMIN_UNAVAILABLE);
        }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("code", 0);
        result.put("message", "success");
        result.put("id", admin.getId());
        result.put("username", admin.getUsername());
        result.put("nickname", admin.getNickname());
        return result;
    }

    @PostMapping("/logout")
    public BaseResponse logout() {
        AdminStpUtil.logout();
        return BaseResponse.ok();
    }
}
