package com.example.evmonitorplatform.controller;

import com.example.evmonitorplatform.common.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.web.bind.annotation.*;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/system")
@RequiredArgsConstructor
public class SystemConfigController {

    private final JdbcTemplate jdbcTemplate;

    @GetMapping("/violation-types")
    public ApiResponse<List<Map<String, Object>>> listTypes() {
        return ApiResponse.success(jdbcTemplate.queryForList(
                "SELECT type_id, type_name, description, deduction_points," +
                        " penalty_amount, appeal_deadline FROM violation_type ORDER BY type_id"));
    }

    @PostMapping("/violation-types")
    public ApiResponse<Integer> createType(@RequestBody Map<String, Object> body) {
        KeyHolder kh = new GeneratedKeyHolder();
        jdbcTemplate.update(conn -> {
            PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO violation_type(type_name,description,deduction_points," +
                            "penalty_amount,appeal_deadline) VALUES(?,?,?,?,?)",
                    Statement.RETURN_GENERATED_KEYS);
            ps.setString(1, str(body, "typeName"));
            ps.setString(2, str(body, "description"));
            ps.setInt(3,    num(body, "deductionPoints", 1));
            ps.setDouble(4, dbl(body, "penaltyAmount",   0.0));
            ps.setInt(5,    num(body, "appealDeadline",  72));
            return ps;
        }, kh);
        return ApiResponse.success(kh.getKey() != null ? kh.getKey().intValue() : 0);
    }

    @PutMapping("/violation-types/{typeId}")
    public ApiResponse<Boolean> updateType(@PathVariable Integer typeId,
                                           @RequestBody Map<String, Object> body) {
        int rows = jdbcTemplate.update(
                "UPDATE violation_type SET type_name=?,description=?,deduction_points=?," +
                        "penalty_amount=?,appeal_deadline=? WHERE type_id=?",
                str(body, "typeName"),
                str(body, "description"),
                num(body, "deductionPoints", 1),
                dbl(body, "penaltyAmount",   0.0),
                num(body, "appealDeadline",  72),
                typeId);
        return ApiResponse.success(rows > 0);
    }

    @DeleteMapping("/violation-types/{typeId}")
    public ApiResponse<Boolean> deleteType(@PathVariable Integer typeId) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM vehicle_illegal_record WHERE illegal_type=? AND deleted=0",
                Integer.class, typeId);
        if (count != null && count > 0) {
            return ApiResponse.error(400, "该违规类型已有" + count + "条违规记录使用，无法删除");
        }
        jdbcTemplate.update("DELETE FROM violation_type WHERE type_id=?", typeId);
        return ApiResponse.success(true);
    }

    private String str(Map<String, Object> m, String k) {
        Object v = m.get(k);
        return v == null ? "" : v.toString();
    }
    private int num(Map<String, Object> m, String k, int def) {
        Object v = m.get(k);
        if (v == null) return def;
        try { return Integer.parseInt(v.toString()); } catch (Exception e) { return def; }
    }
    private double dbl(Map<String, Object> m, String k, double def) {
        Object v = m.get(k);
        if (v == null) return def;
        try { return Double.parseDouble(v.toString()); } catch (Exception e) { return def; }
    }
}