package com.example.evmonitorplatform.controller;

import com.example.evmonitorplatform.agent.AgentWorkbenchService;
import com.example.evmonitorplatform.common.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

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

    @GetMapping("/sessions/{sessionId}/history")
    public ApiResponse<Map<String, Object>> history(@PathVariable String sessionId) { return ApiResponse.success(service.history(sessionId)); }

    private static String asString(Object value) { return value == null ? null : String.valueOf(value); }
}
