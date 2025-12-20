-- 为订单表添加优惠券ID字段
-- 创建时间：2025-12-20
-- 说明：支持订单关联优惠券功能

-- ============================================================
-- 为 bc_order 表添加 coupon_id 字段
-- ============================================================

-- 检查字段是否已存在，如果不存在则添加
SET @column_exists = (
    SELECT COUNT(*) 
    FROM INFORMATION_SCHEMA.COLUMNS 
    WHERE TABLE_SCHEMA = DATABASE() 
      AND TABLE_NAME = 'bc_order' 
      AND COLUMN_NAME = 'coupon_id'
);

SET @sql = IF(@column_exists = 0, 
    'ALTER TABLE bc_order ADD COLUMN coupon_id BIGINT DEFAULT NULL COMMENT ''使用的优惠券ID'' AFTER currency', 
    'SELECT ''Column coupon_id already exists'' AS status'
);

PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- ============================================================
-- 添加索引以提高查询性能
-- ============================================================

-- 为 coupon_id 添加索引（用于查询使用了某张优惠券的订单）
SET @index_exists = (
    SELECT COUNT(*) 
    FROM INFORMATION_SCHEMA.STATISTICS 
    WHERE TABLE_SCHEMA = DATABASE() 
      AND TABLE_NAME = 'bc_order' 
      AND INDEX_NAME = 'idx_tenant_coupon'
);

SET @sql = IF(@index_exists = 0, 
    'CREATE INDEX idx_tenant_coupon ON bc_order (tenant_id, coupon_id)', 
    'SELECT ''Index idx_tenant_coupon already exists'' AS status'
);

PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

