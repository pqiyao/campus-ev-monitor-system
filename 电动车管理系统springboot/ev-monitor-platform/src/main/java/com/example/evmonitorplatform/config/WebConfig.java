package com.example.evmonitorplatform.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import jakarta.annotation.PostConstruct;
import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Value("${ev-monitor.file.upload-path}")
    private String uploadPath;

    private String absoluteUploadPath;

    @PostConstruct
    public void init() {
        // 转换为绝对路径并规范化
        Path path = Paths.get(uploadPath).toAbsolutePath().normalize();
        this.absoluteUploadPath = path.toString().replace('\\', '/') + "/";

        // 创建根上传目录（如果不存在）
        File rootDir = new File(this.absoluteUploadPath);
        if (!rootDir.exists()) {
            boolean created = rootDir.mkdirs();
            if (created) {
                System.out.println("创建上传根目录成功: " + this.absoluteUploadPath);
            } else {
                System.err.println("创建上传根目录失败: " + this.absoluteUploadPath);
            }
        }
        System.out.println("文件上传绝对路径: " + this.absoluteUploadPath);
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOrigins("*")
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(false)
                .maxAge(3600);
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // 映射 /uploads/** 到上传目录
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations("file:" + absoluteUploadPath);
    }
}