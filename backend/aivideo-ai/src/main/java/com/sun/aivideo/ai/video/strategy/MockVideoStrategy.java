package com.sun.aivideo.ai.video.strategy;

import com.sun.aivideo.ai.config.MockVideoProperties;
import com.sun.aivideo.ai.video.model.VideoGenerateRequest;
import com.sun.aivideo.ai.video.model.VideoSubmitResult;
import com.sun.aivideo.ai.video.model.VideoTaskResult;
import com.sun.aivideo.common.enums.VideoProvider;
import com.sun.aivideo.common.enums.VideoTaskState;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 视频生成策略：本地模拟。未配置 API Key 时使用，不产生真实费用。
 * 前 2 秒排队，之后"生成" aivideo.mock.generate-seconds 秒，然后返回示例视频。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MockVideoStrategy implements VideoGenerationStrategy {

    private static final long QUEUE_SECONDS = 2;

    private final MockVideoProperties properties;
    private final Map<String, MockTask> tasks = new ConcurrentHashMap<>();

    private record MockTask(LocalDateTime submittedAt, int duration) {
    }

    @Override
    public VideoProvider provider() {
        return VideoProvider.MOCK;
    }

    @Override
    public VideoSubmitResult submit(VideoGenerateRequest request) {
        String taskId = "mock-" + UUID.randomUUID();
        tasks.put(taskId, new MockTask(LocalDateTime.now(), request.getDuration()));
        log.info("[Mock] 视频任务已创建 taskId={} model={} bizTaskId={}", taskId, request.getModelCode(), request.getBizTaskId());
        return new VideoSubmitResult(taskId, VideoTaskState.PENDING);
    }

    @Override
    public VideoTaskResult query(String providerTaskId) {
        MockTask task = tasks.get(providerTaskId);
        LocalDateTime now = LocalDateTime.now();
        if (task == null) {
            // 服务重启后内存丢失，直接视为完成
            task = new MockTask(now.minusSeconds(QUEUE_SECONDS + properties.getGenerateSeconds()), 5);
        }
        LocalDateTime scheduled = task.submittedAt().plusSeconds(QUEUE_SECONDS);
        LocalDateTime end = scheduled.plusSeconds(properties.getGenerateSeconds());

        VideoTaskResult.VideoTaskResultBuilder result = VideoTaskResult.builder().submitTime(task.submittedAt());
        if (now.isBefore(scheduled)) {
            return result.state(VideoTaskState.PENDING).build();
        }
        result.scheduledTime(scheduled);
        if (now.isBefore(end)) {
            return result.state(VideoTaskState.RUNNING).build();
        }
        tasks.remove(providerTaskId);
        return result.state(VideoTaskState.SUCCEEDED)
                .endTime(end)
                .videoUrl(properties.getVideoUrl())
                .billedDuration(task.duration())
                .build();
    }
}
