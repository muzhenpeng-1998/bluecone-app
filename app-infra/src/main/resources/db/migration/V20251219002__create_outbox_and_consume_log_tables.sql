-- =====================================================
-- Outbox 事件表 + 消费日志表
-- 用于实现可恢复的事件驱动一致性机制
-- =====================================================

-- =====================================================
-- bc_outbox_event: Outbox 事件表
-- 用于事务内写入、事务后异步投递，支持重试/幂等/状态机流转
-- =====================================================
CREATE TABLE IF NOT EXISTS `bc_outbox_event` (
    `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `tenant_id` BIGINT UNSIGNED NULL COMMENT '租户ID（多租户隔离）',
    `store_id` BIGINT UNSIGNED NULL COMMENT '门店ID（业务隔离）',
    
    -- 聚合根信息
    `aggregate_type` VARCHAR(64) NOT NULL COMMENT '聚合根类型：ORDER/PAYMENT/REFUND 等',
    `aggregate_id` VARCHAR(128) NOT NULL COMMENT '聚合根ID（订单ID、支付单ID等）',
    
    -- 事件信息
    `event_type` VARCHAR(128) NOT NULL COMMENT '事件类型：order.checkout_locked/order.paid/order.canceled/order.refunded 等',
    `event_id` VARCHAR(64) NOT NULL COMMENT '事件唯一ID（UUID/ULID）',
    `event_payload` JSON NOT NULL COMMENT '事件载荷（序列化后的事件对象）',
    `event_metadata` JSON NULL COMMENT '事件元数据（traceId/requestId/userId 等）',
    
    -- 状态与重试
    `status` VARCHAR(32) NOT NULL DEFAULT 'NEW' COMMENT '状态：NEW/SENT/FAILED/DEAD',
    `retry_count` INT NOT NULL DEFAULT 0 COMMENT '已重试次数',
    `max_retry_count` INT NOT NULL DEFAULT 10 COMMENT '最大重试次数',
    `next_retry_at` DATETIME NULL COMMENT '下次重试时间（指数退避）',
    `last_error` TEXT NULL COMMENT '最后一次错误信息',
    
    -- 审计字段
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `sent_at` DATETIME NULL COMMENT '投递成功时间',
    
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_event_id` (`event_id`),
    KEY `idx_status_next_retry` (`status`, `next_retry_at`),
    KEY `idx_aggregate` (`aggregate_type`, `aggregate_id`),
    KEY `idx_tenant_status` (`tenant_id`, `status`),
    KEY `idx_created_at` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Outbox 事件表';

-- =====================================================
-- bc_event_consume_log: 事件消费日志表
-- 用于消费者幂等性保证：consumer_name + event_id 唯一
-- =====================================================
CREATE TABLE IF NOT EXISTS `bc_event_consume_log` (
    `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `tenant_id` BIGINT UNSIGNED NULL COMMENT '租户ID（多租户隔离）',
    
    -- 消费者与事件
    `consumer_name` VARCHAR(128) NOT NULL COMMENT '消费者名称：CouponConsumer/WalletConsumer/PointsConsumer',
    `event_id` VARCHAR(64) NOT NULL COMMENT '事件唯一ID（对应 bc_outbox_event.event_id）',
    `event_type` VARCHAR(128) NOT NULL COMMENT '事件类型（冗余，便于查询）',
    
    -- 消费状态
    `status` VARCHAR(32) NOT NULL DEFAULT 'SUCCESS' COMMENT '消费状态：SUCCESS/FAILED/PROCESSING',
    `idempotency_key` VARCHAR(255) NULL COMMENT '幂等键（业务侧幂等键，如 orderId:checkout）',
    
    -- 消费结果
    `consume_result` JSON NULL COMMENT '消费结果（业务返回的结果对象）',
    `error_message` TEXT NULL COMMENT '错误信息（消费失败时记录）',
    
    -- 审计字段
    `consumed_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '消费时间',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_consumer_event` (`consumer_name`, `event_id`),
    KEY `idx_event_id` (`event_id`),
    KEY `idx_tenant_consumer` (`tenant_id`, `consumer_name`),
    KEY `idx_idempotency_key` (`idempotency_key`),
    KEY `idx_consumed_at` (`consumed_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='事件消费日志表（幂等性保证）';

-- =====================================================
-- 设计要点：
-- 
-- bc_outbox_event:
-- 1. event_id 唯一索引确保事件不重复写入
-- 2. status + next_retry_at 索引便于扫描待投递/待重试任务
-- 3. aggregate_type + aggregate_id 索引便于按聚合根查询事件
-- 4. JSON 字段存储灵活载荷与元数据，适配多事件 schema
-- 5. 状态机：NEW → SENT（成功）或 FAILED（失败重试）或 DEAD（超过最大重试次数）
-- 
-- bc_event_consume_log:
-- 1. consumer_name + event_id 唯一索引确保消费幂等性
-- 2. idempotency_key 索引便于业务侧快速查询是否已消费
-- 3. 消费成功后写入 SUCCESS 记录，重复消费时直接返回
-- 4. 消费失败时写入 FAILED 记录，由 Outbox 重试机制驱动重新消费
-- 5. PROCESSING 状态用于分布式锁场景，防止并发消费
-- =====================================================
