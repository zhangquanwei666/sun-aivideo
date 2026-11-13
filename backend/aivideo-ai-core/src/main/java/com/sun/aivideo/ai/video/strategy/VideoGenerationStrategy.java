package com.sun.aivideo.ai.video.strategy;

import com.sun.aivideo.ai.auth.ChannelCredentials;
import com.sun.aivideo.ai.video.model.VideoGenerateRequest;
import com.sun.aivideo.ai.video.model.VideoSubmitResult;
import com.sun.aivideo.ai.video.model.VideoTaskResult;
import com.sun.aivideo.common.enums.VideoProvider;

/**
 * 视频生成策略：每个厂商一个实现。
 * 各家视频 API 基本都是"异步创建任务 + 轮询结果"，统一成 submit / query 两个动作。
 * 新增厂商：实现本接口并声明为 Spring Bean，{@link com.sun.aivideo.ai.video.VideoGenerationStrategyFactory} 会自动收录。
 */
public interface VideoGenerationStrategy {

    /** 策略对应的厂商（工厂按此注册） */
    VideoProvider provider();

    /** 创建生成任务，返回厂商任务 ID */
    VideoSubmitResult submit(VideoGenerateRequest request, ChannelCredentials credentials);

    /** 查询任务状态 */
    VideoTaskResult query(String providerTaskId, ChannelCredentials credentials);
}
