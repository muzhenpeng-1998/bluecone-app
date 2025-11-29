-- File: app-infra/src/main/resources/db/migration/V2024__create_bc_outbox_message.sql
-- Outbox 表：用于事务内写入、事务后异步投递，支持重试/幂等/状态机流转

CREATE TABLE IF NOT EXISTS bc_outbox_message (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    event_type VARCHAR(255) NOT NULL COMMENT '领域事件类型，如 order.paid',
    event_key VARCHAR(255) NOT NULL COMMENT '业务幂等键，用于防重复发布',
    payload JSON NOT NULL COMMENT '事件载荷，序列化后的 DomainEvent',
    headers JSON NULL COMMENT '元数据/扩展头，traceId/tenantId 等',
    tenant_id BIGINT NULL COMMENT '租户 ID，便于多租户隔离与清理',
    status VARCHAR(32) NOT NULL COMMENT 'NEW / DONE / FAILED / DEAD',
    retry_count INT NOT NULL DEFAULT 0 COMMENT '已重试次数',
    next_retry_at TIMESTAMP NULL COMMENT '下一次重试时间',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_event_key (event_key),
    INDEX idx_status_next_retry (status, next_retry_at),
    INDEX idx_tenant_status (tenant_id, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 设计要点：
-- 1) event_key 唯一索引确保业务幂等。
-- 2) status + next_retry_at 便于扫描待投递/待重试任务。
-- 3) JSON 字段存储灵活载荷与头信息，适配多事件 schema。
