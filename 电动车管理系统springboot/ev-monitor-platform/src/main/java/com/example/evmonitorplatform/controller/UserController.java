package com.example.evmonitorplatform.controller;

import  com.example.evmonitorplatform.common.response.ApiResponse;
import  com.example.evmonitorplatform.common.response.PageResponse;
import  com.example.evmonitorplatform.entity.User;
import  com.example.evmonitorplatform.service.UserService;
import  com.example.evmonitorplatform.vo.request.LoginVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
@Tag(name = "用户管理", description = "用户登录、注册与个人信息管理接口")
public class UserController {

    private final UserService userService;

    @PostMapping("/login")
    @Operation(summary = "用户登录", description = "用户登录获取token，无需密码验证")
    public ApiResponse<User> login(@RequestBody @Valid LoginVO loginVO) {
        log.info("用户登录: {}", loginVO.getUsername());
        User user = userService.login(loginVO);
        return ApiResponse.success(user);
    }

    @PostMapping("/register")
    @Operation(summary = "用户注册", description = "用户注册新账号，返回含 userId 的用户信息")
    public ApiResponse<User> register(@RequestBody @Valid User user) {
        log.info("用户注册: {}", user.getUsername());
        User registered = userService.register(user);
        return ApiResponse.success(registered);
    }

    @PostMapping("/update-password")
    @Operation(summary = "修改密码", description = "用户修改密码")
    public ApiResponse<Boolean> updatePassword(
            @RequestParam Integer userId,
            @RequestParam String oldPassword,
            @RequestParam String newPassword) {
        log.info("用户修改密码: {}", userId);
        boolean result = userService.updatePassword(userId, oldPassword, newPassword);
        return ApiResponse.success(result);
    }

    @PostMapping("/reset-password")
    @Operation(summary = "重置密码", description = "管理员重置用户密码")
    public ApiResponse<Boolean> resetPassword(
            @RequestParam Integer userId,
            @RequestParam String newPassword) {
        log.info("重置用户密码: {}", userId);
        boolean result = userService.resetPassword(userId, newPassword);
        return ApiResponse.success(result);
    }

    @PutMapping("/{userId}")
    @Operation(summary = "更新个人信息", description = "更新用户个人信息")
    public ApiResponse<Boolean> updateProfile(
            @PathVariable Integer userId,
            @RequestBody User user) {
        log.info("更新用户信息: {}", userId);
        user.setUserId(userId);
        boolean result = userService.updateProfile(user);
        return ApiResponse.success(result);
    }

    @PostMapping("/{userId}/status")
    @Operation(summary = "更新用户状态", description = "更新用户状态（激活/冻结）")
    public ApiResponse<Boolean> updateStatus(
            @PathVariable Integer userId,
            @RequestParam String status) {
        log.info("更新用户状态: {} -> {}", userId, status);
        boolean result = userService.updateStatus(userId, status);
        return ApiResponse.success(result);
    }

    @GetMapping("/list")
    @Operation(summary = "查询用户列表", description = "分页查询用户信息")
    public PageResponse<List<User>> listUsers(
            @Parameter(description = "搜索关键词") @RequestParam(required = false) String keyword,
            @Parameter(description = "用户类型") @RequestParam(required = false) String userType,
            @Parameter(description = "状态") @RequestParam(required = false) String status,
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") Integer pageNum,
            @Parameter(description = "每页大小") @RequestParam(defaultValue = "10") Integer pageSize) {

        return userService.queryUsers(keyword, userType, status, pageNum, pageSize);
    }

    @GetMapping("/stats")
    @Operation(summary = "获取用户统计", description = "获取用户统计数据")
    public ApiResponse<Map<String, Object>> getStats() {
        Map<String, Object> stats = userService.getUserStats();
        return ApiResponse.success(stats);
    }

    @GetMapping("/dashboard/{userId}")
    @Operation(summary = "获取用户仪表盘", description = "获取用户个人仪表盘数据")
    public ApiResponse<Map<String, Object>> getUserDashboard(@PathVariable Integer userId) {
        Map<String, Object> dashboard = userService.getUserDashboard(userId);
        return ApiResponse.success(dashboard);
    }

    @PostMapping("/{userId}/role")
    @Operation(summary = "分配角色", description = "为用户分配角色")
    public ApiResponse<Boolean> assignRole(
            @PathVariable Integer userId,
            @RequestParam String role) {
        log.info("为用户分配角色: {} -> {}", userId, role);
        boolean result = userService.assignRole(userId, role);
        return ApiResponse.success(result);
    }

    @GetMapping("/{userId}")
    @Operation(summary = "获取用户详情", description = "根据ID获取用户详细信息")
    public ApiResponse<User> getDetail(@PathVariable Integer userId) {
        User user = userService.getById(userId);
        return ApiResponse.success(user);
    }

    @DeleteMapping("/{userId}")
    @Operation(summary = "删除用户", description = "软删除用户")
    public ApiResponse<Boolean> deleteUser(@PathVariable Integer userId) {
        boolean result = userService.removeById(userId);
        return ApiResponse.success(result);
    }

    @PostMapping("/upload-avatar/{userId}")
    @Operation(summary = "上传头像", description = "上传用户头像")
    public ApiResponse<String> uploadAvatar(
            @PathVariable Integer userId,
            @RequestParam("file") MultipartFile file) {
        // 这里需要实现文件上传逻辑
        // String avatarUrl = fileService.uploadAvatar(file);
        // userService.updateAvatar(userId, avatarUrl);
        return ApiResponse.success("avatar_upload_success");
    }
}