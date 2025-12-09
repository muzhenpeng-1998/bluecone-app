-- File: app-infra/src/main/resources/db/migration/V2025120901__create_webhook_config_table.sql
-- Webhook 配置表：用于租户配置事件回调 URL，支持签名验证

CREATE TABLE IF NOT EXISTS bc_webhook_config (
    id              BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    tenant_id       BIGINT       NOT NULL COMMENT '租户ID',
    event_type      VARCHAR(64)  NOT NULL COMMENT '事件类型：ORDER_SUBMITTED/PAYMENT_SUCCESS/ORDER_ACCEPTED 等',
    target_url      VARCHAR(512) NOT NULL COMMENT '回调URL',
    secret          VARCHAR(128) DEFAULT NULL COMMENT '签名密钥，用于生成签名头',
    enabled         TINYINT      NOT NULL DEFAULT 1 COMMENT '是否启用：1启用，0禁用',
    description     VARCHAR(256) DEFAULT NULL COMMENT '备注说明',
    created_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_tenant_event (tenant_id, event_type),
    INDEX idx_tenant_enabled (tenant_id, enabled)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Webhook 配置表';

-- 设计要点：
-- 1) uk_tenant_event 唯一索引确保每个租户每种事件类型只有一个配置（Phase 1 简化模型）。
-- 2) idx_tenant_enabled 便于快速查询租户的已启用 webhook 配置。
-- 3) secret 字段存储用于 HMAC-SHA256 签名的密钥，可选。
