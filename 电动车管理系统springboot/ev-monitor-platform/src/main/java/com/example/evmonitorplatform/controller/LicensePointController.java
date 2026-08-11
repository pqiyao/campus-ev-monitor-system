package com.example.evmonitorplatform.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.evmonitorplatform.common.response.ApiResponse;
import com.example.evmonitorplatform.common.response.PageResponse;
import com.example.evmonitorplatform.entity.LicensePointLog;
import com.example.evmonitorplatform.entity.WelfarePointRequest;
import com.example.evmonitorplatform.service.LicensePointService;
import com.example.evmonitorplatform.vo.request.LicensePointAdminBonusVO;
import com.example.evmonitorplatform.vo.request.LicensePointWelfareReviewVO;
import com.example.evmonitorplatform.vo.request.LicensePointWelfareSubmitVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/license-point")
@RequiredArgsConstructor
@Tag(name = "记分管理", description = "12分制、公益加分、流水查询")
public class LicensePointController {

    private final LicensePointService licensePointService;

    @GetMapping("/summary")
    @Operation(summary = "当前用户记分概要")
    public ApiResponse<Map<String, Object>> summary(@RequestParam Integer userId) {
        return ApiResponse.success(licensePointService.summaryForUser(userId));
    }

    @GetMapping("/log")
    @Operation(summary = "记分流水")
    public PageResponse<List<LicensePointLog>> log(
            @RequestParam(required = false) Integer userId,
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "20") int pageSize) {
        Page<LicensePointLog> p = licensePointService.pageLogs(userId, pageNum, pageSize);
        return new PageResponse<>(200, "success", p.getRecords(),
                (int) p.getCurrent(), (int) p.getSize(), p.getTotal());
    }

    @PostMapping("/welfare/submit")
    @Operation(summary = "学生提交公益加分申请")
    public ApiResponse<WelfarePointRequest> welfareSubmit(@RequestBody LicensePointWelfareSubmitVO body) {
        return ApiResponse.success(licensePointService.submitWelfareRequest(
                body.getUserId(), body.getContent(), body.getImageUrls()));
    }

    @GetMapping("/welfare/my")
    @Operation(summary = "我的公益申请")
    public ApiResponse<List<WelfarePointRequest>> welfareMy(@RequestParam Integer userId) {
        return ApiResponse.success(licensePointService.listMyWelfareRequests(userId));
    }

    @GetMapping("/welfare/pending")
    @Operation(summary = "待审核公益申请（管理端）")
    public PageResponse<List<WelfarePointRequest>> welfarePending(
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize) {
        Page<WelfarePointRequest> p = licensePointService.pagePendingWelfare(pageNum, pageSize);
        return new PageResponse<>(200, "success", p.getRecords(),
                (int) p.getCurrent(), (int) p.getSize(), p.getTotal());
    }

    @PostMapping("/welfare/review")
    @Operation(summary = "审核公益申请（管理端）")
    public ApiResponse<Boolean> welfareReview(@RequestBody LicensePointWelfareReviewVO body) {
        boolean pass = Boolean.TRUE.equals(body.getApprove());
        int pts = body.getPoints() != null ? body.getPoints() : 0;
        licensePointService.reviewWelfareRequest(
                body.getRequestId(), pass, pts,
                body.getAdminRemark(), body.getReviewerId());
        return ApiResponse.success(true);
    }

    @PostMapping("/admin/bonus")
    @Operation(summary = "管理员直接加分（含人性化补偿）")
    public ApiResponse<Boolean> adminBonus(@RequestBody LicensePointAdminBonusVO body) {
        licensePointService.addBonusByAdmin(
                body.getUserId(), body.getPoints(), body.getRemark(),
                body.getEvidenceUrls(), body.getOperatorUserId());
        return ApiResponse.success(true);
    }
}
