package com.sun.aivideo.controller;

import com.sun.aivideo.ai.dashscope.config.DashScopeProperties;
import com.sun.aivideo.ai.config.MockVideoProperties;
import com.sun.aivideo.ai.llm.PromptEnhancer;
import com.sun.aivideo.common.base.BaseResponse;
import com.sun.aivideo.config.AiVideoProperties;
import com.sun.aivideo.config.EnvironmentChecker;
import com.sun.aivideo.model.vo.SystemStatusVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/system")
@RequiredArgsConstructor
public class SystemController {

    private final AiVideoProperties properties;
    private final DashScopeProperties dashScopeProperties;
    private final MockVideoProperties mockProperties;
    private final PromptEnhancer promptEnhancer;
    private final EnvironmentChecker environmentChecker;

    /** 前端据此显示当前环境、是否为 Mock 模式、润色是否可用 */
    @GetMapping("/status")
    public SystemStatusVO status() {
        SystemStatusVO vo = new SystemStatusVO();
        vo.setEnv(environmentChecker.currentEnv());
        vo.setMockMode(!dashScopeProperties.hasApiKey() && mockProperties.isWhenNoKey());
        vo.setLlmAvailable(promptEnhancer.available());
        vo.setLlmModel(promptEnhancer.currentModelName());
        vo.setPollingIntervalMs(properties.getPolling().getIntervalMs());
        return BaseResponse.ok(vo);
    }
}
