package com.example.evmonitorplatform.controller;

import com.example.evmonitorplatform.common.exception.BusinessException;
import com.example.evmonitorplatform.common.response.ApiResponse;
import com.example.evmonitorplatform.common.utils.FileUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
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
import java.util.*;

@Slf4j
@RestController
@RequestMapping("/api/file")
@RequiredArgsConstructor
@Tag(name = "文件管理", description = "文件上传与下载接口")
public class FileController {

    @Value("${ev-monitor.file.upload-path}")
    private String uploadPath;

    @Value("${ev-monitor.file.max-size}")
    private String maxSize;

    private String absoluteUploadPath; // 存储绝对路径

    private static final List<String> ALLOWED_IMAGE_TYPES = Arrays.asList(
            "image/jpeg", "image/png", "image/gif", "image/bmp", "image/webp"
    );

    private static final List<String> ALLOWED_DOC_TYPES = Arrays.asList(
            "application/pdf", "application/msword",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
    );

    @PostConstruct
    public void init() {
        // 转换为绝对路径并规范化
        Path path = Paths.get(uploadPath).toAbsolutePath().normalize();
        this.absoluteUploadPath = path.toString().replace('\\', '/') + "/";

        // 创建根上传目录
        File rootDir = new File(this.absoluteUploadPath);
        if (!rootDir.exists()) {
            boolean created = rootDir.mkdirs();
            if (created) {
                log.info("创建上传根目录成功: {}", this.absoluteUploadPath);
            } else {
                log.error("创建上传根目录失败: {}", this.absoluteUploadPath);
            }
        }
        log.info("文件上传绝对路径: {}", this.absoluteUploadPath);
    }

    @PostMapping("/upload")
    @Operation(summary = "上传文件", description = "上传单个文件")
    public ApiResponse<Map<String, String>> uploadFile(
            @RequestParam("file") MultipartFile file,
            @RequestParam(defaultValue = "general") String type) {

        log.info("上传文件: {}, 类型: {}", file.getOriginalFilename(), type);

        // 验证文件大小
        long maxSizeBytes = parseSize(maxSize);
        if (file.getSize() > maxSizeBytes) {
            throw new BusinessException("文件大小不能超过 " + maxSize);
        }

        // 验证文件类型
        String contentType = file.getContentType();
        if ("image".equals(type) && !ALLOWED_IMAGE_TYPES.contains(contentType)) {
            throw new BusinessException("只允许上传图片文件 (JPG, PNG, GIF, BMP, WEBP)");
        }
        if ("document".equals(type) && !ALLOWED_DOC_TYPES.contains(contentType)) {
            throw new BusinessException("只允许上传文档文件 (PDF, DOC, DOCX)");
        }

        // 生成文件名（兼容无扩展名、文件名为空，避免 substring 抛异常导致 500）
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || originalFilename.isBlank()) {
            originalFilename = "upload";
        }
        int dot = originalFilename.lastIndexOf('.');
        String fileExtension = (dot > 0 && dot < originalFilename.length() - 1)
                ? originalFilename.substring(dot)
                : "";
        String newFilename = UUID.randomUUID().toString().replace("-", "") + (fileExtension.isEmpty() ? ".bin" : fileExtension);

        // 按日期创建子目录：type/yyyy/MM/dd/
        String datePath = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy/MM/dd"));
        // 使用绝对路径拼接
        String fullDirPath = absoluteUploadPath + type + "/" + datePath;
        File dir = new File(fullDirPath);
        if (!dir.exists()) {
            dir.mkdirs(); // 创建多级目录
        }

        // 完整的文件保存路径
        String filePath = fullDirPath + "/" + newFilename;
        File dest = new File(filePath);

        try {
            file.transferTo(dest);
            log.info("文件保存成功: {}", filePath);

            // 构建可访问的 URL（使用正斜杠）
            String fileUrl = "/uploads/" + type + "/" + datePath + "/" + newFilename;

            Map<String, String> result = new HashMap<>();
            result.put("fileName", newFilename);
            result.put("originalName", originalFilename);
            result.put("fileUrl", fileUrl);
            result.put("fileSize", FileUtil.formatSize(file.getSize()));
            result.put("contentType", contentType);

            return ApiResponse.success(result);

        } catch (IOException e) {
            log.error("文件上传失败: ", e);
            throw new BusinessException("文件上传失败: " + e.getMessage());
        }
    }

    @PostMapping("/upload-multiple")
    @Operation(summary = "上传多个文件", description = "同时上传多个文件")
    public ApiResponse<List<Map<String, String>>> uploadMultipleFiles(
            @RequestParam("files") MultipartFile[] files,
            @RequestParam(defaultValue = "general") String type) {

        log.info("上传多个文件: {}个", files.length);

        List<Map<String, String>> results = new ArrayList<>();
        for (MultipartFile file : files) {
            try {
                Map<String, String> result = uploadFile(file, type).getData();
                results.add(result);
            } catch (Exception e) {
                log.error("文件上传失败: {}", file.getOriginalFilename(), e);
                Map<String, String> errorResult = new HashMap<>();
                errorResult.put("fileName", file.getOriginalFilename());
                errorResult.put("error", e.getMessage());
                results.add(errorResult);
            }
        }

        return ApiResponse.success(results);
    }

    @PostMapping("/upload-image")
    @Operation(summary = "上传图片", description = "上传图片文件")
    public ApiResponse<Map<String, String>> uploadImage(@RequestParam("file") MultipartFile file) {
        return uploadFile(file, "image");
    }

    @PostMapping("/upload-document")
    @Operation(summary = "上传文档", description = "上传文档文件")
    public ApiResponse<Map<String, String>> uploadDocument(@RequestParam("file") MultipartFile file) {
        return uploadFile(file, "document");
    }

    @GetMapping("/download/{type}/{date}/{filename}")
    @Operation(summary = "下载文件", description = "下载文件")
    public void downloadFile(
            @PathVariable String type,
            @PathVariable String date,
            @PathVariable String filename) {
        // 实际的文件下载逻辑需要在实现中处理
        // 这里仅作为接口定义
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除文件", description = "删除服务器上的文件")
    public ApiResponse<Boolean> deleteFile(@RequestParam String fileUrl) {
        try {
            // 从URL中提取文件路径（例如：/uploads/image/2026/02/21/xxx.jpg）
            String relativePath = fileUrl.replace("/uploads/", "");
            // 使用绝对路径拼接
            String filePath = absoluteUploadPath + relativePath.replace("/", File.separator);
            File file = new File(filePath);
            if (file.exists() && file.delete()) {
                log.info("文件删除成功: {}", filePath);
                return ApiResponse.success(true);
            } else {
                throw new BusinessException("文件不存在或删除失败");
            }
        } catch (Exception e) {
            log.error("文件删除失败: ", e);
            throw new BusinessException("文件删除失败: " + e.getMessage());
        }
    }

    @GetMapping("/list")
    @Operation(summary = "文件列表", description = "获取文件列表")
    public ApiResponse<List<Map<String, Object>>> listFiles(
            @RequestParam(defaultValue = "image") String type,
            @RequestParam(required = false) String date) {

        List<Map<String, Object>> files = new ArrayList<>();
        // 使用绝对路径拼接搜索路径
        String searchPath = absoluteUploadPath + type;
        if (date != null && !date.isEmpty()) {
            // 支持传入 "2026-02-21" 格式，将其转换为 "2026/02/21"
            searchPath += "/" + date.replace("-", "/");
        }

        File dir = new File(searchPath);
        if (dir.exists() && dir.isDirectory()) {
            collectFiles(dir, files, type, absoluteUploadPath);
        }

        return ApiResponse.success(files);
    }

    /**
     * 递归收集文件信息
     * @param dir 当前目录
     * @param files 结果列表
     * @param type 文件类型
     * @param basePath 基础绝对路径（用于生成相对路径）
     */
    private void collectFiles(File dir, List<Map<String, Object>> files, String type, String basePath) {
        File[] fileList = dir.listFiles();
        if (fileList != null) {
            for (File file : fileList) {
                if (file.isFile()) {
                    Map<String, Object> fileInfo = new HashMap<>();
                    fileInfo.put("name", file.getName());
                    // 生成相对路径（用于 URL）
                    String relativePath = file.getAbsolutePath().replace(basePath, "").replace('\\', '/');
                    fileInfo.put("path", relativePath);
                    fileInfo.put("size", FileUtil.formatSize(file.length()));
                    fileInfo.put("lastModified", new Date(file.lastModified()));
                    fileInfo.put("type", type);
                    files.add(fileInfo);
                } else if (file.isDirectory()) {
                    collectFiles(file, files, type, basePath);
                }
            }
        }
    }

    private long parseSize(String sizeStr) {
        sizeStr = sizeStr.toUpperCase();
        if (sizeStr.endsWith("MB")) {
            return Long.parseLong(sizeStr.replace("MB", "").trim()) * 1024 * 1024;
        } else if (sizeStr.endsWith("KB")) {
            return Long.parseLong(sizeStr.replace("KB", "").trim()) * 1024;
        } else if (sizeStr.endsWith("B")) {
            return Long.parseLong(sizeStr.replace("B", "").trim());
        } else {
            return 10 * 1024 * 1024; // 默认10MB
        }
    }
}