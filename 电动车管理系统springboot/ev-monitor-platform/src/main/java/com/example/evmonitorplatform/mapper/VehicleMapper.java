package com.example.evmonitorplatform.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.evmonitorplatform.entity.Vehicle;

import com.example.evmonitorplatform.vo.request.VehicleQueryVO;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import java.util.List;
import java.util.Map;

public interface VehicleMapper extends BaseMapper<Vehicle> {
    
    IPage<Vehicle> selectVehiclePage(Page<Vehicle> page, @Param("query") VehicleQueryVO query);
    
    @Select("SELECT COUNT(*) FROM vehicle_info WHERE deleted = 0")
    Integer countTotal();
    
    @Select("SELECT COUNT(*) FROM vehicle_info WHERE deleted = 0 AND status = 'approved'")
    Integer countApproved();
    
    @Select("SELECT COUNT(*) FROM vehicle_info WHERE deleted = 0 AND status = 'pending'")
    Integer countPending();
    
    @Select("SELECT COUNT(*) FROM vehicle_info WHERE deleted = 0 AND is_blacklist = 1")
    Integer countBlacklist();
    
    @Select("SELECT user_type as type, COUNT(*) as count FROM vehicle_info v " +
            "LEFT JOIN `user` u ON v.user_id = u.user_id " +
            "WHERE v.deleted = 0 AND v.status = 'approved' " +
            "GROUP BY u.user_type")
    List<Map<String, Object>> countByUserType();
    
    @Select("SELECT v.*, u.real_name as owner_name, u.phone as phone " +
            "FROM vehicle_info v LEFT JOIN `user` u ON v.user_id = u.user_id " +
            "WHERE v.deleted = 0 AND v.license_plate LIKE CONCAT('%', #{plate}, '%')")
    List<Vehicle> searchByPlate(@Param("plate") String plate);

    @Select("SELECT * FROM vehicle_info WHERE frame_num = #{frameNum} AND deleted = 0")
    Vehicle selectByFrameNum(@Param("frameNum") String frameNum);

    @Select("SELECT * FROM vehicle_info WHERE license_plate = #{plate} AND deleted = 0 LIMIT 1")
    Vehicle selectByLicensePlate(@Param("plate") String plate);
}