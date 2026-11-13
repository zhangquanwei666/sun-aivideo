package com.sun.aivideo.ai.video.model;

import com.sun.aivideo.common.enums.VideoTaskState;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 查询厂商任务状态的结果（已转换为统一格式）。
 * 视频结果二选一：videoUrl（厂商返回地址，如百炼）或 videoData（厂商直接返回视频内容，如 Gemini）。
 */
@Data
@Builder
public class VideoTaskResult {

    private VideoTaskState state;
    private String videoUrl;
    /** 厂商直接返回的视频内容 */
    private byte[] videoData;
    /** 计费时长（秒） */
    private Integer billedDuration;
    /** 厂商侧的提交 / 开始执行 / 结束时间，用于计算排队与生成耗时 */
    private LocalDateTime submitTime;
    private LocalDateTime scheduledTime;
    private LocalDateTime endTime;
    private String errorCode;
    private String errorMessage;
}
