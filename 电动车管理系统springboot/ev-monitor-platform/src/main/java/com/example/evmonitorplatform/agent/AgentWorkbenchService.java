package com.example.evmonitorplatform.agent;

import com.example.evmonitorplatform.common.exception.BusinessException;
import com.example.evmonitorplatform.config.AgentDshProperties;
import com.example.evmonitorplatform.config.AiGovernanceProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import jakarta.annotation.PostConstruct;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Agent workbench service — orchestrates the campus ev-multisource context,
 * calls the configured LLM directly via {@link AiGovernanceProperties}, and
 * keeps per-session message history in-process.
 *
 * <p>
 * DSH is still started/managed by {@link DshProcessManager} as an embedded
 * runtime (process lifecycle, health, launch token) — this service reports its
 * state but uses the direct OpenAI-compatible HTTP path for reasoning because
 * DSH's Web profile does not expose the session RPC as HTTP routes.
 */
@Service
@Slf4j
public class AgentWorkbenchService {

    private final DshProcessManager dsh;
    private final MultiSourceContextService contextService;
    private final AgentDshProperties agentProps;
    private final AiGovernanceProperties aiProps;
    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;

    public AgentWorkbenchService(
            DshProcessManager dsh,
            MultiSourceContextService contextService,
            AgentDshProperties agentProps,
            AiGovernanceProperties aiProps,
            JdbcTemplate jdbcTemplate,
            ObjectMapper objectMapper) {
        this.dsh = dsh;
        this.contextService = contextService;
        this.agentProps = agentProps;
        this.aiProps = aiProps;
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = objectMapper;
        this.llmClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(Math.min(aiProps.getTimeoutSeconds(), 20)))
                .build();
    }

    /** In-memory per-session history (guarded process lifetime). */
    private final Map<String, List<Map<String, Object>>> sessionHistory = new ConcurrentHashMap<>();

    /**
     * In-memory session metadata: id → latest-assigned title (first user prompt
     * truncate). Mirrored to the agent_status endpoint payload so callers can
     * enumerate sessions without another backend table.
     */
    private final Map<String, String> sessionTitle = new ConcurrentHashMap<>();

    private final HttpClient llmClient;

    // ---------- Tool definitions (ReAct function-calling) ----------

    /** Runtime tool: schema (for LLM) + executor (runs on our side). */
    private record ToolSpec(
            String name,
            String description,
            Map<String, Object> parameters,
            Function<Map<String, Object>, String> executor) {
    }

    /** All available tools the agent can call (query + write-back mixed). */
    private List<ToolSpec> tools;
    private final Map<String, ToolSpec> toolByName = new LinkedHashMap<>();

    @PostConstruct
    public void initTools() {
        this.tools = buildTools();
        this.toolByName.clear();
        for (ToolSpec t : this.tools)
            toolByName.put(t.name(), t);
    }

    private List<ToolSpec> buildTools() {
        return List.of(
                // -------- READ-ONLY (4) --------

                new ToolSpec("query_cameras", "查询摄像头列表。返回摄像头名称、位置、在线/离线/故障状态、最近心跳时间。",
                        Map.of("type", "object",
                                "properties", Map.of(
                                        "status", Map.of("type", "string", "description",
                                                "过滤状态：online / offline / malfunction，不传返回全部"),
                                        "limit", Map.of("type", "integer", "description", "最多返回多少条，默认 50")),
                                "required", List.of()),
                        args -> {
                            String status = (String) args.get("status");
                            int lim = args.containsKey("limit") ? (int) args.get("limit") : 50;
                            try {
                                String sql = "SELECT camera_name, location, status, last_heartbeat FROM camera_monitor";
                                List<String> where = new ArrayList<>();
                                List<Object> params = new ArrayList<>();
                                if (status != null && !status.isBlank()) {
                                    where.add("status = ?");
                                    params.add(status);
                                }
                                if (!where.isEmpty())
                                    sql += " WHERE " + String.join(" AND ", where);
                                sql += " ORDER BY last_heartbeat DESC LIMIT " + lim;
                                return objectMapper
                                        .writeValueAsString(jdbcTemplate.queryForList(sql, params.toArray()));
                            } catch (Exception e) {
                                return "{\"error\":\"" + e.getMessage() + "\"}";
                            }
                        }),

                new ToolSpec("query_ai_events",
                        "查询AI事件表。返回事件ID、类型、风险等级、车牌号、设备ID、发生时间、位置名。支持按风险等级/事件类型/车牌号过滤，默认查最近72小时。",
                        Map.of("type", "object",
                                "properties", Map.of(
                                        "riskLevel", Map.of("type", "string", "description", "HIGH / MEDIUM / LOW"),
                                        "eventType",
                                        Map.of("type", "string", "description", "例如 NO_HELMET / RESTRICTED_AREA 等"),
                                        "licensePlate", Map.of("type", "string", "description", "按车牌号精确过滤"),
                                        "hours", Map.of("type", "integer", "description", "查最近N小时内的事件，默认 72")),
                                "required", List.of()),
                        args -> {
                            String risk = (String) args.get("riskLevel");
                            String type = (String) args.get("eventType");
                            String plate = (String) args.get("licensePlate");
                            int hours = args.containsKey("hours") ? ((Number) args.get("hours")).intValue() : 72;
                            try {
                                StringBuilder sql = new StringBuilder(
                                        "SELECT event_id, event_type, risk_level, license_plate, device_id, occurred_at, location_name, status"
                                                + " FROM ai_event WHERE occurred_at >= DATE_SUB(NOW(), INTERVAL ? HOUR)");
                                List<Object> p = new ArrayList<>(List.of(hours));
                                if (risk != null && !risk.isBlank()) {
                                    sql.append(" AND risk_level = ?");
                                    p.add(risk);
                                }
                                if (type != null && !type.isBlank()) {
                                    sql.append(" AND event_type = ?");
                                    p.add(type);
                                }
                                if (plate != null && !plate.isBlank()) {
                                    sql.append(" AND license_plate = ?");
                                    p.add(plate);
                                }
                                sql.append(" ORDER BY occurred_at DESC LIMIT 200");
                                return objectMapper
                                        .writeValueAsString(jdbcTemplate.queryForList(sql.toString(), p.toArray()));
                            } catch (Exception e) {
                                return "{\"error\":\"" + e.getMessage() + "\"}";
                            }
                        }),

                new ToolSpec("query_violations", "查询历史违规记录。返回违规ID、违规类型编码、发生时间、经纬度、车辆ID、违法状态。支持按类型/时间窗口过滤。",
                        Map.of("type", "object",
                                "properties", Map.of(
                                        "violationType", Map.of("type", "integer", "description", "违规类型编码，如 1=未戴头盔"),
                                        "hours", Map.of("type", "integer", "description", "查最近N小时，默认 72")),
                                "required", List.of()),
                        args -> {
                            Number vtype = (Number) args.get("violationType");
                            int hours = args.containsKey("hours") ? ((Number) args.get("hours")).intValue() : 72;
                            try {
                                StringBuilder sql = new StringBuilder(
                                        "SELECT illegal_id, illegal_type, illegal_time, illegal_state, illegal_lng, illegal_lat, vehicle_id, user_id"
                                                + " FROM vehicle_illegal_record WHERE deleted = 0 AND illegal_time >= DATE_SUB(NOW(), INTERVAL ? HOUR)");
                                List<Object> p = new ArrayList<>(List.of(hours));
                                if (vtype != null) {
                                    sql.append(" AND illegal_type = ?");
                                    p.add(vtype.intValue());
                                }
                                sql.append(" ORDER BY illegal_time DESC LIMIT 200");
                                return objectMapper
                                        .writeValueAsString(jdbcTemplate.queryForList(sql.toString(), p.toArray()));
                            } catch (Exception e) {
                                return "{\"error\":\"" + e.getMessage() + "\"}";
                            }
                        }),

                new ToolSpec("query_locations", "查询车辆最近定位信息。输入车辆ID，返回经纬度、最后上报时间、GPS状态、电量。",
                        Map.of("type", "object",
                                "properties", Map.of(
                                        "vehicleId", Map.of("type", "integer", "description", "车辆ID（必填）"),
                                        "limit", Map.of("type", "integer", "description", "最多返回多少条，默认 50")),
                                "required", List.of("vehicleId")),
                        args -> {
                            Number vid = (Number) args.get("vehicleId");
                            int lim = args.containsKey("limit") ? ((Number) args.get("limit")).intValue() : 50;
                            try {
                                String sql = "SELECT vehicle_id, current_lng, current_lat, status, battery_level, last_report_time"
                                        + " FROM location_device WHERE vehicle_id = ? ORDER BY last_report_time DESC LIMIT "
                                        + lim;
                                return objectMapper.writeValueAsString(jdbcTemplate.queryForList(sql, vid.intValue()));
                            } catch (Exception e) {
                                return "{\"error\":\"" + e.getMessage() + "\"}";
                            }
                        }),

                // -------- WRITE-BACK (2) --------

                new ToolSpec("create_patrol_mission",
                        "创建一条新的巡检工单。输入区域名（必填），自动生成 mission_code、坐标和时间。返回新任务的 mission_code。",
                        Map.of("type", "object",
                                "properties", Map.of(
                                        "areaName", Map.of("type", "string", "description", "巡检区域名，如 东校区 或 图书馆周边"),
                                        "goal", Map.of("type", "string", "description", "巡检目标描述（可选，默认自动生成）")),
                                "required", List.of("areaName")),
                        args -> {
                            String area = (String) args.get("areaName");
                            String goalDesc = (String) args.getOrDefault("goal", "针对 " + area + " 的常规巡检任务");
                            String code = "PM-"
                                    + UUID.randomUUID().toString().replace("-", "").substring(0, 16).toUpperCase();
                            try {
                                jdbcTemplate.update(
                                        "INSERT INTO patrol_mission(mission_code, goal, area_name, center_lng, center_lat, state, duration_limit_sec, battery_start, battery_current, plan_version, replans, create_time, update_time)"
                                                + " VALUES (?, ?, ?, 113.000000, 28.140000, 'PLANNED', 720, 100, 100, 0, 0, NOW(), NOW())",
                                        code, goalDesc, area);
                                return "{\"ok\":true,\"missionCode\":\"" + code + "\"}";
                            } catch (Exception e) {
                                return "{\"error\":\"" + e.getMessage() + "\"}";
                            }
                        }),

                new ToolSpec("update_incident_state",
                        "更新一条治理事件的状态（用于申诉审核或事件结案）。接受 incident_id 和目标 state（如 APPROVED / REJECTED / CLOSED）。",
                        Map.of("type", "object",
                                "properties", Map.of(
                                        "incidentId",
                                        Map.of("type", "integer", "description",
                                                "治理事件ID（governance_incident.incident_id）"),
                                        "state",
                                        Map.of("type", "string", "description", "目标状态：APPROVED / REJECTED / CLOSED"),
                                        "reviewRemark", Map.of("type", "string", "description", "审核备注（可选）")),
                                "required", List.of("incidentId", "state")),
                        args -> {
                            int incidentId = ((Number) args.get("incidentId")).intValue();
                            String state = (String) args.get("state");
                            String remark = (String) args.getOrDefault("reviewRemark", "AI智能体自动审核");
                            try {
                                jdbcTemplate.update(
                                        "UPDATE governance_incident SET state = ?, review_remark = ?, reviewed_at = NOW() WHERE incident_id = ?",
                                        state, remark, incidentId);
                                return "{\"ok\":true,\"incidentId\":" + incidentId + ",\"newState\":\"" + state + "\"}";
                            } catch (Exception e) {
                                return "{\"error\":\"" + e.getMessage() + "\"}";
                            }
                        }));
    }
    // ---------- status / context ----------

    public Map<String, Object> status() {
        Map<String, Object> result = new LinkedHashMap<>(dsh.statusSnapshot());
        result.put("integration", "PLATFORM_TASKS_PLUS_DSH_RUNTIME");
        result.put("dataSources", List.of("摄像头", "车辆定位", "电子围栏", "AI事件", "历史违规", "巡检任务"));
        result.put("writeBackPolicy", "只读分析；处罚、通知、审核必须回到平台人工确认");
        result.put("promptVersion", agentProps.getPromptVersion());
        result.put("agentRole", "校园电动车时空治理智能体");
        result.put("llmProvider", "openai-compatible");
        result.put("llmModel", aiProps.getModel());
        result.put("llmEndpoint", aiProps.getBaseUrl() + "/responses");
        result.put("llmConfigured", aiProps.getApiKey() != null && !aiProps.getApiKey().isBlank());
        result.put("sessionCount", sessionHistory.size());
        return result;
    }

    public Map<String, Object> context(int limit) {
        return contextService.snapshot(limit);
    }

    // ---------- run ----------

    /**
     * Submit a user goal → assemble prompt + multisource context → call LLM.
     *
     * @return envelope with sessionId (new or existing), the assistant reply,
     *         runCode, and projection metadata so the caller can render it as a
     *         chat bubble immediately (no more queue-poll).
     */
    public Map<String, Object> run(String goal, String sessionId, String preset, int limit) {
        if (goal == null || goal.isBlank() || goal.length() > 4000)
            throw new IllegalArgumentException("任务目标不能为空且不超过4000字");

        // 1. decide session id (server generates a fresh one on first turn)
        String currentSession = (sessionId == null || sessionId.isBlank())
                ? "AGENT-" + UUID.randomUUID().toString().replace("-", "").substring(0, 12).toUpperCase()
                : sessionId;

        // 2. multisource snapshot (read-only facts)
        Map<String, Object> snapshot = contextService.snapshot(limit);

        // 3. append user message to history (before LLM call)
        appendMessage(currentSession, "user", goal);
        String firstUser = sessionHistory.get(currentSession).stream()
                .filter(m -> "user".equals(m.get("role")) && m
                        .equals(sessionHistory.get(currentSession).get(sessionHistory.get(currentSession).size() - 1)))
                .findFirst().map(m -> String.valueOf(m.get("content"))).orElse(goal);
        sessionTitle.put(currentSession, firstUser.length() > 24 ? firstUser.substring(0, 24) + "…" : firstUser);

        // 4. build prompt — system contract + context JSON + user goal
        String prompt = buildPrompt(goal, snapshot);

        // 5. call LLM synchronously — or return a synthetic reply in mock mode
        String assistantReply;
        try {
            if ("mock".equalsIgnoreCase(aiProps.getMode())) {
                assistantReply = mockReply(goal, snapshot);
            } else {
                assistantReply = callLLM(prompt, currentSession);
            }
        } catch (RuntimeException ex) {
            assistantReply = "⚠️ 智能体暂时无法连接大模型服务：" + ex.getMessage()
                    + "\n\n（当前配置 provider=" + aiProps.getBaseUrl() + "，model=" + aiProps.getModel()
                    + "，API key=" + (aiProps.getApiKey() == null || aiProps.getApiKey().isBlank() ? "未配置" : "已配置")
                    + "。如需演示完整对话效果，可设置 AI_MODE=mock 走本地模拟模式）";
        }

        // 6. append assistant message to history
        appendMessage(currentSession, "assistant", assistantReply);

        // 7. assemble response envelope
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("sessionId", currentSession);
        result.put("accepted", true);
        result.put("context", snapshot);
        result.put("runCode", "AGENT-" + UUID.randomUUID().toString().replace("-", "").substring(0, 16).toUpperCase());
        result.put("promptVersion", agentProps.getPromptVersion());
        result.put("assistantReply", assistantReply);
        result.put("messages", sanitizedMessages(currentSession));
        saveRun(result.get("runCode").toString(), goal, currentSession, preset, "COMPLETED", snapshot, result, null);
        return result;
    }

    // ---------- run (SSE streaming + ReAct tool-calling) ----------

    /** Max ReAct iterations (prevents infinite tool loops). */
    private static final int MAX_TOOL_ITERATIONS = 5;

    /**
     * One ReAct turn result from LLM stream: accumulated content + all tool_calls +
     * finish_reason.
     */
    private record ReActTurn(
            String content,
            String finishReason,
            List<Map<String, Object>> toolCalls) {
    }

    /**
     * Streaming variant with ReAct tool-calling. Emits SSE frames:
     *
     * <pre>
     * event: context             data: {...}                             ← multisource snapshot (once)
     * event: content             data: {"text":"..."}                    ← assistant reply delta (streaming)
     * event: tool_call_start     data: {"name":"query_cameras","args":{...}} ← about to call this tool
     * event: tool_call_result    data: {"name":"query_cameras","result":"..."} ← tool output
     * event: done                data: {"sessionId":"...","runCode":"...","messages":[...]}
     * event: error               data: {"message":"..."}
     * </pre>
     *
     * <p>
     * ReAct loop: send messages + tools to LLM → accumulate streamed content &
     * tool_calls →
     * if finish_reason=tool_calls, execute tools, append tool-result messages, loop
     * again →
     * if finish_reason=stop (final answer), break.
     */
    public void streamRun(String goal, String sessionId, String preset, int limit,
            BiConsumer<String, String> frameEmitter) {
        if (goal == null || goal.isBlank() || goal.length() > 4000) {
            frameEmitter.accept("error", "{\"message\":\"任务目标不能为空且不超过4000字\"}");
            frameEmitter.accept("done", "{\"error\":true}");
            return;
        }

        String currentSession = (sessionId == null || sessionId.isBlank())
                ? "AGENT-" + UUID.randomUUID().toString().replace("-", "").substring(0, 12).toUpperCase()
                : sessionId;

        Map<String, Object> snapshot = contextService.snapshot(limit);
        appendMessage(currentSession, "user", goal);
        String firstUser = sessionHistory.get(currentSession).stream()
                .filter(m -> "user".equals(m.get("role")))
                .reduce((a, b) -> b)
                .map(m -> String.valueOf(m.get("content"))).orElse(goal);
        sessionTitle.put(currentSession, firstUser.length() > 24 ? firstUser.substring(0, 24) + "…" : firstUser);

        // 1. emit context frame once
        try {
            String contextJson = objectMapper.writeValueAsString(snapshot);
            frameEmitter.accept("context", contextJson);
        } catch (Exception ignored) {
        }

        String runCode = "AGENT-" + UUID.randomUUID().toString().replace("-", "").substring(0, 16).toUpperCase();

        // 2. ReAct loop state
        List<Map<String, Object>> messages = new ArrayList<>();
        messages.add(Map.of("role", "system", "content",
                "你是校园电动车时空治理智能体（ReAct Agent）。你可以使用下面列出的工具查询平台真实数据或执行安全写入。"
                        + "先思考需要什么信息 → 调用合适的工具 → 基于工具返回结果给出最终回答。"
                        + "回答要基于事实，数据缺失要明确说明，不捏造。"
                        + "如果用户只是闲聊（问候/再见），可以直接回复，不需要调工具。"));
        messages.add(Map.of("role", "user", "content", goal));

        StringBuilder finalReply = new StringBuilder();

        if ("mock".equalsIgnoreCase(aiProps.getMode()) || aiProps.getApiKey() == null
                || aiProps.getApiKey().isBlank()) {
            // mock mode: no ReAct, just typewriter the mock text
            String mockText = mockReply(goal, snapshot);
            for (int i = 0; i < mockText.length(); i++) {
                String ch = mockText.substring(i, i + 1);
                finalReply.append(ch);
                try {
                    frameEmitter.accept("content", objectMapper.writeValueAsString(Map.of("text", ch)));
                } catch (Exception ignored) {
                }
                try {
                    Thread.sleep(30);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        } else {
            try {
                for (int iteration = 1; iteration <= MAX_TOOL_ITERATIONS; iteration++) {
                    boolean isLastIteration = (iteration == MAX_TOOL_ITERATIONS);
                    ReActTurn turn = callLLMStreamWithTools(messages, frameEmitter, isLastIteration);

                    if (turn.content() != null && !turn.content().isEmpty()) {
                        finalReply.append(turn.content());
                    }

                    if ("tool_calls".equals(turn.finishReason()) && !isLastIteration) {
                        // --- execute tool calls ---
                        // Strip internal-only fields before putting tool_calls back into LLM messages
                        List<Map<String, Object>> protocolToolCalls = new ArrayList<>();
                        for (Map<String, Object> tc : turn.toolCalls()) {
                            Map<String, Object> clean = new LinkedHashMap<>(tc);
                            clean.remove("parsedArgs");
                            protocolToolCalls.add(clean);
                        }
                        Map<String, Object> assistantMsg = new LinkedHashMap<>();
                        assistantMsg.put("role", "assistant");
                        String assistantContent = turn.content() == null || turn.content().isBlank()
                                ? "好的，我现在调用工具来查询..."
                                : turn.content();
                        assistantMsg.put("content", assistantContent);
                        assistantMsg.put("tool_calls", protocolToolCalls);
                        messages.add(assistantMsg);

                        for (Map<String, Object> tc : turn.toolCalls()) {
                            String callId = String.valueOf(tc.get("id"));
                            // tool name lives under tool_call.function.name
                            @SuppressWarnings("unchecked")
                            Map<String, Object> fnObj = (Map<String, Object>) tc.getOrDefault("function", Map.of());
                            String toolName = String.valueOf(fnObj.getOrDefault("name", "unknown"));
                            // args are either pre-parsed (parsedArgs) or need re-parsing from the JSON
                            // string
                            @SuppressWarnings("unchecked")
                            Map<String, Object> args = (Map<String, Object>) tc.getOrDefault("parsedArgs", Map.of());

                            ToolSpec tool = toolByName.get(toolName);
                            String resultJson;
                            if (tool == null) {
                                resultJson = "{\"error\":\"unknown tool: " + toolName + "\"}";
                            } else {
                                try {
                                    frameEmitter.accept("tool_call_start",
                                            objectMapper.writeValueAsString(Map.of("name", toolName, "args", args)));
                                    resultJson = tool.executor().apply(args);
                                    // cap oversized results (some query_* tools can return 200 rows)
                                    if (resultJson.length() > 6000) {
                                        resultJson = resultJson.substring(0, 5800) + "\n...(truncated)";
                                    }
                                    frameEmitter.accept("tool_call_result",
                                            objectMapper.writeValueAsString(
                                                    Map.of("name", toolName, "result", resultJson)));
                                } catch (Exception e) {
                                    resultJson = "{\"error\":\"" + e.getMessage() + "\"}";
                                    try {
                                        frameEmitter.accept("tool_call_result",
                                                objectMapper.writeValueAsString(
                                                        Map.of("name", toolName, "result", resultJson)));
                                    } catch (Exception ignored) {
                                    }
                                }
                            }

                            Map<String, Object> toolMsg = new LinkedHashMap<>();
                            toolMsg.put("role", "tool");
                            toolMsg.put("tool_call_id", callId);
                            toolMsg.put("content", resultJson);
                            messages.add(toolMsg);
                        }
                        // continue ReAct loop — tool results now in messages
                    } else {
                        // finish_reason = stop | end_turn | tool_calls (but we're out of iterations)
                        if ("tool_calls".equals(turn.finishReason()) && isLastIteration) {
                            finalReply.append("\n\n> ⚠️ 达到最大工具调用轮次（").append(MAX_TOOL_ITERATIONS)
                                    .append("），以下为当前推理阶段的直接输出，可能未完成完整的工具验证。");
                        }
                        break;
                    }
                }
            } catch (RuntimeException ex) {
                String fallback = "⚠️ 智能体暂时无法连接大模型服务：" + ex.getMessage();
                finalReply.setLength(0);
                finalReply.append(fallback);
                try {
                    frameEmitter.accept("error", objectMapper.writeValueAsString(Map.of("message", ex.getMessage())));
                } catch (Exception ignored) {
                }
            }
        }

        appendMessage(currentSession, "assistant", finalReply.toString());

        // 3. done frame
        try {
            Map<String, Object> done = new LinkedHashMap<>();
            done.put("sessionId", currentSession);
            done.put("runCode", runCode);
            done.put("promptVersion", agentProps.getPromptVersion());
            done.put("messages", sanitizedMessages(currentSession));
            done.put("assistantReply", finalReply.toString());
            frameEmitter.accept("done", objectMapper.writeValueAsString(done));
        } catch (Exception ignored) {
        }

        Map<String, Object> resultForAudit = new LinkedHashMap<>();
        resultForAudit.put("sessionId", currentSession);
        resultForAudit.put("assistantReply", finalReply.toString());
        saveRun(runCode, goal, currentSession, preset, "COMPLETED", snapshot, resultForAudit, null);
    }

    /**
     * One ReAct LLM call — streams content deltas + accumulates tool_calls until
     * finish_reason.
     * Returns the accumulated content, finish_reason, and parsed tool_calls list.
     */
    private ReActTurn callLLMStreamWithTools(
            List<Map<String, Object>> messages,
            BiConsumer<String, String> frameEmitter,
            boolean forceFinish) {

        if ("mock".equalsIgnoreCase(aiProps.getMode()))
            throw new IllegalStateException("AI mode = mock 不支持 ReAct 工具调用，请设 AI_MODE=gpt");
        if (aiProps.getApiKey() == null || aiProps.getApiKey().isBlank())
            throw new IllegalStateException("AI_API_KEY 未配置");

        String endpoint = trimSlash(aiProps.getBaseUrl()) + "/chat/completions";

        List<Map<String, Object>> toolDefs = tools.stream().map(t -> {
            Map<String, Object> fn = new LinkedHashMap<>();
            fn.put("name", t.name());
            fn.put("description", t.description());
            fn.put("parameters", t.parameters());
            Map<String, Object> tool = new LinkedHashMap<>();
            tool.put("type", "function");
            tool.put("function", fn);
            return tool;
        }).toList();

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("model", aiProps.getModel());
        payload.put("stream", true);
        payload.put("messages", messages);
        payload.put("tools", toolDefs);
        payload.put("tool_choice", "auto");
        if (forceFinish)
            payload.put("tool_choice", "none"); // prevent runaway recursion on last turn

        StringBuilder contentAccum = new StringBuilder();
        // tool_calls accumulate across SSE frames — each frame brings a small piece
        Map<Integer, Map<String, Object>> toolCallAccum = new LinkedHashMap<>();
        String finishReason = null;

        try {
            HttpRequest request = HttpRequest.newBuilder(URI.create(endpoint))
                    .timeout(Duration.ofSeconds(Math.max(aiProps.getTimeoutSeconds(), 120)))
                    .header("Authorization", "Bearer " + aiProps.getApiKey())
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(payload)))
                    .build();

            HttpResponse<java.io.InputStream> response = llmClient.send(request,
                    HttpResponse.BodyHandlers.ofInputStream());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new BusinessException("LLM 流式调用失败 HTTP " + response.statusCode());
            }

            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(response.body(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (line.isBlank() || line.startsWith(":"))
                        continue;
                    if (!line.startsWith("data:"))
                        continue;
                    String data = line.substring(5).trim();
                    if ("[DONE]".equals(data))
                        break;
                    if (data.isBlank())
                        continue;
                    try {
                        JsonNode root = objectMapper.readTree(data);
                        JsonNode choice = root.path("choices").path(0);

                        // --- finish_reason ---
                        JsonNode frNode = choice.path("finish_reason");
                        if (!frNode.isMissingNode() && frNode.asText(null) != null) {
                            finishReason = frNode.asText();
                        }

                        JsonNode delta = choice.path("delta");

                        // --- content delta ---
                        JsonNode contentNode = delta.path("content");
                        if (!contentNode.isMissingNode() && contentNode.isTextual()) {
                            String c = contentNode.asText();
                            if (!c.isEmpty()) {
                                contentAccum.append(c);
                                frameEmitter.accept("content", objectMapper.writeValueAsString(Map.of("text", c)));
                            }
                        }

                        // --- tool_calls delta (streamed incrementally) ---
                        JsonNode toolCallsNode = delta.path("tool_calls");
                        if (!toolCallsNode.isMissingNode() && toolCallsNode.isArray()) {
                            for (JsonNode tc : toolCallsNode) {
                                int idx = tc.path("index").asInt(0);
                                toolCallAccum.computeIfAbsent(idx, k -> new LinkedHashMap<>());
                                Map<String, Object> accum = toolCallAccum.get(idx);

                                if (tc.hasNonNull("id"))
                                    accum.put("id", tc.path("id").asText());
                                if (tc.hasNonNull("type"))
                                    accum.put("type", tc.path("type").asText());

                                JsonNode fn = tc.path("function");
                                if (!fn.isMissingNode()) {
                                    if (fn.hasNonNull("name"))
                                        accum.put("name", fn.path("name").asText());
                                    // arguments stream as pieces — accumulate string
                                    if (fn.hasNonNull("arguments")) {
                                        String piece = fn.path("arguments").asText();
                                        String prev = (String) accum.getOrDefault("arguments", "");
                                        accum.put("arguments", prev + piece);
                                    }
                                }
                            }
                        }
                    } catch (Exception jsonEx) {
                        log.debug("skip bad SSE frame: {}", jsonEx.getMessage());
                    }
                }
            }
        } catch (BusinessException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new BusinessException("LLM 流式调用异常: " + ex.getMessage());
        }

        // parse accumulated tool_calls arguments JSON (keep arguments as raw JSON
        // string for next LLM round)
        List<Map<String, Object>> finalToolCalls = new ArrayList<>();
        for (Map.Entry<Integer, Map<String, Object>> e : toolCallAccum.entrySet()) {
            Map<String, Object> raw = e.getValue();
            Map<String, Object> parsed = new LinkedHashMap<>(raw);
            String argsStr = (String) raw.getOrDefault("arguments", "{}");
            // arguments stays as JSON string for LLM protocol compliance;
            // we store a separate "parsedArgs" map purely for executor lookup below.
            try {
                parsed.put("parsedArgs", objectMapper.readValue(argsStr, Map.class));
            } catch (Exception ex) {
                parsed.put("parsedArgs", Map.of("_raw", argsStr));
            }
            // build a proper tool_call object: {id, type: "function", function: {name,
            // arguments: "{\"k\":\"v\"}"}}
            Map<String, Object> fn = new LinkedHashMap<>();
            fn.put("name", parsed.getOrDefault("name", ""));
            fn.put("arguments", argsStr); // MUST be a JSON string for the next LLM round
            Map<String, Object> tc = new LinkedHashMap<>();
            tc.put("id", parsed.getOrDefault("id", "call_unknown"));
            tc.put("type", "function");
            tc.put("function", fn);
            // keep parsedArgs on the top level for executor convenience (stripped later
            // when re-sending to LLM)
            tc.put("parsedArgs", parsed.get("parsedArgs"));
            finalToolCalls.add(tc);
        }

        return new ReActTurn(contentAccum.toString(),
                finishReason == null ? "stop" : finishReason,
                finalToolCalls);
    }

    // ---------- history ----------

    /**
     * Return sanitized messages + trace for a session.
     *
     * <p>
     * Trace is kept as a small static descriptor (no real tool events in the
     * direct HTTP path yet). This keeps the front-end drawer meaningful.
     */
    public Map<String, Object> history(String sessionId) {
        if (sessionId == null || sessionId.isBlank())
            throw new IllegalArgumentException("sessionId不能为空");
        // 先查内存，内存空则从 DB 加载（后端重启后兜底）
        List<Map<String, Object>> msgs = sessionHistory.get(sessionId);
        if (msgs == null || msgs.isEmpty()) {
            msgs = loadMessagesFromDb(sessionId);
            // 回填到内存，避免后续每次都查 DB
            if (!msgs.isEmpty()) sessionHistory.put(sessionId, msgs);
        }
        List<Map<String, Object>> trace = new ArrayList<>();
        trace.add(Map.of("type", "step/assemble-context", "seq", 1, "time", Instant.now().toString()));
        trace.add(Map.of("type", "step/call-llm", "seq", 2, "time", Instant.now().toString()));
        trace.add(Map.of("type", "step/formulate-reply", "seq", 3, "time", Instant.now().toString()));
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("sessionId", sessionId);
        out.put("messages", msgs.stream().filter(m -> !"system".equals(m.get("role"))).toList());
        out.put("trace", trace);
        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("messageCount", msgs.size());
        stats.put("createdAt", msgs.isEmpty() ? null : msgs.get(0).get("time"));
        Map<String, Object> projections = new LinkedHashMap<>();
        projections.put("sessionStats", stats);
        out.put("projections", projections);
        return out;
    }

    // ---------- internals ----------

    private String buildPrompt(String goal, Map<String, Object> snapshot) {
        String systemContract = agentProps.getSystemPrompt();
        if (systemContract == null || systemContract.isBlank()) {
            systemContract = "你是校园电动车监测管理系统的时空治理智能体。必须基于平台事实回答，区分事实、融合证据、模型推断和人工待确认事项；数据缺失要明确说明，不得捏造。你只能提出风险分析、巡检优先级、复核清单和报告草稿，不能直接处罚、通知、冻结账号、修改申诉状态或删除数据。";
        } else {
            systemContract = systemContract.trim();
        }
        boolean casualConversation = goal.trim().matches("(?i).*(你好|您好|嗨|hello|hi|在吗|谢谢|感谢|再见|拜拜|你是谁|你能做什么).*");
        String responseRule = casualConversation
                ? "\n【对话规则】用户当前只是问候或闲聊，请用自然、简短的中文直接回复 1-3 句话，不输出风险报告，不列固定分析结构，也不要臆造任何数据。"
                : "\n【固定输出结构】先给结论；再列多源证据及来源；再给风险等级和置信度；再列不确定性；最后给管理员确认清单。";
        String contextJson;
        try {
            contextJson = objectMapper.writeValueAsString(snapshot);
        } catch (Exception ex) {
            contextJson = "{}";
        }
        return "【系统智能体契约 / " + agentProps.getPromptVersion() + "】\n" + systemContract
                + responseRule
                + "\n【用户目标】" + goal
                + "\n【平台多源上下文(JSON，只读事实)】" + contextJson;
    }

    private String callLLM(String fullPrompt, String sessionId) {
        if ("mock".equalsIgnoreCase(aiProps.getMode())) {
            throw new IllegalStateException("AI mode = mock 不支持真实 LLM 调用，请设 AI_MODE=gpt");
        }
        if (aiProps.getApiKey() == null || aiProps.getApiKey().isBlank()) {
            throw new IllegalStateException("AI_API_KEY 未配置");
        }

        boolean chatCompletions = "chat-completions".equalsIgnoreCase(aiProps.getApiStyle());
        String endpoint = trimSlash(aiProps.getBaseUrl()) + (chatCompletions ? "/chat/completions" : "/responses");

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("model", aiProps.getModel());
        if (chatCompletions) {
            List<Map<String, Object>> messages = new ArrayList<>();
            messages.add(Map.of("role", "system", "content",
                    "你是校园电动车时空治理智能体，基于平台只读事实回答，不捏造数据，不直接处罚。"));
            messages.add(Map.of("role", "user", "content", fullPrompt));
            payload.put("messages", messages);
        } else {
            payload.put("reasoning", Map.of("effort", aiProps.getReasoningEffort()));
            payload.put("input", List.of(
                    Map.of("role", "system", "content",
                            Map.of("type", "input_text", "text", "你是校园电动车时空治理智能体，基于平台只读事实回答，不捏造数据，不直接处罚。")),
                    Map.of("role", "user", "content", Map.of("type", "input_text", "text", fullPrompt))));
        }

        try {
            HttpRequest request = HttpRequest.newBuilder(URI.create(endpoint))
                    .timeout(Duration.ofSeconds(aiProps.getTimeoutSeconds()))
                    .header("Authorization", "Bearer " + aiProps.getApiKey())
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(payload)))
                    .build();
            HttpResponse<String> response = llmClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                log.error("LLM {} 调用失败 HTTP {} body={}", endpoint, response.statusCode(),
                        response.body() == null ? ""
                                : response.body().substring(0, Math.min(300, response.body().length())));
                throw new BusinessException("LLM 调用失败 HTTP " + response.statusCode());
            }
            JsonNode root = objectMapper.readTree(response.body());
            String text = chatCompletions
                    ? root.path("choices").path(0).path("message").path("content").asText(null)
                    : extractResponsesText(root);
            if (text == null || text.isBlank()) {
                throw new BusinessException("LLM 未返回可解析的文本内容");
            }
            return text;
        } catch (BusinessException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new BusinessException("LLM 调用异常: " + ex.getMessage());
        }
    }

    /**
     * Streaming counterpart of {@link #callLLM(String, String)}. Reads SSE
     * frames from the upstream chat-completions endpoint and emits each
     * delta.content through {@code onDelta}.
     */
    private void callLLMStream(String fullPrompt, java.util.function.Consumer<String> onDelta) {
        if ("mock".equalsIgnoreCase(aiProps.getMode()))
            throw new IllegalStateException("AI mode = mock 不支持 ReAct 工具调用，请设 AI_MODE=gpt");
        if (aiProps.getApiKey() == null || aiProps.getApiKey().isBlank())
            throw new IllegalStateException("AI_API_KEY 未配置");

        boolean chatCompletions = "chat-completions".equalsIgnoreCase(aiProps.getApiStyle());
        String endpoint = trimSlash(aiProps.getBaseUrl()) + (chatCompletions ? "/chat/completions" : "/responses");

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("model", aiProps.getModel());
        payload.put("stream", true);
        if (chatCompletions) {
            List<Map<String, Object>> messages = new ArrayList<>();
            messages.add(Map.of("role", "system", "content",
                    "你是校园电动车时空治理智能体，基于平台只读事实回答，不捏造数据，不直接处罚。"));
            messages.add(Map.of("role", "user", "content", fullPrompt));
            payload.put("messages", messages);
        } else {
            payload.put("reasoning", Map.of("effort", aiProps.getReasoningEffort()));
            payload.put("input", List.of(
                    Map.of("role", "system", "content",
                            Map.of("type", "input_text", "text", "你是校园电动车时空治理智能体，基于平台只读事实回答，不捏造数据，不直接处罚。")),
                    Map.of("role", "user", "content", Map.of("type", "input_text", "text", fullPrompt))));
        }

        try {
            HttpRequest request = HttpRequest.newBuilder(URI.create(endpoint))
                    .timeout(Duration.ofSeconds(Math.max(aiProps.getTimeoutSeconds(), 120)))
                    .header("Authorization", "Bearer " + aiProps.getApiKey())
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(payload)))
                    .build();

            HttpResponse<java.io.InputStream> response = llmClient.send(request,
                    HttpResponse.BodyHandlers.ofInputStream());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                log.error("LLM stream {} HTTP {}", endpoint, response.statusCode());
                throw new BusinessException("LLM 流式调用失败 HTTP " + response.statusCode());
            }

            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(response.body(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (line.isBlank() || line.startsWith(":"))
                        continue;
                    if (!line.startsWith("data:"))
                        continue;
                    String data = line.substring(5).trim();
                    if ("[DONE]".equals(data))
                        break;
                    if (data.isBlank())
                        continue;
                    try {
                        JsonNode root = objectMapper.readTree(data);
                        String delta = null;
                        if (chatCompletions) {
                            JsonNode contentNode = root.path("choices").path(0).path("delta").path("content");
                            if (!contentNode.isMissingNode() && contentNode.isTextual())
                                delta = contentNode.asText();
                        } else {
                            // responses API stream events
                            JsonNode textNode = root.path("response.output_text.delta");
                            if (textNode.isTextual())
                                delta = textNode.asText();
                            else if (root.hasNonNull("delta"))
                                delta = root.path("delta").asText();
                        }
                        if (delta != null && !delta.isEmpty())
                            onDelta.accept(delta);
                    } catch (Exception jsonEx) {
                        log.debug("skip bad SSE frame: {}", jsonEx.getMessage());
                    }
                }
            }
        } catch (BusinessException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new BusinessException("LLM 流式调用异常: " + ex.getMessage());
        }
    }

    private String extractResponsesText(JsonNode root) {
        if (root.hasNonNull("output_text")) {
            return root.path("output_text").asText();
        }
        JsonNode output = root.path("output");
        for (JsonNode item : output) {
            if ("message".equals(item.path("type").asText())) {
                JsonNode content = item.path("content");
                for (JsonNode block : content) {
                    if (block.hasNonNull("text")) {
                        return block.path("text").asText();
                    }
                }
            }
        }
        return null;
    }

    private void appendMessage(String sessionId, String role, String content) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("role", role);
        row.put("content", content);
        row.put("seq", System.nanoTime());
        row.put("time", Instant.now().toString());
        sessionHistory.computeIfAbsent(sessionId, k -> new ArrayList<>()).add(row);
        // 同步写 DB —— 后端重启也不会丢会话
        try {
            jdbcTemplate.update(
                    "INSERT IGNORE INTO ai_assistant_session(session_id, user_id, title) VALUES (?, 1, ?)",
                    sessionId, sessionTitle.getOrDefault(sessionId, "对话"));
            jdbcTemplate.update(
                    "INSERT INTO ai_assistant_message(session_id, role, content) VALUES (?, ?, ?)",
                    sessionId, role, content);
            jdbcTemplate.update(
                    "UPDATE ai_assistant_session SET update_time = NOW() WHERE session_id = ?",
                    sessionId);
        } catch (Exception ex) {
            log.debug("appendMessage DB 写入跳过: {}", ex.getMessage());
        }
    }

    private List<Map<String, Object>> sanitizedMessages(String sessionId) {
        List<Map<String, Object>> raw = sessionHistory.getOrDefault(sessionId, List.of());
        List<Map<String, Object>> out = new ArrayList<>();
        for (Map<String, Object> row : raw) {
            Map<String, Object> copy = new LinkedHashMap<>(row);
            out.add(copy);
        }
        return out;
    }

    /** 从 DB 加载消息（后端重启后内存空时兜底）。 */
    private List<Map<String, Object>> loadMessagesFromDb(String sessionId) {
        try {
            return jdbcTemplate.queryForList(
                    "SELECT role, content, create_time AS time FROM ai_assistant_message WHERE session_id = ? ORDER BY create_time ASC, message_id ASC",
                    sessionId);
        } catch (Exception ex) {
            log.debug("loadMessagesFromDb 跳过: {}", ex.getMessage());
            return List.of();
        }
    }

    /** 列出所有 session（供侧边栏历史列表用）。 */
    public List<Map<String, Object>> listSessions(int limit) {
        try {
            return jdbcTemplate.queryForList(
                    "SELECT session_id, title, create_time, update_time FROM ai_assistant_session ORDER BY update_time DESC LIMIT ?",
                    limit);
        } catch (Exception ex) {
            log.debug("listSessions 跳过: {}", ex.getMessage());
            return List.of();
        }
    }

    /** 硬删一个会话（DB + 内存）。 */
    public boolean deleteSession(String sessionId) {
        if (sessionId == null || sessionId.isBlank()) return false;
        try {
            jdbcTemplate.update("DELETE FROM ai_assistant_message WHERE session_id = ?", sessionId);
            jdbcTemplate.update("DELETE FROM ai_assistant_session WHERE session_id = ?", sessionId);
        } catch (Exception ex) {
            log.debug("deleteSession DB 跳过: {}", ex.getMessage());
        }
        sessionHistory.remove(sessionId);
        sessionTitle.remove(sessionId);
        return true;
    }

    private String trimSlash(String value) {
        return value != null && value.endsWith("/") ? value.substring(0, value.length() - 1) : value;
    }

    private void saveRun(String runCode, String goal, String sessionId, String preset, String state,
            Map<String, Object> context, Map<String, Object> result, String error) {
        try {
            String contextJson = context == null ? null : objectMapper.writeValueAsString(context);
            String resultJson = result == null ? null : objectMapper.writeValueAsString(result);
            jdbcTemplate.update(
                    """
                            INSERT INTO ai_agent_run(run_code, goal, dsh_session_id, preset, state, context_json, result_json, error_message, started_at, finished_at)
                            VALUES (?, ?, ?, ?, ?, ?, ?, ?, NOW(), CASE WHEN ? IN ('ACCEPTED','FAILED','COMPLETED') THEN NOW() ELSE NULL END)
                            ON DUPLICATE KEY UPDATE dsh_session_id=VALUES(dsh_session_id), preset=VALUES(preset), state=VALUES(state), context_json=VALUES(context_json), result_json=VALUES(result_json), error_message=VALUES(error_message), finished_at=CASE WHEN ? IN ('ACCEPTED','FAILED','COMPLETED') THEN NOW() ELSE finished_at END
                            """,
                    runCode, goal, sessionId, preset, state, contextJson, resultJson, error, state, state);
        } catch (Exception ex) {
            log.debug("Agent 审计写入跳过: {}", ex.getMessage());
        }
    }

    /**
     * Synthetic reply generator for {@code mode=mock}. Produces a structured
     * analysis based on the actual platform facts in {@code snapshot} so the
     * front-end demo has meaningful content even without a live LLM.
     */
    private String mockReply(String goal, Map<String, Object> snapshot) {
        int events = listSize(snapshot, "events");
        int locations = listSize(snapshot, "locations");
        int violations = listSize(snapshot, "violations");
        int cameras = listSize(snapshot, "cameras");
        int patrols = listSize(snapshot, "patrolMissions");
        int fusions = listSize(snapshot, "fusions");

        StringBuilder sb = new StringBuilder();
        boolean casual = goal.trim().matches("(?i).*(你好|您好|嗨|hello|hi|在吗|谢谢|感谢|再见|拜拜|你是谁|你能做什么).*");
        if (casual) {
            sb.append("你好！我是校园电动车时空治理智能体，基于平台多源数据进行只读分析。你可以让我分析高风险区域、违规趋势、巡检优先级等。\n\n");
            sb.append("当前我可以访问的平台事实：**").append(events).append("** 条 AI 事件、**")
                    .append(locations).append("** 条车辆定位、**").append(cameras).append("** 个摄像头、**")
                    .append(violations).append("** 条历史违规、**").append(fusions).append("** 条融合证据、**")
                    .append(patrols).append("** 个巡检任务。");
            return sb.toString();
        }

        // 结论
        sb.append("### 结论\n");
        if (events > violations) {
            sb.append("当前系统存在 **").append(events - violations).append("** 条尚未人工审核的 AI 事件，其中 ")
                    .append(Math.max(1, events / 3)).append(" 条风险等级较高，建议优先处理。\n\n");
        } else {
            sb.append("近期平台运行平稳，新事件量与历史违规量基本持平。仍有 **").append(fusions)
                    .append("** 条融合证据等待确认。\n\n");
        }

        // 多源证据
        sb.append("### 多源证据\n");
        sb.append("- 摄像头：**").append(cameras).append("** 台在线\n");
        sb.append("- 车辆定位：**").append(locations).append("** 条活跃位置上报\n");
        sb.append("- AI 事件库：**").append(events).append("** 条\n");
        sb.append("- 融合证据：**").append(fusions).append("** 条（事件 × 定位 × 围栏时空对齐）\n");
        sb.append("- 历史违规：**").append(violations).append("** 条\n\n");

        // 风险等级 & 置信度
        sb.append("### 风险评估\n");
        sb.append("- 当前整体风险等级：**").append(events > 15 ? "MEDIUM" : "LOW").append("**\n");
        sb.append("- 综合置信度：**").append(String.format("%.0f%%", 0.75 + Math.min(0.2, events / 50.0)))
                .append("**（基于多源交叉验证）\n\n");

        // 管理员确认清单
        sb.append("### 管理员确认清单\n");
        sb.append("1. [ ] 查看 **").append(events > 0 ? events : 0).append("** 条未审核 AI 事件，确认身份匹配\n");
        sb.append("2. [ ] 巡检任务 **").append(patrols).append("** 条，按融合风险排序优先覆盖\n");
        sb.append("3. [ ] 重点关注摄像头覆盖的校园入口区域\n");

        sb.append(
                "\n> ⚠️ 以上为 **本地 mock 模式** 生成的演示回复（mode=mock，未调用真实大模型）。比赛部署时设置 AI_MODE=gpt + AI_API_KEY 后，回复将来自实际 LLM 推理。");
        return sb.toString();
    }

    private static int listSize(Map<String, Object> map, String key) {
        Object v = map.get(key);
        if (v instanceof List<?> list)
            return list.size();
        return 0;
    }
}
