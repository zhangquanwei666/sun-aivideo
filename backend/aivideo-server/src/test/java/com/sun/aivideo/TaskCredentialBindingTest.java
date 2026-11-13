package com.sun.aivideo;

import cn.dev33.satoken.stp.StpUtil;
import com.sun.aivideo.ai.llm.PromptEnhancer;
import com.sun.aivideo.ai.video.VideoGenerationStrategyFactory;
import com.sun.aivideo.ai.video.strategy.VideoGenerationStrategy;
import com.sun.aivideo.common.enums.*;
import com.sun.aivideo.common.exception.BizException;
import com.sun.aivideo.dao.*;
import com.sun.aivideo.entity.*;
import com.sun.aivideo.model.bo.VideoCostBO;
import com.sun.aivideo.model.dto.VideoGenerateDTO;
import com.sun.aivideo.service.*;
import com.sun.aivideo.service.impl.VideoGenerationServiceImpl;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionSynchronization;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import java.math.BigDecimal;

class TaskCredentialBindingTest {
    @Test void missingOwnKeyRejectsBeforeTaskInsertionAndDoesNotFallBack() {
        var f = new Fixture();
        when(f.credentials.requiredVersion(7L, "DASHSCOPE")).thenThrow(new BizException(ErrorCode.CHANNEL_KEY_REQUIRED, "百炼"));
        try (var session = mockStatic(StpUtil.class)) {
            session.when(StpUtil::getLoginIdAsLong).thenReturn(7L);
            assertThrows(BizException.class, () -> f.service.create(f.request()));
            verify(f.tasks, never()).insert(any(VideoTask.class));
            verifyNoInteractions(f.worker);
        }
    }

    @Test void taskPinsBothVersionsAndWaitsUntilTransactionCommitToStart() {
        var f = new Fixture();
        when(f.credentials.requiredVersion(7L, "DASHSCOPE")).thenReturn(41L);
        when(f.prompts.available()).thenReturn(true);
        doAnswer(i -> { i.<VideoTask>getArgument(0).setId(90L); return 1; }).when(f.tasks).insert(any(VideoTask.class));
        TransactionSynchronizationManager.initSynchronization();
        try (var session = mockStatic(StpUtil.class)) {
            session.when(StpUtil::getLoginIdAsLong).thenReturn(7L);
            var dto = f.request(); dto.setEnhancePrompt(true);
            f.service.create(dto);
            ArgumentCaptor<VideoTask> captured = ArgumentCaptor.forClass(VideoTask.class);
            verify(f.tasks).insert(captured.capture());
            assertEquals(7L, captured.getValue().getUserId());
            assertEquals(41L, captured.getValue().getCredentialVersionId());
            assertEquals(41L, captured.getValue().getLlmCredentialVersionId());
            verifyNoInteractions(f.worker);
            for (TransactionSynchronization sync : TransactionSynchronizationManager.getSynchronizations()) sync.afterCommit();
            verify(f.worker).submitAsync(90L);
        } finally { TransactionSynchronizationManager.clearSynchronization(); }
    }

    private static class Fixture {
        final VideoTaskMapper tasks = mock(VideoTaskMapper.class);
        final ModelConfigService models = mock(ModelConfigService.class);
        final PromptEnhancer prompts = mock(PromptEnhancer.class);
        final CostService cost = mock(CostService.class);
        final VideoGenerationStrategyFactory factory = mock(VideoGenerationStrategyFactory.class);
        final VideoTaskWorker worker = mock(VideoTaskWorker.class);
        final ChannelCredentialService credentials = mock(ChannelCredentialService.class);
        final TaskDispatch dispatch = new TaskDispatch(mock(org.springframework.transaction.PlatformTransactionManager.class));
        final VideoGenerationServiceImpl service = new VideoGenerationServiceImpl(tasks, mock(ModelConfigMapper.class), models,
                prompts, cost, factory, worker, credentials, dispatch);
        Fixture() {
            var model = new ModelConfig(); model.setModelCode("example"); model.setMode("T2V"); model.setProvider("DASHSCOPE");
            model.setProtocol("WAN27_T2V"); model.setResolutions("720P"); model.setRatios("16:9");
            model.setMinDuration(1); model.setMaxDuration(10); model.setCurrency("CNY");
            when(models.getEnabledByCode("example")).thenReturn(model);
            when(cost.unitPrice(model, "720P")).thenReturn(BigDecimal.ONE);
            when(cost.calcVideoCost(model, "720P", 5)).thenReturn(new VideoCostBO("CNY", BigDecimal.ONE, 5, BigDecimal.valueOf(5)));
            when(factory.getStrategy("DASHSCOPE")).thenReturn(mock(VideoGenerationStrategy.class));
        }
        VideoGenerateDTO request() {
            var dto = new VideoGenerateDTO(); dto.setModelCode("example"); dto.setPrompt("test");
            dto.setResolution("720P"); dto.setRatio("16:9"); dto.setDuration(5); return dto;
        }
    }
}
