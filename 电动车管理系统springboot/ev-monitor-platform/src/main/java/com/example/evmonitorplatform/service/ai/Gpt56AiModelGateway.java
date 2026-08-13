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
import org.springframework.beans.factory.annotation.Value;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
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
import java.util.Base64;

@Component
@RequiredArgsConstructor
public class Gpt56AiModelGateway implements AiModelGateway {

    private final AiGovernanceProperties properties;
    private final ObjectMapper objectMapper;

    @Value("${ev-monitor.file.upload-path}")
    private String uploadPath;

    @Override
    public AiModelDecision analyze(AiEvent event) {
        if (!StringUtils.hasText(properties.getApiKey())) {
            throw new BusinessException("在线AI能力未配置有效凭证");
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
                throw new BusinessException("多模态视觉分析服务调用失败，HTTP " + response.statusCode());
            }

            JsonNode root = objectMapper.readTree(response.body());
            String content = responses ? extractResponsesText(root) : root.path("choices").path(0).path("message").path("content").asText();
            if (!StringUtils.hasText(content)) {
                throw new BusinessException("AI视觉分析服务未返回可解析的研判结果");
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
            throw new BusinessException("AI视觉研判失败: " + ex.getMessage());
        }
    }

    private Map<String, Object> responsesPayload(AiEvent event) {
        List<Map<String, Object>> content = new ArrayList<>();
        content.add(Map.of("type", "input_text", "text", eventPrompt(event)));
        evidenceInputs(event).forEach(url -> content.add(Map.of("type", "input_image", "image_url", url)));
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
        evidenceInputs(event).forEach(url -> userContent.add(Map.of("type", "image_url", "image_url", Map.of("url", url))));
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
                + "plateVisible,recognizedPlate,plateConfidence,summary,recommendedAction,notificationText,observations。"
                + "请根据eventType核验证据图片：NO_HELMET检查未佩戴头盔；SMOKE_DETECTED检查是否存在持续烟雾；FIRE_DETECTED检查是否存在明火。"
                + "烟雾和明火可能受雾气、蒸汽、灯光和反射干扰，必须说明可见依据；它们属于安全风险事件，不得建议直接处罚或扣分。"
                + "仅在画面包含清晰车辆身份时读取与目标关联的校园电动车车牌。"
                + "无法清晰读取或存在多个候选车牌时，recognizedPlate必须为null且plateConfidence不得高于0.5。"
                + "车牌必须按图片原文返回，不得猜测；identityReliable仅表示车牌图像是否清晰可靠。"
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

    private List<String> evidenceInputs(AiEvent event) {
        if (!StringUtils.hasText(event.getEvidenceUrls())) {
            return List.of();
        }
        List<String> result = new ArrayList<>();
        try {
            JsonNode array = objectMapper.readTree(event.getEvidenceUrls());
            if (array.isArray()) {
                for (JsonNode item : array) {
                    if (result.size() >= 3) break;
                    String value = item.asText();
                    if (!StringUtils.hasText(value)) continue;
                    if (value.startsWith("/uploads/")) {
                        String dataUri = localUploadAsDataUri(value);
                        if (dataUri != null) result.add(dataUri);
                        continue;
                    }
                if (value.startsWith("https://") || value.startsWith("http://") || value.startsWith("data:")) {
                        result.add(value);
                    }
                }
            }
        } catch (Exception ignored) {
            // Invalid evidence entries are omitted from the model request.
        }
        return result;
    }

    private String localUploadAsDataUri(String url) {
        try {
            String relative = url.substring("/uploads/".length()).replace('/', java.io.File.separatorChar);
            Path root = Paths.get(uploadPath).toAbsolutePath().normalize();
            Path file = root.resolve(relative).normalize();
            if (!file.startsWith(root) || !Files.isRegularFile(file)) return null;
            byte[] bytes = Files.readAllBytes(file);
            if (bytes.length > 10 * 1024 * 1024) return null;
            String contentType = Files.probeContentType(file);
            if (!StringUtils.hasText(contentType) || !contentType.startsWith("image/")) contentType = "image/jpeg";
            return "data:" + contentType + ";base64," + Base64.getEncoder().encodeToString(bytes);
        } catch (Exception ex) {
            return null;
        }
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
            throw new BusinessException("AI返回的置信度不在0到1之间");
        }
        if (decision.getPlateConfidence() < 0 || decision.getPlateConfidence() > 1) {
            throw new BusinessException("AI返回的车牌置信度不在0到1之间");
        }
        String risk = decision.getRiskLevel() == null ? "" : decision.getRiskLevel().toUpperCase(Locale.ROOT);
        if (!List.of("LOW", "MEDIUM", "HIGH", "CRITICAL").contains(risk)) {
            throw new BusinessException("AI返回了不支持的风险等级");
        }
        decision.setRiskLevel(risk);
        if (!StringUtils.hasText(decision.getSummary()) || !StringUtils.hasText(decision.getRecommendedAction())) {
            throw new BusinessException("AI研判结果缺少必要字段");
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
