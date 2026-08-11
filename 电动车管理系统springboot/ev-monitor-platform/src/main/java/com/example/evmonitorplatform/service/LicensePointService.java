package com.example.evmonitorplatform.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.evmonitorplatform.entity.LicensePointLog;
import com.example.evmonitorplatform.entity.Violation;
import com.example.evmonitorplatform.entity.WelfarePointRequest;

import java.util.List;
import java.util.Map;

public interface LicensePointService {

    int maxPoints();

    /** 违规已落库后调用：扣减用户剩余分并写流水，回填 violation.pointsDeducted（及必要时 userId） */
    void applyDeductionForViolation(Violation violation);

    /** 管理端删除违规（软删）前调用：恢复当时扣除的分，不超过 12 */
    void restoreOnViolationDeletion(Violation violation, Integer operatorUserId, String remark);

    /**
     * 申诉通过/部分通过：恢复该违规当时扣除的记分，并将违规上的 points_deducted 置 0，避免重复恢复。
     */
    void restoreOnAppealApproved(Violation violation, Integer operatorUserId, String remark);

    int getRemainingPoints(Integer userId);

    Page<LicensePointLog> pageLogs(Integer userId, int pageNum, int pageSize);

    /** 管理端直接加分 */
    void addBonusByAdmin(Integer userId, int points, String remark, String evidenceUrls, Integer operatorUserId);

    WelfarePointRequest submitWelfareRequest(Integer userId, String content, String imageUrls);

    List<WelfarePointRequest> listMyWelfareRequests(Integer userId);

    Page<WelfarePointRequest> pagePendingWelfare(int pageNum, int pageSize);

    void reviewWelfareRequest(Integer requestId, boolean approve, int points, String adminRemark, Integer reviewerId);

    Map<String, Object> summaryForUser(Integer userId);
}
