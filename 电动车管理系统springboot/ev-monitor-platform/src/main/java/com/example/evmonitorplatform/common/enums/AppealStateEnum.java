package com.example.evmonitorplatform.common.enums;

import lombok.Getter;

/**
 * 申诉状态枚举
 */
@Getter
public enum AppealStateEnum {
    PENDING(0, "待审核", "pending"),
    PROCESSING(1, "审核中", "processing"),
    SUCCESS(2, "申诉成功", "success"),
    FAILED(3, "申诉失败", "failed"),
    CANCELED(4, "已撤销", "canceled"),
    EXPIRED(5, "已过期", "expired");

    private final Integer code;
    private final String description;
    private final String value;

    AppealStateEnum(Integer code, String description, String value) {
        this.code = code;
        this.description = description;
        this.value = value;
    }

    public static AppealStateEnum getByCode(Integer code) {
        for (AppealStateEnum state : values()) {
            if (state.getCode().equals(code)) {
                return state;
            }
        }
        return null;
    }

    public static AppealStateEnum getByValue(String value) {
        for (AppealStateEnum state : values()) {
            if (state.getValue().equals(value)) {
                return state;
            }
        }
        return null;
    }

    public static boolean isValid(Integer code) {
        return getByCode(code) != null;
    }

    public static String getDescriptionByCode(Integer code) {
        AppealStateEnum state = getByCode(code);
        return state != null ? state.getDescription() : "未知";
    }
}