-- Integration Hub 基础表：订阅 + 投递任务

CREATE TABLE IF NOT EXISTS bc_integration_subscription (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键',
    tenant_id BIGINT NOT NULL COMMENT '租户 ID，0 表示平台级订阅',
    event_type VARCHAR(128) NOT NULL COMMENT '事件类型，如 order.paid',
    channel_type VARCHAR(32) NOT NULL COMMENT '通道类型：WEBHOOK / WECHAT_BOT / INTERNAL_HTTP / ...',
    target_url VARCHAR(512) NULL COMMENT '目标地址：Webhook URL 或机器人 Webhook',
    secret VARCHAR(255) NULL COMMENT '签名密钥（HMAC 等）',
    enabled TINYINT NOT NULL DEFAULT 1 COMMENT '是否启用',
    max_retry INT NOT NULL DEFAULT 5 COMMENT '最大重试次数',
    timeout_ms INT NOT NULL DEFAULT 3000 COMMENT '调用超时时间（毫秒）',
    rate_limit_qps INT NULL COMMENT '每秒最大调用数，可为空',
    headers JSON NULL COMMENT '额外 HTTP 头配置（key-value）',
    extra_config JSON NULL COMMENT '通道特定扩展配置（如模板、关键字等）',
    remark VARCHAR(255) NULL COMMENT '备注',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_tenant_event_channel (tenant_id, event_type, channel_type),
    INDEX idx_tenant_event (tenant_id, event_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Integration Hub 订阅配置';

CREATE TABLE IF NOT EXISTS bc_integration_delivery (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键',
    subscription_id BIGINT NOT NULL COMMENT '关联 bc_integration_subscription.id',
    tenant_id BIGINT NOT NULL COMMENT '租户 ID',
    event_id VARCHAR(64) NOT NULL COMMENT '领域事件 ID，如 DomainEvent.eventId',
    event_type VARCHAR(128) NOT NULL COMMENT '事件类型',
    payload JSON NOT NULL COMMENT '事件载荷快照（避免后续变更影响）',
    headers JSON NULL COMMENT '携带到通道的头信息（traceId, tenantId, userId 等）',
    channel_type VARCHAR(32) NOT NULL COMMENT '通道类型',
    status VARCHAR(32) NOT NULL COMMENT 'NEW / SENDING / SUCCESS / FAILED / DEAD',
    retry_count INT NOT NULL DEFAULT 0 COMMENT '已重试次数',
    next_retry_at TIMESTAMP NULL COMMENT '下一次重试时间',
    last_error VARCHAR(1024) NULL COMMENT '最后一次错误信息',
    last_http_status INT NULL COMMENT '最后一次 HTTP 响应码（Webhook 通道）',
    last_duration_ms INT NULL COMMENT '最后一次耗时（毫秒）',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_event_subscription (event_id, subscription_id),
    INDEX idx_status_next_retry (status, next_retry_at),
    INDEX idx_tenant_event (tenant_id, event_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Integration Hub 投递任务记录';

-- 设计要点：
-- 1) subscription 支持多租户（tenant_id=0 为平台级），事件+通道唯一。
-- 2) delivery 记录每次投递的状态机与重试信息，幂等键 event_id+subscription_id。
-- 3) JSON 字段存储 payload/headers/extra 配置，保持 schema 灵活性。
