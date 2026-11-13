package com.sun.aivideo;

import cn.dev33.satoken.stp.StpUtil;
import com.sun.aivideo.ai.config.MockVideoProperties;
import com.sun.aivideo.ai.llm.PromptEnhancer;
import com.sun.aivideo.controller.ChannelCredentialController;
import com.sun.aivideo.handler.GlobalExceptionHandler;
import com.sun.aivideo.service.ChannelCredentialService;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class ChannelCredentialControllerTest {
    @Test void suppliedUserIdCannotOverrideAuthenticatedOwnerAndResponsesOnlyContainSummary() throws Exception {
        var service = mock(ChannelCredentialService.class);
        when(service.list(7L)).thenReturn(List.of(new ChannelCredentialService.Summary("DASHSCOPE", "百炼", true, true, "••••1234", null)));
        var controller = new ChannelCredentialController(service, mock(PromptEnhancer.class), new MockVideoProperties());
        var mvc = MockMvcBuilders.standaloneSetup(controller).setControllerAdvice(new GlobalExceptionHandler()).build();
        try (var session = mockStatic(StpUtil.class)) {
            session.when(StpUtil::getLoginIdAsLong).thenReturn(7L);
            var saved = mvc.perform(put("/api/channel-credentials/DASHSCOPE").contentType(MediaType.APPLICATION_JSON)
                    .content("{\"apiKey\":\"private-example-key\",\"userId\":999}"))
                    .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
            verify(service).save(7L, "DASHSCOPE", "private-example-key");
            assertFalse(saved.contains("private-example-key"));
            var listed = mvc.perform(get("/api/channel-credentials").param("userId", "999"))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.channels[0].maskedKey").value("••••1234"))
                    .andReturn().getResponse().getContentAsString();
            verify(service).list(7L);
            assertFalse(listed.contains("apiKey"));
            assertFalse(listed.contains("ciphertext"));
            assertFalse(listed.contains("currentVersionId"));
        }
    }

    @Test void invalidKeyValidationDoesNotEchoSuppliedSecret() throws Exception {
        var service = mock(ChannelCredentialService.class);
        var controller = new ChannelCredentialController(service, mock(PromptEnhancer.class), new MockVideoProperties());
        var mvc = MockMvcBuilders.standaloneSetup(controller).setControllerAdvice(new GlobalExceptionHandler()).build();
        String secret = "private-secret-".repeat(160);
        String response = mvc.perform(put("/api/channel-credentials/DASHSCOPE").contentType(MediaType.APPLICATION_JSON)
                .content("{\"apiKey\":\"" + secret + "\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.code").value(400))
                .andReturn().getResponse().getContentAsString();
        assertFalse(response.contains("private-secret-"));
        verifyNoInteractions(service);
    }
}
