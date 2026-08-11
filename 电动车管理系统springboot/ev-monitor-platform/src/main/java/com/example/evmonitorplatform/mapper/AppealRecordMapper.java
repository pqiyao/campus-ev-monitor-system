package com.example.evmonitorplatform.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.evmonitorplatform.entity.AppealRecord;
import com.example.evmonitorplatform.vo.request.AppealQueryVO;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public interface AppealRecordMapper extends BaseMapper<AppealRecord> {

    /**
     * 分页查询申诉记录
     */
    IPage<AppealRecord> selectAppealPage(Page<AppealRecord> page, @Param("query") AppealQueryVO query);

    /**
     * 查询申诉详情（包含关联信息）
     */
    @Select("SELECT a.*, " +
            "u.real_name as user_name, u.phone as user_phone, u.user_type as user_type, " +
            "au.real_name as audit_user_name, " +
            "v.license_plate, vt.description as illegal_type_name, " +
            "v.illegal_time, v.vehicle_photo, v.evidence_url, " +
            "v.illegal_lng, v.illegal_lat " +
            "FROM illegal_appeal_record a " +
            "LEFT JOIN `user` u ON a.user_id = u.user_id " +
            "LEFT JOIN `user` au ON a.audit_user_id = au.user_id " +
            "LEFT JOIN vehicle_illegal_record v ON a.illegal_id = v.illegal_id " +
            "LEFT JOIN violation_type vt ON v.illegal_type = vt.type_id " +
            "WHERE a.appeal_id = #{appealId}")
    AppealRecord selectWithDetail(@Param("appealId") Integer appealId);

    /**
     * 统计各状态申诉数量
     */
    @Select("SELECT appeal_state as state, COUNT(*) as count " +
            "FROM illegal_appeal_record " +
            "GROUP BY appeal_state")
    List<Map<String, Object>> countByState();

    /**
     * 统计申诉来源分布
     */
    @Select("SELECT appeal_source as source, COUNT(*) as count " +
            "FROM illegal_appeal_record " +
            "GROUP BY appeal_source")
    List<Map<String, Object>> countBySource();

    /**
     * 统计申诉结果分布
     */
    @Select("SELECT appeal_result as result, COUNT(*) as count " +
            "FROM illegal_appeal_record " +
            "WHERE appeal_result IS NOT NULL " +
            "GROUP BY appeal_result")
    List<Map<String, Object>> countByResult();

    /**
     * 获取平均处理时间（小时）
     */
    @Select("SELECT AVG(TIMESTAMPDIFF(HOUR, appeal_time, audit_time)) as avg_hours " +
            "FROM illegal_appeal_record " +
            "WHERE appeal_state IN (2, 3) " +
            "AND audit_time IS NOT NULL " +
            "AND appeal_time >= #{startTime}")
    Double getAverageProcessTime(@Param("startTime") LocalDateTime startTime);

    /**
     * 获取按时完成率
     */
    @Select("SELECT " +
            "ROUND(SUM(CASE WHEN TIMESTAMPDIFF(HOUR, appeal_time, audit_time) <= #{deadlineHours} THEN 1 ELSE 0 END) * 100.0 / COUNT(*), 1) as on_time_rate " +
            "FROM illegal_appeal_record " +
            "WHERE appeal_state IN (2, 3) " +
            "AND audit_time IS NOT NULL " +
            "AND appeal_time >= #{startTime}")
    Double getOnTimeCompletionRate(@Param("startTime") LocalDateTime startTime,
                                   @Param("deadlineHours") Integer deadlineHours);

    /**
     * 获取审核人统计
     */
    @Select("SELECT " +
            "audit_user_id, " +
            "COUNT(*) as total_count, " +
            "AVG(TIMESTAMPDIFF(HOUR, appeal_time, audit_time)) as avg_hours, " +
            "ROUND(SUM(CASE WHEN TIMESTAMPDIFF(HOUR, appeal_time, audit_time) <= #{deadlineHours} THEN 1 ELSE 0 END) * 100.0 / COUNT(*), 1) as on_time_rate " +
            "FROM illegal_appeal_record " +
            "WHERE audit_user_id IS NOT NULL " +
            "AND appeal_time >= #{startTime} " +
            "GROUP BY audit_user_id " +
            "ORDER BY total_count DESC")
    List<Map<String, Object>> getAuditorStatistics(@Param("startTime") LocalDateTime startTime);

    /**
     * 获取每日处理统计
     */
    @Select("SELECT " +
            "DATE(audit_time) as audit_date, " +
            "COUNT(*) as count, " +
            "AVG(TIMESTAMPDIFF(HOUR, appeal_time, audit_time)) as avg_hours " +
            "FROM illegal_appeal_record " +
            "WHERE audit_time IS NOT NULL " +
            "AND audit_time >= #{startTime} " +
            "GROUP BY DATE(audit_time) " +
            "ORDER BY audit_date DESC")
    List<Map<String, Object>> getDailyProcessStats(@Param("startTime") LocalDateTime startTime);

    /**
     * 获取超时未处理的申诉
     */
    @Select("SELECT a.*, u.real_name as user_name, v.license_plate " +
            "FROM illegal_appeal_record a " +
            "LEFT JOIN `user` u ON a.user_id = u.user_id " +
            "LEFT JOIN vehicle_illegal_record v ON a.illegal_id = v.illegal_id " +
            "WHERE a.appeal_state = 0 " +
            "AND TIMESTAMPDIFF(HOUR, a.appeal_time, NOW()) > #{deadlineHours} " +
            "ORDER BY a.appeal_time ASC")
    List<AppealRecord> selectOverdueAppeals(@Param("deadlineHours") Integer deadlineHours);

    /**
     * 获取用户最近的申诉记录
     */
    @Select("SELECT a.*, v.license_plate, vt.description as illegal_type_name " +
            "FROM illegal_appeal_record a " +
            "LEFT JOIN vehicle_illegal_record v ON a.illegal_id = v.illegal_id " +
            "LEFT JOIN violation_type vt ON v.illegal_type = vt.type_id " +
            "WHERE a.user_id = #{userId} " +
            "ORDER BY a.appeal_time DESC " +
            "LIMIT #{limit}")
    List<AppealRecord> selectUserRecentAppeals(@Param("userId") Integer userId,
                                               @Param("limit") Integer limit);

    /**
     * 获取违规记录的申诉历史
     */
    @Select("SELECT a.*, u.real_name as user_name " +
            "FROM illegal_appeal_record a " +
            "LEFT JOIN `user` u ON a.user_id = u.user_id " +
            "WHERE a.illegal_id = #{illegalId} " +
            "ORDER BY a.appeal_time DESC")
    List<AppealRecord> selectIllegalAppealHistory(@Param("illegalId") Integer illegalId);
}