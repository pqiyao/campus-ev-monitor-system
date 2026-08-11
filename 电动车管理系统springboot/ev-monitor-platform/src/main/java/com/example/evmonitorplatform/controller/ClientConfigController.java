package com.example.evmonitorplatform.controller;

import com.example.evmonitorplatform.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 学生端 / 大屏等前端公用配置（无敏感信息；百度 AK 为浏览器端公开密钥，需在百度控制台配域名/包名白名单）。
 */
@RestController
@RequestMapping("/api/client")
@Tag(name = "客户端配置", description = "地图 AK 等与后端统一的公开参数")
public class ClientConfigController {

    @Value("${ev-monitor.baidu-map.ak:}")
    private String baiduMapAk;

    @GetMapping("/config")
    @Operation(summary = "客户端配置", description = "返回百度地图 AK 等，与 application.yml 中 ev-monitor.baidu-map.ak 一致")
    public ApiResponse<Map<String, Object>> clientConfig() {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("baiduMapAk", baiduMapAk != null ? baiduMapAk.trim() : "");
        return ApiResponse.success(data);
    }
}
