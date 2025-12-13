-- File: docs/sql/bc_event_consume_record.sql
-- 事件消费去重表：用于记录每个 consumer_group 对每个 event_id 的消费状态
--
-- 设计目标：
-- 1) 保证同一 consumer_group + event_id 至多成功处理一次；
-- 2) 支持多实例并发，通过锁租约 (locked_until) 抢占处理权；
-- 3) 支持失败重试，通过 next_retry_at + retry_count 控制退避节奏。

CREATE TABLE IF NOT EXISTS bc_event_consume_record (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    tenant_id BIGINT NOT NULL COMMENT '租户 ID',
    consumer_group VARCHAR(64) NOT NULL COMMENT '消费者组，例如 ORDER/INVENTORY/PAYMENT',
    event_id BINARY(16) NOT NULL COMMENT '事件 ID（ULID128，对应 Outbox event_id）',
    event_type VARCHAR(128) NOT NULL COMMENT '事件类型，如 ORDER_CREATED',
    status TINYINT NOT NULL COMMENT '0=PROCESSING,1=SUCCEEDED,2=FAILED',
    locked_by VARCHAR(64) NOT NULL DEFAULT '' COMMENT '当前持有锁的实例标识',
    locked_until DATETIME NOT NULL COMMENT '锁租约到期时间，防止处理者崩溃卡死',
    next_retry_at DATETIME NOT NULL COMMENT '下次重试时间（失败时）',
    retry_count INT NOT NULL DEFAULT 0 COMMENT '已重试次数',
    error_msg VARCHAR(256) NULL COMMENT '最后一次失败原因（截断版）',
    processed_at DATETIME NULL COMMENT '成功处理时间',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_group_event (consumer_group, event_id),
    KEY idx_status_retry (status, next_retry_at),
    KEY idx_lock (locked_until),
    KEY idx_tenant_group (tenant_id, consumer_group, status, next_retry_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

