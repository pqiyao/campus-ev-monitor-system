package com.example.evmonitorplatform.vo.request;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 红外热成像帧接入请求（机载/机器人/枪机红外相机统一接入）。
 * tempMatrix 为二维温度矩阵（单位摄氏度），matrix.get(row).get(col)。
 */
@Data
public class ThermalFrameRequest {
    @NotBlank(message = "设备ID不能为空")
    private String deviceId;

    /** 证据来源类型：必须落在 AI 事件来源白名单内（CAMERA/DRONE/GROUND_ROBOT等） */
    private String sourceType;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime capturedAt;

    private Double longitude;
    private Double latitude;
    private String locationName;

    @NotNull(message = "温度矩阵不能为空")
    @Size(min = 8, max = 256, message = "温度矩阵每维需在8~256之间")
    private List<List<Double>> tempMatrix;

    /** 环境温度（摄氏度），用于自适应热点分割阈值 */
    private Double ambientTemp;

    /** 场景类型：CHARGING_STATION/PARKING_LOT/ROADSIDE/OTHER */
    private String sceneType;

    /** 设备侧帧序号，与 deviceId/capturedAt 一起构成幂等键 */
    private Integer frameSeq;

    /** 每像素对应地面尺寸（米），用于热点像素坐标换算经纬度，默认0.05 */
    private Double pixelMeters;

    private String idempotencyKey;

    /** 是否自动触发AI研判（默认false，仅落库与风险评分） */
    private Boolean autoAnalyze;
}
