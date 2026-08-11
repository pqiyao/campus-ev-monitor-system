package com.example.evmonitorplatform.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.evmonitorplatform.entity.ETCard;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import java.math.BigDecimal;
import java.util.List;

public interface ETCardMapper extends BaseMapper<ETCard> {

    IPage<ETCard> selectCardPage(Page<ETCard> page,
                                 @Param("keyword") String keyword,
                                 @Param("status") String status,
                                 @Param("userType") String userType);
    
    @Select("SELECT e.*, v.license_plate, u.real_name as owner_name " +
            "FROM etc_card e " +
            "LEFT JOIN vehicle_info v ON e.vehicle_id = v.vehicle_id " +
            "LEFT JOIN `user` u ON v.user_id = u.user_id " +
            "WHERE e.etc_number = #{etcNumber}")
    ETCard selectWithVehicleInfo(@Param("etcNumber") String etcNumber);
    
    @Select("SELECT SUM(balance) FROM etc_card WHERE status = 'normal'")
    BigDecimal getTotalBalance();
    
    @Update("UPDATE etc_card SET balance = balance - #{amount} WHERE etc_id = #{etcId} AND balance >= #{amount}")
    int deductBalance(@Param("etcId") Integer etcId, @Param("amount") BigDecimal amount);
    
    @Update("UPDATE etc_card SET balance = balance + #{amount} WHERE etc_id = #{etcId}")
    int rechargeBalance(@Param("etcId") Integer etcId, @Param("amount") BigDecimal amount);
    
    @Select("SELECT * FROM etc_card WHERE vehicle_id = #{vehicleId}")
    ETCard selectByVehicleId(@Param("vehicleId") Integer vehicleId);
    
    @Select("SELECT e.*, v.license_plate FROM etc_card e " +
            "LEFT JOIN vehicle_info v ON e.vehicle_id = v.vehicle_id " +
            "WHERE v.license_plate LIKE CONCAT('%', #{plate}, '%')")
    List<ETCard> searchByPlate(@Param("plate") String plate);
}