-- 订单履约流转 M3：补充履约时间戳字段
-- 创建时间：2025-12-18
-- 说明：商户端制作流程（ACCEPTED → IN_PROGRESS → READY → COMPLETED）需要的字段扩展

-- ============================================================
-- 1. 补充订单主表字段：履约流转时间戳
-- ============================================================
-- 为订单主表添加履约相关时间字段（MySQL 不支持 IF NOT EXISTS，使用存储过程实现）

-- 检查并添加 started_at 字段（开始制作时间）
SET @col_exists = 0;
SELECT COUNT(*) INTO @col_exists 
FROM information_schema.COLUMNS 
WHERE TABLE_SCHEMA = DATABASE() 
  AND TABLE_NAME = 'bc_order' 
  AND COLUMN_NAME = 'started_at';

SET @sql = IF(@col_exists = 0, 
    'ALTER TABLE bc_order ADD COLUMN started_at DATETIME NULL COMMENT ''开始制作时间（ACCEPTED → IN_PROGRESS）'' AFTER accepted_at', 
    'SELECT ''Column started_at already exists'' AS msg');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 检查并添加 ready_at 字段（出餐/可取时间）
SET @col_exists = 0;
SELECT COUNT(*) INTO @col_exists 
FROM information_schema.COLUMNS 
WHERE TABLE_SCHEMA = DATABASE() 
  AND TABLE_NAME = 'bc_order' 
  AND COLUMN_NAME = 'ready_at';

SET @sql = IF(@col_exists = 0, 
    'ALTER TABLE bc_order ADD COLUMN ready_at DATETIME NULL COMMENT ''出餐/可取时间（IN_PROGRESS → READY）'' AFTER started_at', 
    'SELECT ''Column ready_at already exists'' AS msg');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 检查并添加 completed_at 字段（完成时间）
SET @col_exists = 0;
SELECT COUNT(*) INTO @col_exists 
FROM information_schema.COLUMNS 
WHERE TABLE_SCHEMA = DATABASE() 
  AND TABLE_NAME = 'bc_order' 
  AND COLUMN_NAME = 'completed_at';

SET @sql = IF(@col_exists = 0, 
    'ALTER TABLE bc_order ADD COLUMN completed_at DATETIME NULL COMMENT ''完成时间（READY → COMPLETED）'' AFTER ready_at', 
    'SELECT ''Column completed_at already exists'' AS msg');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 检查并添加 last_state_changed_at 字段（最近一次状态变化时间，用于SLA/统计）
SET @col_exists = 0;
SELECT COUNT(*) INTO @col_exists 
FROM information_schema.COLUMNS 
WHERE TABLE_SCHEMA = DATABASE() 
  AND TABLE_NAME = 'bc_order' 
  AND COLUMN_NAME = 'last_state_changed_at';

SET @sql = IF(@col_exists = 0, 
    'ALTER TABLE bc_order ADD COLUMN last_state_changed_at DATETIME NULL COMMENT ''最近一次状态变化时间（用于SLA统计和超时判断）'' AFTER completed_at', 
    'SELECT ''Column last_state_changed_at already exists'' AS msg');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 检查并添加 operator_id 字段（最近操作人，复用于各个履约环节）
SET @col_exists = 0;
SELECT COUNT(*) INTO @col_exists 
FROM information_schema.COLUMNS 
WHERE TABLE_SCHEMA = DATABASE() 
  AND TABLE_NAME = 'bc_order' 
  AND COLUMN_NAME = 'operator_id';

SET @sql = IF(@col_exists = 0, 
    'ALTER TABLE bc_order ADD COLUMN operator_id BIGINT NULL COMMENT ''最近操作人ID（接单/开始/出餐/完成等操作的操作人）'' AFTER last_state_changed_at', 
    'SELECT ''Column operator_id already exists'' AS msg');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- ============================================================
-- 2. 为超时自动取消功能添加索引
-- ============================================================
-- 用于快速查询待接单超时订单：WHERE status='WAIT_ACCEPT' AND last_state_changed_at < ?

-- 检查并创建索引（用于超时扫描）
SET @index_exists = 0;
SELECT COUNT(*) INTO @index_exists 
FROM information_schema.STATISTICS 
WHERE TABLE_SCHEMA = DATABASE() 
  AND TABLE_NAME = 'bc_order' 
  AND INDEX_NAME = 'idx_tenant_status_state_time';

SET @sql = IF(@index_exists = 0, 
    'CREATE INDEX idx_tenant_status_state_time ON bc_order (tenant_id, status, last_state_changed_at)', 
    'SELECT ''Index idx_tenant_status_state_time already exists'' AS msg');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- ============================================================
-- 说明：
-- 1. started_at：商户点击"开始制作"时记录，状态从 ACCEPTED 流转到 IN_PROGRESS
-- 2. ready_at：商户点击"出餐完成"时记录，状态从 IN_PROGRESS 流转到 READY
-- 3. completed_at：用户取餐/配送完成时记录，状态从 READY 流转到 COMPLETED
-- 4. last_state_changed_at：每次状态变化都更新，用于 SLA 统计和超时判断
-- 5. operator_id：记录最近一次操作人，便于审计和追溯
-- 6. 幂等动作表 bc_order_action_log 已在 M2 创建，M3 扩展 action_type 枚举即可
-- ============================================================
