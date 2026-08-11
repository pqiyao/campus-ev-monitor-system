package com.example.evmonitorplatform.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.evmonitorplatform.common.exception.BusinessException;
import com.example.evmonitorplatform.entity.LicensePointLog;
import com.example.evmonitorplatform.entity.User;
import com.example.evmonitorplatform.entity.Vehicle;
import com.example.evmonitorplatform.entity.Violation;
import com.example.evmonitorplatform.entity.WelfarePointRequest;
import com.example.evmonitorplatform.mapper.LicensePointLogMapper;
import com.example.evmonitorplatform.mapper.UserMapper;
import com.example.evmonitorplatform.mapper.VehicleMapper;
import com.example.evmonitorplatform.mapper.ViolationMapper;
import com.example.evmonitorplatform.mapper.WelfarePointRequestMapper;
import com.example.evmonitorplatform.service.LicensePointService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class LicensePointServiceImpl implements LicensePointService {

    public static final String BIZ_VIOLATION_DEDUCT = "VIOLATION_DEDUCT";
    public static final String BIZ_DELETION_RESTORE = "DELETION_RESTORE";
    public static final String BIZ_ADMIN_BONUS = "ADMIN_BONUS";
    public static final String BIZ_WELFARE_BONUS = "WELFARE_BONUS";
    public static final String BIZ_APPEAL_PASS_RESTORE = "APPEAL_PASS_RESTORE";

    private static final int STATUS_PENDING = 0;
    private static final int STATUS_APPROVED = 1;
    private static final int STATUS_REJECTED = 2;

    private final UserMapper userMapper;
    private final VehicleMapper vehicleMapper;
    private final LicensePointLogMapper licensePointLogMapper;
    private final WelfarePointRequestMapper welfarePointRequestMapper;
    private final ViolationMapper violationMapper;

    @Value("${ev.license.max-points:12}")
    private int maxLicensePoints;

    @Override
    public int maxPoints() {
        return maxLicensePoints;
    }

    private int normRemaining(Integer licensePoints) {
        if (licensePoints == null) {
            return maxLicensePoints;
        }
        return Math.max(0, Math.min(maxLicensePoints, licensePoints));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void applyDeductionForViolation(Violation violation) {
        if (violation == null || violation.getIllegalId() == null) {
            return;
        }
        Integer effectUserId = violation.getUserId();
        if (effectUserId == null && violation.getVehicleId() != null) {
            Vehicle vehicle = vehicleMapper.selectById(violation.getVehicleId());
            if (vehicle != null && vehicle.getUserId() != null) {
                effectUserId = vehicle.getUserId();
                violation.setUserId(effectUserId);
            }
        }
        if (effectUserId == null) {
            violation.setPointsDeducted(0);
            return;
        }

        User user = userMapper.selectById(effectUserId);
        if (user == null) {
            violation.setPointsDeducted(0);
            return;
        }

        int current = normRemaining(user.getLicensePoints());
        int req = violation.getDeductionPoints() != null && violation.getDeductionPoints() > 0
                ? violation.getDeductionPoints() : 2;
        int actual = Math.min(req, current);
        int newBalance = current - actual;

        user.setLicensePoints(newBalance);
        userMapper.updateById(user);
        violation.setPointsDeducted(actual);

        if (actual > 0) {
            insertLog(effectUserId, -actual, newBalance, BIZ_VIOLATION_DEDUCT,
                    violation.getIllegalId(), null,
                    "违规记分扣除", null, null);
        }
    }

    private void insertLog(Integer userId, int delta, int balanceAfter, String bizType,
                           Integer refIllegalId, Integer refWelfareId,
                           String remark, String evidenceUrls, Integer operatorUserId) {
        LicensePointLog logRow = new LicensePointLog();
        logRow.setUserId(userId);
        logRow.setDelta(delta);
        logRow.setBalanceAfter(balanceAfter);
        logRow.setBizType(bizType);
        logRow.setRefIllegalId(refIllegalId);
        logRow.setRefWelfareId(refWelfareId);
        logRow.setRemark(remark);
        logRow.setEvidenceUrls(evidenceUrls);
        logRow.setOperatorUserId(operatorUserId);
        logRow.setCreateTime(LocalDateTime.now());
        licensePointLogMapper.insert(logRow);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void restoreOnViolationDeletion(Violation violation, Integer operatorUserId, String remark) {
        if (violation == null) {
            return;
        }
        Integer uid = violation.getUserId();
        Integer pd = violation.getPointsDeducted();
        if (uid == null || pd == null || pd <= 0) {
            return;
        }
        User user = userMapper.selectById(uid);
        if (user == null) {
            return;
        }
        int current = normRemaining(user.getLicensePoints());
        int nb = Math.min(maxLicensePoints, current + pd);
        user.setLicensePoints(nb);
        userMapper.updateById(user);
        String rm = StringUtils.hasText(remark) ? remark : "删除违规记录，恢复记分";
        insertLog(uid, pd, nb, BIZ_DELETION_RESTORE, violation.getIllegalId(), null,
                rm, null, operatorUserId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void restoreOnAppealApproved(Violation violation, Integer operatorUserId, String remark) {
        if (violation == null || violation.getIllegalId() == null) {
            return;
        }
        Integer pd = violation.getPointsDeducted();
        if (pd == null || pd <= 0) {
            return;
        }
        Integer uid = violation.getUserId();
        if (uid == null) {
            return;
        }
        User user = userMapper.selectById(uid);
        if (user == null) {
            return;
        }
        int current = normRemaining(user.getLicensePoints());
        int nb = Math.min(maxLicensePoints, current + pd);
        user.setLicensePoints(nb);
        userMapper.updateById(user);
        String rm = StringUtils.hasText(remark) ? remark : "申诉通过，恢复违规扣分";
        insertLog(uid, pd, nb, BIZ_APPEAL_PASS_RESTORE, violation.getIllegalId(), null,
                rm, null, operatorUserId);
        violationMapper.update(null, new LambdaUpdateWrapper<Violation>()
                .set(Violation::getPointsDeducted, 0)
                .eq(Violation::getIllegalId, violation.getIllegalId()));
    }

    @Override
    public int getRemainingPoints(Integer userId) {
        if (userId == null) {
            return 0;
        }
        User user = userMapper.selectById(userId);
        if (user == null) {
            return 0;
        }
        return normRemaining(user.getLicensePoints());
    }

    @Override
    public Page<LicensePointLog> pageLogs(Integer userId, int pageNum, int pageSize) {
        Page<LicensePointLog> p = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<LicensePointLog> q = new LambdaQueryWrapper<LicensePointLog>()
                .eq(userId != null, LicensePointLog::getUserId, userId)
                .orderByDesc(LicensePointLog::getCreateTime);
        return licensePointLogMapper.selectPage(p, q);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void addBonusByAdmin(Integer userId, int points, String remark, String evidenceUrls, Integer operatorUserId) {
        if (userId == null || points <= 0) {
            throw new BusinessException("用户与加分分值无效");
        }
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        int current = normRemaining(user.getLicensePoints());
        int actual = Math.min(points, maxLicensePoints - current);
        if (actual <= 0) {
            throw new BusinessException("当前已满 " + maxLicensePoints + " 分，无法继续加分");
        }
        int nb = current + actual;
        user.setLicensePoints(nb);
        userMapper.updateById(user);
        insertLog(userId, actual, nb, BIZ_ADMIN_BONUS, null, null,
                StringUtils.hasText(remark) ? remark : "管理员加分", evidenceUrls, operatorUserId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public WelfarePointRequest submitWelfareRequest(Integer userId, String content, String imageUrls) {
        if (userId == null) {
            throw new BusinessException("用户未登录");
        }
        if (!StringUtils.hasText(content) || content.length() < 6) {
            throw new BusinessException("请填写至少6个字的公益说明");
        }
        WelfarePointRequest r = new WelfarePointRequest();
        r.setUserId(userId);
        r.setContent(content.trim());
        r.setImageUrls(imageUrls);
        r.setStatus(STATUS_PENDING);
        r.setPointsAwarded(0);
        r.setCreateTime(LocalDateTime.now());
        welfarePointRequestMapper.insert(r);
        return r;
    }

    @Override
    public List<WelfarePointRequest> listMyWelfareRequests(Integer userId) {
        return welfarePointRequestMapper.selectList(
                new LambdaQueryWrapper<WelfarePointRequest>()
                        .eq(WelfarePointRequest::getUserId, userId)
                        .orderByDesc(WelfarePointRequest::getCreateTime)
                        .last("LIMIT 50"));
    }

    @Override
    public Page<WelfarePointRequest> pagePendingWelfare(int pageNum, int pageSize) {
        Page<WelfarePointRequest> p = new Page<>(pageNum, pageSize);
        return welfarePointRequestMapper.selectPage(p,
                new LambdaQueryWrapper<WelfarePointRequest>()
                        .eq(WelfarePointRequest::getStatus, STATUS_PENDING)
                        .orderByAsc(WelfarePointRequest::getCreateTime));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void reviewWelfareRequest(Integer requestId, boolean approve, int points, String adminRemark, Integer reviewerId) {
        WelfarePointRequest r = welfarePointRequestMapper.selectById(requestId);
        if (r == null) {
            throw new BusinessException("申请不存在");
        }
        if (!Integer.valueOf(STATUS_PENDING).equals(r.getStatus())) {
            throw new BusinessException("该申请已审核");
        }
        LocalDateTime now = LocalDateTime.now();
        if (!approve) {
            r.setStatus(STATUS_REJECTED);
            r.setAdminRemark(adminRemark);
            r.setReviewerId(reviewerId);
            r.setAuditTime(now);
            r.setUpdateTime(now);
            welfarePointRequestMapper.updateById(r);
            return;
        }
        if (points <= 0) {
            throw new BusinessException("通过时请指定加分分值");
        }
        User user = userMapper.selectById(r.getUserId());
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        int current = normRemaining(user.getLicensePoints());
        int actual = Math.min(points, maxLicensePoints - current);
        if (actual <= 0) {
            throw new BusinessException("该用户已满 " + maxLicensePoints + " 分，无法加分");
        }
        int nb = current + actual;
        user.setLicensePoints(nb);
        userMapper.updateById(user);

        r.setStatus(STATUS_APPROVED);
        r.setPointsAwarded(actual);
        r.setAdminRemark(adminRemark);
        r.setReviewerId(reviewerId);
        r.setAuditTime(now);
        r.setUpdateTime(now);
        welfarePointRequestMapper.updateById(r);

        insertLog(r.getUserId(), actual, nb, BIZ_WELFARE_BONUS, null, r.getRequestId(),
                "公益加分：" + (StringUtils.hasText(adminRemark) ? adminRemark : "审核通过"),
                r.getImageUrls(), reviewerId);
    }

    @Override
    public Map<String, Object> summaryForUser(Integer userId) {
        Map<String, Object> m = new HashMap<>();
        m.put("maxPoints", maxLicensePoints);
        m.put("remaining", getRemainingPoints(userId));
        m.put("deducted", maxLicensePoints - getRemainingPoints(userId));
        return m;
    }
}
