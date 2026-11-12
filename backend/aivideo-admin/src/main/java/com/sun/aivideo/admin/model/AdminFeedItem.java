package com.sun.aivideo.admin.model;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class AdminFeedItem {
    private String type;
    private Long id;
    private Long userId;
    private String username;
    private LocalDateTime createdAt;
    private Object task;
}
