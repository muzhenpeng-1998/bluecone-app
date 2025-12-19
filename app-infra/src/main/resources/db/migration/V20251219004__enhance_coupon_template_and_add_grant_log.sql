-- ===================================================================
-- Migration: V20251219004__enhance_coupon_template_and_add_grant_log.sql
-- Description: 增强优惠券模板（配额/版本控制）+ 新增发券日志表（幂等）
-- Author: System
-- Date: 2025-12-19
-- ===================================================================

-- 1. 增强 bc_coupon_template：添加 issued_count（已发放数）和 version（乐观锁）
ALTER TABLE bc_coupon_template
    ADD COLUMN issued_count INT NOT NULL DEFAULT 0 COMMENT '已发放数量（用于配额控制）' AFTER per_user_limit,
    ADD COLUMN version INT NOT NULL DEFAULT 0 COMMENT '版本号（乐观锁）' AFTER issued_count;

-- 2. 修改 status 字段注释，支持 DRAFT/ONLINE/OFFLINE 三种状态
ALTER TABLE bc_coupon_template
    MODIFY COLUMN status VARCHAR(32) NOT NULL DEFAULT 'DRAFT' COMMENT '模板状态：DRAFT-草稿, ONLINE-上线, OFFLINE-下线';

-- 3. 创建优惠券发放日志表（Grant Log）
CREATE TABLE IF NOT EXISTS bc_coupon_grant_log (
    id BIGINT NOT NULL PRIMARY KEY COMMENT '发放日志ID',
    tenant_id BIGINT NOT NULL COMMENT '租户ID',
    template_id BIGINT NOT NULL COMMENT '模板ID',
    idempotency_key VARCHAR(128) NOT NULL COMMENT '幂等键（确保同一发券请求不重复）',
    user_id BIGINT NOT NULL COMMENT '领取用户ID',
    coupon_id BIGINT DEFAULT NULL COMMENT '生成的券ID（成功后回填）',
    grant_source VARCHAR(32) NOT NULL COMMENT '发放来源：MANUAL_ADMIN-管理员手动, CAMPAIGN-营销活动, REGISTER-注册赠送',
    grant_status VARCHAR(32) NOT NULL DEFAULT 'PROCESSING' COMMENT '发放状态：PROCESSING-处理中, SUCCESS-成功, FAILED-失败',
    operator_id BIGINT DEFAULT NULL COMMENT '操作人ID（管理员发券时）',
    operator_name VARCHAR(64) DEFAULT NULL COMMENT '操作人名称',
    batch_no VARCHAR(64) DEFAULT NULL COMMENT '批次号（批量发券时）',
    grant_reason TEXT DEFAULT NULL COMMENT '发放原因/备注',
    error_code VARCHAR(64) DEFAULT NULL COMMENT '失败错误码',
    error_message TEXT DEFAULT NULL COMMENT '失败错误信息',
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
    UNIQUE KEY uk_tenant_idempotency (tenant_id, idempotency_key),
    INDEX idx_tenant_template (tenant_id, template_id),
    INDEX idx_tenant_user (tenant_id, user_id),
    INDEX idx_coupon (coupon_id),
    INDEX idx_batch (batch_no),
    INDEX idx_status (grant_status),
    INDEX idx_created (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='优惠券发放日志表';

-- 4. 为 bc_coupon 表添加 grant_log_id 字段（关联发放日志）
ALTER TABLE bc_coupon
    ADD COLUMN grant_log_id BIGINT DEFAULT NULL COMMENT '关联的发放日志ID' AFTER template_id,
    ADD INDEX idx_grant_log (grant_log_id);

-- 5. 为 bc_coupon_template 添加索引，优化配额查询
ALTER TABLE bc_coupon_template
    ADD INDEX idx_tenant_status (tenant_id, status);
