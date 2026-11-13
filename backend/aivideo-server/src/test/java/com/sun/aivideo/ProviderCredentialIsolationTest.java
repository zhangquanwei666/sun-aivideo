package com.sun.aivideo;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.aivideo.ai.auth.ChannelCredentials;
import com.sun.aivideo.ai.dashscope.config.DashScopeProperties;
import com.sun.aivideo.ai.dashscope.image.DashScopeWanImageStrategy;
import com.sun.aivideo.ai.dashscope.video.DashScopeWanVideoStrategy;
import com.sun.aivideo.ai.gemini.config.GeminiProperties;
import com.sun.aivideo.ai.gemini.image.GeminiNanoBananaStrategy;
import com.sun.aivideo.ai.gemini.video.GeminiOmniVideoStrategy;
import com.sun.aivideo.ai.image.model.ImageGenerateRequest;
import com.sun.aivideo.ai.video.model.VideoGenerateRequest;
import com.sun.aivideo.ai.exception.AiClientException;
import com.sun.aivideo.common.enums.*;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import java.util.concurrent.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

class ProviderCredentialIsolationTest {
    private final ObjectMapper json = new ObjectMapper();
    private ChannelCredentials key(AiChannel channel, String user) { return new ChannelCredentials(channel, user + "-private-key"); }

    @Test void concurrentVideoSubmissionsAndPollingSendEachUsersKey() throws Exception {
        var builder = RestClient.builder().baseUrl("https://provider.test");
        var server = MockRestServiceServer.bindTo(builder).ignoreExpectOrder(true).build();
        for (String user : new String[]{"alice", "bob"}) {
            server.expect(requestTo("https://provider.test/api/v1/services/aigc/video-generation/video-synthesis"))
                    .andExpect(header("Authorization", "Bearer " + user + "-private-key"))
                    .andRespond(withSuccess("{\"output\":{\"task_id\":\"" + user + "\"}}", MediaType.APPLICATION_JSON));
            server.expect(requestTo("https://provider.test/api/v1/tasks/" + user))
                    .andExpect(header("Authorization", "Bearer " + user + "-private-key"))
                    .andRespond(withSuccess("{\"output\":{\"task_status\":\"RUNNING\"}}", MediaType.APPLICATION_JSON));
        }
        var strategy = new DashScopeWanVideoStrategy(builder.build(), json);
        try (var pool = Executors.newFixedThreadPool(2)) {
            var futures = new java.util.ArrayList<Future<?>>();
            for (String user : new String[]{"alice", "bob"}) futures.add(pool.submit(() -> {
                var result = strategy.submit(video(), key(AiChannel.DASHSCOPE, user));
                assertEquals(user, result.getProviderTaskId());
                assertEquals(VideoTaskState.RUNNING, strategy.query(user, key(AiChannel.DASHSCOPE, user)).getState());
            }));
            for (var future : futures) future.get(10, TimeUnit.SECONDS);
        }
        server.verify();
    }

    @Test void bothImageProvidersUseRequestCredentials() {
        var builder = RestClient.builder().baseUrl("https://provider.test");
        var server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo("https://provider.test/api/v1/services/aigc/multimodal-generation/generation"))
                .andExpect(header("Authorization", "Bearer alice-private-key"))
                .andRespond(withSuccess("{\"output\":{\"choices\":[{\"message\":{\"content\":[{\"image\":\"https://files.test/image.png\"}]}}]}}", MediaType.APPLICATION_JSON));
        server.expect(requestTo("https://provider.test/models/example:generateContent"))
                .andExpect(header("x-goog-api-key", "bob-private-key"))
                .andRespond(withSuccess("{\"candidates\":[{\"content\":{\"parts\":[{\"inlineData\":{\"data\":\"dGVzdA==\"}}]}}]}", MediaType.APPLICATION_JSON));
        RestClient client = builder.build();
        var req = ImageGenerateRequest.builder().modelCode("example").prompt("test").imageSize("1K").aspectRatio("1:1").build();
        assertEquals(1, new DashScopeWanImageStrategy(client, json).generate(req, key(AiChannel.DASHSCOPE, "alice")).getImages().size());
        assertEquals(1, new GeminiNanoBananaStrategy(client, json).generate(req, key(AiChannel.GEMINI, "bob")).getImages().size());
        server.verify();
    }

    @Test void geminiRecoveryAndAuthenticatedDownloadKeepTheSameKeyAndRejectForeignOrigin() {
        var builder = RestClient.builder().baseUrl("https://provider.test");
        var server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo("https://provider.test/interactions/task"))
                .andExpect(header("x-goog-api-key", "alice-private-key"))
                .andRespond(withSuccess("{\"status\":\"completed\",\"steps\":[{\"content\":[{\"type\":\"video\",\"uri\":\"https://provider.test/files/video\"}]}]}", MediaType.APPLICATION_JSON));
        server.expect(requestTo("https://provider.test/files/video"))
                .andExpect(header("x-goog-api-key", "alice-private-key"))
                .andRespond(withSuccess(new byte[]{1,2,3}, MediaType.APPLICATION_OCTET_STREAM));
        server.expect(requestTo("https://provider.test/interactions/evil"))
                .andRespond(withSuccess("{\"status\":\"completed\",\"steps\":[{\"content\":[{\"type\":\"video\",\"uri\":\"https://evil.test/video\"}]}]}", MediaType.APPLICATION_JSON));
        var properties = new GeminiProperties(); properties.setVideoBaseUrl("https://provider.test");
        var strategy = new GeminiOmniVideoStrategy(builder.build(), json, properties);
        assertArrayEquals(new byte[]{1,2,3}, strategy.query("task", key(AiChannel.GEMINI, "alice")).getVideoData());
        assertEquals("UNTRUSTED_DOWNLOAD", assertThrows(AiClientException.class, () -> strategy.query("evil", key(AiChannel.GEMINI, "alice"))).getErrorCode());
        server.verify();
    }

    @Test void geminiSubmitCachesResultsSeparatelyForEachCredential() {
        var builder = RestClient.builder().baseUrl("https://provider.test");
        var server = MockRestServiceServer.bindTo(builder).build();
        String[] users = {"alice", "bob"};
        for (int i = 0; i < users.length; i++) {
            String data = java.util.Base64.getEncoder().encodeToString(new byte[]{(byte) i});
            server.expect(requestTo("https://provider.test/interactions"))
                    .andExpect(header("x-goog-api-key", users[i] + "-private-key"))
                    .andRespond(withSuccess("{\"id\":\"same-task\",\"status\":\"completed\",\"steps\":[{\"content\":[{\"type\":\"video\",\"data\":\"" + data + "\"}]}]}", MediaType.APPLICATION_JSON));
        }
        var properties = new GeminiProperties(); properties.setVideoBaseUrl("https://provider.test");
        var strategy = new GeminiOmniVideoStrategy(builder.build(), json, properties);
        for (String user : users) assertEquals("same-task", strategy.submit(video(), key(AiChannel.GEMINI, user)).getProviderTaskId());
        assertArrayEquals(new byte[]{0}, strategy.query("same-task", key(AiChannel.GEMINI, "alice")).getVideoData());
        assertArrayEquals(new byte[]{1}, strategy.query("same-task", key(AiChannel.GEMINI, "bob")).getVideoData());
        server.verify();
    }

    @Test void vendorErrorsNeverEchoKeyOrPayload() {
        var builder = RestClient.builder().baseUrl("https://provider.test");
        var server = MockRestServiceServer.bindTo(builder).build();
        server.expect(anything()).andRespond(withUnauthorizedRequest().body("alice-private-key"));
        var strategy = new DashScopeWanVideoStrategy(builder.build(), json);
        var error = assertThrows(AiClientException.class, () -> strategy.submit(video(), key(AiChannel.DASHSCOPE, "alice")));
        assertEquals("KEY_INVALID", error.getErrorCode()); assertFalse(error.getMessage().contains("alice-private-key"));
        assertNull(error.getCause()); assertFalse(key(AiChannel.DASHSCOPE, "alice").toString().contains("alice"));
        server.verify();
    }

    private VideoGenerateRequest video() {
        return VideoGenerateRequest.builder().modelCode("example").protocol(ApiProtocol.WAN27_T2V)
                .prompt("test").resolution("720P").ratio("16:9").duration(5).build();
    }

    @Test void qwenUsesEachCallsPersonalKeyWithoutRetainingThePreviousAccount() throws Exception {
        var received = new java.util.concurrent.CopyOnWriteArrayList<String>();
        var server = com.sun.net.httpserver.HttpServer.create(new java.net.InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/chat/completions", exchange -> {
            received.add(exchange.getRequestHeaders().getFirst("Authorization"));
            exchange.getRequestBody().readAllBytes();
            byte[] data = "{\"id\":\"test\",\"object\":\"chat.completion\",\"created\":1,\"model\":\"qwen-plus\",\"choices\":[{\"index\":0,\"message\":{\"role\":\"assistant\",\"content\":\"enhanced\"},\"finish_reason\":\"stop\"}],\"usage\":{\"prompt_tokens\":2,\"completion_tokens\":1,\"total_tokens\":3}}".getBytes(java.nio.charset.StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, data.length);
            try (var out = exchange.getResponseBody()) { out.write(data); }
        });
        server.start();
        try {
            var properties = new com.sun.aivideo.ai.config.LlmProperties();
            properties.setBaseUrl("http://127.0.0.1:" + server.getAddress().getPort());
            var strategy = new com.sun.aivideo.ai.dashscope.llm.QwenLlmStrategy(properties);
            var request = com.sun.aivideo.ai.llm.model.LlmChatRequest.builder().userPrompt("test").build();
            assertEquals("enhanced", strategy.chat(request, key(AiChannel.DASHSCOPE, "alice")).getContent());
            assertEquals("enhanced", strategy.chat(request, key(AiChannel.DASHSCOPE, "bob")).getContent());
            assertEquals(java.util.List.of("Bearer alice-private-key", "Bearer bob-private-key"), received);
        } finally { server.stop(0); }
    }
}
