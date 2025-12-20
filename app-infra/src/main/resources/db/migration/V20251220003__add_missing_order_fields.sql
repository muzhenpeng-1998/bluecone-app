-- 为订单表添加所有缺失的字段
-- 创建时间：2025-12-20
-- 说明：确保所有 OrderPO 中定义的字段都存在于数据库表中

-- ============================================================
-- 1. 添加接单相关字段
-- ============================================================

-- reject_reason_code
SET @column_exists = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'bc_order' AND COLUMN_NAME = 'reject_reason_code');
SET @sql = IF(@column_exists = 0, 'ALTER TABLE bc_order ADD COLUMN reject_reason_code VARCHAR(64) NULL COMMENT ''拒单原因码'' AFTER accepted_at', 'SELECT ''Column reject_reason_code already exists'' AS status');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- reject_reason_desc
SET @column_exists = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'bc_order' AND COLUMN_NAME = 'reject_reason_desc');
SET @sql = IF(@column_exists = 0, 'ALTER TABLE bc_order ADD COLUMN reject_reason_desc VARCHAR(256) NULL COMMENT ''拒单原因描述'' AFTER reject_reason_code', 'SELECT ''Column reject_reason_desc already exists'' AS status');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- rejected_at
SET @column_exists = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'bc_order' AND COLUMN_NAME = 'rejected_at');
SET @sql = IF(@column_exists = 0, 'ALTER TABLE bc_order ADD COLUMN rejected_at DATETIME NULL COMMENT ''拒单时间'' AFTER reject_reason_desc', 'SELECT ''Column rejected_at already exists'' AS status');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- rejected_by
SET @column_exists = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'bc_order' AND COLUMN_NAME = 'rejected_by');
SET @sql = IF(@column_exists = 0, 'ALTER TABLE bc_order ADD COLUMN rejected_by BIGINT NULL COMMENT ''拒单操作人'' AFTER rejected_at', 'SELECT ''Column rejected_by already exists'' AS status');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- ============================================================
-- 2. 添加用户软删除字段
-- ============================================================

-- user_deleted
SET @column_exists = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'bc_order' AND COLUMN_NAME = 'user_deleted');
SET @sql = IF(@column_exists = 0, 'ALTER TABLE bc_order ADD COLUMN user_deleted TINYINT(1) DEFAULT 0 COMMENT ''用户是否删除：0=否，1=是'' AFTER rejected_by', 'SELECT ''Column user_deleted already exists'' AS status');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- user_deleted_at
SET @column_exists = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'bc_order' AND COLUMN_NAME = 'user_deleted_at');
SET @sql = IF(@column_exists = 0, 'ALTER TABLE bc_order ADD COLUMN user_deleted_at DATETIME DEFAULT NULL COMMENT ''用户删除时间'' AFTER user_deleted', 'SELECT ''Column user_deleted_at already exists'' AS status');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- ============================================================
-- 3. 添加关单字段
-- ============================================================

-- close_reason
SET @column_exists = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'bc_order' AND COLUMN_NAME = 'close_reason');
SET @sql = IF(@column_exists = 0, 'ALTER TABLE bc_order ADD COLUMN close_reason VARCHAR(64) NULL COMMENT ''关单原因'' AFTER user_deleted_at', 'SELECT ''Column close_reason already exists'' AS status');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- closed_at
SET @column_exists = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'bc_order' AND COLUMN_NAME = 'closed_at');
SET @sql = IF(@column_exists = 0, 'ALTER TABLE bc_order ADD COLUMN closed_at DATETIME NULL COMMENT ''关单时间'' AFTER close_reason', 'SELECT ''Column closed_at already exists'' AS status');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- ============================================================
-- 4. 添加履约流转字段
-- ============================================================

-- started_at
SET @column_exists = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'bc_order' AND COLUMN_NAME = 'started_at');
SET @sql = IF(@column_exists = 0, 'ALTER TABLE bc_order ADD COLUMN started_at DATETIME NULL COMMENT ''开始制作时间'' AFTER closed_at', 'SELECT ''Column started_at already exists'' AS status');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- ready_at
SET @column_exists = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'bc_order' AND COLUMN_NAME = 'ready_at');
SET @sql = IF(@column_exists = 0, 'ALTER TABLE bc_order ADD COLUMN ready_at DATETIME NULL COMMENT ''出餐时间'' AFTER started_at', 'SELECT ''Column ready_at already exists'' AS status');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- completed_at
SET @column_exists = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'bc_order' AND COLUMN_NAME = 'completed_at');
SET @sql = IF(@column_exists = 0, 'ALTER TABLE bc_order ADD COLUMN completed_at DATETIME NULL COMMENT ''完成时间'' AFTER ready_at', 'SELECT ''Column completed_at already exists'' AS status');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- last_state_changed_at
SET @column_exists = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'bc_order' AND COLUMN_NAME = 'last_state_changed_at');
SET @sql = IF(@column_exists = 0, 'ALTER TABLE bc_order ADD COLUMN last_state_changed_at DATETIME NULL COMMENT ''最近状态变化时间'' AFTER completed_at', 'SELECT ''Column last_state_changed_at already exists'' AS status');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- operator_id
SET @column_exists = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'bc_order' AND COLUMN_NAME = 'operator_id');
SET @sql = IF(@column_exists = 0, 'ALTER TABLE bc_order ADD COLUMN operator_id BIGINT NULL COMMENT ''最近操作人ID'' AFTER last_state_changed_at', 'SELECT ''Column operator_id already exists'' AS status');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- ============================================================
-- 5. 添加取消/退款字段
-- ============================================================

-- canceled_at
SET @column_exists = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'bc_order' AND COLUMN_NAME = 'canceled_at');
SET @sql = IF(@column_exists = 0, 'ALTER TABLE bc_order ADD COLUMN canceled_at DATETIME NULL COMMENT ''取消时间'' AFTER operator_id', 'SELECT ''Column canceled_at already exists'' AS status');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- cancel_reason_code
SET @column_exists = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'bc_order' AND COLUMN_NAME = 'cancel_reason_code');
SET @sql = IF(@column_exists = 0, 'ALTER TABLE bc_order ADD COLUMN cancel_reason_code VARCHAR(64) NULL COMMENT ''取消原因码'' AFTER canceled_at', 'SELECT ''Column cancel_reason_code already exists'' AS status');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- cancel_reason_desc
SET @column_exists = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'bc_order' AND COLUMN_NAME = 'cancel_reason_desc');
SET @sql = IF(@column_exists = 0, 'ALTER TABLE bc_order ADD COLUMN cancel_reason_desc VARCHAR(256) NULL COMMENT ''取消原因描述'' AFTER cancel_reason_code', 'SELECT ''Column cancel_reason_desc already exists'' AS status');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- refunded_at
SET @column_exists = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'bc_order' AND COLUMN_NAME = 'refunded_at');
SET @sql = IF(@column_exists = 0, 'ALTER TABLE bc_order ADD COLUMN refunded_at DATETIME NULL COMMENT ''退款时间'' AFTER cancel_reason_desc', 'SELECT ''Column refunded_at already exists'' AS status');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- refund_order_id
SET @column_exists = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'bc_order' AND COLUMN_NAME = 'refund_order_id');
SET @sql = IF(@column_exists = 0, 'ALTER TABLE bc_order ADD COLUMN refund_order_id BIGINT NULL COMMENT ''退款单ID'' AFTER refunded_at', 'SELECT ''Column refund_order_id already exists'' AS status');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

