package com.example.evmonitorplatform.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.evmonitorplatform.common.enums.AppealStateEnum;
import com.example.evmonitorplatform.common.exception.BusinessException;
import com.example.evmonitorplatform.common.response.ApiResponse;
import com.example.evmonitorplatform.common.response.PageResponse;
import com.example.evmonitorplatform.entity.AppealRecord;
import com.example.evmonitorplatform.entity.User;
import com.example.evmonitorplatform.mapper.UserMapper;
import com.example.evmonitorplatform.service.AppealService;
import com.example.evmonitorplatform.service.NotificationService;
import com.example.evmonitorplatform.vo.request.AppealAuditVO;
import com.example.evmonitorplatform.vo.request.AppealQueryVO;
import com.example.evmonitorplatform.vo.request.AppealSubmitVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/appeal")
@RequiredArgsConstructor
@Tag(name = "申诉管理", description = "违规申诉处理接口")
public class AppealController {

    private final AppealService appealService;
    private final UserMapper userMapper; // 注入UserMapper实例
    private final NotificationService notificationService; // 注入通知服务

    // 审核超时时间（小时）
    private static final int AUDIT_DEADLINE_HOURS = 48;

    @PostMapping("/submit")
    @Operation(summary = "提交申诉", description = "用户提交违规申诉")
    public ApiResponse<Boolean> submitAppeal(@RequestBody @Valid AppealSubmitVO submitVO) {
        log.info("提交申诉: illegalId={}, userId={}", submitVO.getIllegalId(), submitVO.getUserId());
        boolean result = appealService.submitAppeal(submitVO);
        return ApiResponse.success(result);
    }

    @PostMapping("/audit")
    @Operation(summary = "审核申诉", description = "管理员审核申诉")
    public ApiResponse<Boolean> auditAppeal(@RequestBody @Valid AppealAuditVO auditVO) {
        log.info("审核申诉: appealId={}, auditUserId={}", auditVO.getAppealId(), auditVO.getAuditUserId());
        boolean result = appealService.auditAppeal(auditVO);
        return ApiResponse.success(result);
    }

    @PostMapping("/cancel")
    @Operation(summary = "撤销申诉", description = "用户撤销已提交的申诉")
    public ApiResponse<Boolean> cancelAppeal(
            @RequestParam Integer appealId,
            @RequestParam Integer userId,
            @RequestParam(required = false) String reason) {
        log.info("撤销申诉: appealId={}, userId={}", appealId, userId);
        boolean result = appealService.cancelAppeal(appealId, userId, reason);
        return ApiResponse.success(result);
    }

    @PostMapping("/resubmit")
    @Operation(summary = "重新提交申诉", description = "重新提交申诉失败的申诉")
    public ApiResponse<Boolean> resubmitAppeal(
            @RequestParam Integer appealId,
            @RequestBody @Valid AppealSubmitVO submitVO) {
        log.info("重新提交申诉: appealId={}", appealId);
        boolean result = appealService.resubmitAppeal(appealId, submitVO);
        return ApiResponse.success(result);
    }

    @GetMapping("/list")
    @Operation(summary = "查询申诉记录", description = "分页查询申诉记录")
    public PageResponse<List<AppealRecord>> listAppeals(
            @Parameter(description = "申诉ID") @RequestParam(required = false) Integer appealId,
            @Parameter(description = "违规ID") @RequestParam(required = false) Integer illegalId,
            @Parameter(description = "用户ID") @RequestParam(required = false) Integer userId,
            @Parameter(description = "申诉状态") @RequestParam(required = false) Integer appealState,
            @Parameter(description = "申诉结果") @RequestParam(required = false) Integer appealResult,
            @Parameter(description = "审核用户ID") @RequestParam(required = false) Integer auditUserId,
            @Parameter(description = "车牌号") @RequestParam(required = false) String licensePlate,
            @Parameter(description = "用户姓名") @RequestParam(required = false) String userName,
            @Parameter(description = "用户手机号") @RequestParam(required = false) String userPhone,
            @Parameter(description = "开始时间") @RequestParam(required = false) String startTime,
            @Parameter(description = "结束时间") @RequestParam(required = false) String endTime,
            @Parameter(description = "申诉来源") @RequestParam(required = false) String appealSource,
            @Parameter(description = "紧急程度") @RequestParam(required = false) Integer urgencyLevel,
            @Parameter(description = "导员ID") @RequestParam(required = false) Integer counselorId,
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") Integer pageNum,
            @Parameter(description = "每页大小") @RequestParam(defaultValue = "10") Integer pageSize,
            @Parameter(description = "排序字段") @RequestParam(required = false) String orderBy,
            @Parameter(description = "排序方向") @RequestParam(required = false) String orderDirection) {

        AppealQueryVO queryVO = new AppealQueryVO();
        queryVO.setAppealId(appealId);
        queryVO.setIllegalId(illegalId);
        queryVO.setUserId(userId);
        queryVO.setAppealState(appealState);
        queryVO.setAppealResult(appealResult);
        queryVO.setAuditUserId(auditUserId);
        queryVO.setLicensePlate(licensePlate);
        queryVO.setUserName(userName);
        queryVO.setUserPhone(userPhone);
        queryVO.setStartTime(startTime);
        queryVO.setEndTime(endTime);
        queryVO.setAppealSource(appealSource);
        queryVO.setUrgencyLevel(urgencyLevel);
        queryVO.setCounselorId(counselorId);
        queryVO.setPageNum(pageNum);
        queryVO.setPageSize(pageSize);
        queryVO.setOrderBy(orderBy);
        queryVO.setOrderDirection(orderDirection);

        return appealService.queryAppeals(queryVO);
    }

    @GetMapping("/user/{userId}")
    @Operation(summary = "获取用户申诉列表", description = "获取指定用户的申诉记录")
    public PageResponse<List<AppealRecord>> getUserAppeals(
            @PathVariable Integer userId,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        return appealService.getUserAppeals(userId, pageNum, pageSize);
    }

    @GetMapping("/pending")
    @Operation(summary = "获取待审核申诉", description = "获取待审核的申诉记录")
    public PageResponse<List<AppealRecord>> getPendingAppeals(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        return appealService.getPendingAppeals(pageNum, pageSize);
    }

    @GetMapping("/{appealId}")
    @Operation(summary = "获取申诉详情", description = "根据ID获取申诉详细信息")
    public ApiResponse<AppealRecord> getAppealDetail(@PathVariable Integer appealId) {
        AppealRecord appeal = appealService.getAppealDetail(appealId);
        return ApiResponse.success(appeal);
    }

    @GetMapping("/stats")
    @Operation(summary = "获取申诉统计", description = "获取系统申诉统计数据")
    public ApiResponse<Map<String, Object>> getAppealStats() {
        Map<String, Object> stats = appealService.getAppealStats();
        return ApiResponse.success(stats);
    }

    @GetMapping("/stats/user/{userId}")
    @Operation(summary = "获取用户申诉统计", description = "获取指定用户的申诉统计数据")
    public ApiResponse<Map<String, Object>> getUserAppealStats(@PathVariable Integer userId) {
        Map<String, Object> stats = appealService.getUserAppealStats(userId);
        return ApiResponse.success(stats);
    }

    @GetMapping("/dashboard")
    @Operation(summary = "获取申诉仪表盘数据", description = "获取申诉管理仪表盘所需数据")
    public ApiResponse<Map<String, Object>> getAppealDashboard() {
        Map<String, Object> dashboard = appealService.getAppealDashboard();
        return ApiResponse.success(dashboard);
    }

    @GetMapping("/efficiency")
    @Operation(summary = "获取申诉处理效率统计", description = "获取申诉处理时效统计数据")
    public ApiResponse<Map<String, Object>> getAppealEfficiencyStats(
            @RequestParam(required = false, defaultValue = "30") Integer days) {
        Map<String, Object> stats = appealService.getAppealEfficiencyStats(days);
        return ApiResponse.success(stats);
    }

    @PostMapping("/batch-audit")
    @Operation(summary = "批量审核申诉", description = "批量审核申诉记录")
    public ApiResponse<Integer> batchAuditAppeals(
            @RequestBody List<Integer> appealIds,
            @RequestParam Integer auditUserId,
            @RequestParam Integer result,
            @RequestParam(required = false) String remark) {
        log.info("批量审核申诉: count={}, auditUserId={}", appealIds.size(), auditUserId);
        int successCount = appealService.batchAuditAppeals(appealIds, auditUserId, result, remark);
        return ApiResponse.success(successCount);
    }

    @GetMapping("/check")
    @Operation(summary = "检查是否可以申诉", description = "检查违规记录是否可以申诉")
    public ApiResponse<Map<String, Object>> checkAppealable(
            @RequestParam Integer illegalId,
            @RequestParam Integer userId) {
        Map<String, Object> result = appealService.checkAppealable(illegalId, userId);
        return ApiResponse.success(result);
    }

    @PostMapping("/state")
    @Operation(summary = "更新申诉状态", description = "手动更新申诉状态")
    public ApiResponse<Boolean> updateAppealState(
            @RequestParam Integer appealId,
            @RequestParam Integer state,
            @RequestParam(required = false) String remark) {
        log.info("更新申诉状态: appealId={}, state={}", appealId, state);
        boolean result = appealService.updateAppealState(appealId, state, remark);
        return ApiResponse.success(result);
    }

    @GetMapping("/export")
    @Operation(summary = "导出申诉记录", description = "导出申诉记录数据")
    public ApiResponse<List<AppealRecord>> exportAppeals(
            @RequestParam(required = false) Integer illegalId,
            @RequestParam(required = false) Integer userId,
            @RequestParam(required = false) Integer appealState,
            @RequestParam(required = false) String startTime,
            @RequestParam(required = false) String endTime) {

        AppealQueryVO queryVO = new AppealQueryVO();
        queryVO.setIllegalId(illegalId);
        queryVO.setUserId(userId);
        queryVO.setAppealState(appealState);
        queryVO.setStartTime(startTime);
        queryVO.setEndTime(endTime);

        List<AppealRecord> appeals = appealService.exportAppeals(queryVO);
        return ApiResponse.success(appeals);
    }

    @PostMapping("/upload-evidence")
    @Operation(summary = "上传申诉证据", description = "上传申诉证据图片")
    public ApiResponse<String> uploadEvidence(
            @RequestParam("file") MultipartFile file,
            @RequestParam Integer appealId,
            @RequestParam Integer userId) {
        log.info("上传申诉证据: appealId={}, userId={}, filename={}",
                appealId, userId, file.getOriginalFilename());

        // 校验文件
        if (file.isEmpty()) {
            throw new BusinessException("上传文件不能为空");
        }

        // 限制文件大小为10MB
        long fileSize = file.getSize();
        if (fileSize > 10 * 1024 * 1024) {
            throw new BusinessException("文件大小不能超过10MB");
        }

        // 限制文件类型
        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new BusinessException("只能上传图片类型文件");
        }

        // TODO: 实现文件上传逻辑
        // 1. 生成唯一文件名
        // 2. 保存文件到服务器/OSS
        // 3. 返回文件URL
        String fileUrl = "http://example.com/uploads/evidence/" + System.currentTimeMillis() + "_" + file.getOriginalFilename();

        // 更新申诉记录
        AppealRecord appeal = appealService.getById(appealId);
        if (appeal != null && appeal.getUserId().equals(userId)) {
            String evidence = appeal.getAppealEvidence();
            if (evidence == null || evidence.isEmpty()) {
                appeal.setAppealEvidence(fileUrl);
            } else {
                appeal.setAppealEvidence(evidence + "," + fileUrl);
            }
            appealService.updateById(appeal);
        } else {
            throw new BusinessException("无权操作该申诉记录");
        }

        return ApiResponse.success(fileUrl);
    }

    @GetMapping("/overdue")
    @Operation(summary = "获取超时未处理申诉", description = "获取超过审核时限的申诉")
    public ApiResponse<List<AppealRecord>> getOverdueAppeals() {
        log.info("查询超时未处理申诉，超时时间：{}小时", AUDIT_DEADLINE_HOURS);

        // 从数据库中查询超时未处理的申诉
        List<AppealRecord> overdueAppeals = appealService.lambdaQuery()
                .eq(AppealRecord::getAppealState, AppealStateEnum.PENDING.getCode()) // 使用枚举常量，避免硬编码
                .lt(AppealRecord::getAppealTime, LocalDateTime.now().minusHours(AUDIT_DEADLINE_HOURS))
                .orderByAsc(AppealRecord::getAppealTime)
                .list();

        // 补充用户信息 - 修复核心：使用注入的userMapper实例调用方法
        for (AppealRecord appeal : overdueAppeals) {
            if (appeal.getUserId() != null) {
                User user = userMapper.selectById(appeal.getUserId()); // 使用实例调用非静态方法
                if (user != null) {
                    appeal.setUserName(user.getRealName());
                    appeal.setUserPhone(user.getPhone()); // 补充更多用户信息
                }
            }
        }

        return ApiResponse.success(overdueAppeals);
    }

    @PostMapping("/remind")
    @Operation(summary = "催办申诉", description = "用户催办申诉处理")
    public ApiResponse<Boolean> remindAppeal(
            @RequestParam Integer appealId,
            @RequestParam Integer userId,
            @RequestParam(required = false) String message) {
        log.info("催办申诉: appealId={}, userId={}", appealId, userId);

        // 查询申诉记录
        AppealRecord appeal = appealService.getById(appealId);
        if (appeal == null) {
            throw new BusinessException("申诉记录不存在");
        }

        // 校验权限
        if (!appeal.getUserId().equals(userId)) {
            throw new BusinessException("只能催办自己的申诉");
        }

        // 校验状态
        if (appeal.getAppealState() != AppealStateEnum.PENDING.getCode() &&
                appeal.getAppealState() != AppealStateEnum.PROCESSING.getCode()) {
            throw new BusinessException("只有待审核或审核中的申诉可以催办");
        }

        // 发送催办通知给管理员
        String title = "申诉催办通知";
        String userName = "用户" + userId;
        User user = userMapper.selectById(userId);
        if (user != null) {
            userName = user.getRealName();
        }

        String content = String.format(
                "用户 %s 催办申诉（ID：%d）处理。%s",
                userName,
                appealId,
                message != null ? "留言：" + message : ""
        );

        // 发送通知
        try {
            // 获取管理员列表并发送通知
            LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(User::getUserType, "admin")
                    .eq(User::getStatus, "active")
                    .eq(User::getDeleted, false);
            List<User> admins = userMapper.selectList(wrapper);

            if (!admins.isEmpty()) {
                List<Integer> adminIds = admins.stream()
                        .map(User::getUserId)
                        .toList();
                notificationService.sendBatchNotification(title, content, adminIds);
            }

            log.info("催办通知已发送: appealId={}", appealId);
        } catch (Exception e) {
            log.error("发送催办通知失败: {}", e.getMessage(), e);
            throw new BusinessException("催办通知发送失败");
        }

        return ApiResponse.success(true);
    }
}