package com.example.evmonitorplatform.controller;

import com.example.evmonitorplatform.common.response.ApiResponse;
import com.example.evmonitorplatform.common.response.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@Slf4j
@RestController
@RequestMapping("/api/operation-log")
@RequiredArgsConstructor
@Tag(name = "操作日志", description = "申诉操作日志查询接口")
public class OperationLogController {

    private final JdbcTemplate jdbcTemplate;

    @GetMapping("/list")
    @Operation(summary = "查询操作日志", description = "分页查询申诉操作日志")
    public PageResponse<List<Map<String, Object>>> listLogs(
            @RequestParam(required = false) Integer appealId,
            @RequestParam(required = false) String operationType,
            @RequestParam(required = false) Integer operatorId,
            @RequestParam(required = false) String startTime,
            @RequestParam(required = false) String endTime,
            @RequestParam(defaultValue = "1")  Integer pageNum,
            @RequestParam(defaultValue = "20") Integer pageSize) {

        StringBuilder where = new StringBuilder("WHERE 1=1 ");
        List<Object> filterParams = new ArrayList<>();

        if (appealId != null) {
            where.append("AND l.appeal_id = ? ");
            filterParams.add(appealId);
        }
        if (operationType != null && !operationType.isEmpty()) {
            where.append("AND l.operation_type = ? ");
            filterParams.add(operationType);
        }
        if (operatorId != null) {
            where.append("AND l.operator_id = ? ");
            filterParams.add(operatorId);
        }
        if (startTime != null && !startTime.isEmpty()) {
            where.append("AND l.create_time >= ? ");
            filterParams.add(startTime);
        }
        if (endTime != null && !endTime.isEmpty()) {
            where.append("AND l.create_time <= ? ");
            filterParams.add(endTime + " 23:59:59");
        }

        String countSql = "SELECT COUNT(*) FROM appeal_operation_log l " + where;
        Integer total = jdbcTemplate.queryForObject(countSql, Integer.class, filterParams.toArray());

        StringBuilder sql = new StringBuilder(
                "SELECT l.log_id AS logId, l.appeal_id AS appealId, l.operation_type AS operationType, " +
                "l.operator_id AS operatorId, COALESCE(u.real_name,'') AS operatorName, " +
                "l.operation_detail AS operationDetail, l.create_time AS createTime " +
                "FROM appeal_operation_log l " +
                "LEFT JOIN `user` u ON l.operator_id = u.user_id ");
        sql.append(where);
        sql.append("ORDER BY l.create_time DESC LIMIT ? OFFSET ? ");

        List<Object> dataParams = new ArrayList<>(filterParams);
        dataParams.add(pageSize);
        dataParams.add((pageNum - 1) * pageSize);

        List<Map<String, Object>> records = jdbcTemplate.queryForList(sql.toString(), dataParams.toArray());
        int totalCount = total != null ? total : 0;

        return new PageResponse<>(200, "success", records, pageNum, pageSize, totalCount);
    }

    @GetMapping("/appeal/{appealId}")
    @Operation(summary = "获取指定申诉的操作日志")
    public ApiResponse<List<Map<String, Object>>> getByAppealId(@PathVariable Integer appealId) {
        List<Map<String, Object>> logs = jdbcTemplate.queryForList(
            "SELECT l.log_id AS logId, l.appeal_id AS appealId, l.operation_type AS operationType, " +
            "l.operator_id AS operatorId, COALESCE(u.real_name,'') AS operatorName, " +
            "l.operation_detail AS operationDetail, l.create_time AS createTime " +
            "FROM appeal_operation_log l " +
            "LEFT JOIN `user` u ON l.operator_id = u.user_id " +
            "WHERE l.appeal_id = ? ORDER BY l.create_time ASC",
            appealId);
        return ApiResponse.success(logs);
    }

    @GetMapping("/stats")
    @Operation(summary = "操作日志统计")
    public ApiResponse<Map<String, Object>> getStats() {
        Map<String, Object> stats = new HashMap<>();
        try {
            stats.put("total", jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM appeal_operation_log", Integer.class));
            stats.put("todayCount", jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM appeal_operation_log WHERE DATE(create_time) = CURDATE()",
                Integer.class));
            stats.put("typeStats", jdbcTemplate.queryForList(
                "SELECT operation_type, COUNT(*) AS count FROM appeal_operation_log " +
                "GROUP BY operation_type ORDER BY count DESC"));
        } catch (Exception e) {
            log.error("查询操作日志统计失败", e);
        }
        return ApiResponse.success(stats);
    }
}