package com.example.evmonitorplatform.common.enums;

import lombok.Getter;

@Getter
public enum ViolationTypeEnum {
    ILLEGAL_PARKING(1, "违规停放"),
    OVERSPEED(2, "超速行驶"),
    ILLEGAL_CHARGING(3, "违规充电"),
    UNREGISTERED(4, "未登记车辆"),
    NO_ENTRY_ZONE(5, "进入禁行区"),
    OVERLOAD(6, "违规载人"),
    LICENSE_PLATE_COVERED(7, "遮挡车牌"),
    OTHER(8, "其他违规");
    
    private final Integer code;
    private final String description;
    
    ViolationTypeEnum(Integer code, String description) {
        this.code = code;
        this.description = description;
    }
    
    public static ViolationTypeEnum getByCode(Integer code) {
        for (ViolationTypeEnum type : values()) {
            if (type.getCode().equals(code)) {
                return type;
            }
        }
        return null;
    }
    
    public static boolean isValid(Integer code) {
        return getByCode(code) != null;
    }
    
    public static String getDescriptionByCode(Integer code) {
        ViolationTypeEnum type = getByCode(code);
        return type != null ? type.getDescription() : "未知";
    }
}