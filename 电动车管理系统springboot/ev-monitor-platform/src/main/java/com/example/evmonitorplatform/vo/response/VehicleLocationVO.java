// vo/response/VehicleLocationVO.java
package com.example.evmonitorplatform.vo.response;

import lombok.Data;

@Data
public class VehicleLocationVO {
    private Integer vehicleId;
    /** 终端 MAC 等，未绑定车辆时也有 */
    private String deviceId;
    /** 未在系统登记绑定的设备，仅显示点位 */
    private Boolean unbound;
    private String licensePlate;
    private String frameNum;
    private Double lat;
    private Double lng;
    private Double speed;       // km/h
    private Integer battery;    // 0-100%
    private String updateTime;
    private String status;      // online / stale
    private String ownerName;   // 车主姓名
}