-- File: docs/sql/bc_outbox_event.sql
-- Outbox 事件表（规范版）：用于事务内写入、事务后异步投递，支持重试/幂等/多实例竞争
--
-- 说明：
-- 1) 当前代码实现使用表 bc_outbox_message，字段语义与本表基本一致；
--    未来演进时可以将 bc_outbox_message 迁移/重命名为 bc_outbox_event，或直接按本 DDL 新建表。
-- 2) event_id / aggregate_id 推荐使用 ULID128（BINARY(16)），与 app-id 子模块保持一致。

CREATE TABLE IF NOT EXISTS bc_outbox_event (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    tenant_id BIGINT NOT NULL COMMENT '租户 ID',
    event_id BINARY(16) NOT NULL COMMENT '内部事件 ID（ULID128）',
    event_type VARCHAR(128) NOT NULL COMMENT '事件类型，如 ORDER_CREATED',
    aggregate_type VARCHAR(64) NOT NULL COMMENT '聚合类型，如 ORDER / STORE',
    aggregate_id BINARY(16) NULL COMMENT '聚合内部 ID（ULID128，可为空）',
    public_aggregate_id VARCHAR(64) NULL COMMENT '聚合对外 public_id，便于跨系统追踪',
    payload_json MEDIUMTEXT NOT NULL COMMENT '事件载荷 JSON',
    headers_json TEXT NULL COMMENT '事件头 JSON（traceId/requestId/operator 等）',
    status TINYINT NOT NULL COMMENT '0=NEW,1=PROCESSING,2=SENT,3=FAILED',
    locked_by VARCHAR(64) NOT NULL DEFAULT '' COMMENT '当前持有租约的实例标识',
    locked_until DATETIME NOT NULL COMMENT '租约到期时间，防止实例崩溃卡死',
    next_retry_at DATETIME NOT NULL COMMENT '下一次可重试时间',
    retry_count INT NOT NULL DEFAULT 0 COMMENT '已重试次数',
    error_msg VARCHAR(256) NULL COMMENT '最后一次失败原因（截断版）',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_event_id (event_id),
    KEY idx_status_retry (status, next_retry_at),
    KEY idx_lock (locked_until),
    KEY idx_tenant_status (tenant_id, status, next_retry_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

