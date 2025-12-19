-- ===================================================================
-- Migration: V20251218006__create_coupon_tables.sql
-- Description: 优惠券系统：模板/券实例/锁券/核销表
-- Author: System
-- Date: 2025-12-18
-- ===================================================================

-- 优惠券模板表
CREATE TABLE IF NOT EXISTS bc_coupon_template (
    id BIGINT NOT NULL PRIMARY KEY COMMENT '模板ID',
    tenant_id BIGINT NOT NULL COMMENT '租户ID',
    template_code VARCHAR(64) NOT NULL COMMENT '模板编码',
    template_name VARCHAR(128) NOT NULL COMMENT '模板名称',
    coupon_type VARCHAR(32) NOT NULL COMMENT '券类型：DISCOUNT_AMOUNT-满减券, DISCOUNT_RATE-折扣券',
    discount_amount DECIMAL(10, 2) DEFAULT NULL COMMENT '优惠金额（满减券）',
    discount_rate DECIMAL(5, 2) DEFAULT NULL COMMENT '折扣率（折扣券，如85表示85折）',
    min_order_amount DECIMAL(10, 2) NOT NULL DEFAULT 0 COMMENT '最低订单金额门槛',
    max_discount_amount DECIMAL(10, 2) DEFAULT NULL COMMENT '最高优惠金额（折扣券封顶）',
    applicable_scope VARCHAR(32) NOT NULL COMMENT '适用范围：ALL-全场, STORE-指定门店, SKU-指定商品, CATEGORY-指定分类',
    applicable_scope_ids TEXT DEFAULT NULL COMMENT '适用范围ID列表（JSON数组，如门店ID/商品ID/分类ID）',
    valid_days INT DEFAULT NULL COMMENT '有效天数（领取后多少天内有效，NULL表示使用固定时间范围）',
    valid_start_time DATETIME DEFAULT NULL COMMENT '固定有效期开始时间',
    valid_end_time DATETIME DEFAULT NULL COMMENT '固定有效期结束时间',
    total_quantity INT DEFAULT NULL COMMENT '总发行量（NULL表示不限量）',
    per_user_limit INT DEFAULT 1 COMMENT '每人限领数量',
    status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE' COMMENT '模板状态：ACTIVE-有效, INACTIVE-停用',
    description TEXT DEFAULT NULL COMMENT '券描述',
    terms_of_use TEXT DEFAULT NULL COMMENT '使用条款',
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
    INDEX idx_tenant_code (tenant_id, template_code),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='优惠券模板表';

-- 优惠券实例表
CREATE TABLE IF NOT EXISTS bc_coupon (
    id BIGINT NOT NULL PRIMARY KEY COMMENT '券ID',
    tenant_id BIGINT NOT NULL COMMENT '租户ID',
    template_id BIGINT NOT NULL COMMENT '模板ID',
    coupon_code VARCHAR(64) NOT NULL COMMENT '券码（唯一）',
    user_id BIGINT NOT NULL COMMENT '持有用户ID',
    coupon_type VARCHAR(32) NOT NULL COMMENT '券类型：DISCOUNT_AMOUNT-满减券, DISCOUNT_RATE-折扣券',
    discount_amount DECIMAL(10, 2) DEFAULT NULL COMMENT '优惠金额',
    discount_rate DECIMAL(5, 2) DEFAULT NULL COMMENT '折扣率',
    min_order_amount DECIMAL(10, 2) NOT NULL DEFAULT 0 COMMENT '最低订单金额门槛',
    max_discount_amount DECIMAL(10, 2) DEFAULT NULL COMMENT '最高优惠金额',
    applicable_scope VARCHAR(32) NOT NULL COMMENT '适用范围',
    applicable_scope_ids TEXT DEFAULT NULL COMMENT '适用范围ID列表（JSON）',
    valid_start_time DATETIME NOT NULL COMMENT '有效期开始时间',
    valid_end_time DATETIME NOT NULL COMMENT '有效期结束时间',
    status VARCHAR(32) NOT NULL DEFAULT 'ISSUED' COMMENT '券状态：ISSUED-已发放, LOCKED-已锁定, USED-已使用, EXPIRED-已过期',
    grant_time DATETIME NOT NULL COMMENT '发放时间',
    lock_time DATETIME DEFAULT NULL COMMENT '锁定时间',
    use_time DATETIME DEFAULT NULL COMMENT '使用时间',
    order_id BIGINT DEFAULT NULL COMMENT '关联订单ID',
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
    UNIQUE KEY uk_coupon_code (coupon_code),
    INDEX idx_tenant_user (tenant_id, user_id),
    INDEX idx_template (template_id),
    INDEX idx_status (status),
    INDEX idx_valid_end (valid_end_time),
    INDEX idx_order (order_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='优惠券实例表';

-- 优惠券锁定记录表
CREATE TABLE IF NOT EXISTS bc_coupon_lock (
    id BIGINT NOT NULL PRIMARY KEY COMMENT '锁定记录ID',
    tenant_id BIGINT NOT NULL COMMENT '租户ID',
    coupon_id BIGINT NOT NULL COMMENT '券ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    order_id BIGINT NOT NULL COMMENT '订单ID',
    idempotency_key VARCHAR(128) NOT NULL COMMENT '幂等键（如：orderId:couponId）',
    lock_status VARCHAR(32) NOT NULL DEFAULT 'LOCKED' COMMENT '锁定状态：LOCKED-已锁定, RELEASED-已释放, COMMITTED-已提交',
    lock_time DATETIME NOT NULL COMMENT '锁定时间',
    release_time DATETIME DEFAULT NULL COMMENT '释放时间',
    commit_time DATETIME DEFAULT NULL COMMENT '提交时间（核销）',
    expire_time DATETIME NOT NULL COMMENT '锁定过期时间（默认30分钟）',
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
    UNIQUE KEY uk_idempotency (idempotency_key),
    INDEX idx_tenant_coupon (tenant_id, coupon_id),
    INDEX idx_order (order_id),
    INDEX idx_status (lock_status),
    INDEX idx_expire (expire_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='优惠券锁定记录表';

-- 优惠券核销记录表
CREATE TABLE IF NOT EXISTS bc_coupon_redemption (
    id BIGINT NOT NULL PRIMARY KEY COMMENT '核销记录ID',
    tenant_id BIGINT NOT NULL COMMENT '租户ID',
    coupon_id BIGINT NOT NULL COMMENT '券ID',
    template_id BIGINT NOT NULL COMMENT '模板ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    order_id BIGINT NOT NULL COMMENT '订单ID',
    idempotency_key VARCHAR(128) NOT NULL COMMENT '幂等键（如：orderId:couponId）',
    original_amount DECIMAL(10, 2) NOT NULL COMMENT '原始订单金额',
    discount_amount DECIMAL(10, 2) NOT NULL COMMENT '实际优惠金额',
    final_amount DECIMAL(10, 2) NOT NULL COMMENT '最终订单金额',
    redemption_time DATETIME NOT NULL COMMENT '核销时间',
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
    UNIQUE KEY uk_idempotency (idempotency_key),
    INDEX idx_tenant_coupon (tenant_id, coupon_id),
    INDEX idx_tenant_user (tenant_id, user_id),
    INDEX idx_order (order_id),
    INDEX idx_redemption_time (redemption_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='优惠券核销记录表';
