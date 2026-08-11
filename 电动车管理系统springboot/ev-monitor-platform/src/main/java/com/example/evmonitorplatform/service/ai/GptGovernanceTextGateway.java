package com.example.evmonitorplatform.service.ai;

import com.example.evmonitorplatform.common.exception.BusinessException;
import com.example.evmonitorplatform.config.AiGovernanceProperties;
import com.example.evmonitorplatform.vo.response.AiTextResult;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class GptGovernanceTextGateway implements GovernanceTextGateway {
    private final AiGovernanceProperties properties;
    private final ObjectMapper objectMapper;

    @Override
    public AiTextResult complete(String instructions, String prompt) {
        if (!StringUtils.hasText(properties.getApiKey())) throw new BusinessException("未配置AI API Key");
        long started = System.nanoTime();
        try {
            boolean responses = !"chat-completions".equalsIgnoreCase(properties.getApiStyle());
            String endpoint = trimSlash(properties.getBaseUrl()) + (responses ? "/responses" : "/chat/completions");
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("model", properties.getModel());
            if (responses) {
                payload.put("instructions", instructions);
                payload.put("input", prompt);
                payload.put("reasoning", Map.of("effort", properties.getReasoningEffort()));
            } else {
                payload.put("messages", List.of(
                        Map.of("role", "system", "content", instructions),
                        Map.of("role", "user", "content", prompt)));
                payload.put("reasoning_effort", properties.getReasoningEffort());
            }
            HttpClient client = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(Math.min(properties.getTimeoutSeconds(), 20))).build();
            HttpRequest request = HttpRequest.newBuilder(URI.create(endpoint))
                    .timeout(Duration.ofSeconds(properties.getTimeoutSeconds()))
                    .header("Authorization", "Bearer " + properties.getApiKey())
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(payload))).build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new BusinessException("AI文本接口调用失败，HTTP " + response.statusCode());
            }
            JsonNode root = objectMapper.readTree(response.body());
            String text = responses ? extractResponsesText(root)
                    : root.path("choices").path(0).path("message").path("content").asText();
            if (!StringUtils.hasText(text)) throw new BusinessException("AI文本接口未返回内容");
            return new AiTextResult(text.trim(), "openai-compatible",
                    root.path("model").asText(properties.getModel()),
                    (int) ((System.nanoTime() - started) / 1_000_000L), response.body());
        } catch (BusinessException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new BusinessException("AI文本生成失败: " + ex.getMessage());
        }
    }

    private String extractResponsesText(JsonNode root) {
        if (root.hasNonNull("output_text")) return root.path("output_text").asText();
        for (JsonNode output : root.path("output")) {
            for (JsonNode content : output.path("content")) {
                if (content.hasNonNull("text")) return content.path("text").asText();
            }
        }
        return null;
    }

    private String trimSlash(String value) {
        return value != null && value.endsWith("/") ? value.substring(0, value.length() - 1) : value;
    }
}
