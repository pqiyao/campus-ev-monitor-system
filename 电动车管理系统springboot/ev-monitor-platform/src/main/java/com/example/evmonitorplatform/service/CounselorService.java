package com.example.evmonitorplatform.service;

import com.example.evmonitorplatform.dto.CounselorClassSaveDTO;
import com.example.evmonitorplatform.vo.CounselorBindingOverviewVO;
import com.example.evmonitorplatform.vo.CounselorWorkspaceVO;

import java.util.List;

public interface CounselorService {

    CounselorWorkspaceVO buildWorkspace(Integer counselorUserId);

    List<CounselorBindingOverviewVO> listBindingOverview();

    boolean addCounselorClass(CounselorClassSaveDTO dto);

    boolean removeCounselorClass(Integer id);
}
