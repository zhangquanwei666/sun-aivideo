package com.sun.aivideo.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.sun.aivideo.common.base.BaseResponse;
import com.sun.aivideo.service.ChannelCredentialService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController @RequestMapping("/api/channel-credentials") @RequiredArgsConstructor
public class ChannelCredentialController {
    private final ChannelCredentialService service;
    private final com.sun.aivideo.ai.llm.PromptEnhancer promptEnhancer;
    private final com.sun.aivideo.ai.config.MockVideoProperties mockProperties;
    @Getter @Setter public static class KeyInput {
        @NotBlank(message="请输入密钥") @Size(max=2048, message="密钥过长") private String apiKey;
    }
    public record EnabledInput(@NotNull Boolean enabled) {}
    @Getter @Setter public static class ListResponse extends BaseResponse {
        private List<ChannelCredentialService.Summary> channels;
    }
    @GetMapping public ListResponse list() {
        var response = new ListResponse(); response.setChannels(service.list(StpUtil.getLoginIdAsLong()));
        return BaseResponse.ok(response);
    }
    @GetMapping("/status") public com.sun.aivideo.model.vo.SystemStatusVO status() {
        var response = new com.sun.aivideo.model.vo.SystemStatusVO();
        response.setMockMode(mockProperties.isEnabled());
        response.setLlmAvailable(promptEnhancer.available() && (mockProperties.isEnabled()
                || service.configured(StpUtil.getLoginIdAsLong(), com.sun.aivideo.common.enums.AiChannel.DASHSCOPE)));
        response.setLlmModel(promptEnhancer.currentModelName());
        return BaseResponse.ok(response);
    }
    @PutMapping("/{channel}") public BaseResponse save(@PathVariable String channel, @Valid @RequestBody KeyInput input) {
        service.save(StpUtil.getLoginIdAsLong(), channel, input.getApiKey()); return BaseResponse.ok();
    }
    @PutMapping("/{channel}/enabled") public BaseResponse enabled(@PathVariable String channel, @Valid @RequestBody EnabledInput input) {
        service.setEnabled(StpUtil.getLoginIdAsLong(), channel, input.enabled()); return BaseResponse.ok();
    }
    @DeleteMapping("/{channel}") public BaseResponse delete(@PathVariable String channel) {
        service.delete(StpUtil.getLoginIdAsLong(), channel); return BaseResponse.ok();
    }
}
