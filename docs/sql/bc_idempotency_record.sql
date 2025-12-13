-- 幂等记录表，用于跨模块复用的幂等控制基础设施
CREATE TABLE IF NOT EXISTS `bc_idempotency_record` (
  `id`           BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
  `tenant_id`    BIGINT       NOT NULL COMMENT '租户ID',
  `biz_type`     VARCHAR(32)  NOT NULL COMMENT '业务类型，例如 ORDER_CREATE / STORE_CREATE',
  `idem_key`     VARCHAR(128) NOT NULL COMMENT '幂等键（来自 Idempotency-Key 或业务生成）',
  `request_hash` CHAR(64)     NOT NULL COMMENT '请求摘要（SHA-256 HEX）',
  `status`       TINYINT      NOT NULL COMMENT '状态：0=PROCESSING,1=SUCCEEDED,2=FAILED',
  `result_ref`   VARCHAR(128) DEFAULT NULL COMMENT '结果引用（如 public_id）',
  `result_json`  MEDIUMTEXT   DEFAULT NULL COMMENT '结果 JSON（小体积，可选）',
  `error_code`   VARCHAR(64)  DEFAULT NULL COMMENT '错误码',
  `error_msg`    VARCHAR(256) DEFAULT NULL COMMENT '错误信息（简要）',
  `expire_at`    DATETIME     NOT NULL COMMENT '记录过期时间，过期后可重新执行',
  `lock_until`   DATETIME     NOT NULL COMMENT '执行租约到期时间，用于执行者崩溃后的夺回',
  `version`      INT          NOT NULL DEFAULT 0 COMMENT '版本号（预留给乐观锁）',
  `created_at`   DATETIME     NOT NULL COMMENT '创建时间',
  `updated_at`   DATETIME     NOT NULL COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_tenant_biz_key` (`tenant_id`, `biz_type`, `idem_key`),
  KEY `idx_tenant_expire` (`tenant_id`, `expire_at`),
  KEY `idx_status_lock` (`status`, `lock_until`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

