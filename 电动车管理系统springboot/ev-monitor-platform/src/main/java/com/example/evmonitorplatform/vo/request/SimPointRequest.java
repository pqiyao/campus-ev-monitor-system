package com.example.evmonitorplatform.vo.request;

import lombok.Data;

/**
 * 地图屏幕用虚拟车辆点（不依赖数据库车辆，仅供大屏展示）
 */
@Data
public class SimPointRequest {
    /** Redis 键后缀，同一批次内需唯一，如 1、plate1 */
    private String id;
    private String licensePlate;
    private String ownerName;
    private Double lat;
    private Double lng;
    private Double speed;
    private Integer battery;
}
