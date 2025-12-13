-- 公共 ID 映射表（public_id -> internal_id）
CREATE TABLE IF NOT EXISTS bc_public_id_map (
  id BIGINT NOT NULL AUTO_INCREMENT COMMENT '自增主键',
  tenant_id BIGINT NOT NULL COMMENT '租户ID',
  resource_type VARCHAR(32) NOT NULL COMMENT '资源类型：TENANT/STORE/ORDER/USER/PRODUCT等',
  public_id VARCHAR(64) NOT NULL COMMENT '对外ID，prefix_ulid',
  internal_id BINARY(16) NOT NULL COMMENT '内部ID，ULID128',
  status TINYINT NOT NULL DEFAULT 1 COMMENT '1有效 0无效（预留）',
  created_at DATETIME NOT NULL,
  updated_at DATETIME NOT NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uk_tenant_type_public (tenant_id, resource_type, public_id),
  UNIQUE KEY uk_tenant_type_internal (tenant_id, resource_type, internal_id),
  KEY idx_tenant_type (tenant_id, resource_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='公共ID映射表（public_id->internal_id）';

