-- 订单接单 M2：补充拒单字段和幂等动作表
-- 创建时间：2025-12-18
-- 说明：商户接单/拒单需要的字段扩展和幂等保护

-- ============================================================
-- 1. 补充订单主表字段：拒单原因
-- ============================================================
-- 为订单主表添加拒单相关字段（MySQL 不支持 IF NOT EXISTS，使用存储过程实现）

-- 检查并添加 accept_operator_id 字段（如果不存在）
SET @col_exists = 0;
SELECT COUNT(*) INTO @col_exists 
FROM information_schema.COLUMNS 
WHERE TABLE_SCHEMA = DATABASE() 
  AND TABLE_NAME = 'bc_order' 
  AND COLUMN_NAME = 'accept_operator_id';

SET @sql = IF(@col_exists = 0, 
    'ALTER TABLE bc_order ADD COLUMN accept_operator_id BIGINT NULL COMMENT ''接单操作人ID'' AFTER session_version', 
    'SELECT ''Column accept_operator_id already exists'' AS msg');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 检查并添加 accepted_at 字段（如果不存在）
SET @col_exists = 0;
SELECT COUNT(*) INTO @col_exists 
FROM information_schema.COLUMNS 
WHERE TABLE_SCHEMA = DATABASE() 
  AND TABLE_NAME = 'bc_order' 
  AND COLUMN_NAME = 'accepted_at';

SET @sql = IF(@col_exists = 0, 
    'ALTER TABLE bc_order ADD COLUMN accepted_at DATETIME NULL COMMENT ''接单时间'' AFTER accept_operator_id', 
    'SELECT ''Column accepted_at already exists'' AS msg');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 检查并添加 reject_reason_code 字段
SET @col_exists = 0;
SELECT COUNT(*) INTO @col_exists 
FROM information_schema.COLUMNS 
WHERE TABLE_SCHEMA = DATABASE() 
  AND TABLE_NAME = 'bc_order' 
  AND COLUMN_NAME = 'reject_reason_code';

SET @sql = IF(@col_exists = 0, 
    'ALTER TABLE bc_order ADD COLUMN reject_reason_code VARCHAR(64) NULL COMMENT ''拒单原因码（如：OUT_OF_STOCK、BUSY、OTHER）'' AFTER accepted_at', 
    'SELECT ''Column reject_reason_code already exists'' AS msg');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 检查并添加 reject_reason_desc 字段
SET @col_exists = 0;
SELECT COUNT(*) INTO @col_exists 
FROM information_schema.COLUMNS 
WHERE TABLE_SCHEMA = DATABASE() 
  AND TABLE_NAME = 'bc_order' 
  AND COLUMN_NAME = 'reject_reason_desc';

SET @sql = IF(@col_exists = 0, 
    'ALTER TABLE bc_order ADD COLUMN reject_reason_desc VARCHAR(256) NULL COMMENT ''拒单原因说明（商户填写）'' AFTER reject_reason_code', 
    'SELECT ''Column reject_reason_desc already exists'' AS msg');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 检查并添加 rejected_at 字段
SET @col_exists = 0;
SELECT COUNT(*) INTO @col_exists 
FROM information_schema.COLUMNS 
WHERE TABLE_SCHEMA = DATABASE() 
  AND TABLE_NAME = 'bc_order' 
  AND COLUMN_NAME = 'rejected_at';

SET @sql = IF(@col_exists = 0, 
    'ALTER TABLE bc_order ADD COLUMN rejected_at DATETIME NULL COMMENT ''拒单时间'' AFTER reject_reason_desc', 
    'SELECT ''Column rejected_at already exists'' AS msg');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 检查并添加 rejected_by 字段
SET @col_exists = 0;
SELECT COUNT(*) INTO @col_exists 
FROM information_schema.COLUMNS 
WHERE TABLE_SCHEMA = DATABASE() 
  AND TABLE_NAME = 'bc_order' 
  AND COLUMN_NAME = 'rejected_by';

SET @sql = IF(@col_exists = 0, 
    'ALTER TABLE bc_order ADD COLUMN rejected_by BIGINT NULL COMMENT ''拒单操作人ID'' AFTER rejected_at', 
    'SELECT ''Column rejected_by already exists'' AS msg');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- ============================================================
-- 2. 创建幂等动作日志表：防止重复接单/拒单
-- ============================================================
CREATE TABLE IF NOT EXISTS bc_order_action_log (
    id                  BIGINT          NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    tenant_id           BIGINT          NOT NULL COMMENT '租户ID',
    store_id            BIGINT          NOT NULL COMMENT '门店ID',
    order_id            BIGINT          NOT NULL COMMENT '订单ID',
    
    -- 动作类型与唯一键
    action_type         VARCHAR(32)     NOT NULL COMMENT '动作类型：ACCEPT（接单）、REJECT（拒单）',
    action_key          VARCHAR(255)    NOT NULL COMMENT '幂等唯一键：{tenantId}:{storeId}:{orderId}:{actionType}:{requestId}',
    
    -- 操作人与时间
    operator_id         BIGINT          NULL COMMENT '操作人ID',
    operator_name       VARCHAR(128)    NULL COMMENT '操作人姓名（快照）',
    
    -- 执行状态与结果
    status              VARCHAR(32)     NOT NULL DEFAULT 'PROCESSING' COMMENT '执行状态：PROCESSING、SUCCESS、FAILED',
    result_json         TEXT            NULL COMMENT '执行结果JSON（订单状态等）',
    error_code          VARCHAR(64)     NULL COMMENT '错误码',
    error_msg           VARCHAR(512)    NULL COMMENT '错误消息',
    
    -- 扩展字段
    ext_json            TEXT            NULL COMMENT '扩展信息JSON（拒单原因等）',
    
    -- 审计字段
    created_at          DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at          DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    
    PRIMARY KEY (id),
    UNIQUE KEY uk_tenant_action_key (tenant_id, action_key),
    KEY idx_tenant_order_action (tenant_id, order_id, action_type),
    KEY idx_tenant_store_created (tenant_id, store_id, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='订单动作幂等日志表';

-- ============================================================
-- 说明：
-- 1. action_key 格式：{tenantId}:{storeId}:{orderId}:{actionType}:{requestId}
-- 2. 同一个 requestId 重复调用接单/拒单接口，会因为唯一索引冲突而返回已有结果
-- 3. 不同 requestId 调用接单/拒单，如果订单状态不允许操作则失败
-- 4. 乐观锁（order_version）确保并发安全，幂等键确保重复请求安全
-- ============================================================
