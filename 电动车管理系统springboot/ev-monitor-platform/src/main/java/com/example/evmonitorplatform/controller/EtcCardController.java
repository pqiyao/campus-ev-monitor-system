package com.example.evmonitorplatform.controller;

import com.example.evmonitorplatform.common.response.ApiResponse;
import com.example.evmonitorplatform.common.response.PageResponse;
import com.example.evmonitorplatform.entity.ETCard;
import com.example.evmonitorplatform.service.EtcCardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/etc-card")
@RequiredArgsConstructor
@Tag(name = "RFID/ETC卡", description = "校园卡绑定与发卡")
public class EtcCardController {

    private final EtcCardService etcCardService;

    @PostMapping("/bind")
    @Operation(summary = "将卡号绑定到车辆")
    public ApiResponse<Void> bind(@RequestBody BindBody body) {
        etcCardService.bindEtcToVehicle(body.getEtcNumber(), body.getVehicleId());
        return ApiResponse.success();
    }

    @PostMapping("/issue")
    @Operation(summary = "发卡登记（绑定车辆并可选有效期）")
    public ApiResponse<Void> issue(@RequestBody IssueBody body) {
        etcCardService.issueToVehicle(body.getVehicleId(), body.getEtcNumber(), body.getExpireDate());
        return ApiResponse.success();
    }

    @GetMapping("/list")
    @Operation(summary = "分页查询 RFID/ETC 卡")
    public PageResponse<List<ETCard>> list(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String userType,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "12") Integer pageSize) {
        return etcCardService.listCards(keyword, status, userType, pageNum, pageSize);
    }

    @GetMapping("/stats")
    @Operation(summary = "卡状态统计")
    public ApiResponse<Map<String, Object>> stats() {
        return ApiResponse.success(etcCardService.cardStatusStats());
    }

    @PostMapping("/{etcId}/status")
    @Operation(summary = "更新卡状态", description = "normal / lost / disabled")
    public ApiResponse<Void> setStatus(@PathVariable Integer etcId, @RequestParam String status) {
        etcCardService.updateCardStatus(etcId, status);
        return ApiResponse.success();
    }

    @Data
    public static class BindBody {
        private String etcNumber;
        private Integer vehicleId;
    }

    @Data
    public static class IssueBody {
        private Integer vehicleId;
        private String etcNumber;
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        private LocalDate expireDate;
    }
}
