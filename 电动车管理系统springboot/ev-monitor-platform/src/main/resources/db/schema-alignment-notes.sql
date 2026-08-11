-- ============================================================
-- 与当前业务代码对齐的说明与可选补丁（在你已有全量脚本基础上）
-- 数据库：ev_monitor
-- ============================================================

USE ev_monitor;

-- 1) 电子围栏 fence_type：后端已使用 5 = 「允许停车区」（违停静置判定用）
--    若表注释仍只写到 4，可执行下面一条（幂等、仅更新注释）：
ALTER TABLE geofence
    MODIFY COLUMN fence_type TINYINT NOT NULL DEFAULT 1
        COMMENT '1=禁止进入 2=禁止驶出 3=限速区域 4=监控区域 5=允许停车区';

-- 2) etc_card：vehicle_id 为 NOT NULL + UNIQUE，换绑逻辑为「删原车蓝牙标签行 / 或 update 标签编号行到目标车」
--    不要执行将 vehicle_id 更新为 NULL 的语句。

-- 3) vehicle_illegal_record.illegal_type 与 violation_type.type_id 对应关系（你脚本中一致）：
--    1违规停放 2超速 3违规充电 4未登记 5禁行区 6违规载人 7遮挡车牌 8其他
--    自动违停写入默认使用 type_id=1（可在 application.yml 调整 illegal-parking-type-id）

-- 4) location_device：一车一行（vehicle_id UNIQUE），定位器 MAC 存在 device_number，
--    与 MQTT payload 字段 D、主题 ev/location/{MAC} 对应。

-- 5) 通知用户维度已读/「从我的列表移除」：执行 db/notification_user_inbox.sql 建表
--    否则 /api/notification/user/*、/hide、按用户已读 会报错。

SELECT 'schema-alignment-notes 已阅读/可选 ALTER 已执行则注释已更新' AS msg;
