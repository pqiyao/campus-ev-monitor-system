package com.example.evmonitorplatform.controller;

import com.example.evmonitorplatform.common.response.ApiResponse;
import com.example.evmonitorplatform.common.response.PageResponse;
import com.example.evmonitorplatform.entity.PassRecord;
import com.example.evmonitorplatform.service.PassRecordService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/pass-record")
@RequiredArgsConstructor
@Tag(name = "通行记录", description = "RFID 通行记录查询")
public class PassRecordController {

    private final PassRecordService passRecordService;

    @GetMapping("/list")
    @Operation(summary = "分页查询通行记录", description = "查询 RFID 通行记录")
    public PageResponse<List<PassRecord>> list(
            @Parameter(description = "卡号/车牌号关键词") @RequestParam(required = false) String keyword,
            @Parameter(description = "通行方向：in/out") @RequestParam(required = false) String direction,
            @Parameter(description = "通行结果：pass/deny") @RequestParam(required = false) String passResult,
            @Parameter(description = "车辆ID") @RequestParam(required = false) Integer vehicleId,
            @Parameter(description = "用户ID") @RequestParam(required = false) Integer userId,
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") Integer pageNum,
            @Parameter(description = "每页大小") @RequestParam(defaultValue = "10") Integer pageSize) {
        return passRecordService.queryPassRecords(keyword, direction, passResult, vehicleId, userId, pageNum, pageSize);
    }

    @GetMapping("/recent")
    @Operation(summary = "获取最近通行记录", description = "获取最近的 N 条通行记录")
    public ApiResponse<List<PassRecord>> recent(
            @Parameter(description = "记录数") @RequestParam(defaultValue = "10") Integer limit) {
        return ApiResponse.success(passRecordService.getRecentRecords(limit));
    }

    @GetMapping("/stats")
    @Operation(summary = "获取通行统计", description = "获取通行统计数据")
    public ApiResponse<Map<String, Object>> stats() {
        return ApiResponse.success(passRecordService.getPassStatistics());
    }

    @GetMapping("/{passId}")
    @Operation(summary = "获取通行记录详情")
    public ApiResponse<PassRecord> detail(@PathVariable Integer passId) {
        PassRecord record = passRecordService.getById(passId);
        return ApiResponse.success(record);
    }
}
