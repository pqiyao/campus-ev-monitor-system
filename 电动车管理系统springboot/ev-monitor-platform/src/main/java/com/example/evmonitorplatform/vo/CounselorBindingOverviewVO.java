package com.example.evmonitorplatform.vo;

import com.example.evmonitorplatform.entity.CounselorClass;
import lombok.Data;

import java.util.List;

@Data
public class CounselorBindingOverviewVO {
    private Integer counselorUserId;
    private String counselorName;
    /** 班级名称列表（展示用） */
    private List<String> classes;
    private int studentCount;
    /** 明细行（编辑/删除用） */
    private List<CounselorClass> rows;
}
