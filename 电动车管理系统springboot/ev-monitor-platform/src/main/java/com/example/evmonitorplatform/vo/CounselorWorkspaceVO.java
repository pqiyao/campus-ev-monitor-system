package com.example.evmonitorplatform.vo;

import lombok.Data;

import java.util.List;

@Data
public class CounselorWorkspaceVO {
    private Integer userId;
    private String name;
    private Integer studentCount;
    /** 学院 → 专业 → 班级 */
    private List<CollegeGroupVO> groups;
    /** 扁平班级卡片（与前端原 classList 结构兼容） */
    private List<ClassCardVO> classList;

    @Data
    public static class ClassCardVO {
        private String name;
        private String college;
        private String major;
        private int studentCount;
        private int pendingCount;
        private int violationCount;
    }

    @Data
    public static class CollegeGroupVO {
        private String college;
        private List<MajorGroupVO> majors;
    }

    @Data
    public static class MajorGroupVO {
        private String major;
        private List<ClassCardVO> classes;
    }
}
