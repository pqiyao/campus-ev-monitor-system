package com.example.evmonitorplatform.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.evmonitorplatform.common.exception.BusinessException;
import com.example.evmonitorplatform.common.response.PageResponse;
import com.example.evmonitorplatform.entity.Notification;
import com.example.evmonitorplatform.entity.NotificationUserInbox;
import com.example.evmonitorplatform.entity.User;
import com.example.evmonitorplatform.mapper.NotificationMapper;
import com.example.evmonitorplatform.mapper.NotificationUserInboxMapper;
import com.example.evmonitorplatform.mapper.UserMapper;
import com.example.evmonitorplatform.service.NotificationService;
import com.example.evmonitorplatform.config.NotificationUserInboxSchema;
import com.example.evmonitorplatform.vo.request.NotificationSendVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationServiceImpl extends ServiceImpl<NotificationMapper, Notification> implements NotificationService {

    private final NotificationUserInboxMapper notificationUserInboxMapper;
    private final UserMapper userMapper;
    private final NotificationUserInboxSchema notificationUserInboxSchema;
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean sendNotification(NotificationSendVO sendVO) {
        log.info("发送通知: {}", sendVO.getTitle());
        
        Notification notification = new Notification();
        notification.setTitle(sendVO.getTitle());
        notification.setContent(sendVO.getContent());
        notification.setType(sendVO.getType());
        notification.setIsUrgent(sendVO.getIsUrgent());
        notification.setReceiverType(sendVO.getReceiverType());
        
        if (sendVO.getReceiverIds() != null && !sendVO.getReceiverIds().isEmpty()) {
            notification.setReceiverIds(String.join(",", sendVO.getReceiverIds()));
            notification.setTotalCount(sendVO.getReceiverIds().size());
        } else {
            notification.setTotalCount(0);
        }
        
        notification.setSendTime(LocalDateTime.now());
        notification.setStatus("sent");
        notification.setReadCount(0);
        
        return this.save(notification);
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean sendPersonalNotification(String title, String content, Integer receiverId) {
        List<Integer> receiverIds = new ArrayList<>();
        receiverIds.add(receiverId);
        
        NotificationSendVO sendVO = new NotificationSendVO();
        sendVO.setTitle(title);
        sendVO.setContent(content);
        sendVO.setType("personal");
        sendVO.setReceiverType("specified");
        sendVO.setReceiverIds(receiverIds.stream().map(String::valueOf).collect(Collectors.toList()));
        
        return sendNotification(sendVO);
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean sendBatchNotification(String title, String content, List<Integer> receiverIds) {
        NotificationSendVO sendVO = new NotificationSendVO();
        sendVO.setTitle(title);
        sendVO.setContent(content);
        sendVO.setType("batch");
        sendVO.setReceiverType("specified");
        sendVO.setReceiverIds(receiverIds.stream().map(String::valueOf).collect(Collectors.toList()));
        
        return sendNotification(sendVO);
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean markAsRead(Integer notificationId, Integer userId) {
        Notification notification = this.getById(notificationId);
        if (notification == null) {
            throw new BusinessException("通知不存在");
        }
        if (!userCanReceive(notification, userId)) {
            throw new BusinessException("您不是该通知的接收人");
        }
        if (Boolean.TRUE.equals(isHiddenForUser(userId, notificationId))) {
            throw new BusinessException("通知已移除");
        }
        upsertReadAt(userId, notificationId);
        return true;
    }
    
    @Override
    public PageResponse<List<Notification>> queryNotifications(String type, Boolean isUrgent, 
                                                             String status, Integer pageNum, Integer pageSize) {
        Page<Notification> page = new Page<>(pageNum, pageSize);
        Page<Notification> resultPage = (Page<Notification>) baseMapper.selectNotificationPage(page, type, isUrgent, status);
        
        return new PageResponse<>(
            200,
            "success",
            resultPage.getRecords(),
            (int) resultPage.getCurrent(),
            (int) resultPage.getSize(),
            resultPage.getTotal()
        );
    }
    
    @Override
    public List<Notification> getUserNotifications(Integer userId, Integer limit) {
        User viewer = userMapper.selectById(userId);
        String className = null;
        if (viewer != null && viewer.getClassName() != null && !viewer.getClassName().isBlank()) {
            className = viewer.getClassName().trim();
        }
        List<Notification> list = baseMapper.selectCandidateForUser(userId, className, limit);
        if (list.isEmpty()) {
            return list;
        }
        List<Integer> ids = list.stream().map(Notification::getNotificationId).collect(Collectors.toList());
        if (notificationUserInboxSchema.ensureTableExists()) {
            try {
                List<Integer> hidden = notificationUserInboxMapper.selectHiddenNotificationIds(userId, ids);
                if (hidden != null && !hidden.isEmpty()) {
                    Set<Integer> hset = new HashSet<>(hidden);
                    list.removeIf(n -> hset.contains(n.getNotificationId()));
                }
            } catch (Exception e) {
                log.warn("notification_user_inbox 查询失败，跳过「已移除」过滤: {}", e.getMessage());
            }
        }
        if (list.isEmpty()) {
            return list;
        }
        ids = list.stream().map(Notification::getNotificationId).collect(Collectors.toList());
        if (notificationUserInboxSchema.ensureTableExists()) {
            try {
                List<NotificationUserInbox> states = notificationUserInboxMapper.selectByUserAndNotificationIds(userId, ids);
                Map<Integer, NotificationUserInbox> byNid = states.stream()
                        .collect(Collectors.toMap(NotificationUserInbox::getNotificationId, s -> s, (a, b) -> a));
                for (Notification n : list) {
                    NotificationUserInbox s = byNid.get(n.getNotificationId());
                    n.setReadByMe(s != null && s.getReadAt() != null);
                }
            } catch (Exception e) {
                log.warn("notification_user_inbox 查询失败，readByMe 暂为 false: {}", e.getMessage());
                list.forEach(n -> n.setReadByMe(false));
            }
        } else {
            list.forEach(n -> n.setReadByMe(false));
        }
        return list;
    }
    
    @Override
    public Map<String, Object> getNotificationStats() {
        Map<String, Object> stats = new HashMap<>();
        
        stats.put("total", baseMapper.countTotal());
        stats.put("sent", baseMapper.countSent());
        stats.put("draft", baseMapper.countDraft());
        stats.put("typeStats", baseMapper.countByType());
        
        // 计算送达率
        List<Map<String, Object>> notifications = baseMapper.countByType();
        long totalSent = notifications.stream()
            .mapToLong(m -> Long.parseLong(m.get("count").toString()))
            .sum();
        
        double deliveryRate = totalSent > 0 ? 95.0 : 0; // 模拟送达率
        stats.put("deliveryRate", String.format("%.1f%%", deliveryRate));
        
        return stats;
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteNotification(Integer notificationId) {
        return this.removeById(notificationId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean hideNotificationForUser(Integer notificationId, Integer userId) {
        Notification notification = this.getById(notificationId);
        if (notification == null) {
            throw new BusinessException("通知不存在");
        }
        if (!userCanReceive(notification, userId)) {
            throw new BusinessException("您不是该通知的接收人");
        }
        upsertHidden(userId, notificationId);
        return true;
    }

    @Override
    public Notification getNotificationForUser(Integer notificationId, Integer userId) {
        Notification n = this.getById(notificationId);
        if (n == null) {
            throw new BusinessException("通知不存在");
        }
        if (!userCanReceive(n, userId)) {
            throw new BusinessException("无权查看该通知");
        }
        if (Boolean.TRUE.equals(isHiddenForUser(userId, notificationId))) {
            throw new BusinessException("通知不存在");
        }
        n.setReadByMe(false);
        if (notificationUserInboxSchema.ensureTableExists()) {
            try {
                LambdaQueryWrapper<NotificationUserInbox> w = new LambdaQueryWrapper<>();
                w.eq(NotificationUserInbox::getUserId, userId).eq(NotificationUserInbox::getNotificationId, notificationId);
                NotificationUserInbox row = notificationUserInboxMapper.selectOne(w);
                n.setReadByMe(row != null && row.getReadAt() != null);
            } catch (Exception e) {
                log.debug("读取通知已读状态失败: {}", e.getMessage());
            }
        }
        return n;
    }

    private boolean userCanReceive(Notification n, Integer userId) {
        if (userId == null) {
            return false;
        }
        String rt = n.getReceiverType();
        if (rt != null && "all".equalsIgnoreCase(rt.trim())) {
            return true;
        }
        if (rt != null && "class".equalsIgnoreCase(rt.trim())) {
            User u = userMapper.selectById(userId);
            String cn = u != null && u.getClassName() != null ? u.getClassName().trim() : "";
            if (cn.isEmpty()) {
                return false;
            }
            String classIds = n.getReceiverIds();
            if (classIds == null || classIds.isBlank()) {
                return false;
            }
            for (String raw : classIds.split(",")) {
                String p = raw.replace('，', ',').trim();
                if (p.isEmpty()) {
                    continue;
                }
                if (p.equals(cn)) {
                    return true;
                }
            }
            return false;
        }
        String ids = n.getReceiverIds();
        if (ids == null || ids.isBlank()) {
            return false;
        }
        String normalized = ids.replace(" ", "").replace('，', ',');
        for (String part : normalized.split(",")) {
            if (part.isEmpty()) {
                continue;
            }
            if (part.equals(String.valueOf(userId))) {
                return true;
            }
        }
        return false;
    }

    private Boolean isHiddenForUser(Integer userId, Integer notificationId) {
        try {
            LambdaQueryWrapper<NotificationUserInbox> w = new LambdaQueryWrapper<>();
            w.eq(NotificationUserInbox::getUserId, userId)
                    .eq(NotificationUserInbox::getNotificationId, notificationId)
                    .eq(NotificationUserInbox::getHidden, true);
            return notificationUserInboxMapper.selectCount(w) > 0;
        } catch (Exception e) {
            log.debug("inbox hidden 检查跳过: {}", e.getMessage());
            return false;
        }
    }

    private void upsertReadAt(Integer userId, Integer notificationId) {
        if (!notificationUserInboxSchema.ensureTableExists()) {
            throw new BusinessException("通知状态表不可用：请确认数据库连接正常，或为账号授予 CREATE TABLE 权限；亦可手动执行 resources/db/notification_user_inbox.sql");
        }
        LambdaQueryWrapper<NotificationUserInbox> w = new LambdaQueryWrapper<>();
        w.eq(NotificationUserInbox::getUserId, userId).eq(NotificationUserInbox::getNotificationId, notificationId);
        NotificationUserInbox row = notificationUserInboxMapper.selectOne(w);
        LocalDateTime now = LocalDateTime.now();
        if (row == null) {
            row = new NotificationUserInbox();
            row.setUserId(userId);
            row.setNotificationId(notificationId);
            row.setHidden(false);
            row.setReadAt(now);
            notificationUserInboxMapper.insert(row);
        } else {
            if (Boolean.TRUE.equals(row.getHidden())) {
                throw new BusinessException("通知已移除");
            }
            row.setReadAt(now);
            notificationUserInboxMapper.updateById(row);
        }
    }

    private void upsertHidden(Integer userId, Integer notificationId) {
        if (!notificationUserInboxSchema.ensureTableExists()) {
            throw new BusinessException("通知状态表不可用：请确认数据库连接正常，或为账号授予 CREATE TABLE 权限；亦可手动执行 resources/db/notification_user_inbox.sql");
        }
        LambdaQueryWrapper<NotificationUserInbox> w = new LambdaQueryWrapper<>();
        w.eq(NotificationUserInbox::getUserId, userId).eq(NotificationUserInbox::getNotificationId, notificationId);
        NotificationUserInbox row = notificationUserInboxMapper.selectOne(w);
        if (row == null) {
            row = new NotificationUserInbox();
            row.setUserId(userId);
            row.setNotificationId(notificationId);
            row.setHidden(true);
            row.setReadAt(null);
            notificationUserInboxMapper.insert(row);
        } else {
            row.setHidden(true);
            notificationUserInboxMapper.updateById(row);
        }
    }
}