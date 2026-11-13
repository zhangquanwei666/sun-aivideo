package com.sun.aivideo.admin.controller;

import com.sun.aivideo.admin.model.PageResponse;
import com.sun.aivideo.admin.model.OverviewResponse;
import com.sun.aivideo.admin.service.AdminUserService;
import com.sun.aivideo.common.base.BaseResponse;
import com.sun.aivideo.dao.bo.UserListBO;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminUserController {

    private final AdminUserService service;

    public record StatusRequest(@NotBlank String status) {
    }

    @GetMapping("/users")
    public PageResponse<UserListBO> users(
            @RequestParam(defaultValue = "1") @Min(1) long pageNum,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) long pageSize,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String status) {
        return service.page(pageNum, pageSize, keyword, status);
    }

    @GetMapping("/users/overview")
    public OverviewResponse overview() {
        return OverviewResponse.of(service.overview());
    }

    @PutMapping("/users/{userId}/status")
    public BaseResponse changeStatus(@PathVariable long userId, @Valid @RequestBody StatusRequest request) {
        return service.changeStatus(userId, request.status());
    }

}
