package com.sun.aivideo.admin.controller;

import com.sun.aivideo.admin.model.AdminFeedItem;
import com.sun.aivideo.admin.model.PageResponse;
import com.sun.aivideo.admin.service.AdminFeedService;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/feed")
@RequiredArgsConstructor
public class AdminFeedController {

    private final AdminFeedService service;

    @GetMapping
    public PageResponse<AdminFeedItem> page(
            @RequestParam(defaultValue = "1") @Min(1) long pageNum,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) long pageSize,
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String keyword) {
        return service.page(pageNum, pageSize, userId, type, keyword);
    }
}
