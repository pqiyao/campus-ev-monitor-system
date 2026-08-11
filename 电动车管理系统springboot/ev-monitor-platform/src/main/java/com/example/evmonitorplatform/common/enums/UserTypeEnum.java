package com.example.evmonitorplatform.common.enums;

import lombok.Getter;

@Getter
public enum UserTypeEnum {
    STUDENT("student", "学生"),
    TEACHER("teacher", "教师"),
    SERVICE("service", "后勤人员"),
    TEMPORARY("temporary", "临时人员"),
    ADMIN("admin", "管理员");
    
    private final String code;
    private final String description;
    
    UserTypeEnum(String code, String description) {
        this.code = code;
        this.description = description;
    }
    
    public static UserTypeEnum getByCode(String code) {
        for (UserTypeEnum type : values()) {
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