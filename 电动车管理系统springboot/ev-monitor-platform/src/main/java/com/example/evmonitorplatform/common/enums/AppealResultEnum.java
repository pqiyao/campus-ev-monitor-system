package com.example.evmonitorplatform.common.enums;

import lombok.Getter;

/**
 * 申诉结果枚举
 */
@Getter
public enum AppealResultEnum {
    PASS(1, "通过", "申诉成立，取消违规记录"),
    REJECT(2, "拒绝", "申诉不成立，维持原判"),
    PARTIAL_PASS(3, "部分通过", "部分申诉成立，减轻处罚"),
    TRANSFER(4, "转办", "转交其他部门处理");

    private final Integer code;
    private final String description;
    private final String remark;

    AppealResultEnum(Integer code, String description, String remark) {
        this.code = code;
        this.description = description;
        this.remark = remark;
    }

    public static AppealResultEnum getByCode(Integer code) {
        for (AppealResultEnum result : values()) {
            if (result.getCode().equals(code)) {
                return result;
            }
        }
        return null;
    }

    public static boolean isValid(Integer code) {
        return getByCode(code) != null;
    }
}