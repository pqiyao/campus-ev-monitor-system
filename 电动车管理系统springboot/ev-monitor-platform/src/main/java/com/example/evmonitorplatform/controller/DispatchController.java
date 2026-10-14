package com.example.evmonitorplatform.controller;

import com.example.evmonitorplatform.common.response.ApiResponse;
import com.example.evmonitorplatform.service.dispatch.DispatchService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 空地协同调度端点（管理端操作，JWT 鉴权）。
 */
@RestController
@RequestMapping("/api/dispatch")
@RequiredArgsConstructor
public class DispatchController {
    private final DispatchService dispatchService;

    @PostMapping("/robots")
    public ApiResponse<Map<String, Object>> registerRobot(@RequestBody Map<String, Object> request) {
        return ApiResponse.success(dispatchService.registerRobot(request));
    }

    @GetMapping("/robots")
    public ApiResponse<List<Map<String, Object>>> robots() {
        return ApiResponse.success(dispatchService.listRobots());
    }

    /** 评分引擎透明预览：只算分不派单。 */
    @GetMapping("/events/{eventId}/scores")
    public ApiResponse<List<Map<String, Object>>> scores(@PathVariable Long eventId) {
        return ApiResponse.success(dispatchService.previewScores(eventId));
    }

    @PostMapping("/events/{eventId}/dispatch")
    public ApiResponse<Map<String, Object>> dispatch(@PathVariable Long eventId) {
        return ApiResponse.success(dispatchService.dispatchForEvent(eventId));
    }

    @PostMapping("/auto")
    public ApiResponse<Map<String, Object>> auto(@RequestParam(defaultValue = "5") Integer limit) {
        return ApiResponse.success(dispatchService.dispatchPendingEvents(limit));
    }

    @GetMapping("/orders")
    public ApiResponse<List<Map<String, Object>>> orders(@RequestParam(required = false) String state,
                                                         @RequestParam(defaultValue = "50") Integer limit) {
        return ApiResponse.success(dispatchService.listOrders(state, limit));
    }

    @GetMapping("/orders/{orderId}")
    public ApiResponse<Map<String, Object>> order(@PathVariable Long orderId) {
        return ApiResponse.success(dispatchService.getOrderDetail(orderId));
    }

    @PostMapping("/orders/{orderId}/fault")
    public ApiResponse<Map<String, Object>> fault(@PathVariable Long orderId, @RequestBody(required = false) Map<String, Object> request) {
        String faultType = request == null ? null : String.valueOf(request.get("faultType"));
        @SuppressWarnings("unchecked")
        Map<String, Object> parameters = request == null ? Map.of() : (Map<String, Object>) request.getOrDefault("parameters", Map.of());
        return ApiResponse.success(dispatchService.injectFault(orderId, faultType, parameters));
    }

    @PostMapping("/orders/{orderId}/clear-fault")
    public ApiResponse<Map<String, Object>> clearFault(@PathVariable Long orderId) {
        return ApiResponse.success(dispatchService.clearFault(orderId));
    }
}
