package com.example.evmonitorplatform.common.enums;

import lombok.Getter;

@Getter
public enum VehicleStatusEnum {
    PENDING("pending", "待审核"),
    APPROVED("approved", "已通过"),
    REJECTED("rejected", "已拒绝"),
    EXPIRED("expired", "已过期"),
    FROZEN("frozen", "已冻结"),
    DELETED("deleted", "已删除");
    
    private final String code;
    private final String description;
    
    VehicleStatusEnum(String code, String description) {
        this.code = code;
        this.description = description;
    }
    
    public static VehicleStatusEnum getByCode(String code) {
        for (VehicleStatusEnum status : values()) {
            if (status.getCode().equals(code)) {
                return status;
            }
        }
        return null;
    }
    
    public static boolean isValid(String code) {
        return getByCode(code) != null;
    }
}