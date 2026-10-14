package com.example.evmonitorplatform.controller;

import com.example.evmonitorplatform.agent.AgentWorkbenchService;
import com.example.evmonitorplatform.common.response.ApiResponse;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;
import java.util.Map;

/** New, isolated Agent workbench API. Existing governance APIs remain unchanged. */
@RestController
@RequestMapping("/api/agent")
@RequiredArgsConstructor
public class AgentWorkbenchController {
    private final AgentWorkbenchService service;

    @GetMapping("/status")
    public ApiResponse<Map<String, Object>> status() { return ApiResponse.success(service.status()); }

    @GetMapping("/context")
    public ApiResponse<Map<String, Object>> context(@RequestParam(defaultValue = "30") int limit) { return ApiResponse.success(service.context(limit)); }

    @PostMapping("/runs")
    public ApiResponse<Map<String, Object>> run(@RequestBody Map<String, Object> body) {
        String goal = body == null ? "" : String.valueOf(body.getOrDefault("goal", ""));
        String sessionId = body == null ? null : asString(body.get("sessionId"));
        String preset = body == null ? null : asString(body.get("preset"));
        int limit = body == null || body.get("limit") == null ? 30 : Integer.parseInt(String.valueOf(body.get("limit")));
        return ApiResponse.success(service.run(goal, sessionId, preset, limit));
    }

    /** Server-Sent Events (SSE) streaming endpoint — typewriter bubble UI. */
    @PostMapping("/runs/stream")
    public void runStream(@RequestBody(required = false) Map<String, Object> body, HttpServletResponse response) throws IOException {
        String goal = body == null ? "" : String.valueOf(body.getOrDefault("goal", ""));
        String sessionId = body == null ? null : asString(body.get("sessionId"));
        String preset = body == null ? null : asString(body.get("preset"));
        int limit = body == null || body.get("limit") == null ? 30 : Integer.parseInt(String.valueOf(body.get("limit")));

        response.setContentType("text/event-stream;charset=UTF-8");
        response.setHeader("Cache-Control", "no-cache, no-transform");
        response.setHeader("Connection", "keep-alive");
        response.setHeader("X-Accel-Buffering", "no");

        PrintWriter writer = response.getWriter();
        try {
            service.streamRun(goal, sessionId, preset, limit, (event, payload) -> {
                try {
                    writer.write("event: " + event + "\n");
                    writer.write("data: " + payload + "\n\n");
                    writer.flush();
                } catch (Exception ex) {
                    throw new RuntimeException("SSE write failed: " + ex.getMessage(), ex);
                }
            });
        } finally {
            try { writer.close(); } catch (Exception ignored) {}
        }
    }

    @GetMapping("/sessions/{sessionId}/history")
    public ApiResponse<Map<String, Object>> history(@PathVariable String sessionId) { return ApiResponse.success(service.history(sessionId)); }

    @GetMapping("/sessions")
    public ApiResponse<List<Map<String, Object>>> listSessions(@RequestParam(defaultValue = "50") int limit) {
        return ApiResponse.success(service.listSessions(limit));
    }

    private static String asString(Object value) { return value == null ? null : String.valueOf(value); }
}
