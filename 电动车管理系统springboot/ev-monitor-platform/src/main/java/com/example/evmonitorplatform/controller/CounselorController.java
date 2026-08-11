package com.example.evmonitorplatform.controller;

import com.example.evmonitorplatform.common.response.ApiResponse;
import com.example.evmonitorplatform.dto.CounselorClassSaveDTO;
import com.example.evmonitorplatform.service.CounselorService;
import com.example.evmonitorplatform.vo.CounselorBindingOverviewVO;
import com.example.evmonitorplatform.vo.CounselorWorkspaceVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/counselor")
@RequiredArgsConstructor
@Tag(name = "导员组织", description = "导员工作台管辖范围与班级绑定（简化版）")
public class CounselorController {

    private final CounselorService counselorService;

    @GetMapping("/workspace")
    @Operation(summary = "导员工作台数据", description = "按学院/专业分组及扁平班级列表")
    public ApiResponse<CounselorWorkspaceVO> workspace(@RequestParam Integer counselorUserId) {
        return ApiResponse.success(counselorService.buildWorkspace(counselorUserId));
    }

    @GetMapping("/bindings")
    @Operation(summary = "全部导员班级绑定概览", description = "用户管理页展示用")
    public ApiResponse<List<CounselorBindingOverviewVO>> bindings() {
        return ApiResponse.success(counselorService.listBindingOverview());
    }

    @PostMapping("/class")
    @Operation(summary = "新增导员-班级绑定")
    public ApiResponse<Boolean> addClass(@RequestBody @Valid CounselorClassSaveDTO dto) {
        return ApiResponse.success(counselorService.addCounselorClass(dto));
    }

    @DeleteMapping("/class/{id}")
    @Operation(summary = "删除导员-班级绑定")
    public ApiResponse<Boolean> removeClass(@PathVariable Integer id) {
        return ApiResponse.success(counselorService.removeCounselorClass(id));
    }
}
