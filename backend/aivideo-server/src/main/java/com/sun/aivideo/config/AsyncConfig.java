package com.sun.aivideo.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

@Configuration
public class AsyncConfig {

    public static final String VIDEO_EXECUTOR = "videoTaskExecutor";

    /** 提交视频任务、润色提示词、下载视频等耗时操作使用的线程池 */
    @Bean(VIDEO_EXECUTOR)
    public Executor videoTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(4);
        executor.setMaxPoolSize(16);
        executor.setQueueCapacity(200);
        executor.setThreadNamePrefix("video-task-");
        // 满载时明确拒绝，不能在提交事务的 afterCommit 线程内执行长任务。
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.AbortPolicy());
        executor.initialize();
        return executor;
    }
}
