-- =====================================================
-- 订单计价快照表
-- 用于存储订单计价的完整快照，确保可追溯和可对账
-- =====================================================

CREATE TABLE IF NOT EXISTS `order_pricing_snapshot` (
    `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `tenant_id` BIGINT UNSIGNED NOT NULL COMMENT '租户ID',
    `order_id` BIGINT UNSIGNED NOT NULL COMMENT '订单ID',
    `quote_id` VARCHAR(64) NOT NULL COMMENT '报价单ID（用于幂等和追溯）',
    `pricing_version` VARCHAR(32) NOT NULL COMMENT '计价版本',
    
    -- 金额字段
    `original_amount` DECIMAL(10,2) NOT NULL DEFAULT 0.00 COMMENT '商品原价（基价+规格加价）',
    `member_discount_amount` DECIMAL(10,2) NOT NULL DEFAULT 0.00 COMMENT '会员优惠金额',
    `promo_discount_amount` DECIMAL(10,2) NOT NULL DEFAULT 0.00 COMMENT '活动优惠金额',
    `coupon_discount_amount` DECIMAL(10,2) NOT NULL DEFAULT 0.00 COMMENT '优惠券抵扣金额',
    `points_discount_amount` DECIMAL(10,2) NOT NULL DEFAULT 0.00 COMMENT '积分抵扣金额',
    `delivery_fee` DECIMAL(10,2) NOT NULL DEFAULT 0.00 COMMENT '配送费',
    `packing_fee` DECIMAL(10,2) NOT NULL DEFAULT 0.00 COMMENT '打包费',
    `rounding_amount` DECIMAL(10,2) NOT NULL DEFAULT 0.00 COMMENT '抹零金额',
    `payable_amount` DECIMAL(10,2) NOT NULL DEFAULT 0.00 COMMENT '应付金额（最终金额）',
    `currency` VARCHAR(8) NOT NULL DEFAULT 'CNY' COMMENT '币种',
    
    -- 优惠信息
    `applied_coupon_id` BIGINT UNSIGNED NULL COMMENT '使用的优惠券ID',
    `applied_points` INT UNSIGNED NULL COMMENT '使用的积分数量',
    
    -- 明细行（JSON格式）
    `breakdown_lines` JSON NOT NULL COMMENT '计价明细行列表（可解释）',
    
    -- 扩展信息
    `ext_info` JSON NULL COMMENT '扩展信息',
    
    -- 计价时间
    `pricing_time` DATETIME NOT NULL COMMENT '计价时间',
    
    -- 审计字段
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `created_by` BIGINT UNSIGNED NULL COMMENT '创建人ID',
    `updated_by` BIGINT UNSIGNED NULL COMMENT '更新人ID',
    `deleted` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '逻辑删除标记：0=未删除，1=已删除',
    
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_order_id` (`order_id`, `deleted`),
    UNIQUE KEY `uk_quote_id` (`quote_id`, `deleted`),
    KEY `idx_tenant_id` (`tenant_id`),
    KEY `idx_pricing_time` (`pricing_time`),
    KEY `idx_created_at` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='订单计价快照表';

-- =====================================================
-- 说明：
-- 1. 每个订单对应一条计价快照记录
-- 2. breakdown_lines 存储完整的计价明细行，确保可解释性
-- 3. pricing_version 用于版本控制，确保计价规则变更可追溯
-- 4. quote_id 用于幂等性保证，同一个 quote 只能被使用一次
-- 5. 所有金额字段都是 DECIMAL(10,2)，确保精度
-- =====================================================
