package com.example.evmonitorplatform.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.example.evmonitorplatform.entity.OrgClass;
import com.example.evmonitorplatform.mapper.OrgClassMapper;
import com.example.evmonitorplatform.service.OrgClassService;
import com.example.evmonitorplatform.vo.OrgClassOptionVO;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class OrgClassServiceImpl implements OrgClassService {

    private final OrgClassMapper orgClassMapper;
    private final JdbcTemplate jdbcTemplate;

    private static String nz(String s) {
        return s == null ? "" : s.trim();
    }

    private static String key(String college, String major, String className) {
        return nz(college) + "\u0001" + nz(major) + "\u0001" + nz(className);
    }

    @Override
    public List<OrgClassOptionVO> listMergedOptions() {
        Map<String, OrgClassOptionVO> ordered = new LinkedHashMap<>();

        List<OrgClass> catalog = orgClassMapper.selectList(
                Wrappers.<OrgClass>lambdaQuery()
                        .orderByAsc(OrgClass::getSortOrder, OrgClass::getId));
        for (OrgClass row : catalog) {
            if (nz(row.getClassName()).isEmpty()) {
                continue;
            }
            String k = key(row.getCollege(), row.getMajor(), row.getClassName());
            ordered.putIfAbsent(k, new OrgClassOptionVO(row.getId(), row.getCollege(), row.getMajor(), row.getClassName()));
        }

        String sql = "SELECT DISTINCT college, major, class_name FROM `user` "
                + "WHERE deleted = 0 AND user_type = 'student' "
                + "AND class_name IS NOT NULL AND TRIM(class_name) <> '' "
                + "AND college IS NOT NULL AND TRIM(college) <> '' "
                + "AND major IS NOT NULL AND TRIM(major) <> ''";
        List<OrgClassOptionVO> fromStudents = jdbcTemplate.query(sql, (rs, rowNum) -> {
            String c = rs.getString("college");
            String m = rs.getString("major");
            String cn = rs.getString("class_name");
            return new OrgClassOptionVO(null,
                    c != null ? c.trim() : "",
                    m != null ? m.trim() : "",
                    cn != null ? cn.trim() : "");
        });
        for (OrgClassOptionVO vo : fromStudents) {
            String k = key(vo.getCollege(), vo.getMajor(), vo.getClassName());
            if (nz(vo.getClassName()).isEmpty()) {
                continue;
            }
            ordered.putIfAbsent(k, vo);
        }

        return new ArrayList<>(ordered.values());
    }
}
