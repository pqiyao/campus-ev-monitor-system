package com.example.evmonitorplatform.service;

import com.example.evmonitorplatform.vo.OrgClassOptionVO;

import java.util.List;

public interface OrgClassService {

    /**
     * 标准表 org_class 全部记录 + 学生表中已出现但未入标准表的去重班级，合并为下拉列表。
     */
    List<OrgClassOptionVO> listMergedOptions();
}
