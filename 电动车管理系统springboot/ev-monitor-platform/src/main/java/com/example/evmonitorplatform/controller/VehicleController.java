package com.example.evmonitorplatform.controller;

import com.example.evmonitorplatform.common.exception.BusinessException;
import com.example.evmonitorplatform.common.response.ApiResponse;
import com.example.evmonitorplatform.common.response.PageResponse;
import com.example.evmonitorplatform.entity.Vehicle;
import com.example.evmonitorplatform.service.VehicleService;
import com.example.evmonitorplatform.vo.request.VehicleQueryVO;
import com.example.evmonitorplatform.vo.request.VehicleRegisterVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import jakarta.annotation.PostConstruct;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/vehicle")
@RequiredArgsConstructor
@Tag(name = "车辆管理", description = "电动车注册与管理接口")
public class VehicleController {

    private final VehicleService vehicleService;

    @Value("${ev-monitor.file.upload-path}")
    private String uploadPath;

    /**
     * 初始化上传目录：转换为绝对路径，确保目录存在
     */
    @PostConstruct
    public void init() {
        // 1. 转换为绝对路径并规范化
        Path path = Paths.get(uploadPath).toAbsolutePath().normalize();
        this.uploadPath = path.toString() + File.separator; // 确保以分隔符结尾

        // 2. 创建根上传目录（如果不存在）
        File rootDir = new File(this.uploadPath);
        if (!rootDir.exists()) {
            boolean created = rootDir.mkdirs();
            if (created) {
                log.info("创建上传根目录成功: {}", this.uploadPath);
            } else {
                log.error("创建上传根目录失败: {}", this.uploadPath);
            }
        }
        log.info("文件上传绝对路径: {}", this.uploadPath);
    }

    @PostMapping("/register")
    @Operation(summary = "车辆注册", description = "用户提交车辆注册申请")
    public ApiResponse<Boolean> register(@RequestBody @Valid VehicleRegisterVO registerVO) {
        log.info("车辆注册: {}", registerVO.getLicensePlate());
        boolean result = vehicleService.registerVehicle(registerVO);
        return ApiResponse.success(result);
    }

    @GetMapping("/list")
    @Operation(summary = "查询车辆列表", description = "分页查询车辆信息")
    public PageResponse<List<Vehicle>> listVehicles(
            @Parameter(description = "车主用户ID") @RequestParam(required = false) Integer userId,
            @Parameter(description = "车牌号") @RequestParam(required = false) String licensePlate,
            @Parameter(description = "用户类型") @RequestParam(required = false) String userType,
            @Parameter(description = "状态") @RequestParam(required = false) String status,
            @Parameter(description = "是否黑名单") @RequestParam(required = false) Boolean isBlacklist,
            @Parameter(description = "开始时间") @RequestParam(required = false) String startTime,
            @Parameter(description = "结束时间") @RequestParam(required = false) String endTime,
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") Integer pageNum,
            @Parameter(description = "每页大小") @RequestParam(defaultValue = "10") Integer pageSize) {

        VehicleQueryVO queryVO = VehicleQueryVO.builder()
                .userId(userId)
                .licensePlate(licensePlate)
                .userType(userType)
                .status(status)
                .isBlacklist(isBlacklist)
                .startTime(startTime)
                .endTime(endTime)
                .pageNum(pageNum)
                .pageSize(pageSize)
                .build();

        return vehicleService.queryVehicles(queryVO);
    }

    @PostMapping("/approve/{vehicleId}")
    @Operation(summary = "审核通过", description = "审核员审核通过车辆")
    public ApiResponse<Boolean> approve(
            @PathVariable Integer vehicleId,
            @RequestParam Integer auditorId,
            @RequestParam(required = false) String remark) {
        log.info("审核通过车辆: {}, 审核员: {}", vehicleId, auditorId);
        boolean result = vehicleService.approveVehicle(vehicleId, auditorId, remark);
        return ApiResponse.success(result);
    }

    @PostMapping("/reject/{vehicleId}")
    @Operation(summary = "审核拒绝", description = "审核员拒绝车辆")
    public ApiResponse<Boolean> reject(
            @PathVariable Integer vehicleId,
            @RequestParam Integer auditorId,
            @RequestParam String reason) {
        log.info("审核拒绝车辆: {}, 审核员: {}", vehicleId, auditorId);
        boolean result = vehicleService.rejectVehicle(vehicleId, auditorId, reason);
        return ApiResponse.success(result);
    }

    @PostMapping("/batch-approve")
    @Operation(summary = "批量审核", description = "批量审核通过车辆")
    public ApiResponse<Boolean> batchApprove(
            @RequestBody List<Integer> vehicleIds,
            @RequestParam Integer auditorId) {
        log.info("批量审核车辆: {}, 审核员: {}", vehicleIds.size(), auditorId);
        boolean result = vehicleService.batchApprove(vehicleIds, auditorId);
        return ApiResponse.success(result);
    }

    @PostMapping("/freeze/{vehicleId}")
    @Operation(summary = "冻结/解冻车辆", description = "冻结或解冻车辆")
    public ApiResponse<Boolean> toggleFreeze(
            @PathVariable Integer vehicleId,
            @RequestParam Boolean freeze) {
        log.info("{}车辆: {}", freeze ? "冻结" : "解冻", vehicleId);
        boolean result = vehicleService.toggleFreeze(vehicleId, freeze);
        return ApiResponse.success(result);
    }

    @PostMapping("/blacklist/{vehicleId}")
    @Operation(summary = "加入/移出黑名单", description = "将车辆加入或移出黑名单")
    public ApiResponse<Boolean> toggleBlacklist(
            @PathVariable Integer vehicleId,
            @RequestParam Boolean blacklist) {
        log.info("{}车辆到黑名单: {}", blacklist ? "加入" : "移出", vehicleId);
        boolean result = vehicleService.toggleBlacklist(vehicleId, blacklist);
        return ApiResponse.success(result);
    }

    @GetMapping("/stats")
    @Operation(summary = "获取统计信息", description = "获取车辆统计数据")
    public ApiResponse<Map<String, Object>> getStats() {
        Map<String, Object> stats = vehicleService.getVehicleStats();
        return ApiResponse.success(stats);
    }

    @PostMapping("/upload-photo/{vehicleId}")
    @Operation(summary = "上传车辆图片", description = "上传车辆图片")
    public ApiResponse<String> uploadPhoto(
            @PathVariable Integer vehicleId,
            @RequestParam("file") MultipartFile file) {

        // 1. 校验车辆是否存在
        Vehicle vehicle = vehicleService.getById(vehicleId);
        if (vehicle == null) {
            throw new BusinessException("车辆不存在");
        }

        // 2. 校验文件类型和大小
        if (file.isEmpty()) {
            throw new BusinessException("文件不能为空");
        }
        String contentType = file.getContentType();
        if (!Arrays.asList("image/jpeg", "image/png", "image/gif", "image/bmp", "image/webp").contains(contentType)) {
            throw new BusinessException("只允许上传 JPG/PNG/GIF/BMP/WEBP 格式的图片");
        }
        long maxSize = 10 * 1024 * 1024; // 10MB
        if (file.getSize() > maxSize) {
            throw new BusinessException("图片大小不能超过10MB");
        }

        // 3. 生成存储路径和文件名
        String originalFilename = file.getOriginalFilename();
        String extension = originalFilename.substring(originalFilename.lastIndexOf("."));
        String newFilename = UUID.randomUUID().toString().replace("-", "") + extension;

        // 4. 按日期组织目录
        String datePath = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy/MM/dd"));
        String relativePath = "vehicle/" + datePath + "/" + newFilename;

        // 5. 构建绝对路径（自动处理分隔符）
        Path fullPath = Paths.get(this.uploadPath, relativePath);
        File destFile = fullPath.toFile();

        // 6. 自动创建父目录
        if (!destFile.getParentFile().exists()) {
            destFile.getParentFile().mkdirs();
        }

        // 7. 保存文件
        try {
            file.transferTo(destFile);
            log.info("车辆图片保存成功: {}", fullPath);
        } catch (IOException e) {
            log.error("文件保存失败", e);
            throw new BusinessException("文件保存失败: " + e.getMessage());
        }

        // 8. 生成可访问的 URL（使用正斜杠）
        String fileUrl = "/uploads/" + relativePath.replace("\\", "/");

        // 9. 更新车辆记录的 vehicle_photo 字段
        vehicleService.updateVehiclePhoto(vehicleId, fileUrl);

        // 10. 返回图片 URL
        return ApiResponse.success(fileUrl);
    }

    @GetMapping("/search")
    @Operation(summary = "搜索车辆", description = "根据条件搜索车辆")
    public ApiResponse<List<Vehicle>> searchVehicles(
            @RequestParam(required = false) String plate,
            @RequestParam(required = false) String brand,
            @RequestParam(required = false) String color) {
        List<Vehicle> vehicles = vehicleService.searchVehicles(plate, brand, color);
        return ApiResponse.success(vehicles);
    }

    @GetMapping("/{vehicleId}")
    @Operation(summary = "获取车辆详情", description = "根据ID获取车辆详细信息")
    public ApiResponse<Vehicle> getDetail(@PathVariable Integer vehicleId) {
        Vehicle vehicle = vehicleService.getById(vehicleId);
        return ApiResponse.success(vehicle);
    }

    @PutMapping("/{vehicleId}")
    @Operation(summary = "更新车辆信息", description = "更新车辆信息")
    public ApiResponse<Boolean> updateVehicle(
            @PathVariable Integer vehicleId,
            @RequestBody Vehicle vehicle) {
        vehicle.setVehicleId(vehicleId);
        boolean result = vehicleService.updateById(vehicle);
        return ApiResponse.success(result);
    }

    @DeleteMapping("/{vehicleId}")
    @Operation(summary = "删除车辆", description = "软删除车辆记录")
    public ApiResponse<Boolean> deleteVehicle(@PathVariable Integer vehicleId) {
        boolean result = vehicleService.removeById(vehicleId);
        return ApiResponse.success(result);
    }
}