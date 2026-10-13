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

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

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
        if (!sessionId.matches("[A-Za-z0-9._:-]{1,160}"))
            throw new IllegalArgumentException("sessionId格式无效");
        List<Map<String, Object>> msgs = sanitizedMessages(sessionId);
        List<Map<String, Object>> trace = new ArrayList<>();
        trace.add(Map.of("type", "step/assemble-context", "seq", 1, "time", Instant.now().toString()));
        trace.add(Map.of("type", "step/call-llm", "seq", 2, "time", Instant.now().toString()));
        trace.add(Map.of("type", "step/formulate-reply", "seq", 3, "time", Instant.now().toString()));
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("sessionId", sessionId);
        out.put("messages", msgs);
        out.put("trace", trace);
        Map<String, Object> projections = new LinkedHashMap<>();
        projections.put("sessionStats",
                Map.of("messageCount", msgs.size(), "createdAt", msgs.isEmpty() ? null : msgs.get(0).get("time")));
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
        if (!"gpt".equalsIgnoreCase(aiProps.getMode())) {
            throw new IllegalStateException("AI mode 不是 gpt，当前 mode=" + aiProps.getMode());
        }
        if (aiProps.getApiKey() == null || aiProps.getApiKey().isBlank()) {
            throw new IllegalStateException("AI_API_KEY 未配置");
        }

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("model", aiProps.getModel());
        payload.put("reasoning", Map.of("effort", aiProps.getReasoningEffort()));
        payload.put("input", List.of(
                Map.of("role", "system", "content",
                        Map.of("type", "input_text", "text", "你是校园电动车时空治理智能体，基于平台只读事实回答，不捏造数据，不直接处罚。")),
                Map.of("role", "user", "content", Map.of("type", "input_text", "text", fullPrompt))));

        String endpoint = trimSlash(aiProps.getBaseUrl()) + "/responses";
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
            String text = extractResponsesText(objectMapper.readTree(response.body()));
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
