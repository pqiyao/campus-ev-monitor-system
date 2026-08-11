package com.example.evmonitorplatform.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.example.evmonitorplatform.dto.CounselorClassSaveDTO;
import com.example.evmonitorplatform.entity.CounselorClass;
import com.example.evmonitorplatform.entity.User;
import com.example.evmonitorplatform.mapper.CounselorClassMapper;
import com.example.evmonitorplatform.mapper.UserMapper;
import com.example.evmonitorplatform.service.CounselorService;
import com.example.evmonitorplatform.vo.CounselorBindingOverviewVO;
import com.example.evmonitorplatform.vo.CounselorWorkspaceVO;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CounselorServiceImpl implements CounselorService {

    private final CounselorClassMapper counselorClassMapper;
    private final UserMapper userMapper;
    private final JdbcTemplate jdbcTemplate;

    private static String nz(String s) {
        return s == null ? "" : s;
    }

    private int countStudents(String className) {
        if (className == null || className.isBlank()) {
            return 0;
        }
        Integer n = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM `user` WHERE deleted = 0 AND user_type = 'student' AND class_name = ?",
                Integer.class,
                className);
        return n == null ? 0 : n;
    }

    private int countViolations(String className, Integer illegalStateOrNull) {
        if (className == null || className.isBlank()) {
            return 0;
        }
        String sql = "SELECT COUNT(*) FROM vehicle_illegal_record v "
                + "INNER JOIN `user` u ON v.user_id = u.user_id "
                + "WHERE v.deleted = 0 AND u.deleted = 0 AND u.class_name = ?";
        if (illegalStateOrNull != null) {
            Integer n = jdbcTemplate.queryForObject(
                    sql + " AND v.illegal_state = ?", Integer.class, className, illegalStateOrNull);
            return n == null ? 0 : n;
        }
        Integer n = jdbcTemplate.queryForObject(sql, Integer.class, className);
        return n == null ? 0 : n;
    }

    @Override
    public CounselorWorkspaceVO buildWorkspace(Integer counselorUserId) {
        User counselor = userMapper.selectById(counselorUserId);
        String name = counselor != null && counselor.getRealName() != null ? counselor.getRealName() : "导员";

        List<CounselorClass> rows = counselorClassMapper.selectList(
                Wrappers.<CounselorClass>lambdaQuery()
                        .eq(CounselorClass::getCounselorUserId, counselorUserId)
                        .orderByAsc(CounselorClass::getCollege, CounselorClass::getMajor, CounselorClass::getClassName));

        List<CounselorWorkspaceVO.ClassCardVO> classList = new ArrayList<>();
        int totalStudents = 0;
        for (CounselorClass row : rows) {
            String cn = row.getClassName();
            int sc = countStudents(cn);
            int pend = countViolations(cn, 0);
            int vio = countViolations(cn, null);
            totalStudents += sc;

            CounselorWorkspaceVO.ClassCardVO card = new CounselorWorkspaceVO.ClassCardVO();
            card.setName(cn);
            card.setCollege(nz(row.getCollege()));
            card.setMajor(nz(row.getMajor()));
            card.setStudentCount(sc);
            card.setPendingCount(pend);
            card.setViolationCount(vio);
            classList.add(card);
        }

        Map<String, Map<String, List<CounselorWorkspaceVO.ClassCardVO>>> byCollege = new LinkedHashMap<>();
        for (CounselorWorkspaceVO.ClassCardVO c : classList) {
            String col = c.getCollege().isEmpty() ? "未填学院" : c.getCollege();
            String maj = c.getMajor().isEmpty() ? "未填专业" : c.getMajor();
            byCollege.computeIfAbsent(col, k -> new LinkedHashMap<>())
                    .computeIfAbsent(maj, k -> new ArrayList<>())
                    .add(c);
        }

        List<CounselorWorkspaceVO.CollegeGroupVO> groups = new ArrayList<>();
        for (Map.Entry<String, Map<String, List<CounselorWorkspaceVO.ClassCardVO>>> ce : byCollege.entrySet()) {
            CounselorWorkspaceVO.CollegeGroupVO cg = new CounselorWorkspaceVO.CollegeGroupVO();
            cg.setCollege(ce.getKey());
            List<CounselorWorkspaceVO.MajorGroupVO> majors = new ArrayList<>();
            for (Map.Entry<String, List<CounselorWorkspaceVO.ClassCardVO>> me : ce.getValue().entrySet()) {
                CounselorWorkspaceVO.MajorGroupVO mg = new CounselorWorkspaceVO.MajorGroupVO();
                mg.setMajor(me.getKey());
                mg.setClasses(me.getValue());
                majors.add(mg);
            }
            cg.setMajors(majors);
            groups.add(cg);
        }

        CounselorWorkspaceVO vo = new CounselorWorkspaceVO();
        vo.setUserId(counselorUserId);
        vo.setName(name);
        vo.setStudentCount(totalStudents);
        vo.setClassList(classList);
        vo.setGroups(groups);
        return vo;
    }

    @Override
    public List<CounselorBindingOverviewVO> listBindingOverview() {
        List<CounselorClass> all = counselorClassMapper.selectList(
                Wrappers.<CounselorClass>lambdaQuery()
                        .orderByAsc(CounselorClass::getCounselorUserId, CounselorClass::getCollege, CounselorClass::getMajor));

        Map<Integer, List<CounselorClass>> byCounselor = all.stream()
                .collect(Collectors.groupingBy(CounselorClass::getCounselorUserId, LinkedHashMap::new, Collectors.toList()));

        List<CounselorBindingOverviewVO> out = new ArrayList<>();
        for (Map.Entry<Integer, List<CounselorClass>> e : byCounselor.entrySet()) {
            User u = userMapper.selectById(e.getKey());
            CounselorBindingOverviewVO vo = new CounselorBindingOverviewVO();
            vo.setCounselorUserId(e.getKey());
            vo.setCounselorName(u != null && u.getRealName() != null ? u.getRealName() : ("用户#" + e.getKey()));
            vo.setRows(e.getValue());
            vo.setClasses(e.getValue().stream().map(CounselorClass::getClassName).collect(Collectors.toList()));
            int sum = 0;
            for (CounselorClass cc : e.getValue()) {
                sum += countStudents(cc.getClassName());
            }
            vo.setStudentCount(sum);
            out.add(vo);
        }
        return out;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean addCounselorClass(CounselorClassSaveDTO dto) {
        CounselorClass row = new CounselorClass();
        row.setCounselorUserId(dto.getCounselorUserId());
        row.setCollege(dto.getCollege());
        row.setMajor(dto.getMajor());
        row.setClassName(dto.getClassName().trim());
        return counselorClassMapper.insert(row) > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean removeCounselorClass(Integer id) {
        return counselorClassMapper.deleteById(id) > 0;
    }
}
