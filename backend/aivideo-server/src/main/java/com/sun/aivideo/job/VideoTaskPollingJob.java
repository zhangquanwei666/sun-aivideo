package com.sun.aivideo.job;

import com.sun.aivideo.service.VideoTaskWorker;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 定时轮询进行中的视频任务。
 */
@Component
@RequiredArgsConstructor
public class VideoTaskPollingJob {

    private final VideoTaskWorker videoTaskWorker;

    @Scheduled(initialDelay = 5000, fixedDelayString = "${aivideo.polling.interval-ms:10000}")
    public void poll() {
        videoTaskWorker.pollActiveTasks();
    }
}
