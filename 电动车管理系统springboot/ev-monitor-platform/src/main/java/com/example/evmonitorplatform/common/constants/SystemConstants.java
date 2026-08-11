package com.example.evmonitorplatform.common.constants;

public class SystemConstants {
    
    // 用户状态
    public static final String USER_STATUS_ACTIVE = "active";
    public static final String USER_STATUS_FROZEN = "frozen";
    public static final String USER_STATUS_DELETED = "deleted";
    
    // 车辆状态
    public static final String VEHICLE_STATUS_PENDING = "pending";
    public static final String VEHICLE_STATUS_APPROVED = "approved";
    public static final String VEHICLE_STATUS_REJECTED = "rejected";
    public static final String VEHICLE_STATUS_EXPIRED = "expired";
    
    // 违规状态
    public static final Integer VIOLATION_STATE_PENDING = 0;
    public static final Integer VIOLATION_STATE_RECTIFYING = 1;
    public static final Integer VIOLATION_STATE_RECTIFIED = 2;
    public static final Integer VIOLATION_STATE_OVERDUE = 3;
    public static final Integer VIOLATION_STATE_APPEALING = 4;
    
    // 整改核实状态
    public static final Integer RECTIFY_VERIFY_PENDING = 0;
    public static final Integer RECTIFY_VERIFY_PROCESSING = 1;
    public static final Integer RECTIFY_VERIFY_PASSED = 2;
    public static final Integer RECTIFY_VERIFY_FAILED = 3;
    
    // 申诉状态
    public static final Integer APPEAL_STATE_PENDING = 0;
    public static final Integer APPEAL_STATE_PROCESSING = 1;
    public static final Integer APPEAL_STATE_SUCCESS = 2;
    public static final Integer APPEAL_STATE_FAILED = 3;
    
    // 摄像头状态
    public static final String CAMERA_STATUS_ONLINE = "online";
    public static final String CAMERA_STATUS_OFFLINE = "offline";
    public static final String CAMERA_STATUS_MALFUNCTION = "malfunction";
    
    // 通知状态
    public static final String NOTIFICATION_STATUS_DRAFT = "draft";
    public static final String NOTIFICATION_STATUS_SENT = "sent";
    public static final String NOTIFICATION_STATUS_FAILED = "failed";
    
    // 文件上传限制
    public static final long MAX_FILE_SIZE = 10 * 1024 * 1024; // 10MB
    public static final long MAX_REQUEST_SIZE = 50 * 1024 * 1024; // 50MB
    
    // 缓存键前缀
    public static final String CACHE_USER_PREFIX = "user:";
    public static final String CACHE_VEHICLE_PREFIX = "vehicle:";
    public static final String CACHE_VIOLATION_PREFIX = "violation:";
    public static final String CACHE_NOTIFICATION_PREFIX = "notification:";
    
    // Token过期时间（秒）
    public static final long TOKEN_EXPIRE_TIME = 86400L; // 24小时
    
    // 分页默认值
    public static final int DEFAULT_PAGE_NUM = 1;
    public static final int DEFAULT_PAGE_SIZE = 10;
    public static final int MAX_PAGE_SIZE = 100;
    
    // 时间格式
    public static final String DATE_FORMAT = "yyyy-MM-dd";
    public static final String DATETIME_FORMAT = "yyyy-MM-dd HH:mm:ss";
    public static final String TIME_FORMAT = "HH:mm:ss";
    
    // 文件路径
    public static final String UPLOAD_PATH = "./uploads/";
    public static final String AVATAR_PATH = "avatar/";
    public static final String VEHICLE_PHOTO_PATH = "vehicle/";
    public static final String EVIDENCE_PATH = "evidence/";
    
    // 正则表达式
    public static final String PHONE_REGEX = "^1[3-9]\\d{9}$";
    public static final String EMAIL_REGEX = "^[A-Za-z0-9+_.-]+@(.+)$";
    public static final String LICENSE_PLATE_REGEX = "^[\\u4e00-\\u9fa5]{1}[A-Z]{1}[A-Z0-9]{5}$";
    
    // 系统配置
    public static final String SYSTEM_NAME = "电动车智能监控平台";
    public static final String SYSTEM_VERSION = "1.0.0";
    public static final String COPYRIGHT = "© 2024 衡阳师范学院";
}