package com.example.evmonitorplatform.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.evmonitorplatform.entity.GeoFenceAlert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface GeoFenceAlertMapper extends BaseMapper<GeoFenceAlert> {

    @Select("SELECT * FROM geofence_alert ORDER BY create_time DESC LIMIT #{limit}")
    List<GeoFenceAlert> selectRecentAlerts(@Param("limit") int limit);

    @Update("UPDATE geofence_alert SET is_processed = 1 WHERE alert_id = #{alertId}")
    int markProcessed(@Param("alertId") Integer alertId);

    @Select("SELECT COUNT(*) FROM geofence_alert WHERE is_processed = 0")
    int countUnprocessed();

    @Select("SELECT COUNT(*) FROM geofence_alert WHERE DATE(create_time) = CURDATE()")
    int countToday();
}