package com.example.evmonitorplatform.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class UserDTO {
    private Integer userId;
    private String username;
    private String realName;
    private String userType;
    private String typeName;
    private String studentId;
    private String jobId;
    private String phone;
    private String email;
    private String avatar;
    private String department;
    private String college;
    private String major;
    private String className;
    private String status;
    private LocalDateTime createTime;
    private Integer vehicleCount;
    private Integer violationCount;
    private Integer notificationCount;
}