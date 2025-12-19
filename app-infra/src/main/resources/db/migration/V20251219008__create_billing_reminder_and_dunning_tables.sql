-- =====================================================
-- Billing D3: 到期提醒 + Dunning 重试表结构
-- =====================================================

-- =====================================================
-- bc_billing_reminder_task: 订阅到期提醒任务表
-- 提前 7/3/1/0 天生成提醒任务，支持重试
-- =====================================================
CREATE TABLE IF NOT EXISTS `bc_billing_reminder_task` (
    `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    
    -- 订阅信息
    `subscription_id` BIGINT UNSIGNED NOT NULL COMMENT '订阅ID',
    `tenant_id` BIGINT UNSIGNED NOT NULL COMMENT '租户ID',
    
    -- 提醒信息
    `reminder_type` VARCHAR(32) NOT NULL COMMENT '提醒类型：EXPIRE_7D/EXPIRE_3D/EXPIRE_1D/EXPIRE_0D/GRACE_3D',
    `remind_at` DATETIME NOT NULL COMMENT '提醒时间（用于幂等判断）',
    `expire_at` DATETIME NOT NULL COMMENT '订阅到期时间（快照）',
    
    -- 套餐信息（快照）
    `plan_code` VARCHAR(64) NOT NULL COMMENT '套餐编码',
    `plan_name` VARCHAR(128) NOT NULL COMMENT '套餐名称',
    
    -- 状态与重试
    `status` VARCHAR(32) NOT NULL DEFAULT 'PENDING' COMMENT '状态：PENDING/SENT/FAILED',
    `retry_count` INT NOT NULL DEFAULT 0 COMMENT '重试次数',
    `max_retry_count` INT NOT NULL DEFAULT 3 COMMENT '最大重试次数',
    `next_retry_at` DATETIME NULL COMMENT '下次重试时间（指数退避）',
    `last_error` TEXT NULL COMMENT '最后一次失败原因',
    
    -- 发送信息
    `sent_at` DATETIME NULL COMMENT '发送成功时间',
    `notification_channels` VARCHAR(255) NULL COMMENT '通知渠道：IN_APP,EMAIL,SMS（逗号分隔）',
    
    -- 审计字段
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_subscription_remind_at` (`subscription_id`, `remind_at`),
    KEY `idx_tenant_id` (`tenant_id`),
    KEY `idx_status_next_retry` (`status`, `next_retry_at`),
    KEY `idx_remind_at` (`remind_at`),
    KEY `idx_created_at` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='订阅到期提醒任务表';

-- =====================================================
-- bc_billing_dunning_log: Dunning 发送日志表
-- 记录每次提醒发送的详细日志（用于审计与重试分析）
-- =====================================================
CREATE TABLE IF NOT EXISTS `bc_billing_dunning_log` (
    `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    
    -- 关联信息
    `reminder_task_id` BIGINT UNSIGNED NOT NULL COMMENT '提醒任务ID',
    `subscription_id` BIGINT UNSIGNED NOT NULL COMMENT '订阅ID',
    `tenant_id` BIGINT UNSIGNED NOT NULL COMMENT '租户ID',
    
    -- 发送信息
    `reminder_type` VARCHAR(32) NOT NULL COMMENT '提醒类型',
    `notification_channel` VARCHAR(32) NOT NULL COMMENT '通知渠道：IN_APP/EMAIL/SMS',
    `recipient` VARCHAR(255) NULL COMMENT '接收者（邮箱/手机号/用户ID）',
    
    -- 结果
    `send_result` VARCHAR(32) NOT NULL COMMENT '发送结果：SUCCESS/FAILED',
    `error_message` TEXT NULL COMMENT '错误信息',
    `response_data` TEXT NULL COMMENT '渠道响应数据（JSON）',
    
    -- 审计字段
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    
    PRIMARY KEY (`id`),
    KEY `idx_reminder_task_id` (`reminder_task_id`),
    KEY `idx_subscription_id` (`subscription_id`),
    KEY `idx_tenant_id` (`tenant_id`),
    KEY `idx_send_result` (`send_result`),
    KEY `idx_created_at` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Dunning 发送日志表';

-- =====================================================
-- 设计要点：
-- 
-- bc_billing_reminder_task:
-- 1. subscription_id + remind_at 唯一索引，确保提醒任务幂等
-- 2. reminder_type 定义提醒时机：7天前/3天前/1天前/当天/宽限期3天
-- 3. retry_count + next_retry_at 支持指数退避重试（1min, 5min, 30min）
-- 4. status 状态机：PENDING → SENT（成功）或 FAILED（重试耗尽）
-- 5. notification_channels 支持多渠道发送（站内通知 + 邮件 + 短信）
-- 
-- bc_billing_dunning_log:
-- 1. 记录每次发送尝试的详细日志，用于审计与问题排查
-- 2. 一个 reminder_task 可能产生多条 dunning_log（重试 + 多渠道）
-- 3. send_result 记录发送结果，便于统计成功率
-- 4. response_data 存储渠道响应，便于问题排查
-- =====================================================
