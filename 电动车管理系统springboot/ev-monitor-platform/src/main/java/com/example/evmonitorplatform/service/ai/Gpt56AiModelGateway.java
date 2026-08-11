package com.example.evmonitorplatform.service.ai;

import com.example.evmonitorplatform.common.exception.BusinessException;
import com.example.evmonitorplatform.config.AiGovernanceProperties;
import com.example.evmonitorplatform.entity.AiEvent;
import com.example.evmonitorplatform.vo.response.AiModelDecision;
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
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class Gpt56AiModelGateway implements AiModelGateway {

    private final AiGovernanceProperties properties;
    private final ObjectMapper objectMapper;

    @Override
    public AiModelDecision analyze(AiEvent event) {
        if (!StringUtils.hasText(properties.getApiKey())) {
            throw new BusinessException("AI_MODE=gpt 时必须配置 AI_API_KEY");
        }
        long started = System.nanoTime();
        try {
            boolean responses = !"chat-completions".equalsIgnoreCase(properties.getApiStyle());
            String endpoint = trimSlash(properties.getBaseUrl()) + (responses ? "/responses" : "/chat/completions");
            Map<String, Object> payload = responses ? responsesPayload(event) : chatPayload(event);
            String requestJson = objectMapper.writeValueAsString(payload);

            HttpClient client = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(Math.min(properties.getTimeoutSeconds(), 20)))
                    .build();
            HttpRequest request = HttpRequest.newBuilder(URI.create(endpoint))
                    .timeout(Duration.ofSeconds(properties.getTimeoutSeconds()))
                    .header("Authorization", "Bearer " + properties.getApiKey())
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(requestJson))
                    .build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new BusinessException("GPT兼容接口调用失败，HTTP " + response.statusCode());
            }

            JsonNode root = objectMapper.readTree(response.body());
            String content = responses ? extractResponsesText(root) : root.path("choices").path(0).path("message").path("content").asText();
            if (!StringUtils.hasText(content)) {
                throw new BusinessException("GPT兼容接口未返回可解析的研判结果");
            }
            AiModelDecision decision = objectMapper.readValue(stripCodeFence(content), AiModelDecision.class);
            validateDecision(decision);
            decision.setProvider("openai-compatible");
            decision.setModelName(root.path("model").asText(properties.getModel()));
            decision.setRawResponse(response.body());
            decision.setLatencyMs((int) ((System.nanoTime() - started) / 1_000_000L));
            JsonNode usage = root.path("usage");
            decision.setInputTokens(intOrNull(usage, responses ? "input_tokens" : "prompt_tokens"));
            decision.setOutputTokens(intOrNull(usage, responses ? "output_tokens" : "completion_tokens"));
            return decision;
        } catch (BusinessException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new BusinessException("GPT研判失败: " + ex.getMessage());
        }
    }

    private Map<String, Object> responsesPayload(AiEvent event) {
        List<Map<String, Object>> content = new ArrayList<>();
        content.add(Map.of("type", "input_text", "text", eventPrompt(event)));
        firstRemoteEvidence(event).ifPresent(url -> content.add(Map.of("type", "input_image", "image_url", url)));
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("model", properties.getModel());
        payload.put("reasoning", Map.of("effort", properties.getReasoningEffort()));
        payload.put("instructions", systemPrompt());
        payload.put("input", List.of(Map.of("role", "user", "content", content)));
        return payload;
    }

    private Map<String, Object> chatPayload(AiEvent event) {
        List<Map<String, Object>> userContent = new ArrayList<>();
        userContent.add(Map.of("type", "text", "text", eventPrompt(event)));
        firstRemoteEvidence(event).ifPresent(url -> userContent.add(Map.of("type", "image_url", "image_url", Map.of("url", url))));
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("model", properties.getModel());
        payload.put("reasoning_effort", properties.getReasoningEffort());
        payload.put("response_format", Map.of("type", "json_object"));
        payload.put("messages", List.of(
                Map.of("role", "system", "content", systemPrompt()),
                Map.of("role", "user", "content", userContent)
        ));
        return payload;
    }

    private String systemPrompt() {
        return "你是校园电动车治理事件研判器。只返回JSON，不执行处罚。"
                + "字段必须包含valid,eventType,confidence,riskLevel,identityReliable,evidenceSufficient,"
                + "summary,recommendedAction,notificationText,observations。"
                + "confidence范围0到1，riskLevel仅允许LOW、MEDIUM、HIGH、CRITICAL。";
    }

    private String eventPrompt(AiEvent event) {
        return "请研判事件：type=" + event.getEventType()
                + ", source=" + event.getSourceType()
                + ", device=" + event.getDeviceId()
                + ", plate=" + event.getLicensePlate()
                + ", location=" + event.getLocationName()
                + ", detectionConfidence=" + event.getDetectionConfidence()
                + ", identityConfidence=" + event.getIdentityConfidence()
                + ", occurredAt=" + event.getOccurredAt() + "。";
    }

    private java.util.Optional<String> firstRemoteEvidence(AiEvent event) {
        if (!StringUtils.hasText(event.getEvidenceUrls())) {
            return java.util.Optional.empty();
        }
        try {
            JsonNode array = objectMapper.readTree(event.getEvidenceUrls());
            if (array.isArray() && !array.isEmpty()) {
                String value = array.get(0).asText();
                if (value.startsWith("https://") || value.startsWith("http://") || value.startsWith("data:")) {
                    return java.util.Optional.of(value);
                }
            }
        } catch (Exception ignored) {
            // Non-JSON evidence is omitted from the remote request.
        }
        return java.util.Optional.empty();
    }

    private String extractResponsesText(JsonNode root) {
        if (root.hasNonNull("output_text")) {
            return root.path("output_text").asText();
        }
        for (JsonNode output : root.path("output")) {
            for (JsonNode content : output.path("content")) {
                if (content.hasNonNull("text")) {
                    return content.path("text").asText();
                }
            }
        }
        return null;
    }

    private void validateDecision(AiModelDecision decision) {
        if (decision.getConfidence() < 0 || decision.getConfidence() > 1) {
            throw new BusinessException("GPT返回的confidence不在0到1之间");
        }
        String risk = decision.getRiskLevel() == null ? "" : decision.getRiskLevel().toUpperCase(Locale.ROOT);
        if (!List.of("LOW", "MEDIUM", "HIGH", "CRITICAL").contains(risk)) {
            throw new BusinessException("GPT返回了不支持的riskLevel");
        }
        decision.setRiskLevel(risk);
        if (!StringUtils.hasText(decision.getSummary()) || !StringUtils.hasText(decision.getRecommendedAction())) {
            throw new BusinessException("GPT返回结果缺少summary或recommendedAction");
        }
    }

    private Integer intOrNull(JsonNode node, String name) {
        return node.has(name) && node.path(name).canConvertToInt() ? node.path(name).asInt() : null;
    }

    private String stripCodeFence(String value) {
        String text = value.trim();
        if (text.startsWith("```")) {
            int firstNewline = text.indexOf('\n');
            int lastFence = text.lastIndexOf("```");
            if (firstNewline >= 0 && lastFence > firstNewline) {
                return text.substring(firstNewline + 1, lastFence).trim();
            }
        }
        return text;
    }

    private String trimSlash(String value) {
        return value != null && value.endsWith("/") ? value.substring(0, value.length() - 1) : value;
    }
}
