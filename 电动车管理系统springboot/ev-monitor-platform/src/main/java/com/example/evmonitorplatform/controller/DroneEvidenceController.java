package com.example.evmonitorplatform.controller;

import com.example.evmonitorplatform.common.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/drone")
@RequiredArgsConstructor
public class DroneEvidenceController {

    private final JdbcTemplate jdbcTemplate;

    @GetMapping("/page")
    public ApiResponse<Map<String, Object>> page(
            @RequestParam(required = false) String  licensePlate,
            @RequestParam(required = false) Integer illegalId,
            @RequestParam(required = false) String  startTime,
            @RequestParam(required = false) String  endTime,
            @RequestParam(defaultValue = "1")  int pageNum,
            @RequestParam(defaultValue = "10") int pageSize) {

        List<Object> params = new ArrayList<>();
        StringBuilder where = new StringBuilder(" WHERE 1=1 ");
        if (licensePlate != null && !licensePlate.isBlank()) {
            where.append(" AND d.license_plate LIKE ?");
            params.add("%" + licensePlate + "%");
        }
        if (illegalId != null) {
            where.append(" AND d.illegal_id = ?"); params.add(illegalId);
        }
        if (startTime != null && !startTime.isBlank()) {
            where.append(" AND d.capture_time >= ?"); params.add(startTime + " 00:00:00");
        }
        if (endTime != null && !endTime.isBlank()) {
            where.append(" AND d.capture_time <= ?"); params.add(endTime + " 23:59:59");
        }

        String baseSql =
                " FROM drone_evidence d" +
                        " LEFT JOIN vehicle_illegal_record vir ON d.illegal_id = vir.illegal_id" +
                        " LEFT JOIN vehicle_info vi ON vir.vehicle_id = vi.vehicle_id";

        Integer total = jdbcTemplate.queryForObject(
                "SELECT COUNT(*)" + baseSql + where, Integer.class, params.toArray());

        int offset = (pageNum - 1) * pageSize;
        List<Object> dp = new ArrayList<>(params);
        dp.add(pageSize); dp.add(offset);
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                "SELECT d.*, vi.license_plate, vi.vehicle_photo," +
                        " vir.illegal_type, vir.illegal_time" +
                        baseSql + where +
                        " ORDER BY d.capture_time DESC LIMIT ? OFFSET ?",
                dp.toArray());

        Map<String, Object> res = new LinkedHashMap<>();
        res.put("total",    total != null ? total : 0);
        res.put("pageNum",  pageNum);
        res.put("pageSize", pageSize);
        res.put("records",  rows);
        return ApiResponse.success(res);
    }

    @GetMapping("/{evidenceId}")
    public ApiResponse<Map<String, Object>> detail(@PathVariable Integer evidenceId) {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                "SELECT d.*, vi.license_plate, vi.vehicle_photo, vir.illegal_type, vir.illegal_time" +
                        " FROM drone_evidence d" +
                        " LEFT JOIN vehicle_illegal_record vir ON d.illegal_id = vir.illegal_id" +
                        " LEFT JOIN vehicle_info vi ON vir.vehicle_id = vi.vehicle_id" +
                        " WHERE d.evidence_id = ?", evidenceId);
        if (rows.isEmpty()) {
            return ApiResponse.error(404, "记录不存在");
        }
        return ApiResponse.success(rows.get(0));
    }

    @PutMapping("/{evidenceId}/link")
    public ApiResponse<Boolean> linkToViolation(@PathVariable Integer evidenceId,
                                                @RequestParam Integer illegalId) {
        int rows = jdbcTemplate.update(
                "UPDATE drone_evidence SET illegal_id=? WHERE evidence_id=?",
                illegalId, evidenceId);
        return ApiResponse.success(rows > 0);
    }

    @GetMapping("/stats")
    public ApiResponse<Map<String, Object>> stats() {
        Integer total   = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM drone_evidence", Integer.class);
        Integer linked  = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM drone_evidence WHERE illegal_id IS NOT NULL", Integer.class);
        Integer today   = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM drone_evidence WHERE DATE(capture_time)=CURDATE()", Integer.class);
        Map<String, Object> res = new LinkedHashMap<>();
        res.put("total",   total != null ? total : 0);
        res.put("linked",  linked != null ? linked : 0);
        res.put("unlinked",(total != null ? total : 0) - (linked != null ? linked : 0));
        res.put("today",   today != null ? today : 0);
        return ApiResponse.success(res);
    }
}