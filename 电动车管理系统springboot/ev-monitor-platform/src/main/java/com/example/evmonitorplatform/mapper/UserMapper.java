package com.example.evmonitorplatform.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.evmonitorplatform.entity.User;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import java.util.List;
import java.util.Map;

public interface UserMapper extends BaseMapper<User> {
    
    @Select("SELECT * FROM user WHERE username = #{username} AND deleted = 0")
    User selectByUsername(@Param("username") String username);
    
    @Select("SELECT * FROM user WHERE phone = #{phone} AND deleted = 0")
    User selectByPhone(@Param("phone") String phone);

    @Select("SELECT * FROM user WHERE student_id = #{studentId} AND deleted = 0 LIMIT 1")
    User selectByStudentId(@Param("studentId") String studentId);
    
    @Select("SELECT user_type as type, COUNT(*) as count FROM user WHERE deleted = 0 GROUP BY user_type")
    List<Map<String, Object>> countByUserType();
    
    @Select("SELECT status, COUNT(*) as count FROM user WHERE deleted = 0 GROUP BY status")
    List<Map<String, Object>> countByStatus();
    
    @Update("UPDATE user SET password = #{newPassword} WHERE user_id = #{userId}")
    int updatePassword(@Param("userId") Integer userId, @Param("newPassword") String newPassword);
    
    IPage<User> selectUserPage(Page<User> page, @Param("keyword") String keyword, 
                              @Param("userType") String userType, @Param("status") String status);
    
    @Select("SELECT u.*, COUNT(v.vehicle_id) as vehicle_count FROM user u " +
            "LEFT JOIN vehicle_info v ON u.user_id = v.user_id AND v.deleted = 0 " +
            "WHERE u.deleted = 0 AND u.user_id = #{userId}")
    Map<String, Object> selectUserWithStats(@Param("userId") Integer userId);
}