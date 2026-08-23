package com.example.evmonitorplatform.service.ai;

import com.example.evmonitorplatform.config.AiGovernanceProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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

/**
 * Turns a natural-language patrol goal into a bounded, auditable plan. The
 * model is advisory; route safety constraints remain enforced by PatrolService.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PatrolGoalPlanner {
    private final AiGovernanceProperties properties;
    private final ObjectMapper objectMapper;

    public PlannerResult plan(String goal, String area, int durationLimitSec) {
        long started = System.nanoTime();
        if ("gpt".equalsIgnoreCase(properties.getMode()) && StringUtils.hasText(properties.getApiKey())) {
            try {
                Map<String, Object> parsed = requestModel(goal, area, durationLimitSec);
                return new PlannerResult("openai-compatible", properties.getModel(), normalize(parsed, goal, area, durationLimitSec),
                        objectMapper.writeValueAsString(parsed), elapsed(started));
            } catch (Exception ex) {
                log.warn("巡检目标结构化规划失败，使用规则兜底: {}", ex.getMessage());
                Map<String, Object> fallback = fallback(goal, area, durationLimitSec);
                fallback.put("fallbackReason", ex.getMessage());
                return new PlannerResult("rule-fallback", "patrol-rule-v1", fallback, "fallback:" + ex.getMessage(), elapsed(started));
            }
        }
        return new PlannerResult("rule-fallback", "patrol-rule-v1", fallback(goal, area, durationLimitSec), "fallback:ai-not-configured", elapsed(started));
    }

    private Map<String, Object> requestModel(String goal, String area, int durationLimitSec) throws Exception {
        String endpoint = trimSlash(properties.getBaseUrl()) +
                ("chat-completions".equalsIgnoreCase(properties.getApiStyle()) ? "/chat/completions" : "/responses");
        String instruction = "你是校园低空巡检任务规划器。只返回JSON，不直接控制无人机。"
                + "必须包含 taskType, targetTypes, priority, area, durationLimitSec, sensorRequirements, "
                + "constraints, replanTriggers, executionSteps。"
                + "targetTypes只能从 NO_HELMET, OVERLOAD, ILLEGAL_PARKING, RESTRICTED_AREA, "
                + "SMOKE_DETECTED, FIRE_DETECTED, THEFT_RISK 中选择；"
                + "constraints必须包含 ACTIVE_GEOFENCE、RETURN_BATTERY_25，且不得绕过禁飞区。";
        String user = "目标=" + goal + "；区域=" + area + "；时限=" + durationLimitSec + "秒。";
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("model", properties.getModel());
        if ("chat-completions".equalsIgnoreCase(properties.getApiStyle())) {
            payload.put("response_format", Map.of("type", "json_object"));
            payload.put("messages", List.of(
                    Map.of("role", "system", "content", instruction),
                    Map.of("role", "user", "content", user)));
        } else {
            payload.put("instructions", instruction);
            payload.put("input", user);
            payload.put("reasoning", Map.of("effort", properties.getReasoningEffort()));
        }
        HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(Math.min(properties.getTimeoutSeconds(), 20)))
                .build();
        HttpRequest request = HttpRequest.newBuilder(URI.create(endpoint))
                .timeout(Duration.ofSeconds(properties.getTimeoutSeconds()))
                .header("Authorization", "Bearer " + properties.getApiKey())
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(payload)))
                .build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IllegalStateException("HTTP " + response.statusCode());
        }
        JsonNode root = objectMapper.readTree(response.body());
        String content = "chat-completions".equalsIgnoreCase(properties.getApiStyle())
                ? root.path("choices").path(0).path("message").path("content").asText()
                : extractResponsesText(root);
        if (!StringUtils.hasText(content)) throw new IllegalStateException("模型未返回JSON规划");
        JsonNode plan = objectMapper.readTree(stripCodeFence(content));
        if (!plan.isObject()) throw new IllegalStateException("模型规划不是JSON对象");
        return objectMapper.convertValue(plan, Map.class);
    }

    private Map<String, Object> normalize(Map<String, Object> raw, String goal, String area, int duration) {
        Map<String, Object> plan = fallback(goal, area, duration);
        if (raw == null) return plan;
        raw.forEach((key, value) -> { if (value != null) plan.put(key, value); });
        plan.put("area", area);
        plan.put("durationLimitSec", duration);
        return plan;
    }

    private Map<String, Object> fallback(String goal, String area, int duration) {
        String upper = goal == null ? "" : goal.toUpperCase(Locale.ROOT);
        List<String> targets = new ArrayList<>(List.of("ILLEGAL_PARKING", "RESTRICTED_AREA", "NO_HELMET"));
        if (upper.contains("充电") || upper.contains("烟") || upper.contains("火")) targets.add("SMOKE_DETECTED");
        if (upper.contains("盗") || upper.contains("异常移动")) targets.add("THEFT_RISK");
        Map<String, Object> plan = new LinkedHashMap<>();
        plan.put("taskType", "CAMPUS_EBIKE_PATROL");
        plan.put("targetTypes", targets);
        plan.put("priority", upper.contains("应急") || upper.contains("火") ? "CRITICAL" : "HIGH");
        plan.put("area", area);
        plan.put("durationLimitSec", duration);
        plan.put("sensorRequirements", List.of("RGB_CAMERA", "GPS", "TELEMETRY"));
        plan.put("constraints", List.of("ACTIVE_GEOFENCE", "NO_FLY_ZONE", "RETURN_BATTERY_25", "HOME_RETURN"));
        plan.put("replanTriggers", List.of("NEW_AI_EVENT", "LOW_BATTERY", "TEMPORARY_NO_FLY_ZONE", "WAYPOINT_UNREACHABLE"));
        plan.put("executionSteps", List.of("UNDERSTAND", "OBSERVE", "PLAN", "EXECUTE", "MONITOR", "REPLAN", "DELIVER"));
        return plan;
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

    private String stripCodeFence(String value) {
        String text = value.trim();
        if (text.startsWith("```")) {
            int firstNewline = text.indexOf('\n');
            int lastFence = text.lastIndexOf("```");
            if (firstNewline >= 0 && lastFence > firstNewline) return text.substring(firstNewline + 1, lastFence).trim();
        }
        return text;
    }

    private String trimSlash(String value) { return value != null && value.endsWith("/") ? value.substring(0, value.length() - 1) : value; }
    private long elapsed(long started) { return (System.nanoTime() - started) / 1_000_000L; }

    public record PlannerResult(String provider, String model, Map<String, Object> plan, String rawResponse, long latencyMs) {}
}
