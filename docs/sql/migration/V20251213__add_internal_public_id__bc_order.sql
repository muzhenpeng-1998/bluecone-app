-- Migration: add internal_id & public_id to bc_order
-- 1) internal_id: 内部主键 ULID128，对应应用层 Ulid128/BINARY(16)
-- 2) public_id: 对外 ID（PublicId，prefix_ulid）
-- 3) 为 internal_id 建唯一索引；为 (tenant_id, public_id) 建唯一索引以支持按租户约束对外 ID 唯一性

ALTER TABLE bc_order
    ADD COLUMN internal_id BINARY(16) NULL COMMENT '内部主键 ULID128',
    ADD COLUMN public_id   VARCHAR(40) NULL COMMENT '对外ID prefix_ulid',
    ADD UNIQUE KEY uk_bc_order_internal_id (internal_id),
    ADD UNIQUE KEY uk_bc_order_tenant_public_id (tenant_id, public_id);

