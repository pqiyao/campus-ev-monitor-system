package com.example.evmonitorplatform.controller;

import com.example.evmonitorplatform.common.response.ApiResponse;
import com.example.evmonitorplatform.service.OrgClassService;
import com.example.evmonitorplatform.vo.OrgClassOptionVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/org-class")
@RequiredArgsConstructor
@Tag(name = "学籍班级", description = "标准班级字典与下拉选项")
public class OrgClassController {

    private final OrgClassService orgClassService;

    @GetMapping("/options")
    @Operation(summary = "班级下拉选项", description = "标准表 + 已有学生班级去重合并，供注册与导员绑定使用")
    public ApiResponse<List<OrgClassOptionVO>> options() {
        return ApiResponse.success(orgClassService.listMergedOptions());
    }
}
