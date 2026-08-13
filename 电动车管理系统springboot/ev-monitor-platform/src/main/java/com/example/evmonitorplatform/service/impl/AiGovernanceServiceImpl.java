package com.example.evmonitorplatform.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.evmonitorplatform.common.exception.BusinessException;
import com.example.evmonitorplatform.config.AiGovernanceProperties;
import com.example.evmonitorplatform.config.AiGovernanceSchema;
import com.example.evmonitorplatform.entity.AiAnalysis;
import com.example.evmonitorplatform.entity.AiEvent;
import com.example.evmonitorplatform.entity.GovernanceIncident;
import com.example.evmonitorplatform.entity.Vehicle;
import com.example.evmonitorplatform.entity.Violation;
import com.example.evmonitorplatform.entity.WorkflowTask;
import com.example.evmonitorplatform.mapper.AiAnalysisMapper;
import com.example.evmonitorplatform.mapper.AiEventMapper;
import com.example.evmonitorplatform.mapper.GovernanceIncidentMapper;
import com.example.evmonitorplatform.mapper.VehicleMapper;
import com.example.evmonitorplatform.mapper.WorkflowTaskMapper;
import com.example.evmonitorplatform.service.AiGovernanceService;
import com.example.evmonitorplatform.service.AiIntelligenceService;
import com.example.evmonitorplatform.service.NotificationService;
import com.example.evmonitorplatform.service.ViolationService;
import com.example.evmonitorplatform.service.ai.AiDecisionRuleService;
import com.example.evmonitorplatform.service.ai.AiModelGateway;
import com.example.evmonitorplatform.service.ai.Gpt56AiModelGateway;
import com.example.evmonitorplatform.service.ai.MockAiModelGateway;
import com.example.evmonitorplatform.vo.request.AiDemoRunRequest;
import com.example.evmonitorplatform.vo.request.AiEventCreateRequest;
import com.example.evmonitorplatform.vo.request.AiIncidentReviewRequest;
import com.example.evmonitorplatform.vo.response.AiModelDecision;
import com.example.evmonitorplatform.vo.response.AiRuleDecision;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiGovernanceServiceImpl implements AiGovernanceService {
    private static final List<String> SUPPORTED_EVENT_TYPES = List.of(
            "NO_HELMET", "OVERLOAD", "ILLEGAL_PARKING", "RESTRICTED_AREA", "THEFT_RISK",
            "SMOKE_DETECTED", "FIRE_DETECTED");
    private static final List<String> SUPPORTED_SOURCE_TYPES = List.of(
            "SIMULATOR", "CAMERA", "DRONE", "GEOFENCE", "GPS", "MANUAL", "THIRD_PARTY");
    private final AiEventMapper eventMapper;
    private final AiAnalysisMapper analysisMapper;
    private final GovernanceIncidentMapper incidentMapper;
    private final WorkflowTaskMapper taskMapper;
    private final VehicleMapper vehicleMapper;
    private final ViolationService violationService;
    private final NotificationService notificationService;
    private final AiIntelligenceService intelligenceService;
    private final AiGovernanceProperties properties;
    private final AiGovernanceSchema schema;
    private final AiDecisionRuleService ruleService;
    private final MockAiModelGateway mockGateway;
    private final Gpt56AiModelGateway gptGateway;
    private final ObjectMapper objectMapper;
    private final JdbcTemplate jdbcTemplate;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> runDemo(AiDemoRunRequest request) {
        AiDemoRunRequest input = request == null ? new AiDemoRunRequest() : request;
        String scenario = StringUtils.hasText(input.getScenario()) ? input.getScenario().toUpperCase(Locale.ROOT) : "NO_HELMET";
        AiEventCreateRequest event = new AiEventCreateRequest();
        event.setEventType(scenario);
        event.setSourceType("SIMULATOR");
        event.setDeviceId("demo-camera-east-gate");
        event.setOccurredAt(LocalDateTime.now());
        event.setLocationName(StringUtils.hasText(input.getLocationName()) ? input.getLocationName() : "东校区正门");
        event.setLicensePlate(input.getLicensePlate());
        event.setDetectionConfidence(0.94D);
        event.setEvidenceUrls(List.of("/demo/evidence/" + scenario.toLowerCase(Locale.ROOT) + ".jpg"));
        event.setAutoAnalyze(input.getAutoAnalyze() == null || input.getAutoAnalyze());
        Map<String, Object> result = ingest(event);
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> ingest(AiEventCreateRequest request) {
        requireSchema();
        if (request == null || !StringUtils.hasText(request.getEventType()) || !StringUtils.hasText(request.getSourceType())) {
            throw new BusinessException("事件类型和来源类型不能为空");
        }
        String plate = trimToNull(request.getLicensePlate());
        String dedupe = StringUtils.hasText(request.getIdempotencyKey())
                ? digest("client|" + request.getIdempotencyKey().trim())
                : digest(request.getSourceType() + "|" + request.getDeviceId() + "|" + request.getEventType() + "|" + plate + "|" + (request.getOccurredAt() == null ? "" : request.getOccurredAt()));
        AiEvent existing = eventMapper.selectOne(new LambdaQueryWrapper<AiEvent>().eq(AiEvent::getDedupeKey, dedupe));
        if (existing != null) {
            return getEventDetail(existing.getEventId());
        }

        String eventType = request.getEventType().trim().toUpperCase(Locale.ROOT);
        String sourceType = request.getSourceType().trim().toUpperCase(Locale.ROOT);
        if (!SUPPORTED_EVENT_TYPES.contains(eventType)) {
            throw new BusinessException("不支持的AI事件类型：" + eventType);
        }
        if (!SUPPORTED_SOURCE_TYPES.contains(sourceType)) {
            throw new BusinessException("不支持的事件来源：" + sourceType);
        }
        if (request.getEvidenceUrls() != null && request.getEvidenceUrls().size() > 20) {
            throw new BusinessException("单个事件最多接入20条证据");
        }

        AiEvent event = new AiEvent();
        event.setEventCode("AI-" + UUID.randomUUID().toString().replace("-", "").substring(0, 16).toUpperCase(Locale.ROOT));
        event.setEventType(eventType);
        event.setSourceType(sourceType);
        event.setDeviceId(trimToNull(request.getDeviceId()));
        event.setOccurredAt(request.getOccurredAt() == null ? LocalDateTime.now() : request.getOccurredAt());
        event.setLocationName(trimToNull(request.getLocationName()));
        event.setLongitude(request.getLongitude());
        event.setLatitude(request.getLatitude());
        event.setLicensePlate(plate);
        event.setVehicleId(request.getVehicleId());
        event.setUserId(request.getUserId());
        event.setDetectionConfidence(request.getDetectionConfidence() == null ? 0.0D : request.getDetectionConfidence());
        event.setEvidenceUrls(json(request.getEvidenceUrls() == null ? List.of() : request.getEvidenceUrls()));
        event.setRawPayload(json(request.getRawPayload() == null ? Map.of() : request.getRawPayload()));
        event.setStatus("INGESTED");
        event.setRiskLevel("UNKNOWN");
        event.setDedupeKey(dedupe);
        event.setCreateTime(LocalDateTime.now());
        event.setUpdateTime(LocalDateTime.now());
        bindIdentity(event);
        if (eventMapper.insert(event) <= 0) {
            throw new BusinessException("AI事件写入失败");
        }
        addTimeline(event.getEventId(), null, "INGESTED", "事件接入", "来源：" + event.getSourceType(), "SYSTEM", null);
        saveIdentitySnapshot(event);
        saveEvidence(event);
        intelligenceService.correlateEvent(event.getEventId());
        if (Boolean.TRUE.equals(request.getAutoAnalyze())) {
            return analyze(event.getEventId());
        }
        return getEventDetail(event.getEventId());
    }

    @Override
    public List<Map<String, Object>> listEvents(String status, String eventType, Integer limit) {
        requireSchema();
        int size = limit == null ? 50 : Math.max(1, Math.min(limit, 200));
        StringBuilder sql = new StringBuilder("SELECT e.*, i.incident_id, i.state AS incident_state, i.decision_type, i.title AS incident_title FROM ai_event e LEFT JOIN governance_incident i ON i.event_id=e.event_id WHERE 1=1");
        List<Object> args = new ArrayList<>();
        if (StringUtils.hasText(status)) { sql.append(" AND e.status=?"); args.add(status.trim().toUpperCase(Locale.ROOT)); }
        if (StringUtils.hasText(eventType)) { sql.append(" AND e.event_type=?"); args.add(eventType.trim().toUpperCase(Locale.ROOT)); }
        sql.append(" ORDER BY e.occurred_at DESC LIMIT ").append(size);
        return jdbcTemplate.queryForList(sql.toString(), args.toArray());
    }

    @Override
    public Map<String, Object> getEventDetail(Long eventId) {
        requireSchema();
        if (eventId == null) throw new BusinessException("事件ID不能为空");
        List<Map<String, Object>> rows = jdbcTemplate.queryForList("SELECT e.*, i.incident_id, i.incident_code, i.title AS incident_title, i.summary AS incident_summary, i.risk_level AS incident_risk_level, i.decision_type, i.state AS incident_state, i.deadline, i.violation_id, i.reviewer_id, i.review_remark, i.reviewed_at FROM ai_event e LEFT JOIN governance_incident i ON i.event_id=e.event_id WHERE e.event_id=?", eventId);
        if (rows.isEmpty()) throw new BusinessException("AI事件不存在");
        Map<String, Object> result = new LinkedHashMap<>(rows.get(0));
        result.put("analyses", jdbcTemplate.queryForList("SELECT * FROM ai_analysis WHERE event_id=? ORDER BY create_time DESC", eventId));
        result.put("evidence", jdbcTemplate.queryForList("SELECT * FROM ai_evidence WHERE event_id=? ORDER BY create_time", eventId));
        result.put("identityMatches", jdbcTemplate.queryForList("SELECT * FROM ai_identity_match WHERE event_id=? ORDER BY create_time DESC", eventId));
        result.put("timeline", jdbcTemplate.queryForList("SELECT * FROM event_timeline WHERE event_id=? ORDER BY create_time", eventId));
        result.put("relations", jdbcTemplate.queryForList("SELECT * FROM ai_event_relation WHERE source_event_id=? OR target_event_id=? ORDER BY create_time DESC", eventId, eventId));
        if (result.get("incident_id") != null) result.put("tasks", jdbcTemplate.queryForList("SELECT * FROM workflow_task WHERE incident_id=? ORDER BY create_time DESC", result.get("incident_id")));
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> analyze(Long eventId) {
        requireSchema();
        AiEvent event = eventMapper.selectById(eventId);
        if (event == null) throw new BusinessException("AI事件不存在");
        if ("CONFIRMED".equals(event.getStatus()) || "REJECTED".equals(event.getStatus())) {
            throw new BusinessException("已完成处置的事件不能重新研判");
        }
        bindIdentity(event);
        event.setStatus("ANALYZING");
        event.setUpdateTime(LocalDateTime.now());
        eventMapper.updateById(event);
        addTimeline(eventId, null, "ANALYZING", "启动AI智能研判", "多模态视觉分析引擎已接入", "SYSTEM", null);
        boolean onlineMode = "gpt".equalsIgnoreCase(properties.getMode());
        AiModelGateway gateway = onlineMode ? gptGateway : mockGateway;
        AiModelDecision model;
        try {
            model = gateway.analyze(event);
        } catch (Exception ex) {
            if (!onlineMode || !properties.isFallbackToMock()) {
                event.setStatus("ANALYSIS_FAILED");
                event.setUpdateTime(LocalDateTime.now());
                eventMapper.updateById(event);
                addTimeline(eventId, null, "ANALYSIS_FAILED", "AI智能研判失败", safeError(ex), "SYSTEM", null);
                return getEventDetail(eventId);
            }
            log.warn("在线AI研判失败，已降级到演示分析，eventId={}, reason={}", eventId, safeError(ex));
            addTimeline(eventId, null, "MODEL_FALLBACK", "AI智能分析降级", safeError(ex), "SYSTEM", null);
            model = mockGateway.analyze(event);
            model.setProvider("mock-fallback");
            model.setRawResponse("fallback-reason:" + safeError(ex));
            model.getObservations().add("在线AI分析暂不可用，本次结果由演示分析生成，必须人工复核");
            model.setIdentityReliable(false);
        }
        applyRecognizedIdentity(event, model);
        intelligenceService.correlateEvent(eventId);
        AiAnalysis analysis = toAnalysis(event, model);
        analysisMapper.insert(analysis);
        AiRuleDecision rule = ruleService.decide(event, model);
        GovernanceIncident incident = incidentMapper.selectOne(new LambdaQueryWrapper<GovernanceIncident>().eq(GovernanceIncident::getEventId, eventId));
        if (incident == null) incident = createIncident(event, model, rule);
        else updateIncident(incident, model, rule);
        createTaskIfNeeded(incident, rule);
        if ("REJECTED".equals(rule.getDecisionCode())) {
            jdbcTemplate.update("UPDATE workflow_task SET state='CANCELLED', completed_at=?, result_remark=?, update_time=? WHERE incident_id=? AND state='OPEN'",
                    LocalDateTime.now(), "重研判结果无效，任务自动取消", LocalDateTime.now(), incident.getIncidentId());
        }
        writeDecision(eventId, incident.getIncidentId(), rule, "RULE_ENGINE", null);
        addTimeline(eventId, incident.getIncidentId(), "DECIDED", "规则引擎完成决策", rule.getExplanation(), "SYSTEM", null);
        event.setRiskLevel(model.getRiskLevel());
        event.setStatus(statusForDecision(rule.getDecisionCode()));
        event.setUpdateTime(LocalDateTime.now());
        eventMapper.updateById(event);
        return getEventDetail(eventId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> confirm(Long incidentId, Integer operatorUserId, AiIncidentReviewRequest request) {
        return review(incidentId, operatorUserId, request, true);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> reject(Long incidentId, Integer operatorUserId, AiIncidentReviewRequest request) {
        return review(incidentId, operatorUserId, request, false);
    }

    private Map<String, Object> review(Long incidentId, Integer operator, AiIncidentReviewRequest request, boolean confirm) {
        requireSchema();
        List<Long> locked = jdbcTemplate.queryForList(
                "SELECT incident_id FROM governance_incident WHERE incident_id=? FOR UPDATE",
                Long.class, incidentId);
        if (locked.isEmpty()) throw new BusinessException("治理事件不存在");
        GovernanceIncident incident = incidentMapper.selectById(incidentId);
        if (incident == null) throw new BusinessException("治理事件不存在");
        if ("CONFIRMED".equals(incident.getState()) || "REJECTED".equals(incident.getState())) throw new BusinessException("该事件已完成审核");
        String remark = request == null ? null : request.getRemark();
        if (operator == null) throw new BusinessException("操作人不能为空");
        AiEvent event = eventMapper.selectById(incident.getEventId());
        if (confirm) {
            if (incident.getVehicleId() == null || incident.getUserId() == null) throw new BusinessException("身份未可靠匹配，无法确认处罚");
            Integer violationTypeId = resolveViolationTypeId(incident.getEventType());
            if (violationTypeId != null) {
                Map<String, Object> type = findViolationType(violationTypeId);
                Violation violation = new Violation();
                violation.setVehicleId(incident.getVehicleId());
                violation.setUserId(incident.getUserId());
                violation.setIllegalType(violationTypeId);
                violation.setDeductionPoints(numberValue(type.get("deduction_points"), 0));
                violation.setIllegalLng(event.getLongitude());
                violation.setIllegalLat(event.getLatitude());
                violation.setIllegalTime(event.getOccurredAt());
                violation.setEvidenceUrl(firstEvidence(event));
                violation.setIllegalSource(3);
                violationService.reportViolation(violation);
                incident.setViolationId(violation.getIllegalId());
            }
            if (violationTypeId == null) {
                notificationService.sendPersonalNotification(incident.getTitle(), incident.getSummary(), incident.getUserId());
            }
            incident.setState("CONFIRMED"); event.setStatus("CONFIRMED");
        } else { incident.setState("REJECTED"); event.setStatus("REJECTED"); }
        incident.setReviewerId(operator); incident.setReviewRemark(trimToNull(remark)); incident.setReviewedAt(LocalDateTime.now()); incident.setUpdateTime(LocalDateTime.now()); incidentMapper.updateById(incident);
        jdbcTemplate.update("UPDATE workflow_task SET state=?, completed_at=?, result_remark=?, update_time=? WHERE incident_id=? AND state='OPEN'", confirm ? "COMPLETED" : "CANCELLED", LocalDateTime.now(), remark, LocalDateTime.now(), incidentId);
        event.setUpdateTime(LocalDateTime.now()); eventMapper.updateById(event);
        String confirmExplanation = incident.getViolationId() == null ? "管理员确认安全事件并完成通知" : "管理员确认并进入正式违规链路";
        writeDecision(event.getEventId(), incidentId, new AiRuleDecision(confirm ? "CONFIRMED" : "REJECTED", confirm ? confirmExplanation : "管理员驳回事件", List.of(confirm ? "管理员确认" : "管理员驳回")), "ADMIN", operator);
        addTimeline(event.getEventId(), incidentId, confirm ? "CONFIRMED" : "REJECTED", confirm ? "管理员确认" : "管理员驳回", remark, "ADMIN", operator);
        return getEventDetail(event.getEventId());
    }

    @Override
    public Map<String, Object> stats() {
        requireSchema();
        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("total", scalar("SELECT COUNT(*) FROM ai_event"));
        stats.put("pendingReview", scalar("SELECT COUNT(*) FROM governance_incident WHERE state IN ('PENDING_REVIEW','PENDING_CONFIRMATION')"));
        stats.put("confirmed", scalar("SELECT COUNT(*) FROM governance_incident WHERE state='CONFIRMED'"));
        stats.put("rejected", scalar("SELECT COUNT(*) FROM governance_incident WHERE state='REJECTED'"));
        stats.put("openTasks", scalar("SELECT COUNT(*) FROM workflow_task WHERE state='OPEN'"));
        stats.put("byType", jdbcTemplate.queryForList("SELECT event_type AS type, COUNT(*) AS count FROM ai_event GROUP BY event_type ORDER BY count DESC"));
        return stats;
    }

    private void bindIdentity(AiEvent event) {
        if (event.getVehicleId() == null && StringUtils.hasText(event.getLicensePlate())) {
            Vehicle vehicle = vehicleMapper.selectByLicensePlate(event.getLicensePlate());
            if (vehicle != null) { event.setVehicleId(vehicle.getVehicleId()); event.setUserId(vehicle.getUserId()); event.setIdentityMethod("LICENSE_PLATE"); event.setIdentityConfidence(0.98D); }
        }
        if (event.getIdentityConfidence() == null) event.setIdentityConfidence(event.getVehicleId() != null && event.getUserId() != null ? 0.92D : 0.0D);
        if (event.getIdentityMethod() == null) event.setIdentityMethod(event.getVehicleId() != null && event.getUserId() != null ? "PAYLOAD" : "UNMATCHED");
    }

    private void applyRecognizedIdentity(AiEvent event, AiModelDecision model) {
        if (event.getVehicleId() != null && event.getUserId() != null) {
            return;
        }
        if (!model.isPlateVisible() || !StringUtils.hasText(model.getRecognizedPlate())
                || model.getPlateConfidence() < properties.getIdentityThreshold()) {
            model.setIdentityReliable(false);
            return;
        }
        String plate = model.getRecognizedPlate().replaceAll("[\\s·•]", "").toUpperCase(Locale.ROOT);
        if (plate.length() < 5 || plate.length() > 12) {
            model.setIdentityReliable(false);
            model.getObservations().add("识别出的车牌格式无效，已转人工复核");
            return;
        }
        event.setLicensePlate(plate);
        event.setVehicleId(null);
        event.setUserId(null);
        event.setIdentityMethod(null);
        event.setIdentityConfidence(null);
        bindIdentity(event);
        if (event.getVehicleId() == null || event.getUserId() == null) {
            event.setIdentityMethod("VISION_PLATE_UNMATCHED");
            event.setIdentityConfidence(model.getPlateConfidence());
            model.setIdentityReliable(false);
            model.getObservations().add("车牌 " + plate + " 未匹配到已登记车辆，需人工复核");
        } else {
            event.setIdentityMethod("AI_VISION_LICENSE_PLATE");
            event.setIdentityConfidence(model.getPlateConfidence());
            model.setIdentityReliable(true);
            model.getObservations().add("车牌 " + plate + " 已匹配车辆及车主档案");
        }
        event.setUpdateTime(LocalDateTime.now());
        eventMapper.updateById(event);
        saveIdentitySnapshot(event);
    }

    private void saveIdentitySnapshot(AiEvent event) {
        jdbcTemplate.update("INSERT INTO ai_identity_match (event_id, vehicle_id, user_id, match_method, confidence, match_status, explanation) VALUES (?, ?, ?, ?, ?, ?, ?)", event.getEventId(), event.getVehicleId(), event.getUserId(), event.getIdentityMethod(), event.getIdentityConfidence(), event.getVehicleId() != null && event.getUserId() != null ? "MATCHED" : "UNMATCHED", event.getVehicleId() != null && event.getUserId() != null ? "车牌与车辆档案匹配" : "未找到可靠车辆身份");
    }

    private AiAnalysis toAnalysis(AiEvent event, AiModelDecision model) {
        AiAnalysis a = new AiAnalysis(); a.setEventId(event.getEventId()); a.setProvider(model.getProvider()); a.setModelName(model.getModelName()); a.setPromptVersion("governance-v1"); a.setValidEvent(model.isValid()); a.setConfidence(model.getConfidence()); a.setRiskLevel(model.getRiskLevel()); a.setIdentityReliable(model.isIdentityReliable()); a.setEvidenceSufficient(model.isEvidenceSufficient()); a.setSummary(model.getSummary()); a.setRecommendedAction(limit(model.getRecommendedAction(), 64)); a.setNotificationText(model.getNotificationText()); a.setObservationsJson(json(model.getObservations())); a.setRawResponse(model.getRawResponse()); a.setLatencyMs(model.getLatencyMs()); a.setInputTokens(model.getInputTokens()); a.setOutputTokens(model.getOutputTokens()); a.setCreateTime(LocalDateTime.now()); return a;
    }

    private GovernanceIncident createIncident(AiEvent event, AiModelDecision model, AiRuleDecision rule) {
        GovernanceIncident i = new GovernanceIncident(); i.setIncidentCode("GI-" + UUID.randomUUID().toString().replace("-", "").substring(0, 16).toUpperCase(Locale.ROOT)); i.setEventId(event.getEventId()); i.setVehicleId(event.getVehicleId()); i.setUserId(event.getUserId()); i.setEventType(event.getEventType()); i.setTitle("AI治理：" + displayType(event.getEventType())); i.setSummary(model.getSummary()); i.setRiskLevel(model.getRiskLevel()); i.setDecisionType(rule.getDecisionCode()); i.setState(incidentStateForDecision(rule.getDecisionCode())); i.setDeadline(LocalDateTime.now().plusHours("CRITICAL".equals(model.getRiskLevel()) ? 2 : 24)); i.setCreateTime(LocalDateTime.now()); i.setUpdateTime(LocalDateTime.now()); incidentMapper.insert(i); return i;
    }

    private void updateIncident(GovernanceIncident i, AiModelDecision model, AiRuleDecision rule) { i.setSummary(model.getSummary()); i.setRiskLevel(model.getRiskLevel()); i.setDecisionType(rule.getDecisionCode()); i.setState(incidentStateForDecision(rule.getDecisionCode())); i.setUpdateTime(LocalDateTime.now()); incidentMapper.updateById(i); }
    private void createTaskIfNeeded(GovernanceIncident i, AiRuleDecision rule) { if ("REJECTED".equals(rule.getDecisionCode()) || taskMapper.selectCount(new LambdaQueryWrapper<WorkflowTask>().eq(WorkflowTask::getIncidentId, i.getIncidentId()).eq(WorkflowTask::getState, "OPEN")) > 0) return; WorkflowTask t = new WorkflowTask(); t.setIncidentId(i.getIncidentId()); t.setTaskType("INCIDENT_REVIEW"); t.setTaskTitle(i.getTitle()); t.setState("OPEN"); t.setPriority(i.getRiskLevel()); t.setDueAt(i.getDeadline()); t.setCreateTime(LocalDateTime.now()); t.setUpdateTime(LocalDateTime.now()); taskMapper.insert(t); }
    private void saveEvidence(AiEvent e) { if (!StringUtils.hasText(e.getEvidenceUrls())) return; try { List<?> urls = objectMapper.readValue(e.getEvidenceUrls(), List.class); for (Object url : urls) jdbcTemplate.update("INSERT INTO ai_evidence (event_id, evidence_type, evidence_url, metadata_json) VALUES (?, 'IMAGE', ?, ?)", e.getEventId(), String.valueOf(url), "{}"); } catch (Exception ignored) { log.warn("事件证据解析失败 eventId={}", e.getEventId()); } }
    private void writeDecision(Long eventId, Long incidentId, AiRuleDecision d, String source, Integer operator) { jdbcTemplate.update("INSERT INTO decision_log (event_id, incident_id, decision_source, decision_code, rule_hits_json, explanation, operator_user_id) VALUES (?, ?, ?, ?, ?, ?, ?)", eventId, incidentId, source, d.getDecisionCode(), json(d.getRuleHits()), d.getExplanation(), operator); }
    private void addTimeline(Long eventId, Long incidentId, String stage, String title, String detail, String actor, Integer actorId) { jdbcTemplate.update("INSERT INTO event_timeline (event_id, incident_id, stage, title, detail, actor_type, actor_id) VALUES (?, ?, ?, ?, ?, ?, ?)", eventId, incidentId, stage, title, detail, actor, actorId); }
    private String firstEvidence(AiEvent e) { if (!StringUtils.hasText(e.getEvidenceUrls())) return null; try { List<?> list = objectMapper.readValue(e.getEvidenceUrls(), List.class); return list.isEmpty() ? null : String.valueOf(list.get(0)); } catch (Exception ex) { return null; } }
    private Integer resolveViolationTypeId(String eventType) {
        if (!StringUtils.hasText(eventType) || properties.getViolationTypeMapping() == null) return null;
        return properties.getViolationTypeMapping().entrySet().stream()
                .filter(entry -> eventType.equalsIgnoreCase(entry.getKey()))
                .map(Map.Entry::getValue)
                .findFirst()
                .orElse(null);
    }

    private Map<String, Object> findViolationType(Integer typeId) {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                "SELECT type_id, type_name, deduction_points FROM violation_type WHERE type_id=?", typeId);
        if (rows.isEmpty()) {
            throw new BusinessException("事件对应的违规类型不存在，请检查AI违规类型映射：" + typeId);
        }
        return rows.get(0);
    }

    private int numberValue(Object value, int defaultValue) {
        return value instanceof Number number ? number.intValue() : defaultValue;
    }
    private String statusForDecision(String code) { return "REJECTED".equals(code) ? "REJECTED" : "AUTO_ACCEPTED".equals(code) ? "AUTO_ACCEPTED" : "REVIEW_PENDING"; }
    private String incidentStateForDecision(String code) { return "REJECTED".equals(code) ? "REJECTED" : "AUTO_ACCEPTED".equals(code) ? "PENDING_CONFIRMATION" : "PENDING_REVIEW"; }
    private String displayType(String type) { return switch (type) { case "ILLEGAL_PARKING" -> "违规停车"; case "OVERLOAD" -> "违规载人"; case "RESTRICTED_AREA" -> "禁区通行"; case "THEFT_RISK" -> "异常移动"; case "SMOKE_DETECTED" -> "烟雾风险"; case "FIRE_DETECTED" -> "明火风险"; default -> "未佩戴头盔"; }; }
    private void requireSchema() { if (!schema.ensureTablesExist()) throw new BusinessException("AI治理数据表未就绪，请检查数据库连接"); }
    private Object scalar(String sql) { return jdbcTemplate.queryForObject(sql, Object.class); }
    private String json(Object value) { try { return objectMapper.writeValueAsString(value); } catch (Exception ex) { return "{}"; } }
    private String digest(String text) { try { byte[] hash = MessageDigest.getInstance("SHA-256").digest(text.getBytes(StandardCharsets.UTF_8)); StringBuilder b = new StringBuilder(); for (byte x : hash) b.append(String.format("%02x", x)); return b.toString(); } catch (Exception ex) { return UUID.randomUUID().toString(); } }
    private String safeError(Exception ex) { String message = ex.getMessage(); if (!StringUtils.hasText(message)) return ex.getClass().getSimpleName(); return message.length() > 400 ? message.substring(0, 400) : message; }
    private String limit(String value, int maxLength) { return value == null || value.length() <= maxLength ? value : value.substring(0, maxLength); }
    private String trimToNull(String value) { return StringUtils.hasText(value) ? value.trim() : null; }
}
