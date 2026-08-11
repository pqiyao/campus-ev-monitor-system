package com.example.evmonitorplatform.common.enums;

import lombok.Getter;

@Getter
public enum NotificationTypeEnum {
    SYSTEM("system", "系统通知"),
    VIOLATION("violation", "违规通知"),
    SECURITY("security", "安全提醒"),
    ACTIVITY("activity", "活动通知"),
    PERSONAL("personal", "个人通知"),
    BATCH("batch", "批量通知");
    
    private final String code;
    private final String description;
    
    NotificationTypeEnum(String code, String description) {
        this.code = code;
        this.description = description;
    }
    
    public static NotificationTypeEnum getByCode(String code) {
        for (NotificationTypeEnum type : values()) {
            if (type.getCode().equals(code)) {
                return type;
            }
        }
        return null;
    }
    
    public static boolean isValid(String code) {
        return getByCode(code) != null;
    }
}