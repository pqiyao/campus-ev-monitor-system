package com.example.evmonitorplatform.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.evmonitorplatform.entity.NotificationUserInbox;
import org.apache.ibatis.annotations.Param;

import java.util.Collection;
import java.util.List;

public interface NotificationUserInboxMapper extends BaseMapper<NotificationUserInbox> {

    List<NotificationUserInbox> selectByUserAndNotificationIds(
            @Param("userId") Integer userId,
            @Param("notificationIds") Collection<Integer> notificationIds);

    List<Integer> selectHiddenNotificationIds(
            @Param("userId") Integer userId,
            @Param("notificationIds") Collection<Integer> notificationIds);
}
