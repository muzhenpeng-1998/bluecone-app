-- =====================================================
-- Notification Center Tables
-- 统一消息中心：支持多渠道通知、模板化、频控、审计
-- =====================================================

-- =====================================================
-- bc_notify_template: 通知模板表
-- 集中管理通知文案模板，支持变量替换
-- =====================================================
CREATE TABLE IF NOT EXISTS `bc_notify_template` (
    `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `tenant_id` BIGINT UNSIGNED NULL COMMENT '租户ID（NULL=系统级模板）',
    
    -- 模板标识
    `template_code` VARCHAR(64) NOT NULL COMMENT '模板编码：INVOICE_PAID_REMINDER、RENEWAL_SUCCESS、ORDER_READY 等',
    `template_name` VARCHAR(128) NOT NULL COMMENT '模板名称（管理员可见）',
    `biz_type` VARCHAR(64) NOT NULL COMMENT '业务类型：BILLING、ORDER、REFUND、SUBSCRIPTION 等',
    
    -- 渠道与内容
    `channel` VARCHAR(32) NOT NULL COMMENT '通知渠道：IN_APP、EMAIL、WECHAT、SMS',
    `title_template` VARCHAR(255) NULL COMMENT '标题模板（站内信/邮件主题）',
    `content_template` TEXT NOT NULL COMMENT '内容模板，支持变量占位符 {{varName}}',
    `template_variables` JSON NULL COMMENT '模板变量定义：[{name: "invoiceNo", type: "string", description: "账单号"}]',
    
    -- 状态与配置
    `status` VARCHAR(32) NOT NULL DEFAULT 'ENABLED' COMMENT '状态：ENABLED、DISABLED',
    `priority` INT NOT NULL DEFAULT 50 COMMENT '优先级：数值越大优先级越高',
    
    -- 频控配置（可选，覆盖全局策略）
    `rate_limit_config` JSON NULL COMMENT '频控配置：{dailyLimit: 3, quietHoursStart: "22:00", quietHoursEnd: "08:00"}',
    
    -- 审计字段
    `created_by` VARCHAR(64) NULL COMMENT '创建人',
    `updated_by` VARCHAR(64) NULL COMMENT '更新人',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_template_code_channel` (`template_code`, `channel`, `tenant_id`),
    KEY `idx_biz_type_status` (`biz_type`, `status`),
    KEY `idx_tenant_status` (`tenant_id`, `status`),
    KEY `idx_created_at` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='通知模板表';

-- =====================================================
-- bc_notify_task: 通知任务表
-- 从 outbox 事件生成，等待 dispatcher 发送
-- =====================================================
CREATE TABLE IF NOT EXISTS `bc_notify_task` (
    `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `tenant_id` BIGINT UNSIGNED NULL COMMENT '租户ID',
    `user_id` BIGINT UNSIGNED NOT NULL COMMENT '接收用户ID',
    
    -- 业务关联
    `biz_type` VARCHAR(64) NOT NULL COMMENT '业务类型：BILLING、ORDER、REFUND 等',
    `biz_id` VARCHAR(128) NOT NULL COMMENT '业务ID（订单号、账单号、退款单号）',
    
    -- 通知配置
    `template_code` VARCHAR(64) NOT NULL COMMENT '模板编码',
    `channel` VARCHAR(32) NOT NULL COMMENT '通知渠道：IN_APP、EMAIL、WECHAT、SMS',
    `priority` INT NOT NULL DEFAULT 50 COMMENT '优先级：数值越大优先级越高',
    
    -- 渲染数据
    `variables` JSON NOT NULL COMMENT '模板变量值：{"invoiceNo": "INV20251219001", "amount": "99.00"}',
    `title` VARCHAR(255) NULL COMMENT '已渲染标题（缓存）',
    `content` TEXT NULL COMMENT '已渲染内容（缓存）',
    
    -- 幂等性
    `idempotency_key` VARCHAR(255) NOT NULL COMMENT '幂等键：tenantId:bizType:bizId:channel',
    
    -- 状态与重试
    `status` VARCHAR(32) NOT NULL DEFAULT 'PENDING' COMMENT '状态：PENDING、SENDING、SENT、FAILED、RATE_LIMITED、CANCELLED',
    `retry_count` INT NOT NULL DEFAULT 0 COMMENT '已重试次数',
    `max_retry_count` INT NOT NULL DEFAULT 3 COMMENT '最大重试次数',
    `next_retry_at` DATETIME NULL COMMENT '下次重试时间（指数退避）',
    `last_error` TEXT NULL COMMENT '最后一次错误信息',
    
    -- 频控检查
    `rate_limit_checked_at` DATETIME NULL COMMENT '频控检查时间',
    `rate_limit_passed` TINYINT(1) NULL COMMENT '频控是否通过：1=通过，0=未通过',
    
    -- 审计字段
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `sent_at` DATETIME NULL COMMENT '发送成功时间',
    
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_idempotency_key` (`idempotency_key`),
    KEY `idx_status_next_retry` (`status`, `next_retry_at`),
    KEY `idx_user_biz` (`user_id`, `biz_type`, `biz_id`),
    KEY `idx_tenant_status` (`tenant_id`, `status`),
    KEY `idx_created_at` (`created_at`),
    KEY `idx_channel_status` (`channel`, `status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='通知任务表';

-- =====================================================
-- bc_notify_send_log: 通知发送日志表
-- 记录每次发送尝试，用于审计与回溯
-- =====================================================
CREATE TABLE IF NOT EXISTS `bc_notify_send_log` (
    `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `task_id` BIGINT UNSIGNED NOT NULL COMMENT '任务ID（关联 bc_notify_task.id）',
    `tenant_id` BIGINT UNSIGNED NULL COMMENT '租户ID（冗余）',
    `user_id` BIGINT UNSIGNED NOT NULL COMMENT '接收用户ID（冗余）',
    
    -- 发送信息
    `channel` VARCHAR(32) NOT NULL COMMENT '通知渠道',
    `biz_type` VARCHAR(64) NOT NULL COMMENT '业务类型（冗余）',
    `biz_id` VARCHAR(128) NOT NULL COMMENT '业务ID（冗余）',
    
    -- 发送内容
    `title` VARCHAR(255) NULL COMMENT '发送标题',
    `content` TEXT NULL COMMENT '发送内容',
    `recipient` VARCHAR(255) NULL COMMENT '接收方地址（邮箱/手机号/OpenID）',
    
    -- 发送结果
    `send_status` VARCHAR(32) NOT NULL COMMENT '发送状态：SUCCESS、FAILED、RATE_LIMITED',
    `error_code` VARCHAR(64) NULL COMMENT '错误码（失败时）',
    `error_message` TEXT NULL COMMENT '错误信息（失败时）',
    `provider_response` JSON NULL COMMENT '第三方渠道响应（邮件服务商/短信服务商）',
    
    -- 性能指标
    `send_duration_ms` INT NULL COMMENT '发送耗时（毫秒）',
    
    -- 审计字段
    `sent_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '发送时间',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    
    PRIMARY KEY (`id`),
    KEY `idx_task_id` (`task_id`),
    KEY `idx_user_channel` (`user_id`, `channel`, `sent_at`),
    KEY `idx_tenant_biz` (`tenant_id`, `biz_type`, `biz_id`),
    KEY `idx_send_status` (`send_status`, `sent_at`),
    KEY `idx_sent_at` (`sent_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='通知发送日志表';

-- =====================================================
-- bc_notify_user_preference: 用户通知偏好表（可选）
-- 用户级别的通知偏好与免打扰设置
-- =====================================================
CREATE TABLE IF NOT EXISTS `bc_notify_user_preference` (
    `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `tenant_id` BIGINT UNSIGNED NULL COMMENT '租户ID',
    `user_id` BIGINT UNSIGNED NOT NULL COMMENT '用户ID',
    
    -- 渠道开关
    `channel_preferences` JSON NOT NULL COMMENT '渠道偏好：{"IN_APP": true, "EMAIL": true, "WECHAT": false, "SMS": false}',
    
    -- 免打扰设置
    `quiet_hours_enabled` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否启用免打扰：1=启用，0=禁用',
    `quiet_hours_start` TIME NULL COMMENT '免打扰开始时间（如 22:00）',
    `quiet_hours_end` TIME NULL COMMENT '免打扰结束时间（如 08:00）',
    
    -- 业务类型订阅
    `subscribed_biz_types` JSON NULL COMMENT '订阅的业务类型：["BILLING", "ORDER"]（NULL=全部订阅）',
    
    -- 审计字段
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_id` (`user_id`),
    KEY `idx_tenant_id` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户通知偏好表';

-- =====================================================
-- 设计要点：
-- 
-- 1. bc_notify_template:
--    - template_code + channel + tenant_id 唯一索引，支持租户级模板覆盖
--    - template_variables 定义变量类型与描述，便于前端表单生成
--    - rate_limit_config 支持模板级频控覆盖
--    - status 字段支持模板上下线
-- 
-- 2. bc_notify_task:
--    - idempotency_key 唯一索引确保幂等性（tenantId:bizType:bizId:channel）
--    - status + next_retry_at 索引便于 dispatcher 扫描待发送任务
--    - title/content 缓存已渲染内容，避免重复渲染
--    - rate_limit_checked_at/rate_limit_passed 记录频控检查结果
--    - 支持指数退避重试（retry_count + next_retry_at）
-- 
-- 3. bc_notify_send_log:
--    - 记录每次发送尝试（成功/失败）
--    - provider_response 记录第三方渠道返回，便于排查问题
--    - send_duration_ms 记录发送耗时，用于性能监控
--    - user_id + channel + sent_at 索引支持用户维度频控查询
-- 
-- 4. bc_notify_user_preference:
--    - user_id 唯一索引，每个用户一条偏好记录
--    - channel_preferences 支持用户级渠道开关
--    - quiet_hours 支持用户自定义免打扰时间
--    - subscribed_biz_types 支持业务类型订阅（可选）
-- 
-- 5. 幂等性保证：
--    - notify_task.idempotency_key 唯一约束兜底
--    - NotifyTaskCreator 创建任务前检查 idempotency_key
--    - 同一业务事件同一渠道只生成一个任务
-- 
-- 6. 频控策略：
--    - 全局策略：NotificationPolicy 定义
--    - 模板级策略：bc_notify_template.rate_limit_config
--    - 用户级策略：bc_notify_user_preference.quiet_hours
--    - 查询 bc_notify_send_log 统计用户维度发送次数
-- 
-- 7. 审计与回溯：
--    - bc_notify_send_log 记录所有发送尝试
--    - 支持按 user/tenant/biz/channel 多维度查询
--    - 保留 provider_response 便于问题排查
-- =====================================================
