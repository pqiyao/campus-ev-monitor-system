package com.example.evmonitorplatform.service.impl;

import com.example.evmonitorplatform.common.exception.BusinessException;
import com.example.evmonitorplatform.config.AiGovernanceProperties;
import com.example.evmonitorplatform.config.AiGovernanceSchema;
import com.example.evmonitorplatform.entity.AppealRecord;
import com.example.evmonitorplatform.entity.Violation;
import com.example.evmonitorplatform.service.AiIntelligenceService;
import com.example.evmonitorplatform.service.AppealService;
import com.example.evmonitorplatform.service.ViolationService;
import com.example.evmonitorplatform.service.ai.GovernanceTextGateway;
import com.example.evmonitorplatform.vo.request.AiAssistantRequest;
import com.example.evmonitorplatform.vo.request.AiReportRequest;
import com.example.evmonitorplatform.vo.request.PolicyDocumentRequest;
import com.example.evmonitorplatform.vo.response.AiTextResult;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiIntelligenceServiceImpl implements AiIntelligenceService {
    private static final List<String> DOMAIN_TERMS = List.of(
            "头盔", "车牌", "载人", "超速", "禁行", "围栏", "停车", "违停", "消防", "证据", "复核",
            "身份", "轨迹", "扣分", "处罚", "冻结", "申诉", "图片", "视频", "盗抢", "异常移动", "安保", "通知");

    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;
    private final AiGovernanceSchema schema;
    private final AiGovernanceProperties properties;
    private final GovernanceTextGateway textGateway;
    private final AppealService appealService;
    private final ViolationService violationService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> chat(AiAssistantRequest request, Integer operatorUserId) {
        requireSchema();
        String sessionId = ensureSession(request, operatorUserId);
        saveMessage(sessionId, "user", request.getQuestion(), null, null, null);

        // 离线模式保留可用的本地对话；在线模式将普通对话交给真实大模型。
        if (isConversationQuestion(request.getQuestion()) && !"gpt".equalsIgnoreCase(properties.getMode())) {
            String simulated = simulatedConversation(request.getQuestion(), sessionId);
            Map<String, Object> answer = new LinkedHashMap<>();
            answer.put("sessionId", sessionId);
            answer.put("intent", "CONVERSATION");
            answer.put("answer", simulated);
            answer.put("provider", "local-simulation");
            answer.put("model", "simulated-governance-agent");
            answer.put("latencyMs", 8);
            answer.put("executionMode", "LOCAL_SIMULATION");
            answer.put("goal", request.getQuestion());
            answer.put("plan", List.of(Map.of("step", 1, "action", "理解对话意图", "status", "COMPLETED")));
            answer.put("toolCalls", List.of());
            answer.put("analysis", Map.of("type", "conversation", "sessionId", sessionId));
            answer.put("policyReferences", List.of());
            answer.put("suggestedQuestions", List.of("近7天哪个区域事件最多？", "当前待复核任务有多少？", "生成治理报告预览"));
            saveMessage(sessionId, "assistant", simulated, "CONVERSATION", "CONVERSATION_SIMULATOR", null);
            return answer;
        }
        if (isConversationQuestion(request.getQuestion())) {
            String fallback = "我已收到你的消息。我可以协助分析校园电动车治理数据、热点区域、事件趋势、重复车辆、待复核任务和治理报告。涉及处罚、通知或申诉状态的操作仍需管理员确认。";
            AiTextResult generated = generateConversationNarrative(request.getQuestion(), sessionId, fallback);
            Map<String, Object> answer = new LinkedHashMap<>();
            answer.put("sessionId", sessionId);
            answer.put("intent", "CONVERSATION");
            answer.put("answer", generated.getText());
            answer.put("provider", generated.getProvider());
            answer.put("model", generated.getModelName());
            answer.put("latencyMs", generated.getLatencyMs());
            answer.put("executionMode", isLiveProvider(generated.getProvider()) ? "LIVE_MODEL" : "DEGRADED");
            answer.put("goal", request.getQuestion());
            answer.put("plan", List.of(Map.of("step", 1, "action", "理解并回复对话", "status", "COMPLETED")));
            answer.put("toolCalls", List.of());
            answer.put("analysis", Map.of("type", "conversation"));
            answer.put("policyReferences", List.of());
            answer.put("suggestedQuestions", List.of("近7天哪个区域事件最多？", "当前待复核任务有多少？", "生成治理报告预览"));
            saveMessage(sessionId, "assistant", generated.getText(), "CONVERSATION", "MODEL_CONVERSATION", null);
            return answer;
        }
        String intent = classifyIntent(request);
        Map<String, Object> analysis = runTool(intent, request);
        List<Map<String, Object>> policies = searchPolicyChunks(request.getQuestion(), 5);
        String fallback = fallbackAnswer(intent, analysis, policies);
        AiTextResult generated = generateNarrative(request.getQuestion(), intent, analysis, policies, fallback);

        Map<String, Object> answer = new LinkedHashMap<>();
        answer.put("sessionId", sessionId);
        answer.put("intent", intent);
        answer.put("answer", generated.getText());
        answer.put("provider", generated.getProvider());
        answer.put("model", generated.getModelName());
        answer.put("latencyMs", generated.getLatencyMs());
        answer.put("executionMode", isLiveProvider(generated.getProvider()) ? "LIVE_MODEL" : "LOCAL_SIMULATION");
        answer.put("goal", request.getQuestion());
        answer.put("plan", List.of(Map.of("step", 1, "action", "读取治理分析数据", "tool", toolName(intent), "status", "COMPLETED"),
                Map.of("step", 2, "action", "整理结论与规则依据", "tool", "POLICY_SEARCH", "status", "COMPLETED")));
        answer.put("toolCalls", List.of(Map.of("name", toolName(intent), "status", "COMPLETED", "resultCount", resultCount(analysis)),
                Map.of("name", "POLICY_SEARCH", "status", "COMPLETED", "resultCount", policies.size())));
        answer.put("analysis", analysis);
        answer.put("policyReferences", policies);
        answer.put("suggestedQuestions", suggestedQuestions(intent));
        saveMessage(sessionId, "assistant", generated.getText(), intent,
                json(List.of(toolName(intent), "POLICY_SEARCH")), json(policies));
        return answer;
    }

    private boolean isConversationQuestion(String question) {
        String q = question == null ? "" : question.trim().toLowerCase(Locale.ROOT);
        if (q.isBlank()) return true;
        return q.matches(".*(你好|您好|嗨|hello|hi|在吗|谢谢|感谢|再见|拜拜|你是谁|你能做什么|怎么用|帮助|功能介绍|继续|刚才|上一条|明白了吗|懂了吗).*" )
                || q.length() <= 6 && q.matches("[?？!！。,.，、 ]*");
    }

    private String simulatedConversation(String question, String sessionId) {
        String q = question == null ? "" : question.trim();
        if (q.matches(".*(你好|您好|嗨|hello|hi).*")) {
            return "你好，我是校园电动车治理助手。你可以直接问我热点区域、事件趋势、重复车辆、待复核任务、申诉预审或治理报告。当前为本地模拟对话模式。";
        }
        if (q.matches(".*(你是谁|你能做什么|功能|帮助|怎么用).*")) {
            return "我可以协助查看校园电动车治理数据、定位高风险区域、梳理多源事件、整理待复核任务，并生成报告预览。模拟模式只提供分析建议，不会自动处罚、通知或修改申诉状态。";
        }
        if (q.matches(".*(谢谢|感谢|再见|拜拜).*")) {
            return "不客气。需要分析时，直接告诉我时间范围、区域或事件编号即可。";
        }
        if (q.matches(".*(继续|刚才|上一条|明白了吗|懂了吗).*")) {
            return "可以继续。请补充你希望深入的方向，例如“把刚才的热点区域按风险等级排序”或“结合待复核任务给出优先清单”。当前会话编号：" + sessionId + "。";
        }
        return "我已收到你的消息。当前是本地模拟智能体，会结合当前会话提供治理分析建议。你可以直接询问近7天热点、趋势、重复车辆、待复核任务或报告预览。";
    }

    private int resultCount(Map<String, Object> analysis) {
        for (String key : List.of("rows", "hotspots", "trend", "timeline", "relations")) {
            Object value = analysis.get(key);
            if (value instanceof List<?> list) return list.size();
        }
        return analysis.isEmpty() ? 0 : 1;
    }

    private boolean isLiveProvider(String provider) {
        return provider != null && (provider.equalsIgnoreCase("openai-compatible") || provider.equalsIgnoreCase("deepseek"));
    }

    private AiTextResult generateConversationNarrative(String question, String sessionId, String fallback) {
        try {
            String instructions = "你是校园电动车监测管理系统中的中文治理助手。自然、简洁地和管理员对话。你可以介绍自己的能力，但不能编造系统数据；涉及处罚、通知、冻结、申诉状态变更时，必须说明需要管理员确认。";
            String history = json(jdbcTemplate.queryForList("SELECT role, content FROM ai_assistant_message WHERE session_id=? ORDER BY message_id DESC LIMIT 8", sessionId));
            return textGateway.complete(instructions, "最近会话：" + history + "\n管理员消息：" + question);
        } catch (Exception ex) {
            log.warn("对话模型调用失败，使用降级回复: {}", ex.getMessage());
            return new AiTextResult(fallback, "rule-engine-fallback", "degraded-conversation", 0, ex.getMessage());
        }
    }

    @Override
    public List<Map<String, Object>> getSessionMessages(String sessionId, Integer operatorUserId) {
        requireSchema();
        List<Map<String, Object>> sessions = jdbcTemplate.queryForList(
                "SELECT user_id FROM ai_assistant_session WHERE session_id=?", sessionId);
        if (sessions.isEmpty()) throw new BusinessException("会话不存在");
        if (!Objects.equals(number(sessions.get(0).get("user_id")).intValue(), operatorUserId)) {
            throw new BusinessException("无权访问该会话");
        }
        return jdbcTemplate.queryForList("SELECT message_id, role, content, intent, tool_names, citations_json, create_time FROM ai_assistant_message WHERE session_id=? ORDER BY create_time, message_id", sessionId);
    }

    @Override
    public List<Map<String, Object>> listPolicies(String keyword) {
        requireSchema();
        if (StringUtils.hasText(keyword)) return searchPolicyChunks(keyword, 50);
        return jdbcTemplate.queryForList("SELECT document_id, title, category, version, effective_date, source, status, create_time FROM policy_document ORDER BY create_time DESC");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> createPolicy(PolicyDocumentRequest request) {
        requireSchema();
        jdbcTemplate.update("INSERT INTO policy_document (title, category, version, content, source, status) VALUES (?, ?, ?, ?, ?, 'ACTIVE')",
                request.getTitle(), defaultText(request.getCategory(), "CAMPUS_RULE"), request.getVersion(), request.getContent(), request.getSource());
        Long documentId = jdbcTemplate.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
        String[] paragraphs = request.getContent().split("(?:\\r?\\n){1,}");
        int index = 1;
        for (String paragraph : paragraphs) {
            if (!StringUtils.hasText(paragraph)) continue;
            jdbcTemplate.update("INSERT INTO policy_chunk (document_id, section_code, heading, content, keywords) VALUES (?, ?, ?, ?, ?)",
                    documentId, "CUSTOM-" + index, request.getTitle() + " 第" + index + "段", paragraph.trim(), extractKeywords(paragraph));
            index++;
        }
        return jdbcTemplate.queryForMap("SELECT document_id, title, category, version, source, status, create_time FROM policy_document WHERE document_id=?", documentId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public List<Map<String, Object>> correlateEvent(Long eventId) {
        requireSchema();
        List<Map<String, Object>> rows = jdbcTemplate.queryForList("SELECT * FROM ai_event WHERE event_id=?", eventId);
        if (rows.isEmpty()) throw new BusinessException("AI事件不存在");
        Map<String, Object> event = rows.get(0);
        Object occurredAt = event.get("occurred_at");
        List<Map<String, Object>> candidates = jdbcTemplate.queryForList("""
                SELECT event_id, event_code, event_type, source_type, device_id, occurred_at, location_name,
                       longitude, latitude, vehicle_id, user_id, license_plate, identity_confidence, detection_confidence
                FROM ai_event
                WHERE event_id<>? AND occurred_at BETWEEN DATE_SUB(?, INTERVAL 60 MINUTE) AND DATE_ADD(?, INTERVAL 60 MINUTE)
                ORDER BY occurred_at DESC LIMIT 200
                """, eventId, occurredAt, occurredAt);
        Set<String> sourceTypes = new HashSet<>();
        sourceTypes.add(text(event.get("source_type"), "UNKNOWN"));
        int relatedCount = 0;
        double bestSpatial = 0D, bestTemporal = 0D, bestIdentity = 0D, bestEvidence = decimal(event.get("detection_confidence"));
        for (Map<String, Object> candidate : candidates) {
            double distance = distanceMeters(event, candidate);
            double minutes = timeDistanceMinutes(event.get("occurred_at"), candidate.get("occurred_at"));
            double spatial = distance < 0 ? 0D : Math.max(0D, 1D - distance / 120D);
            double temporal = Math.max(0D, 1D - minutes / 60D);
            double identity = sameIdentity(event, candidate) ? identityConfidence(event, candidate) : 0D;
            boolean sameSource = Objects.equals(event.get("source_type"), candidate.get("source_type"));
            boolean crossDevice = !Objects.equals(event.get("device_id"), candidate.get("device_id"));
            boolean eligible = identity >= 0.80D || (distance >= 0D && distance <= 120D && minutes <= 10D);
            if (!eligible) continue;
            relatedCount++;
            sourceTypes.add(text(candidate.get("source_type"), "UNKNOWN"));
            bestSpatial = Math.max(bestSpatial, spatial);
            bestTemporal = Math.max(bestTemporal, temporal);
            bestIdentity = Math.max(bestIdentity, identity);
            bestEvidence = Math.max(bestEvidence, decimal(candidate.get("detection_confidence")));
            long source = Math.min(eventId, number(candidate.get("event_id")).longValue());
            long target = Math.max(eventId, number(candidate.get("event_id")).longValue());
            boolean crossSource = !sameSource;
            String type = crossSource ? "CROSS_SOURCE"
                    : (crossDevice ? "CROSS_CAMERA"
                    : (identity >= 0.80D ? "REPEAT_BEHAVIOR" : "SPATIOTEMPORAL"));
            // When the registered vehicle identity is exact, do not let a
            // missing coordinate from an edge event dilute that evidence.
            // Otherwise combine spatial, temporal and identity signals.
            double confidence = identity >= 0.80D
                    ? identity
                    : clamp(0.45D * spatial + 0.25D * temporal + 0.30D * identity);
            String explanation = identity >= 0.80D
                    ? "同一车辆/车牌在时间窗口内被多源感知设备捕获"
                    : String.format(Locale.ROOT, "不同来源在%.0f米、%.1f分钟内对同一区域形成时空证据", distance, minutes);
            jdbcTemplate.update("INSERT IGNORE INTO ai_event_relation (source_event_id, target_event_id, relation_type, confidence, explanation) VALUES (?, ?, ?, ?, ?)",
                    source, target, type, confidence, explanation);
        }
        double sourceBonus = sourceTypes.size() > 1 ? 0.10D : 0D;
        double fusedRisk = clamp(0.35D * bestEvidence + 0.25D * bestSpatial + 0.15D * bestTemporal + 0.15D * bestIdentity + sourceBonus);
        String fusedLevel = fusedRisk >= 0.78D ? "HIGH" : fusedRisk >= 0.50D ? "MEDIUM" : "LOW";
        String explanation = relatedCount == 0
                ? "当前事件暂未找到时间和空间均满足阈值的其他来源证据"
                : String.format(Locale.ROOT, "融合%d类来源、%d条关联证据；空间%.2f，时间%.2f，身份%.2f，检测%.2f",
                sourceTypes.size(), relatedCount, bestSpatial, bestTemporal, bestIdentity, bestEvidence);
        jdbcTemplate.update("""
                INSERT INTO ai_event_fusion (event_id, source_count, source_types, related_event_count, spatial_score,
                    temporal_score, identity_score, evidence_score, fused_risk_score, fused_risk_level, explanation)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                ON DUPLICATE KEY UPDATE source_count=VALUES(source_count), source_types=VALUES(source_types),
                    related_event_count=VALUES(related_event_count), spatial_score=VALUES(spatial_score),
                    temporal_score=VALUES(temporal_score), identity_score=VALUES(identity_score),
                    evidence_score=VALUES(evidence_score), fused_risk_score=VALUES(fused_risk_score),
                    fused_risk_level=VALUES(fused_risk_level), explanation=VALUES(explanation)
                """, eventId, sourceTypes.size(), String.join(",", sourceTypes), relatedCount, bestSpatial,
                bestTemporal, bestIdentity, bestEvidence, fusedRisk, fusedLevel, explanation);
        return relationsForEvent(eventId);
    }

    @Override
    public List<Map<String, Object>> listCorrelations(Integer limit) {
        requireSchema();
        int size = limit == null ? 30 : Math.max(1, Math.min(limit, 200));
        return jdbcTemplate.queryForList("""
                SELECT r.*, s.event_code AS source_code, s.event_type AS source_type, s.device_id AS source_device,
                       s.location_name AS source_location, s.occurred_at AS source_time,
                       t.event_code AS target_code, t.event_type AS target_type, t.device_id AS target_device,
                       t.location_name AS target_location, t.occurred_at AS target_time,
                       COALESCE(s.license_plate, t.license_plate) AS license_plate
                FROM ai_event_relation r
                JOIN ai_event s ON s.event_id=r.source_event_id
                JOIN ai_event t ON t.event_id=r.target_event_id
                ORDER BY r.create_time DESC LIMIT ?
                """, size);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> reviewAppeal(Integer appealId) {
        requireSchema();
        AppealRecord appeal = appealService.getAppealDetail(appealId);
        if (appeal == null) throw new BusinessException("申诉记录不存在");
        Violation violation = appeal.getIllegalId() == null ? null : violationService.getViolationDetail(appeal.getIllegalId());
        boolean appealEvidence = StringUtils.hasText(appeal.getAppealEvidence());
        boolean originalEvidence = violation != null && StringUtils.hasText(violation.getEvidenceUrl());
        String conclusion;
        String recommendation;
        double confidence;
        if (!originalEvidence && appealEvidence) {
            conclusion = "ORIGINAL_EVIDENCE_WEAK";
            recommendation = "RECOMMEND_ACCEPT";
            confidence = 0.82D;
        } else if (!appealEvidence) {
            conclusion = "APPEAL_EVIDENCE_MISSING";
            recommendation = "REQUEST_EVIDENCE";
            confidence = 0.88D;
        } else {
            conclusion = "EVIDENCE_CONFLICT_REVIEW";
            recommendation = "MANUAL_REVIEW";
            confidence = 0.76D;
        }
        List<Map<String, Object>> policies = searchPolicyChunks("申诉 证据 复核 " + defaultText(appeal.getIllegalTypeName(), ""), 5);
        Map<String, Object> context = new LinkedHashMap<>();
        context.put("appealId", appealId);
        context.put("reason", appeal.getAppealReason());
        context.put("appealEvidenceProvided", appealEvidence);
        context.put("originalEvidenceProvided", originalEvidence);
        context.put("violationType", appeal.getIllegalTypeName());
        context.put("violationTime", appeal.getIllegalTime());
        context.put("location", appeal.getLocation());
        context.put("preliminaryConclusion", conclusion);
        String fallback = "AI预审结论：" + conclusion + "。建议：" + recommendation + "。该结果仅供管理员审核参考。";
        AiTextResult generated = generateNarrative("请复核申诉 " + appealId, "APPEAL_REVIEW", context, policies, fallback);
        jdbcTemplate.update("INSERT INTO ai_appeal_review (appeal_id, conclusion, confidence, evidence_analysis, policy_refs_json, recommendation, model_name, raw_response) VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
                appealId, conclusion, confidence, generated.getText(), json(policies), recommendation, generated.getModelName(), generated.getRawResponse());
        Map<String, Object> result = new LinkedHashMap<>(context);
        result.put("conclusion", conclusion);
        result.put("confidence", confidence);
        result.put("recommendation", recommendation);
        result.put("analysis", generated.getText());
        result.put("provider", generated.getProvider());
        result.put("policyReferences", policies);
        result.put("appeal", appeal);
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> generateReport(AiReportRequest request, Integer operatorUserId) {
        requireSchema();
        LocalDateTime end = request.getEndTime() == null ? LocalDateTime.now() : request.getEndTime();
        LocalDateTime start = request.getStartTime() == null ? end.minusDays(7) : request.getStartTime();
        if (!start.isBefore(end)) throw new BusinessException("报告开始时间必须早于结束时间");
        Map<String, Object> content = new LinkedHashMap<>();
        content.put("summary", periodSummary(start, end));
        content.put("trend", eventTrend(start, end));
        content.put("hotspots", hotspots(start, end));
        content.put("repeatVehicles", repeatVehicles(start, end));
        content.put("correlations", jdbcTemplate.queryForList("SELECT relation_type AS type, COUNT(*) AS count FROM ai_event_relation WHERE create_time BETWEEN ? AND ? GROUP BY relation_type", start, end));
        List<Map<String, Object>> policies = searchPolicyChunks("证据 复核 处罚 停车 头盔 载人 禁行", 6);
        String fallback = reportFallback(start, end, content);
        AiTextResult generated = generateNarrative("生成校园电动车治理报告", "AUTO_REPORT", content, policies, fallback);
        content.put("narrative", generated.getText());
        content.put("policyReferences", policies);
        String code = "RPT-" + UUID.randomUUID().toString().replace("-", "").substring(0, 16).toUpperCase(Locale.ROOT);
        String type = defaultText(request.getReportType(), "WEEKLY").toUpperCase(Locale.ROOT);
        String title = "校园电动车" + ("DAILY".equals(type) ? "日度" : "周期") + "治理报告";
        jdbcTemplate.update("INSERT INTO ai_generated_report (report_code, report_type, title, period_start, period_end, content_json, generated_by, model_name) VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
                code, type, title, start, end, json(content), operatorUserId, generated.getModelName());
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("reportCode", code); result.put("reportType", type); result.put("title", title);
        result.put("periodStart", start); result.put("periodEnd", end); result.put("content", content);
        result.put("provider", generated.getProvider()); result.put("model", generated.getModelName());
        return result;
    }

    @Override
    public List<Map<String, Object>> listReports(Integer limit) {
        requireSchema();
        int size = limit == null ? 20 : Math.max(1, Math.min(limit, 100));
        return jdbcTemplate.queryForList("SELECT report_id, report_code, report_type, title, period_start, period_end, generated_by, model_name, create_time FROM ai_generated_report ORDER BY create_time DESC LIMIT ?", size);
    }

    private String ensureSession(AiAssistantRequest request, Integer userId) {
        if (StringUtils.hasText(request.getSessionId())) {
            List<Map<String, Object>> rows = jdbcTemplate.queryForList("SELECT user_id FROM ai_assistant_session WHERE session_id=?", request.getSessionId());
            if (rows.isEmpty() || !Objects.equals(number(rows.get(0).get("user_id")).intValue(), userId)) throw new BusinessException("会话不存在或无权访问");
            jdbcTemplate.update("UPDATE ai_assistant_session SET context_event_id=COALESCE(?, context_event_id), update_time=? WHERE session_id=?", request.getContextEventId(), LocalDateTime.now(), request.getSessionId());
            return request.getSessionId();
        }
        String id = "AS-" + UUID.randomUUID().toString().replace("-", "").substring(0, 20).toUpperCase(Locale.ROOT);
        String title = request.getQuestion().length() > 40 ? request.getQuestion().substring(0, 40) : request.getQuestion();
        jdbcTemplate.update("INSERT INTO ai_assistant_session (session_id, user_id, title, context_event_id) VALUES (?, ?, ?, ?)", id, userId, title, request.getContextEventId());
        return id;
    }

    private String classifyIntent(AiAssistantRequest request) {
        String q = request.getQuestion();
        if (request.getAppealId() != null || q.contains("申诉")) return "APPEAL_ANALYSIS";
        if (request.getContextEventId() != null || q.contains("这起事件") || q.contains("当前事件")) return "EVENT_CONTEXT";
        if (q.contains("报告") || q.contains("周报") || q.contains("日报")) return "REPORT_PREVIEW";
        if (q.contains("热点") || q.contains("区域") || q.contains("地点") || q.contains("最多")) return "HOTSPOT_ANALYSIS";
        if (q.contains("趋势") || q.contains("近7天") || q.contains("最近")) return "TREND_ANALYSIS";
        if (q.contains("重复") || q.contains("多次") || q.contains("高频车辆")) return "REPEAT_VEHICLE_ANALYSIS";
        if (q.contains("待复核") || q.contains("积压") || q.contains("任务")) return "REVIEW_BACKLOG";
        return "GOVERNANCE_OVERVIEW";
    }

    private Map<String, Object> runTool(String intent, AiAssistantRequest request) {
        LocalDateTime end = LocalDateTime.now();
        LocalDateTime start = end.minusDays(7);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("periodStart", start); result.put("periodEnd", end);
        switch (intent) {
            case "HOTSPOT_ANALYSIS" -> { result.put("chartType", "bar"); result.put("rows", hotspots(start, end)); }
            case "TREND_ANALYSIS" -> { result.put("chartType", "line"); result.put("rows", eventTrend(start, end)); }
            case "REPEAT_VEHICLE_ANALYSIS" -> { result.put("chartType", "table"); result.put("rows", repeatVehicles(start, end)); }
            case "REVIEW_BACKLOG" -> { result.put("chartType", "table"); result.put("summary", periodSummary(start, end)); result.put("rows", pendingReviews()); }
            case "EVENT_CONTEXT" -> { result.put("chartType", "timeline"); result.putAll(eventContext(request.getContextEventId())); }
            case "APPEAL_ANALYSIS" -> { result.put("chartType", "table"); result.put("appealId", request.getAppealId()); result.put("pendingAppeals", pendingAppeals()); }
            case "REPORT_PREVIEW" -> { result.put("chartType", "mixed"); result.put("summary", periodSummary(start, end)); result.put("hotspots", hotspots(start, end)); result.put("trend", eventTrend(start, end)); }
            default -> { result.put("chartType", "metrics"); result.put("summary", periodSummary(start, end)); result.put("hotspots", hotspots(start, end)); }
        }
        return result;
    }

    private Map<String, Object> periodSummary(LocalDateTime start, LocalDateTime end) {
        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("events", scalar("SELECT COUNT(*) FROM ai_event WHERE occurred_at BETWEEN ? AND ?", start, end));
        summary.put("confirmed", scalar("SELECT COUNT(*) FROM governance_incident WHERE state='CONFIRMED' AND create_time BETWEEN ? AND ?", start, end));
        summary.put("pendingReview", scalar("SELECT COUNT(*) FROM governance_incident WHERE state IN ('PENDING_REVIEW','PENDING_CONFIRMATION') AND create_time BETWEEN ? AND ?", start, end));
        summary.put("rejected", scalar("SELECT COUNT(*) FROM governance_incident WHERE state='REJECTED' AND create_time BETWEEN ? AND ?", start, end));
        summary.put("openTasks", scalar("SELECT COUNT(*) FROM workflow_task WHERE state='OPEN'"));
        summary.put("pendingAppeals", scalar("SELECT COUNT(*) FROM illegal_appeal_record WHERE appeal_state IN (0,1)"));
        summary.put("crossCameraRelations", scalar("SELECT COUNT(*) FROM ai_event_relation WHERE relation_type='CROSS_CAMERA' AND create_time BETWEEN ? AND ?", start, end));
        return summary;
    }

    private List<Map<String, Object>> eventTrend(LocalDateTime start, LocalDateTime end) {
        return jdbcTemplate.queryForList("SELECT DATE(occurred_at) AS date, COUNT(*) AS count FROM ai_event WHERE occurred_at BETWEEN ? AND ? GROUP BY DATE(occurred_at) ORDER BY date", start, end);
    }

    private List<Map<String, Object>> hotspots(LocalDateTime start, LocalDateTime end) {
        return jdbcTemplate.queryForList("SELECT COALESCE(location_name, '未知位置') AS location, COUNT(*) AS count, MAX(risk_level) AS max_risk FROM ai_event WHERE occurred_at BETWEEN ? AND ? GROUP BY COALESCE(location_name, '未知位置') ORDER BY count DESC LIMIT 10", start, end);
    }

    private List<Map<String, Object>> repeatVehicles(LocalDateTime start, LocalDateTime end) {
        return jdbcTemplate.queryForList("SELECT license_plate, vehicle_id, COUNT(*) AS event_count, COUNT(DISTINCT device_id) AS device_count, MAX(occurred_at) AS latest_time FROM ai_event WHERE occurred_at BETWEEN ? AND ? AND license_plate IS NOT NULL GROUP BY license_plate, vehicle_id HAVING COUNT(*)>1 ORDER BY event_count DESC LIMIT 20", start, end);
    }

    private List<Map<String, Object>> pendingReviews() {
        return jdbcTemplate.queryForList("SELECT incident_id, incident_code, title, event_type, risk_level, state, deadline, create_time FROM governance_incident WHERE state IN ('PENDING_REVIEW','PENDING_CONFIRMATION') ORDER BY FIELD(risk_level,'CRITICAL','HIGH','MEDIUM','LOW'), create_time LIMIT 20");
    }

    private List<Map<String, Object>> pendingAppeals() {
        return jdbcTemplate.queryForList("SELECT appeal_id, illegal_id, user_id, appeal_reason, urgency_level, appeal_time FROM illegal_appeal_record WHERE appeal_state IN (0,1) ORDER BY urgency_level DESC, appeal_time LIMIT 20");
    }

    private Map<String, Object> eventContext(Long eventId) {
        if (eventId == null) return Map.of("message", "请从事件详情进入助手，或提供 contextEventId");
        List<Map<String, Object>> rows = jdbcTemplate.queryForList("SELECT e.*, i.incident_id, i.state AS incident_state, i.summary AS incident_summary FROM ai_event e LEFT JOIN governance_incident i ON i.event_id=e.event_id WHERE e.event_id=?", eventId);
        if (rows.isEmpty()) throw new BusinessException("上下文事件不存在");
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("event", rows.get(0)); result.put("relations", relationsForEvent(eventId));
        result.put("fusion", fusionForEvent(eventId));
        result.put("timeline", jdbcTemplate.queryForList("SELECT stage, title, detail, actor_type, create_time FROM event_timeline WHERE event_id=? ORDER BY create_time", eventId));
        return result;
    }

    private List<Map<String, Object>> relationsForEvent(Long eventId) {
        return jdbcTemplate.queryForList("""
                SELECT r.*, s.event_code AS source_code, s.device_id AS source_device, s.location_name AS source_location,
                       t.event_code AS target_code, t.device_id AS target_device, t.location_name AS target_location
                FROM ai_event_relation r JOIN ai_event s ON s.event_id=r.source_event_id JOIN ai_event t ON t.event_id=r.target_event_id
                WHERE r.source_event_id=? OR r.target_event_id=? ORDER BY r.create_time DESC
                """, eventId, eventId);
    }

    private Map<String, Object> fusionForEvent(Long eventId) {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList("SELECT * FROM ai_event_fusion WHERE event_id=?", eventId);
        return rows.isEmpty() ? Map.of("source_count", 1, "related_event_count", 0, "fused_risk_level", "LOW", "explanation", "尚未形成跨源融合证据") : rows.get(0);
    }

    private boolean sameIdentity(Map<String, Object> first, Map<String, Object> second) {
        Object firstVehicle = first.get("vehicle_id");
        Object secondVehicle = second.get("vehicle_id");
        if (firstVehicle != null && secondVehicle != null && Objects.equals(String.valueOf(firstVehicle), String.valueOf(secondVehicle))) return true;
        String firstPlate = text(first.get("license_plate"), "");
        String secondPlate = text(second.get("license_plate"), "");
        return !firstPlate.isBlank() && firstPlate.equalsIgnoreCase(secondPlate);
    }

    private double identityConfidence(Map<String, Object> first, Map<String, Object> second) {
        Object firstVehicle = first.get("vehicle_id");
        Object secondVehicle = second.get("vehicle_id");
        boolean vehicleMatch = firstVehicle != null && secondVehicle != null
                && Objects.equals(String.valueOf(firstVehicle), String.valueOf(secondVehicle));
        String firstPlate = text(first.get("license_plate"), "");
        String secondPlate = text(second.get("license_plate"), "");
        boolean plateMatch = !firstPlate.isBlank() && firstPlate.equalsIgnoreCase(secondPlate);
        // An exact registered vehicle match is stronger than a plate-only
        // match. The values are confidence priors, then any detector-provided
        // confidence can raise them further.
        double confidence = vehicleMatch ? 0.96D : (plateMatch ? 0.90D : 0D);
        confidence = Math.max(confidence, decimal(first.get("identity_confidence")));
        confidence = Math.max(confidence, decimal(second.get("identity_confidence")));
        return clamp(confidence);
    }

    private double distanceMeters(Map<String, Object> first, Map<String, Object> second) {
        if (first.get("longitude") == null || first.get("latitude") == null || second.get("longitude") == null || second.get("latitude") == null) return -1D;
        double lat1 = decimal(first.get("latitude")), lat2 = decimal(second.get("latitude"));
        double dLat = Math.toRadians(lat2 - lat1);
        double dLng = Math.toRadians(decimal(second.get("longitude")) - decimal(first.get("longitude")));
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) * Math.sin(dLng / 2) * Math.sin(dLng / 2);
        return 6371000D * 2D * Math.atan2(Math.sqrt(a), Math.sqrt(Math.max(0D, 1D - a)));
    }

    private double timeDistanceMinutes(Object first, Object second) {
        try {
            LocalDateTime a = LocalDateTime.parse(String.valueOf(first).replace(' ', 'T'));
            LocalDateTime b = LocalDateTime.parse(String.valueOf(second).replace(' ', 'T'));
            return Math.abs(Duration.between(a, b).toSeconds()) / 60D;
        } catch (Exception ex) { return 999D; }
    }

    private double decimal(Object value) {
        try { return value == null ? 0D : Double.parseDouble(String.valueOf(value)); }
        catch (Exception ex) { return 0D; }
    }

    private double clamp(double value) { return Math.max(0D, Math.min(1D, value)); }

    private String text(Object value, String fallback) { return value == null || String.valueOf(value).isBlank() ? fallback : String.valueOf(value); }

    private List<Map<String, Object>> searchPolicyChunks(String query, int limit) {
        List<String> terms = new ArrayList<>();
        for (String term : DOMAIN_TERMS) if (query.contains(term)) terms.add(term);
        if (terms.isEmpty()) terms.add("证据");
        StringBuilder sql = new StringBuilder("SELECT pd.document_id, pd.title, pd.version, pc.section_code, pc.heading, pc.content FROM policy_chunk pc JOIN policy_document pd ON pd.document_id=pc.document_id WHERE pd.status='ACTIVE' AND (");
        List<Object> args = new ArrayList<>();
        for (int i = 0; i < terms.size(); i++) {
            if (i > 0) sql.append(" OR ");
            sql.append("pc.content LIKE ? OR pc.keywords LIKE ?");
            args.add("%" + terms.get(i) + "%"); args.add("%" + terms.get(i) + "%");
        }
        sql.append(") ORDER BY pc.chunk_id LIMIT ").append(Math.max(1, Math.min(limit, 50)));
        return jdbcTemplate.queryForList(sql.toString(), args.toArray());
    }

    private AiTextResult generateNarrative(String question, String intent, Object data,
                                           List<Map<String, Object>> policies, String fallback) {
        if (!"gpt".equalsIgnoreCase(properties.getMode())) return new AiTextResult(fallback, "rule-engine", "deterministic-analytics", 0, null);
        try {
            String instructions = "你是校园电动车治理分析助手。只能依据提供的只读查询结果和校规片段回答，不得编造数据，不得声称已执行处罚。输出简洁中文，先给结论，再列关键数据和规则依据。";
            String prompt = "用户问题：" + question + "\n意图：" + intent + "\n只读数据：" + json(data) + "\n校规片段：" + json(policies);
            return textGateway.complete(instructions, prompt);
        } catch (Exception ex) {
            log.warn("治理助手模型调用失败，使用确定性分析: {}", ex.getMessage());
            return new AiTextResult(fallback, "rule-engine-fallback", "deterministic-analytics", 0, ex.getMessage());
        }
    }

    private String fallbackAnswer(String intent, Map<String, Object> analysis, List<Map<String, Object>> policies) {
        String citation = policies.isEmpty() ? "未检索到直接规则条款" : "参考 " + policies.get(0).get("section_code") + "《" + policies.get(0).get("heading") + "》";
        return switch (intent) {
            case "HOTSPOT_ANALYSIS" -> "已完成近7天热点分析，共返回 " + sizeOf(analysis.get("rows")) + " 个区域。" + citation + "。";
            case "TREND_ANALYSIS" -> "已完成近7天事件趋势统计，共形成 " + sizeOf(analysis.get("rows")) + " 个时间点。" + citation + "。";
            case "REPEAT_VEHICLE_ANALYSIS" -> "已筛选近7天重复事件车辆，共 " + sizeOf(analysis.get("rows")) + " 辆，需要结合跨设备记录复核。";
            case "REVIEW_BACKLOG" -> "已查询当前复核队列，共返回 " + sizeOf(analysis.get("rows")) + " 条高优先级任务。" + citation + "。";
            case "EVENT_CONTEXT" -> "已汇总当前事件、跨设备关联和处理时间线。" + citation + "。";
            case "APPEAL_ANALYSIS" -> "已读取待处理申诉概况；AI建议仅供管理员预审，不会自动改变申诉结论。" + citation + "。";
            case "REPORT_PREVIEW" -> "已生成治理报告预览，包含周期指标、热点区域和事件趋势。";
            default -> "已完成近7天校园电动车治理概览。数据来自AI事件、治理任务、跨设备关联和申诉记录。" + citation + "。";
        };
    }

    private String reportFallback(LocalDateTime start, LocalDateTime end, Map<String, Object> content) {
        return "报告周期：" + start + " 至 " + end + "。已汇总事件总量、处置状态、热点区域、重复车辆和跨设备关联。详细数据见报告结构化附表。";
    }

    private List<String> suggestedQuestions(String intent) {
        return switch (intent) {
            case "HOTSPOT_ANALYSIS" -> List.of("这些热点区域主要是什么事件？", "生成本周治理报告", "查看重复违规车辆");
            case "TREND_ANALYSIS" -> List.of("哪个区域增长最快？", "当前有多少待复核任务？", "生成近7天报告");
            default -> List.of("近7天哪个区域事件最多？", "有哪些重复违规车辆？", "当前待复核任务有多少？");
        };
    }

    private String toolName(String intent) {
        return switch (intent) {
            case "HOTSPOT_ANALYSIS" -> "QUERY_EVENT_HOTSPOTS";
            case "TREND_ANALYSIS" -> "QUERY_EVENT_TREND";
            case "REPEAT_VEHICLE_ANALYSIS" -> "QUERY_REPEAT_VEHICLES";
            case "REVIEW_BACKLOG" -> "QUERY_REVIEW_BACKLOG";
            case "EVENT_CONTEXT" -> "READ_EVENT_CONTEXT";
            case "APPEAL_ANALYSIS" -> "QUERY_PENDING_APPEALS";
            case "REPORT_PREVIEW" -> "BUILD_REPORT_PREVIEW";
            default -> "QUERY_GOVERNANCE_OVERVIEW";
        };
    }

    private void saveMessage(String sessionId, String role, String content, String intent, String tools, String citations) {
        jdbcTemplate.update("INSERT INTO ai_assistant_message (session_id, role, content, intent, tool_names, citations_json) VALUES (?, ?, ?, ?, ?, ?)", sessionId, role, content, intent, tools, citations);
        jdbcTemplate.update("UPDATE ai_assistant_session SET update_time=? WHERE session_id=?", LocalDateTime.now(), sessionId);
    }

    private Object scalar(String sql, Object... args) {
        return jdbcTemplate.queryForObject(sql, Object.class, args);
    }

    private Number number(Object value) {
        return value instanceof Number number ? number : 0;
    }

    private int sizeOf(Object value) {
        return value instanceof List<?> list ? list.size() : 0;
    }

    private String extractKeywords(String content) {
        List<String> matches = new ArrayList<>();
        for (String term : DOMAIN_TERMS) if (content.contains(term)) matches.add(term);
        return String.join(",", matches);
    }

    private String defaultText(String value, String fallback) {
        return StringUtils.hasText(value) ? value.trim() : fallback;
    }

    private String json(Object value) {
        try { return objectMapper.writeValueAsString(value); }
        catch (Exception ex) { return "{}"; }
    }

    private void requireSchema() {
        if (!schema.ensureTablesExist()) throw new BusinessException("AI智能分析数据表未就绪");
    }
}
