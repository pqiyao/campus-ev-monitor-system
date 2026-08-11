package com.example.evmonitorplatform.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.example.evmonitorplatform.common.response.PageResponse;
import com.example.evmonitorplatform.entity.Notification;
import com.example.evmonitorplatform.vo.request.NotificationSendVO;
import java.util.List;
import java.util.Map;

public interface NotificationService extends IService<Notification> {
    
    boolean sendNotification(NotificationSendVO sendVO);
    
    boolean sendPersonalNotification(String title, String content, Integer receiverId);
    
    boolean sendBatchNotification(String title, String content, List<Integer> receiverIds);
    
    boolean markAsRead(Integer notificationId, Integer userId);
    
    PageResponse<List<Notification>> queryNotifications(String type, Boolean isUrgent, 
                                                       String status, Integer pageNum, Integer pageSize);
    
    List<Notification> getUserNotifications(Integer userId, Integer limit);
    
    Map<String, Object> getNotificationStats();
    
    boolean deleteNotification(Integer notificationId);

    /** 用户从自己的收件箱移除（不删除全局通知，管理端仍可见） */
    boolean hideNotificationForUser(Integer notificationId, Integer userId);

    /** 学生端查看详情：校验接收资格且未隐藏 */
    Notification getNotificationForUser(Integer notificationId, Integer userId);
}