package com.example.evmonitorplatform.controller;

import com.example.evmonitorplatform.common.response.ApiResponse;
import com.example.evmonitorplatform.common.response.PageResponse;
import com.example.evmonitorplatform.entity.Notification;
import com.example.evmonitorplatform.service.NotificationService;
import com.example.evmonitorplatform.vo.request.NotificationSendVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/notification")
@RequiredArgsConstructor
@Tag(name = "通知管理", description = "系统通知发送与管理接口")
public class NotificationController {
    
    private final NotificationService notificationService;
    
    @PostMapping("/send")
    @Operation(summary = "发送通知", description = "发送系统通知")
    public ApiResponse<Boolean> sendNotification(@RequestBody @Valid NotificationSendVO sendVO) {
        log.info("发送通知: {}", sendVO.getTitle());
        boolean result = notificationService.sendNotification(sendVO);
        return ApiResponse.success(result);
    }
    
    @PostMapping("/send-personal")
    @Operation(summary = "发送个人通知", description = "发送个人通知给指定用户")
    public ApiResponse<Boolean> sendPersonalNotification(
            @RequestParam String title,
            @RequestParam String content,
            @RequestParam Integer receiverId) {
        log.info("发送个人通知: {} -> {}", title, receiverId);
        boolean result = notificationService.sendPersonalNotification(title, content, receiverId);
        return ApiResponse.success(result);
    }
    
    @PostMapping("/send-batch")
    @Operation(summary = "发送批量通知", description = "发送批量通知给多个用户")
    public ApiResponse<Boolean> sendBatchNotification(
            @RequestParam String title,
            @RequestParam String content,
            @RequestBody List<Integer> receiverIds) {
        log.info("发送批量通知: {} -> {}个用户", title, receiverIds.size());
        boolean result = notificationService.sendBatchNotification(title, content, receiverIds);
        return ApiResponse.success(result);
    }
    
    @PostMapping("/{notificationId}/read")
    @Operation(summary = "标记已读", description = "标记通知为已读（按用户维度，不修改全局记录）")
    public ApiResponse<Boolean> markAsRead(
            @PathVariable Integer notificationId,
            @RequestParam Integer userId) {
        log.info("标记通知已读: {} -> {}", notificationId, userId);
        boolean result = notificationService.markAsRead(notificationId, userId);
        return ApiResponse.success(result);
    }

    @PostMapping("/{notificationId}/hide")
    @Operation(summary = "用户移除通知", description = "从当前用户收件箱移除，不删除系统内通知实体")
    public ApiResponse<Boolean> hideForUser(
            @PathVariable Integer notificationId,
            @RequestParam Integer userId) {
        log.info("用户移除通知: {} userId={}", notificationId, userId);
        boolean result = notificationService.hideNotificationForUser(notificationId, userId);
        return ApiResponse.success(result);
    }
    
    @GetMapping("/list")
    @Operation(summary = "查询通知列表", description = "分页查询系统通知")
    public PageResponse<List<Notification>> listNotifications(
            @Parameter(description = "通知类型") @RequestParam(required = false) String type,
            @Parameter(description = "是否紧急") @RequestParam(required = false) Boolean isUrgent,
            @Parameter(description = "状态") @RequestParam(required = false) String status,
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") Integer pageNum,
            @Parameter(description = "每页大小") @RequestParam(defaultValue = "10") Integer pageSize) {
        
        return notificationService.queryNotifications(type, isUrgent, status, pageNum, pageSize);
    }
    
    @GetMapping("/user/{userId}")
    @Operation(summary = "获取用户通知", description = "获取指定用户的未读通知")
    public ApiResponse<List<Notification>> getUserNotifications(
            @PathVariable Integer userId,
            @RequestParam(defaultValue = "10") Integer limit) {
        List<Notification> notifications = notificationService.getUserNotifications(userId, limit);
        return ApiResponse.success(notifications);
    }
    
    @GetMapping("/stats")
    @Operation(summary = "获取通知统计", description = "获取通知发送统计数据")
    public ApiResponse<Map<String, Object>> getStats() {
        Map<String, Object> stats = notificationService.getNotificationStats();
        return ApiResponse.success(stats);
    }
    
    @DeleteMapping("/{notificationId}")
    @Operation(summary = "删除通知", description = "管理端删除通知记录（逻辑删除）")
    public ApiResponse<Boolean> deleteNotification(@PathVariable Integer notificationId) {
        boolean result = notificationService.deleteNotification(notificationId);
        return ApiResponse.success(result);
    }
    
    @GetMapping("/{notificationId}")
    @Operation(summary = "获取通知详情", description = "管理端不传 userId；学生端传 userId 校验接收人并返回 readByMe")
    public ApiResponse<Notification> getDetail(
            @PathVariable Integer notificationId,
            @Parameter(description = "学生端当前用户 ID，用于鉴权") @RequestParam(required = false) Integer userId) {
        Notification notification = userId != null
                ? notificationService.getNotificationForUser(notificationId, userId)
                : notificationService.getById(notificationId);
        return ApiResponse.success(notification);
    }
}