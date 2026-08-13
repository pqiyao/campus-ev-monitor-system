package com.example.evmonitorplatform.service.ai;

import com.example.evmonitorplatform.common.exception.BusinessException;
import com.example.evmonitorplatform.config.AiGovernanceProperties;
import com.example.evmonitorplatform.entity.AiEvent;
import com.example.evmonitorplatform.vo.response.AiModelDecision;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.concurrent.atomic.AtomicReference;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class Gpt56AiModelGatewayTest {
    private HttpServer server;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @AfterEach
    void stopServer() {
        if (server != null) server.stop(0);
    }

    @Test
    void parsesResponsesApiAndSendsMultimodalPayload() throws Exception {
        AtomicReference<String> authorization = new AtomicReference<>();
        AtomicReference<JsonNode> requestBody = new AtomicReference<>();
        String decisionJson = objectMapper.writeValueAsString(decisionPayload(0.96D, "HIGH", true));
        String response = objectMapper.writeValueAsString(Map.of(
                "model", "gpt-5.6",
                "output_text", decisionJson,
                "usage", Map.of("input_tokens", 123, "output_tokens", 45)));
        startServer("/v1/responses", authorization, requestBody, response);
        AiGovernanceProperties properties = properties("responses");
        AiModelDecision decision = new Gpt56AiModelGateway(properties, objectMapper).analyze(event());

        assertEquals("Bearer test-key", authorization.get());
        assertEquals("gpt-5.6", requestBody.get().path("model").asText());
        assertTrue(requestBody.get().toString().contains("input_image"));
        assertEquals(0.96D, decision.getConfidence());
        assertEquals("湘A12345", decision.getRecognizedPlate());
        assertEquals(0.97D, decision.getPlateConfidence());
        assertEquals(123, decision.getInputTokens());
        assertEquals("openai-compatible", decision.getProvider());
    }

    @Test
    void parsesChatCompletionsFallback() throws Exception {
        AtomicReference<String> authorization = new AtomicReference<>();
        AtomicReference<JsonNode> requestBody = new AtomicReference<>();
        String decisionJson = objectMapper.writeValueAsString(decisionPayload(0.88D, "MEDIUM", false));
        String response = objectMapper.writeValueAsString(Map.of(
                "model", "gpt-5.6",
                "choices", List.of(Map.of("message", Map.of("content", decisionJson))),
                "usage", Map.of("prompt_tokens", 80, "completion_tokens", 20)));
        startServer("/v1/chat/completions", authorization, requestBody, response);
        AiGovernanceProperties properties = properties("chat-completions");
        AiModelDecision decision = new Gpt56AiModelGateway(properties, objectMapper).analyze(event());

        assertEquals("json_object", requestBody.get().path("response_format").path("type").asText());
        assertEquals(0.88D, decision.getConfidence());
        assertEquals(80, decision.getInputTokens());
        assertEquals(20, decision.getOutputTokens());
    }

    @Test
    void refusesOnlineModeWithoutApiKey() {
        AiGovernanceProperties properties = new AiGovernanceProperties();
        properties.setApiKey("");
        Gpt56AiModelGateway gateway = new Gpt56AiModelGateway(properties, objectMapper);
        assertThrows(BusinessException.class, () -> gateway.analyze(event()));
    }

    private void startServer(String path, AtomicReference<String> authorization,
                             AtomicReference<JsonNode> requestBody, String response) throws Exception {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext(path, exchange -> {
            authorization.set(exchange.getRequestHeaders().getFirst("Authorization"));
            requestBody.set(objectMapper.readTree(exchange.getRequestBody()));
            byte[] bytes = response.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, bytes.length);
            exchange.getResponseBody().write(bytes);
            exchange.close();
        });
        server.start();
    }

    private AiGovernanceProperties properties(String apiStyle) {
        AiGovernanceProperties properties = new AiGovernanceProperties();
        properties.setBaseUrl("http://127.0.0.1:" + server.getAddress().getPort() + "/v1");
        properties.setApiKey("test-key");
        properties.setApiStyle(apiStyle);
        properties.setTimeoutSeconds(5);
        return properties;
    }

    private Map<String, Object> decisionPayload(double confidence, String risk, boolean identityReliable) {
        return Map.ofEntries(
                Map.entry("valid", true),
                Map.entry("eventType", "NO_HELMET"),
                Map.entry("confidence", confidence),
                Map.entry("riskLevel", risk),
                Map.entry("identityReliable", identityReliable),
                Map.entry("evidenceSufficient", true),
                Map.entry("plateVisible", true),
                Map.entry("recognizedPlate", "湘A12345"),
                Map.entry("plateConfidence", 0.97D),
                Map.entry("summary", "需要复核"),
                Map.entry("recommendedAction", "CREATE_REVIEW_TASK"),
                Map.entry("notificationText", "请关注"),
                Map.entry("observations", List.of("连续帧一致")));
    }

    private AiEvent event() {
        AiEvent event = new AiEvent();
        event.setEventType("NO_HELMET");
        event.setSourceType("CAMERA");
        event.setDeviceId("camera-1");
        event.setOccurredAt(LocalDateTime.now());
        event.setLocationName("东校区正门");
        event.setLicensePlate("校A0001");
        event.setDetectionConfidence(0.94D);
        event.setIdentityConfidence(0.98D);
        event.setEvidenceUrls("[\"https://example.com/evidence.jpg\"]");
        return event;
    }
}
