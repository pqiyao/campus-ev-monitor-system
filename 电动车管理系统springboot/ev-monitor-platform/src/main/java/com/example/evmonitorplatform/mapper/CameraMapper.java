package com.example.evmonitorplatform.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.evmonitorplatform.entity.Camera;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import java.util.List;
import java.util.Map;

public interface CameraMapper extends BaseMapper<Camera> {
    
    @Select("SELECT COUNT(*) FROM camera_monitor")
    Integer countTotal();
    
    @Select("SELECT COUNT(*) FROM camera_monitor WHERE status = 'online'")
    Integer countOnline();
    
    @Select("SELECT status, COUNT(*) as count FROM camera_monitor GROUP BY status")
    List<Map<String, Object>> countByStatus();
    
    @Select("SELECT location, COUNT(*) as count FROM camera_monitor GROUP BY location")
    List<Map<String, Object>> countByLocation();
    
    @Select("SELECT * FROM camera_monitor WHERE location LIKE CONCAT('%', #{location}, '%')")
    List<Camera> selectByLocation(@Param("location") String location);
}