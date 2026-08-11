package com.example.evmonitorplatform.controller;

import com.example.evmonitorplatform.common.response.PageResponse;
import com.example.evmonitorplatform.entity.RectifyRecord;
import com.example.evmonitorplatform.service.ViolationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/rectify")
@RequiredArgsConstructor
@Tag(name = "整改记录", description = "违规整改核实列表")
public class RectifyController {

    private final ViolationService violationService;

    @GetMapping("/list")
    @Operation(summary = "分页查询整改记录", description = "用于管理员核实整改材料")
    public PageResponse<List<RectifyRecord>> list(
            @Parameter(description = "核实状态：0待核实 2已通过 3已驳回") @RequestParam(required = false) Integer verifyState,
            @Parameter(description = "车牌模糊查询") @RequestParam(required = false) String licensePlate,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        return violationService.queryRectifyRecords(verifyState, licensePlate, pageNum, pageSize);
    }
}
