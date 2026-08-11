package com.example.evmonitorplatform.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.evmonitorplatform.entity.Notification;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

public interface NotificationMapper extends BaseMapper<Notification> {
    
    IPage<Notification> selectNotificationPage(Page<Notification> page, 
                                              @Param("type") String type,
                                              @Param("isUrgent") Boolean isUrgent,
                                              @Param("status") String status);

    /** 候选通知（不含按用户隐藏过滤；隐藏在 Java 中处理，避免未建 inbox 表时整表查询失败） */
    List<Notification> selectCandidateForUser(
            @Param("userId") Integer userId,
            @Param("className") String className,
            @Param("limit") Integer limit);
    
    @Select("SELECT COUNT(*) FROM notification WHERE deleted = 0")
    Integer countTotal();
    
    @Select("SELECT COUNT(*) FROM notification WHERE deleted = 0 AND status = 'sent'")
    Integer countSent();
    
    @Select("SELECT COUNT(*) FROM notification WHERE deleted = 0 AND status = 'draft'")
    Integer countDraft();
    
    @Select("SELECT type, COUNT(*) as count FROM notification WHERE deleted = 0 GROUP BY type")
    java.util.List<java.util.Map<String, Object>> countByType();
}