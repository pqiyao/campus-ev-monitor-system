package com.example.evmonitorplatform.controller;

import com.example.evmonitorplatform.common.response.ApiResponse;
import com.example.evmonitorplatform.common.response.PageResponse;
import com.example.evmonitorplatform.entity.Violation;
import com.example.evmonitorplatform.service.ViolationService;
import com.example.evmonitorplatform.vo.request.ViolationQueryVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/violation")
@RequiredArgsConstructor
@Tag(name = "违规管理", description = "违规处理与申诉接口")
public class ViolationController {

    private final ViolationService violationService;

    @GetMapping("/list")
    @Operation(summary = "查询违规记录", description = "分页查询违规记录")
    public PageResponse<List<Violation>> listViolations(
            @Parameter(description = "违规类型")  @RequestParam(required = false) Integer illegalType,
            @Parameter(description = "违规状态")  @RequestParam(required = false) Integer illegalState,
            @Parameter(description = "车牌号")    @RequestParam(required = false) String vehiclePlate,
            @Parameter(description = "车主姓名")  @RequestParam(required = false) String ownerName,
            @Parameter(description = "开始时间")  @RequestParam(required = false) String startTime,
            @Parameter(description = "结束时间")  @RequestParam(required = false) String endTime,
            @Parameter(description = "导员ID")    @RequestParam(required = false) Integer counselorId,
            @Parameter(description = "班级名称")  @RequestParam(required = false) String className,
            @Parameter(description = "页码")      @RequestParam(defaultValue = "1")  Integer pageNum,
            @Parameter(description = "每页大小")  @RequestParam(defaultValue = "10") Integer pageSize) {

        ViolationQueryVO queryVO = ViolationQueryVO.builder()
                .illegalType(illegalType).illegalState(illegalState)
                .vehiclePlate(vehiclePlate).ownerName(ownerName).startTime(startTime)
                .endTime(endTime).counselorId(counselorId).className(className)
                .pageNum(pageNum).pageSize(pageSize)
                .build();
        return violationService.queryViolations(queryVO);
    }

    /** 新增：前端不再硬编码类型，从数据库读取 */
    @GetMapping("/types")
    @Operation(summary = "获取违规类型列表", description = "返回 violation_type 表中配置的所有违规类型")
    public ApiResponse<List<Map<String, Object>>> getViolationTypes() {
        return ApiResponse.success(violationService.getViolationTypes());
    }

    @PostMapping("/report")
    @Operation(summary = "人工上报违规")
    public ApiResponse<Boolean> reportViolation(@RequestBody Violation violation) {
        log.info("人工上报违规: {}", violation);
        return ApiResponse.success(violationService.reportViolation(violation));
    }

    @GetMapping("/report/resolve-binding")
    @Operation(summary = "人工上报-解析车辆", description = "学号、车牌、车架号至少填一项；若填多项，优先车架号，其次车牌，再次学号（学号取该用户最近一条车辆）")
    public ApiResponse<Map<String, Object>> resolveReportBinding(
            @RequestParam(required = false) String studentId,
            @RequestParam(required = false) String licensePlate,
            @RequestParam(required = false) String frameNum) {
        return ApiResponse.success(violationService.resolveVehicleForReport(studentId, licensePlate, frameNum));
    }

    @PostMapping("/{violationId}/rectify")
    @Operation(summary = "开始整改")
    public ApiResponse<Boolean> startRectify(
            @PathVariable Integer violationId,
            @RequestParam Integer userId,
            @RequestParam(required = false) String reason) {
        return ApiResponse.success(violationService.startRectify(violationId, userId, reason));
    }

    @PostMapping("/rectify/{rectifyId}/submit")
    @Operation(summary = "提交整改材料")
    public ApiResponse<Boolean> submitRectify(
            @PathVariable Integer rectifyId,
            @RequestParam String photos,
            @RequestParam String description) {
        return ApiResponse.success(violationService.submitRectify(rectifyId, photos, description));
    }

    @PostMapping("/rectify/{rectifyId}/verify")
    @Operation(summary = "核实整改结果")
    public ApiResponse<Boolean> verifyRectify(
            @PathVariable Integer rectifyId,
            @RequestParam Integer verifyUserId,
            @RequestParam Boolean pass,
            @RequestParam(required = false) String remark,
            @RequestParam(required = false) String evidence) {
        return ApiResponse.success(
                violationService.verifyRectify(rectifyId, verifyUserId, pass, remark, evidence));
    }

    @GetMapping("/stats")
    @Operation(summary = "获取违规统计数据")
    public ApiResponse<Map<String, Object>> getStats() {
        return ApiResponse.success(violationService.getViolationStats());
    }

    @GetMapping("/{violationId}")
    @Operation(summary = "获取违规详情")
    public ApiResponse<Violation> getDetail(@PathVariable Integer violationId) {
        return ApiResponse.success(violationService.getViolationDetail(violationId));
    }

    @DeleteMapping("/{illegalId}")
    @Operation(summary = "删除违规记录（软删）", description = "人性化处理：恢复该条违规当时扣除的记分（不超过12分）")
    public ApiResponse<Boolean> adminDelete(
            @PathVariable Integer illegalId,
            @RequestParam(required = false) Integer operatorUserId,
            @RequestParam(required = false) String reason) {
        return ApiResponse.success(violationService.adminDeleteViolation(illegalId, operatorUserId, reason));
    }
}