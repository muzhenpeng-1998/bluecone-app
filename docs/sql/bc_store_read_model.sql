-- Read model table for store creation demo.
-- 仅用于演示 Outbox + EventHandlerTemplate + 消费幂等的最小链路。

CREATE TABLE IF NOT EXISTS bc_store_read_model (
    store_internal_id BINARY(16)    NOT NULL COMMENT '门店内部主键（ULID128/BINARY(16)）',
    public_id         VARCHAR(40)   NOT NULL COMMENT '对外门店 ID（sto_{ulid}）',
    store_no          BIGINT UNSIGNED NULL COMMENT '门店数字编号（Snowflake long）',
    tenant_id         BIGINT        NOT NULL COMMENT '租户 ID',
    store_name        VARCHAR(255)  NOT NULL COMMENT '门店名称',
    updated_at        DATETIME      NOT NULL COMMENT '最近一次快照更新时间',
    PRIMARY KEY (store_internal_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='门店只读快照（消费端幂等演示）';

