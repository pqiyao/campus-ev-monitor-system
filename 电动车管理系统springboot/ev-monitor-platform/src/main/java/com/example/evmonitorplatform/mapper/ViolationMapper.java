package com.example.evmonitorplatform.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.evmonitorplatform.entity.Violation;
import com.example.evmonitorplatform.vo.request.ViolationQueryVO;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import java.util.List;
import java.util.Map;

public interface ViolationMapper extends BaseMapper<Violation> {
    
    IPage<Violation> selectViolationPage(Page<Violation> page, @Param("query") ViolationQueryVO query);
    
    @Select("SELECT v.*, ve.license_plate, ve.vehicle_photo, u.real_name as owner_name " +
            "FROM vehicle_illegal_record v " +
            "LEFT JOIN vehicle_info ve ON v.vehicle_id = ve.vehicle_id " +
            "LEFT JOIN `user` u ON v.user_id = u.user_id " +
            "WHERE v.deleted = 0 AND v.illegal_id = #{id}")
    Violation selectWithVehicleInfo(@Param("id") Integer id);
    
    @Select("SELECT COUNT(*) FROM vehicle_illegal_record WHERE deleted = 0")
    Integer countTotal();
    
    @Select("SELECT COUNT(*) FROM vehicle_illegal_record WHERE deleted = 0 AND illegal_state = 0")
    Integer countPendingRectify();
    
    @Select("SELECT illegal_type as type, COUNT(*) as count " +
            "FROM vehicle_illegal_record WHERE deleted = 0 " +
            "GROUP BY illegal_type")
    List<Map<String, Object>> countByType();
    
    @Select("SELECT DATE(illegal_time) as date, COUNT(*) as count " +
            "FROM vehicle_illegal_record WHERE deleted = 0 " +
            "AND illegal_time >= DATE_SUB(NOW(), INTERVAL 7 DAY) " +
            "GROUP BY DATE(illegal_time)")
    List<Map<String, Object>> countLast7Days();
}