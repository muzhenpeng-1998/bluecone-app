-- Migration: add internal_id & public_id to bc_tenant
-- 1) internal_id: 内部主键 ULID128，对应应用层 Ulid128/BINARY(16)
-- 2) public_id: 对外 ID（PublicId，prefix_ulid，格式：tnt_01HN8X5K9G3QRST2VW4XYZ）
-- 3) 为 internal_id 建唯一索引；为 public_id 建唯一索引（租户表不需要 tenant_id 约束）

ALTER TABLE bc_tenant
    ADD COLUMN internal_id BINARY(16) NULL COMMENT '内部主键 ULID128',
    ADD COLUMN public_id   VARCHAR(40) NULL COMMENT '对外ID prefix_ulid',
    ADD UNIQUE KEY uk_bc_tenant_internal_id (internal_id),
    ADD UNIQUE KEY uk_bc_tenant_public_id (public_id);

