package com.example.evmonitorplatform.vo.request;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class VehicleQueryVO {
    /** 按车主用户筛选（学生端「我的车辆」） */
    private Integer userId;
    private String licensePlate;
    private String userType;
    private String status;
    private Boolean isBlacklist;
    private String startTime;
    private String endTime;
    private Integer pageNum;
    private Integer pageSize;
}