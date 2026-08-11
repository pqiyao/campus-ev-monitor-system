package com.example.evmonitorplatform.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.evmonitorplatform.entity.LocationDevice;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface LocationDeviceMapper extends BaseMapper<LocationDevice> {
    
    @Select("SELECT * FROM location_device WHERE device_number = #{deviceMac}")
    LocationDevice selectByDeviceMac(@Param("deviceMac") String deviceMac);
    
    @Select("SELECT * FROM location_device WHERE vehicle_id = #{vehicleId}")
    LocationDevice selectByVehicleId(@Param("vehicleId") Integer vehicleId);
}
