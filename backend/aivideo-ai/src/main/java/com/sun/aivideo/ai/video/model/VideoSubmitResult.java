package com.sun.aivideo.ai.video.model;

import com.sun.aivideo.common.enums.VideoTaskState;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 厂商创建任务的返回。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class VideoSubmitResult {

    private String providerTaskId;
    /** 初始状态，通常是 PENDING */
    private VideoTaskState state;
}
