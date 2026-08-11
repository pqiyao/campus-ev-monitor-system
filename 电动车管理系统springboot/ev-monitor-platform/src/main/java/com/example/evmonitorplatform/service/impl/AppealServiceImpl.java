package com.example.evmonitorplatform.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.support.SFunction;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.evmonitorplatform.common.enums.AppealResultEnum;
import com.example.evmonitorplatform.common.enums.AppealStateEnum;
import com.example.evmonitorplatform.common.enums.ViolationTypeEnum;
import com.example.evmonitorplatform.common.exception.BusinessException;
import com.example.evmonitorplatform.common.response.PageResponse;
import com.example.evmonitorplatform.entity.AppealRecord;
import com.example.evmonitorplatform.entity.User;
import com.example.evmonitorplatform.entity.Violation;
import com.example.evmonitorplatform.mapper.AppealRecordMapper;
import com.example.evmonitorplatform.mapper.UserMapper;
import com.example.evmonitorplatform.mapper.ViolationMapper;
import com.example.evmonitorplatform.service.AppealService;
import com.example.evmonitorplatform.service.LicensePointService;
import com.example.evmonitorplatform.service.NotificationService;
import com.example.evmonitorplatform.vo.request.AppealAuditVO;
import com.example.evmonitorplatform.vo.request.AppealQueryVO;
import com.example.evmonitorplatform.vo.request.AppealSubmitVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AppealServiceImpl extends ServiceImpl<AppealRecordMapper, AppealRecord> implements AppealService {

    private final ViolationMapper violationMapper;
    private final UserMapper userMapper;
    private final NotificationService notificationService;
    private final JdbcTemplate jdbcTemplate;
    private final LicensePointService licensePointService;

    // 申诉配置
    private static final int APPEAL_DEADLINE_HOURS = 72; // 申诉截止时间：72小时
    private static final int AUDIT_DEADLINE_HOURS  = 48; // 审核截止时间：48小时
    private static final int MAX_APPEAL_TIMES      = 2;  // 最大申诉次数

    // ======================== 申诉提交 ========================
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean submitAppeal(AppealSubmitVO submitVO) {
        log.info("提交申诉: illegalId={}, userId={}", submitVO.getIllegalId(), submitVO.getUserId());

        // 1. 验证违规记录
        Violation violation = violationMapper.selectById(submitVO.getIllegalId());
        if (violation == null) {
            throw new BusinessException("违规记录不存在");
        }
        if (!violation.getUserId().equals(submitVO.getUserId())) {
            throw new BusinessException("只能对自己的违规记录进行申诉");
        }

        // 2. 检查申诉时限
        LocalDateTime illegalTime = violation.getIllegalTime();
        LocalDateTime now = LocalDateTime.now();
        Duration duration = Duration.between(illegalTime, now);
        if (duration.toHours() > APPEAL_DEADLINE_HOURS) {
            throw new BusinessException(String.format("已超过申诉时限（%d小时），无法提交申诉", APPEAL_DEADLINE_HOURS));
        }

        // 3. 检查申诉次数
        LambdaQueryWrapper<AppealRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AppealRecord::getIllegalId, submitVO.getIllegalId())
                .eq(AppealRecord::getUserId, submitVO.getUserId());
        long appealCount = this.count(wrapper);
        if (appealCount >= MAX_APPEAL_TIMES) {
            throw new BusinessException(String.format("该违规记录已达到最大申诉次数（%d次）", MAX_APPEAL_TIMES));
        }

        // 4. 检查是否有正在进行的申诉
        wrapper.in(AppealRecord::getAppealState,
                AppealStateEnum.PENDING.getCode(),
                AppealStateEnum.PROCESSING.getCode());
        long pendingCount = this.count(wrapper);
        if (pendingCount > 0) {
            throw new BusinessException("该违规记录已有正在处理的申诉，请等待处理结果");
        }

        // 5. 创建申诉记录
        AppealRecord appeal = new AppealRecord();
        appeal.setIllegalId(submitVO.getIllegalId());
        appeal.setUserId(submitVO.getUserId());
        appeal.setAppealReason(submitVO.getAppealReason());
        appeal.setAppealEvidence(submitVO.getAppealEvidence());
        appeal.setAppealTime(now);
        appeal.setAppealState(AppealStateEnum.PENDING.getCode());
        appeal.setAppealSource("user");
        if (submitVO.getUrgencyLevel() != null) {
            appeal.setUrgencyLevel(submitVO.getUrgencyLevel());
        }

        boolean saved = this.save(appeal);

        if (saved) {
            // 6. 更新违规记录的申诉状态
            violation.setIllegalState(4); // 申诉中
            violationMapper.updateById(violation);

            // 7. 发送通知给管理员（发送前补充用户信息）
            enrichAppealInfo(appeal);
            sendNewAppealNotification(appeal, violation);

            // 8. 记录操作日志（写入数据库）
            writeOperationLog(appeal.getAppealId(), "submit", submitVO.getUserId(), "提交申诉");

            log.info("申诉提交成功: appealId={}", appeal.getAppealId());
        }

        return saved;
    }

    // ======================== 申诉审核 ========================
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean auditAppeal(AppealAuditVO auditVO) {
        log.info("审核申诉: appealId={}, auditUserId={}, result={}",
                auditVO.getAppealId(), auditVO.getAuditUserId(), auditVO.getAppealResult());

        // 1. 验证申诉记录
        AppealRecord appeal = this.getById(auditVO.getAppealId());
        if (appeal == null) {
            throw new BusinessException("申诉记录不存在");
        }
        if (appeal.getAppealState() != AppealStateEnum.PENDING.getCode() &&
                appeal.getAppealState() != AppealStateEnum.PROCESSING.getCode()) {
            throw new BusinessException("申诉记录状态不正确，无法审核");
        }

        // 2. 验证审核结果
        if (!AppealResultEnum.isValid(auditVO.getAppealResult())) {
            throw new BusinessException("无效的审核结果");
        }

        // 3. 更新申诉记录
        appeal.setAuditUserId(auditVO.getAuditUserId());
        appeal.setAuditOpinion(auditVO.getAuditOpinion());
        appeal.setAppealResult(auditVO.getAppealResult());
        appeal.setEvidencePhotos(auditVO.getEvidencePhotos());
        appeal.setAuditTime(LocalDateTime.now());

        // 根据审核结果设置状态
        switch (Objects.requireNonNull(AppealResultEnum.getByCode(auditVO.getAppealResult()))) {
            case PASS:
            case PARTIAL_PASS:
                appeal.setAppealState(AppealStateEnum.SUCCESS.getCode());
                break;
            case REJECT:
                appeal.setAppealState(AppealStateEnum.FAILED.getCode());
                appeal.setRejectReason(auditVO.getRejectReason());
                break;
            case TRANSFER:
                appeal.setAppealState(AppealStateEnum.PROCESSING.getCode());
                break;
        }

        boolean updated = this.updateById(appeal);

        if (updated) {
            // 4. 更新违规记录状态；申诉通过/部分通过时恢复 12 分制中该条违规已扣分数
            Violation violation = violationMapper.selectById(appeal.getIllegalId());
            if (violation != null) {
                Integer ar = auditVO.getAppealResult();
                boolean appealRestoresPoints = AppealResultEnum.PASS.getCode().equals(ar)
                        || AppealResultEnum.PARTIAL_PASS.getCode().equals(ar);
                if (appealRestoresPoints) {
                    AppealResultEnum arEnum = AppealResultEnum.getByCode(ar);
                    String restoreRemark = arEnum != null
                            ? ("申诉" + arEnum.getDescription() + "，恢复记分")
                            : "申诉通过，恢复记分";
                    licensePointService.restoreOnAppealApproved(violation, auditVO.getAuditUserId(), restoreRemark);
                    violation.setPointsDeducted(0);
                }
                if (AppealResultEnum.PASS.getCode().equals(ar)) {
                    violation.setIllegalState(5); // 申诉成功
                } else if (AppealResultEnum.REJECT.getCode().equals(ar)) {
                    violation.setIllegalState(3); // 申诉失败，回到待整改状态
                } else if (AppealResultEnum.PARTIAL_PASS.getCode().equals(ar)) {
                    violation.setIllegalState(6); // 申诉部分通过
                }
                violationMapper.updateById(violation);
            }

            // 5. 发送通知给用户
            if (Boolean.TRUE.equals(auditVO.getNotifyUser())) {
                enrichAppealInfo(appeal);
                sendAppealResultNotification(appeal, auditVO);
            }

            // 6. 记录操作日志
            String desc = String.format("审核申诉，结果：%s",
                    Objects.requireNonNull(AppealResultEnum.getByCode(auditVO.getAppealResult())).getDescription());
            writeOperationLog(appeal.getAppealId(), "audit", auditVO.getAuditUserId(), desc);

            log.info("申诉审核完成: appealId={}, result={}", appeal.getAppealId(), auditVO.getAppealResult());
        }

        return updated;
    }

    // ======================== 撤销申诉 ========================
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean cancelAppeal(Integer appealId, Integer userId, String reason) {
        log.info("撤销申诉: appealId={}, userId={}, reason={}", appealId, userId, reason);

        // 1. 验证申诉记录
        AppealRecord appeal = this.getById(appealId);
        if (appeal == null) {
            throw new BusinessException("申诉记录不存在");
        }
        if (!appeal.getUserId().equals(userId)) {
            throw new BusinessException("只能撤销自己的申诉");
        }
        if (appeal.getAppealState() != AppealStateEnum.PENDING.getCode()) {
            throw new BusinessException("只能撤销待审核的申诉");
        }

        // 2. 更新申诉记录
        appeal.setAppealState(AppealStateEnum.CANCELED.getCode());
        appeal.setRejectReason(reason);

        boolean updated = this.updateById(appeal);

        if (updated) {
            // 3. 更新违规记录状态
            Violation violation = violationMapper.selectById(appeal.getIllegalId());
            if (violation != null && violation.getIllegalState() == 4) {
                violation.setIllegalState(0); // 回到待整改状态
                violationMapper.updateById(violation);
            }

            // 4. 发送通知给管理员
            enrichAppealInfo(appeal);
            sendAppealCancelNotification(appeal, reason);

            // 5. 记录操作日志
            writeOperationLog(appealId, "cancel", userId, "撤销申诉，原因：" + reason);

            log.info("申诉已撤销: appealId={}", appealId);
        }

        return updated;
    }

    // ======================== 获取申诉详情 ========================
    @Override
    public AppealRecord getAppealDetail(Integer appealId) {
        AppealRecord appeal = this.getById(appealId);
        if (appeal != null) {
            enrichAppealInfo(appeal);
        }
        return appeal;
    }

    // ======================== 分页查询申诉 ========================
    @Override
    public PageResponse<List<AppealRecord>> queryAppeals(AppealQueryVO queryVO) {
        Page<AppealRecord> page = new Page<>(queryVO.getPageNum(), queryVO.getPageSize());

        LambdaQueryWrapper<AppealRecord> wrapper = buildQueryWrapper(queryVO);

        // 设置排序 - 使用SFunction
        if (StringUtils.hasText(queryVO.getOrderBy())) {
            boolean isAsc = "asc".equalsIgnoreCase(queryVO.getOrderDirection());
            SFunction<AppealRecord, ?> orderField = getOrderByField(queryVO.getOrderBy());
            if (orderField != null) {
                wrapper.orderBy(true, isAsc, orderField);
            } else {
                wrapper.orderByDesc(AppealRecord::getAppealTime);
            }
        } else {
            wrapper.orderByDesc(AppealRecord::getAppealTime);
        }

        Page<AppealRecord> resultPage = this.page(page, wrapper);

        // 补充信息
        resultPage.getRecords().forEach(this::enrichAppealInfo);

        return new PageResponse<>(
                200,
                "success",
                resultPage.getRecords(),
                (int) resultPage.getCurrent(),
                (int) resultPage.getSize(),
                resultPage.getTotal()
        );
    }

    // ======================== 用户申诉列表 ========================
    @Override
    public PageResponse<List<AppealRecord>> getUserAppeals(Integer userId, Integer pageNum, Integer pageSize) {
        AppealQueryVO queryVO = new AppealQueryVO();
        queryVO.setUserId(userId);
        queryVO.setPageNum(pageNum);
        queryVO.setPageSize(pageSize);
        queryVO.setOrderBy("appeal_time");
        queryVO.setOrderDirection("desc");
        return queryAppeals(queryVO);
    }

    // ======================== 待处理申诉列表 ========================
    @Override
    public PageResponse<List<AppealRecord>> getPendingAppeals(Integer pageNum, Integer pageSize) {
        AppealQueryVO queryVO = new AppealQueryVO();
        queryVO.setAppealState(AppealStateEnum.PENDING.getCode());
        queryVO.setPageNum(pageNum);
        queryVO.setPageSize(pageSize);
        queryVO.setOrderBy("appeal_time");
        queryVO.setOrderDirection("asc");
        return queryAppeals(queryVO);
    }

    // ======================== 申诉统计 ========================
    @Override
    public Map<String, Object> getAppealStats() {
        Map<String, Object> stats = new HashMap<>();

        // 总申诉数
        long total = this.count();
        stats.put("total", total);

        // 各状态数量
        for (AppealStateEnum state : AppealStateEnum.values()) {
            long count = this.lambdaQuery()
                    .eq(AppealRecord::getAppealState, state.getCode())
                    .count();
            stats.put(state.getValue() + "Count", count);
        }

        // 各结果数量
        for (AppealResultEnum result : AppealResultEnum.values()) {
            long count = this.lambdaQuery()
                    .eq(AppealRecord::getAppealResult, result.getCode())
                    .count();
            stats.put("result" + result.getCode() + "Count", count);
        }

        // 申诉成功率
        long successCount = this.lambdaQuery()
                .eq(AppealRecord::getAppealState, AppealStateEnum.SUCCESS.getCode())
                .count();
        double successRate = total > 0 ? (double) successCount / total * 100 : 0;
        stats.put("successRate", String.format("%.1f%%", successRate));

        // 平均处理时间
        stats.put("avgProcessTime", getAverageProcessTime() + "小时");

        // 今日申诉数
        LocalDateTime todayStart = LocalDateTime.now().withHour(0).withMinute(0).withSecond(0);
        LocalDateTime todayEnd = LocalDateTime.now().withHour(23).withMinute(59).withSecond(59);
        long todayCount = this.lambdaQuery()
                .ge(AppealRecord::getAppealTime, todayStart)
                .le(AppealRecord::getAppealTime, todayEnd)
                .count();
        stats.put("todayCount", todayCount);

        // 超时未处理数
        LocalDateTime deadline = LocalDateTime.now().minusHours(AUDIT_DEADLINE_HOURS);
        long overdueCount = this.lambdaQuery()
                .eq(AppealRecord::getAppealState, AppealStateEnum.PENDING.getCode())
                .lt(AppealRecord::getAppealTime, deadline)
                .count();
        stats.put("overdueCount", overdueCount);

        // 申诉来源统计
        Map<String, Long> sourceStats = new HashMap<>();
        sourceStats.put("user", this.lambdaQuery()
                .eq(AppealRecord::getAppealSource, "user")
                .count());
        sourceStats.put("system", this.lambdaQuery()
                .eq(AppealRecord::getAppealSource, "system")
                .count());
        sourceStats.put("admin", this.lambdaQuery()
                .eq(AppealRecord::getAppealSource, "admin")
                .count());
        stats.put("sourceStats", sourceStats);

        return stats;
    }

    // ======================== 用户申诉统计 ========================
    @Override
    public Map<String, Object> getUserAppealStats(Integer userId) {
        Map<String, Object> stats = new HashMap<>();

        // 用户总申诉数
        long total = this.lambdaQuery()
                .eq(AppealRecord::getUserId, userId)
                .count();
        stats.put("total", total);

        // 各状态数量
        for (AppealStateEnum state : AppealStateEnum.values()) {
            long count = this.lambdaQuery()
                    .eq(AppealRecord::getUserId, userId)
                    .eq(AppealRecord::getAppealState, state.getCode())
                    .count();
            stats.put(state.getValue() + "Count", count);
        }

        // 申诉成功率
        long successCount = this.lambdaQuery()
                .eq(AppealRecord::getUserId, userId)
                .eq(AppealRecord::getAppealState, AppealStateEnum.SUCCESS.getCode())
                .count();
        double successRate = total > 0 ? (double) successCount / total * 100 : 0;
        stats.put("successRate", String.format("%.1f%%", successRate));

        // 最近申诉时间
        AppealRecord latest = this.lambdaQuery()
                .eq(AppealRecord::getUserId, userId)
                .orderByDesc(AppealRecord::getAppealTime)
                .last("LIMIT 1")
                .one();
        if (latest != null) {
            stats.put("latestAppealTime", latest.getAppealTime());
            stats.put("latestAppealState", latest.getAppealStateDesc());
        }

        return stats;
    }

    // ======================== 申诉仪表盘 ========================
    @Override
    public Map<String, Object> getAppealDashboard() {
        Map<String, Object> dashboard = new HashMap<>();

        Map<String, Object> stats = getAppealStats();
        dashboard.putAll(stats);

        // 待处理申诉列表（最近5条）
        PageResponse<List<AppealRecord>> pending = getPendingAppeals(1, 5);
        dashboard.put("pendingAppeals", pending.getData());

        // 近期申诉趋势（最近7天）
        Map<String, Long> trend = getAppealTrendLast7Days();
        dashboard.put("appealTrend", trend);

        // 违规类型分布（改用数据库聚合查询，避免N+1）
        Map<String, Long> typeDistribution = getAppealTypeDistributionFromDb();
        dashboard.put("typeDistribution", typeDistribution);

        // 处理效率排名（简化实现）
        List<Map<String, Object>> efficiencyRanking = getAuditEfficiencyRanking();
        dashboard.put("efficiencyRanking", efficiencyRanking);

        return dashboard;
    }

    // ======================== 导出申诉 ========================
    @Override
    public List<AppealRecord> exportAppeals(AppealQueryVO queryVO) {
        LambdaQueryWrapper<AppealRecord> wrapper = buildQueryWrapper(queryVO);
        wrapper.orderByDesc(AppealRecord::getAppealTime);

        List<AppealRecord> appeals = this.list(wrapper);
        appeals.forEach(this::enrichAppealInfo);

        return appeals;
    }

    // ======================== 批量审核 ========================
    @Override
    @Transactional(rollbackFor = Exception.class)
    public int batchAuditAppeals(List<Integer> appealIds, Integer auditUserId, Integer result, String remark) {
        log.info("批量审核申诉: count={}, auditUserId={}, result={}",
                appealIds.size(), auditUserId, result);

        if (appealIds.isEmpty()) {
            return 0;
        }

        // 根据result决定最终状态
        Integer targetState;
        switch (result) {
            case 1: // PASS
            case 3: // PARTIAL_PASS
                targetState = AppealStateEnum.SUCCESS.getCode();
                break;
            case 2: // REJECT
                targetState = AppealStateEnum.FAILED.getCode();
                break;
            case 4: // TRANSFER
                targetState = AppealStateEnum.PROCESSING.getCode();
                break;
            default:
                throw new BusinessException("无效的审核结果：" + result);
        }

        int successCount = 0;
        LocalDateTime now = LocalDateTime.now();

        for (Integer appealId : appealIds) {
            try {
                AppealRecord appeal = this.getById(appealId);
                if (appeal == null) {
                    log.warn("申诉记录不存在: appealId={}", appealId);
                    continue;
                }
                if (appeal.getAppealState() != AppealStateEnum.PENDING.getCode() &&
                        appeal.getAppealState() != AppealStateEnum.PROCESSING.getCode()) {
                    log.warn("申诉状态不正确: appealId={}, state={}", appealId, appeal.getAppealState());
                    continue;
                }

                // 更新申诉记录
                appeal.setAuditUserId(auditUserId);
                appeal.setAuditOpinion(remark);
                appeal.setAppealResult(result);
                appeal.setAuditTime(now);
                appeal.setAppealState(targetState);

                boolean updated = this.updateById(appeal);
                if (updated) {
                    // 更新违规记录
                    Violation violation = violationMapper.selectById(appeal.getIllegalId());
                    if (violation != null) {
                        if (targetState.equals(AppealStateEnum.SUCCESS.getCode())) {
                            violation.setIllegalState(5); // 申诉成功
                        } else if (targetState.equals(AppealStateEnum.FAILED.getCode())) {
                            violation.setIllegalState(3); // 申诉失败
                        } // 其他状态保持原样
                        violationMapper.updateById(violation);
                    }

                    // 发送通知
                    sendBatchAuditNotification(appeal, remark);

                    // 记录操作日志
                    writeOperationLog(appealId, "batch_audit", auditUserId,
                            "批量审核，结果：" + result);

                    successCount++;
                    log.info("批量审核成功: appealId={}", appealId);
                }
            } catch (Exception e) {
                log.error("批量审核失败: appealId={}, error={}", appealId, e.getMessage(), e);
            }
        }

        return successCount;
    }

    // ======================== 申诉效率统计 ========================
    @Override
    public Map<String, Object> getAppealEfficiencyStats(Integer days) {
        Map<String, Object> stats = new HashMap<>();

        int daysToCheck = days != null ? days : 30;
        LocalDateTime startTime = LocalDateTime.now().minusDays(daysToCheck);

        // 平均处理时间
        Double avgTime = baseMapper.getAverageProcessTime(startTime);
        stats.put("avgProcessTime", avgTime != null ? String.format("%.1f小时", avgTime) : "0小时");

        // 按时完成率
        Double onTimeRate = baseMapper.getOnTimeCompletionRate(startTime, AUDIT_DEADLINE_HOURS);
        stats.put("onTimeRate", onTimeRate != null ? String.format("%.1f%%", onTimeRate) : "0%");

        // 各审核人处理统计
        List<Map<String, Object>> auditorStats = baseMapper.getAuditorStatistics(startTime);
        stats.put("auditorStats", auditorStats);

        // 每日处理量
        List<Map<String, Object>> dailyStats = baseMapper.getDailyProcessStats(startTime);
        stats.put("dailyStats", dailyStats);

        return stats;
    }

    // ======================== 检查是否可以申诉 ========================
    @Override
    public Map<String, Object> checkAppealable(Integer illegalId, Integer userId) {
        Map<String, Object> result = new HashMap<>();

        // 检查违规记录
        Violation violation = violationMapper.selectById(illegalId);
        if (violation == null) {
            result.put("appealable", false);
            result.put("message", "违规记录不存在");
            return result;
        }

        // 检查用户权限
        if (!violation.getUserId().equals(userId)) {
            result.put("appealable", false);
            result.put("message", "只能对自己的违规记录进行申诉");
            return result;
        }

        // 检查申诉时限
        LocalDateTime illegalTime = violation.getIllegalTime();
        LocalDateTime now = LocalDateTime.now();
        Duration duration = Duration.between(illegalTime, now);
        long hoursPassed = duration.toHours();

        if (hoursPassed > APPEAL_DEADLINE_HOURS) {
            result.put("appealable", false);
            result.put("message", String.format("已超过申诉时限（%d小时），无法申诉", APPEAL_DEADLINE_HOURS));
            result.put("hoursPassed", hoursPassed);
            result.put("deadlineHours", APPEAL_DEADLINE_HOURS);
            return result;
        }

        // 检查申诉次数
        LambdaQueryWrapper<AppealRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AppealRecord::getIllegalId, illegalId)
                .eq(AppealRecord::getUserId, userId);
        long appealCount = this.count(wrapper);

        if (appealCount >= MAX_APPEAL_TIMES) {
            result.put("appealable", false);
            result.put("message", String.format("已达到最大申诉次数（%d次）", MAX_APPEAL_TIMES));
            result.put("appealCount", appealCount);
            result.put("maxAppealTimes", MAX_APPEAL_TIMES);
            return result;
        }

        // 检查是否有正在进行的申诉
        wrapper.in(AppealRecord::getAppealState,
                AppealStateEnum.PENDING.getCode(),
                AppealStateEnum.PROCESSING.getCode());
        long pendingCount = this.count(wrapper);

        if (pendingCount > 0) {
            result.put("appealable", false);
            result.put("message", "该违规记录已有正在处理的申诉");
            result.put("pendingCount", pendingCount);
            return result;
        }

        // 可以申诉
        result.put("appealable", true);
        result.put("message", "可以提交申诉");
        result.put("remainingHours", APPEAL_DEADLINE_HOURS - hoursPassed);
        result.put("remainingTimes", MAX_APPEAL_TIMES - appealCount);
        result.put("illegalInfo", violation);

        return result;
    }

    // ======================== 更新申诉状态 ========================
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateAppealState(Integer appealId, Integer state, String remark) {
        log.info("更新申诉状态: appealId={}, state={}", appealId, state);

        if (!AppealStateEnum.isValid(state)) {
            throw new BusinessException("无效的申诉状态");
        }

        AppealRecord appeal = this.getById(appealId);
        if (appeal == null) {
            throw new BusinessException("申诉记录不存在");
        }

        // 检查状态转移是否合法
        if (!isValidStateTransition(appeal.getAppealState(), state)) {
            throw new BusinessException("状态转移不合法");
        }

        // 更新状态
        appeal.setAppealState(state);
        if (StringUtils.hasText(remark)) {
            String oldOpinion = appeal.getAuditOpinion() != null ? appeal.getAuditOpinion() : "";
            appeal.setAuditOpinion(oldOpinion + " " + remark);
        }

        boolean updated = this.updateById(appeal);

        if (updated) {
            // 记录操作日志
            writeOperationLog(appealId, "update_state", null,
                    String.format("更新状态：%s -> %s, 备注：%s",
                            AppealStateEnum.getDescriptionByCode(appeal.getAppealState()),
                            AppealStateEnum.getDescriptionByCode(state),
                            remark));
        }

        return updated;
    }

    // ======================== 重新提交申诉 ========================
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean resubmitAppeal(Integer appealId, AppealSubmitVO submitVO) {
        log.info("重新提交申诉: appealId={}", appealId);

        // 获取原申诉记录
        AppealRecord oldAppeal = this.getById(appealId);
        if (oldAppeal == null) {
            throw new BusinessException("原申诉记录不存在");
        }

        // 检查是否可以重新提交
        if (oldAppeal.getAppealState() != AppealStateEnum.FAILED.getCode()) {
            throw new BusinessException("只有申诉失败的记录可以重新提交");
        }

        // 创建新的申诉记录
        AppealRecord newAppeal = new AppealRecord();
        BeanUtils.copyProperties(oldAppeal, newAppeal,
                "appealId", "appealReason", "appealEvidence", "appealTime",
                "appealState", "auditUserId", "auditOpinion", "appealResult",
                "rejectReason", "evidencePhotos", "auditTime");

        newAppeal.setAppealReason(submitVO.getAppealReason());
        newAppeal.setAppealEvidence(submitVO.getAppealEvidence());
        newAppeal.setAppealTime(LocalDateTime.now());
        newAppeal.setAppealState(AppealStateEnum.PENDING.getCode());
        newAppeal.setAppealSource("user");
        newAppeal.setUrgencyLevel(submitVO.getUrgencyLevel());

        boolean saved = this.save(newAppeal);

        if (saved) {
            // 更新原申诉记录
            oldAppeal.setRejectReason("已重新提交申诉");
            this.updateById(oldAppeal);

            // 更新违规记录状态
            Violation violation = violationMapper.selectById(newAppeal.getIllegalId());
            if (violation != null) {
                violation.setIllegalState(4); // 申诉中
                violationMapper.updateById(violation);
            }

            // 发送通知
            enrichAppealInfo(newAppeal);
            sendResubmitNotification(newAppeal);

            log.info("申诉重新提交成功: newAppealId={}, oldAppealId={}",
                    newAppeal.getAppealId(), appealId);
        }

        return saved;
    }

    // ======================== 私有辅助方法 ========================

    /**
     * 构建查询条件
     */
    private LambdaQueryWrapper<AppealRecord> buildQueryWrapper(AppealQueryVO queryVO) {
        LambdaQueryWrapper<AppealRecord> wrapper = new LambdaQueryWrapper<>();

        if (queryVO.getAppealId() != null) {
            wrapper.eq(AppealRecord::getAppealId, queryVO.getAppealId());
        }
        if (queryVO.getIllegalId() != null) {
            wrapper.eq(AppealRecord::getIllegalId, queryVO.getIllegalId());
        }
        if (queryVO.getUserId() != null) {
            wrapper.eq(AppealRecord::getUserId, queryVO.getUserId());
        }
        if (queryVO.getAppealState() != null) {
            wrapper.eq(AppealRecord::getAppealState, queryVO.getAppealState());
        }
        if (queryVO.getAppealResult() != null) {
            wrapper.eq(AppealRecord::getAppealResult, queryVO.getAppealResult());
        }
        if (queryVO.getAuditUserId() != null) {
            wrapper.eq(AppealRecord::getAuditUserId, queryVO.getAuditUserId());
        }
        if (StringUtils.hasText(queryVO.getAppealSource())) {
            wrapper.eq(AppealRecord::getAppealSource, queryVO.getAppealSource());
        }
        if (queryVO.getUrgencyLevel() != null) {
            wrapper.eq(AppealRecord::getUrgencyLevel, queryVO.getUrgencyLevel());
        }
        if (StringUtils.hasText(queryVO.getStartTime())) {
            wrapper.ge(AppealRecord::getAppealTime, queryVO.getStartTime());
        }
        if (StringUtils.hasText(queryVO.getEndTime())) {
            wrapper.le(AppealRecord::getAppealTime, queryVO.getEndTime());
        }
        if (queryVO.getCounselorId() != null) {
            wrapper.apply(
                    "EXISTS (SELECT 1 FROM counselor_class cc WHERE cc.counselor_user_id = {0} "
                            + "AND cc.class_name <=> (SELECT u2.class_name FROM `user` u2 WHERE u2.user_id = illegal_appeal_record.user_id AND u2.deleted = 0))",
                    queryVO.getCounselorId());
        }

        // 车牌号、用户名、手机号需要联表查询，这里先不处理，在Service层过滤（enrich时处理）

        return wrapper;
    }

    /**
     * 获取排序字段 - 返回实体类属性引用
     */
    private SFunction<AppealRecord, ?> getOrderByField(String orderBy) {
        switch (orderBy) {
            case "appeal_time":
                return AppealRecord::getAppealTime;
            case "audit_time":
                return AppealRecord::getAuditTime;
            case "create_time":
                return AppealRecord::getCreateTime; // 假设实体类有该属性的getter方法
            case "urgency_level":
                return AppealRecord::getUrgencyLevel;
            default:
                return AppealRecord::getAppealTime;
        }
    }

    /**
     * 补充申诉信息（用户、违规详情等）
     */
    private void enrichAppealInfo(AppealRecord appeal) {
        if (appeal.getUserId() != null) {
            User user = userMapper.selectById(appeal.getUserId());
            if (user != null) {
                appeal.setUserName(user.getRealName());
                appeal.setUserPhone(user.getPhone());
                appeal.setUserType(user.getUserType());
                appeal.setClassName(user.getClassName());
            }
        }

        if (appeal.getAuditUserId() != null) {
            User auditUser = userMapper.selectById(appeal.getAuditUserId());
            if (auditUser != null) {
                appeal.setAuditUserName(auditUser.getRealName());
            }
        }

        if (appeal.getIllegalId() != null) {
            Violation violation = violationMapper.selectWithVehicleInfo(appeal.getIllegalId());
            if (violation != null) {
                appeal.setIllegalType(violation.getIllegalType());
                appeal.setLicensePlate(violation.getLicensePlate());
                appeal.setIllegalTypeName(ViolationTypeEnum.getDescriptionByCode(violation.getIllegalType()));
                if (violation.getIllegalTime() != null) {
                    appeal.setIllegalTime(violation.getIllegalTime()
                            .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
                }
                appeal.setVehiclePhoto(violation.getVehiclePhoto());
                appeal.setEvidenceUrl(violation.getEvidenceUrl());

                // 位置信息
                if (violation.getIllegalLng() != null && violation.getIllegalLat() != null) {
                    appeal.setLocation(String.format("经度：%s, 纬度：%s",
                            violation.getIllegalLng(), violation.getIllegalLat()));
                }
            }
        }
    }

    /**
     * 写入操作日志到数据库
     */
    private void writeOperationLog(Integer appealId, String operation,
                                   Integer operatorId, String remark) {
        try {
            jdbcTemplate.update(
                    "INSERT INTO appeal_operation_log (appeal_id, operation_type, operator_id, operation_detail, create_time) " +
                            "VALUES (?, ?, ?, ?, NOW())",
                    appealId, operation, operatorId, remark
            );
        } catch (Exception e) {
            // 日志写失败不影响主流程，仅记录警告
            log.warn("写入操作日志失败: appealId={}, op={}, err={}", appealId, operation, e.getMessage());
        }
    }

    /**
     * 发送新申诉通知给管理员
     */
    private void sendNewAppealNotification(AppealRecord appeal, Violation violation) {
        try {
            String userName = StringUtils.hasText(appeal.getUserName())
                    ? appeal.getUserName() : "用户" + appeal.getUserId();
            String plate = violation.getLicensePlate() != null ? violation.getLicensePlate() : "未知车牌";
            String time = violation.getIllegalTime() != null
                    ? violation.getIllegalTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")) : "未知时间";
            String content = String.format("用户 %s 对违规记录（车牌：%s，时间：%s）提交了申诉，请及时处理。",
                    userName, plate, time);

            notificationService.sendBatchNotification("新的违规申诉待处理", content, getAdminUserIds());
            log.info("新申诉通知已发送");
        } catch (Exception e) {
            log.error("发送新申诉通知失败: {}", e.getMessage(), e);
        }
    }

    /**
     * 发送申诉结果通知给用户
     */
    private void sendAppealResultNotification(AppealRecord appeal, AppealAuditVO auditVO) {
        try {
            String resultDesc = Objects.requireNonNull(
                    AppealResultEnum.getByCode(auditVO.getAppealResult())).getDescription();
            String title = String.format("申诉处理结果：%s", resultDesc);

            StringBuilder content = new StringBuilder();
            content.append(String.format("您的申诉（ID：%d）已处理完成。\n", appeal.getAppealId()));
            content.append(String.format("处理结果：%s\n", resultDesc));
            content.append(String.format("处理意见：%s\n", auditVO.getAuditOpinion()));
            if (StringUtils.hasText(auditVO.getRejectReason())) {
                content.append(String.format("拒绝原因：%s\n", auditVO.getRejectReason()));
            }
            content.append(String.format("处理时间：%s\n",
                    LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))));
            if (StringUtils.hasText(auditVO.getHandlingSuggestion())) {
                content.append(String.format("后续建议：%s\n", auditVO.getHandlingSuggestion()));
            }

            notificationService.sendBatchNotification(title, content.toString(), List.of(appeal.getUserId()));
            log.info("申诉结果通知已发送给用户: userId={}", appeal.getUserId());
        } catch (Exception e) {
            log.error("发送申诉结果通知失败: {}", e.getMessage(), e);
        }
    }

    /**
     * 发送申诉撤销通知给管理员
     */
    private void sendAppealCancelNotification(AppealRecord appeal, String reason) {
        try {
            String userName = StringUtils.hasText(appeal.getUserName())
                    ? appeal.getUserName() : "用户" + appeal.getUserId();
            String content = String.format("用户 %s 撤销了申诉（ID：%d），原因：%s",
                    userName, appeal.getAppealId(), reason);

            notificationService.sendBatchNotification("用户撤销申诉", content, getAdminUserIds());
            log.info("申诉撤销通知已发送");
        } catch (Exception e) {
            log.error("发送申诉撤销通知失败: {}", e.getMessage(), e);
        }
    }

    /**
     * 发送批量审核通知
     */
    private void sendBatchAuditNotification(AppealRecord appeal, String remark) {
        try {
            String content = String.format("您的申诉（ID：%d）已通过批量审核方式处理完成。\n处理意见：%s",
                    appeal.getAppealId(), remark);
            notificationService.sendBatchNotification("申诉批量处理结果", content, List.of(appeal.getUserId()));
        } catch (Exception e) {
            log.error("发送批量审核通知失败: {}", e.getMessage(), e);
        }
    }

    /**
     * 发送重新提交通知
     */
    private void sendResubmitNotification(AppealRecord appeal) {
        try {
            String userName = StringUtils.hasText(appeal.getUserName())
                    ? appeal.getUserName() : "用户" + appeal.getUserId();
            String content = String.format("用户 %s 重新提交了申诉（新申诉ID：%d），请及时处理。",
                    userName, appeal.getAppealId());
            notificationService.sendBatchNotification("申诉重新提交通知", content, getAdminUserIds());
        } catch (Exception e) {
            log.error("发送重新提交通知失败: {}", e.getMessage(), e);
        }
    }

    /**
     * 获取管理员用户ID列表
     */
    private List<Integer> getAdminUserIds() {
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getUserType, "admin")
                .eq(User::getStatus, "active")
                .eq(User::getDeleted, false);
        List<User> admins = userMapper.selectList(wrapper);
        return admins.stream()
                .map(User::getUserId)
                .collect(Collectors.toList());
    }

    /**
     * 获取平均处理时间（基于已完成申诉）
     */
    private double getAverageProcessTime() {
        List<AppealRecord> completed = this.lambdaQuery()
                .in(AppealRecord::getAppealState,
                        AppealStateEnum.SUCCESS.getCode(),
                        AppealStateEnum.FAILED.getCode())
                .isNotNull(AppealRecord::getAuditTime)
                .list();

        if (completed.isEmpty()) {
            return 0;
        }

        return completed.stream()
                .mapToLong(a -> Duration.between(a.getAppealTime(), a.getAuditTime()).toHours())
                .average()
                .orElse(0);
    }

    /**
     * 获取申诉趋势（最近7天）
     */
    private Map<String, Long> getAppealTrendLast7Days() {
        Map<String, Long> trend = new LinkedHashMap<>();
        LocalDateTime now = LocalDateTime.now();

        for (int i = 6; i >= 0; i--) {
            LocalDateTime day = now.minusDays(i);
            String dateKey = day.format(DateTimeFormatter.ofPattern("MM-dd"));

            LocalDateTime dayStart = day.withHour(0).withMinute(0).withSecond(0);
            LocalDateTime dayEnd = day.withHour(23).withMinute(59).withSecond(59);

            long count = this.lambdaQuery()
                    .ge(AppealRecord::getAppealTime, dayStart)
                    .le(AppealRecord::getAppealTime, dayEnd)
                    .count();

            trend.put(dateKey, count);
        }

        return trend;
    }

    /**
     * 从数据库聚合查询申诉类型分布（避免 N+1）
     */
    private Map<String, Long> getAppealTypeDistributionFromDb() {
        Map<String, Long> distribution = new LinkedHashMap<>();
        try {
            String sql = "SELECT v.illegal_type, COUNT(*) AS cnt " +
                    "FROM illegal_appeal_record a " +
                    "JOIN vehicle_illegal_record v ON a.illegal_id = v.illegal_id " +
                    "GROUP BY v.illegal_type " +
                    "ORDER BY cnt DESC";
            jdbcTemplate.query(sql, rs -> {
                Integer typeCode = rs.getInt("illegal_type");
                String typeName = ViolationTypeEnum.getDescriptionByCode(typeCode);
                distribution.put(typeName, rs.getLong("cnt"));
            });
        } catch (Exception e) {
            log.warn("查询申诉类型分布失败", e);
        }
        return distribution;
    }

    /**
     * 获取审核效率排名（简化实现，可改为数据库查询）
     */
    private List<Map<String, Object>> getAuditEfficiencyRanking() {
        // 实际可改为从数据库统计，这里返回空列表
        return new ArrayList<>();
    }

    /**
     * 检查状态转移是否合法
     */
    private boolean isValidStateTransition(Integer fromState, Integer toState) {
        Map<Integer, List<Integer>> allowedTransitions = new HashMap<>();

        // 待审核可以转到：审核中、成功、失败、已撤销、已过期
        allowedTransitions.put(AppealStateEnum.PENDING.getCode(),
                Arrays.asList(
                        AppealStateEnum.PROCESSING.getCode(),
                        AppealStateEnum.SUCCESS.getCode(),
                        AppealStateEnum.FAILED.getCode(),
                        AppealStateEnum.CANCELED.getCode(),
                        AppealStateEnum.EXPIRED.getCode()
                ));

        // 审核中可以转到：成功、失败
        allowedTransitions.put(AppealStateEnum.PROCESSING.getCode(),
                Arrays.asList(
                        AppealStateEnum.SUCCESS.getCode(),
                        AppealStateEnum.FAILED.getCode()
                ));

        // 其他状态是终止状态，不允许转移

        List<Integer> allowed = allowedTransitions.get(fromState);
        return allowed != null && allowed.contains(toState);
    }
}