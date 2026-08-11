package com.example.evmonitorplatform.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.example.evmonitorplatform.common.response.PageResponse;
import com.example.evmonitorplatform.entity.User;
import com.example.evmonitorplatform.vo.request.LoginVO;
import com.example.evmonitorplatform.vo.response.DashboardVO;
import java.util.List;
import java.util.Map;

public interface UserService extends IService<User> {
    
    User login(LoginVO loginVO);
    
    User register(User user);
    
    boolean updatePassword(Integer userId, String oldPassword, String newPassword);
    
    boolean resetPassword(Integer userId, String newPassword);
    
    boolean updateProfile(User user);
    
    boolean updateStatus(Integer userId, String status);
    
    PageResponse<List<User>> queryUsers(String keyword, String userType, 
                                       String status, Integer pageNum, Integer pageSize);
    
    Map<String, Object> getUserStats();
    
    Map<String, Object> getUserDashboard(Integer userId);
    
    boolean assignRole(Integer userId, String role);

    User getById(Number id);
}