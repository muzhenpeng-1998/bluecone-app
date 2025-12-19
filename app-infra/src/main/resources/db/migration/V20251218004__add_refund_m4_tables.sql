-- 订单取消/退款 M4：退款单表 + 退款回调日志表 + bc_order 补充字段
-- 创建时间：2025-12-18
-- 说明：支持"已支付订单退款"，保证幂等与并发安全

-- ============================================================
-- 1. 退款单表 bc_refund_order
-- ============================================================
CREATE TABLE IF NOT EXISTS bc_refund_order (
    id                  BIGINT          NOT NULL COMMENT '退款单ID（内部主键，ULID）',
    tenant_id           BIGINT          NOT NULL COMMENT '租户ID',
    store_id            BIGINT          NOT NULL COMMENT '门店ID',
    order_id            BIGINT          NOT NULL COMMENT '订单ID（关联bc_order.id）',
    public_order_no     VARCHAR(64)     NOT NULL COMMENT '订单号（冗余，用于快速查询）',
    
    -- 退款单编号
    refund_id           VARCHAR(64)     NOT NULL COMMENT '退款单号（对外展示，PublicId格式：rfd_xxx）',
    
    -- 退款渠道
    channel             VARCHAR(32)     NOT NULL COMMENT '退款渠道：WECHAT、ALIPAY',
    
    -- 退款金额（单位：元，保留2位小数）
    refund_amount       DECIMAL(18,2)   NOT NULL DEFAULT 0.00 COMMENT '退款金额（实际退款金额）',
    currency            VARCHAR(8)      NOT NULL DEFAULT 'CNY' COMMENT '币种',
    
    -- 退款状态
    status              VARCHAR(32)     NOT NULL COMMENT '退款状态：INIT、PROCESSING、SUCCESS、FAILED',
    
    -- 第三方退款单号（退款成功后由回调填充）
    refund_no           VARCHAR(128)    DEFAULT NULL COMMENT '第三方退款单号（如微信退款单号）',
    
    -- 退款原因
    reason_code         VARCHAR(64)     DEFAULT NULL COMMENT '退款原因码（USER_CANCEL、MERCHANT_REJECT、OUT_OF_STOCK等）',
    reason_desc         VARCHAR(256)    DEFAULT NULL COMMENT '退款原因描述（用户或商户填写）',
    
    -- 幂等键（唯一，保证同一请求只创建一个退款单）
    idem_key            VARCHAR(128)    NOT NULL COMMENT '幂等键（格式：{tenantId}:{storeId}:{orderId}:refund:{requestId}）',
    
    -- 第三方支付单号（用于退款时传给支付网关）
    pay_order_id        BIGINT          DEFAULT NULL COMMENT '支付单ID（关联bc_payment_order.id）',
    pay_no              VARCHAR(128)    DEFAULT NULL COMMENT '第三方支付单号（如微信transaction_id）',
    
    -- 退款时间
    refund_requested_at DATETIME        NOT NULL COMMENT '退款发起时间',
    refund_completed_at DATETIME        DEFAULT NULL COMMENT '退款完成时间（SUCCESS时填充）',
    
    -- 扩展字段
    ext_json            TEXT            DEFAULT NULL COMMENT '扩展信息JSON（存储第三方退款响应等）',
    
    -- 审计字段
    version             INT             NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
    created_at          DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    created_by          BIGINT          DEFAULT NULL COMMENT '创建人',
    updated_at          DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    updated_by          BIGINT          DEFAULT NULL COMMENT '更新人',
    
    PRIMARY KEY (id),
    UNIQUE KEY uk_tenant_idem_key (tenant_id, idem_key),
    UNIQUE KEY uk_tenant_refund_id (tenant_id, refund_id),
    KEY idx_tenant_order (tenant_id, order_id),
    KEY idx_tenant_status (tenant_id, status, created_at),
    KEY idx_refund_requested_at (refund_requested_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='退款单表';

-- ============================================================
-- 2. 退款回调日志表 bc_refund_notify_log
-- ============================================================
CREATE TABLE IF NOT EXISTS bc_refund_notify_log (
    id                  BIGINT          NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    tenant_id           BIGINT          NOT NULL COMMENT '租户ID',
    
    -- 通知唯一标识（幂等键）
    notify_id           VARCHAR(128)    NOT NULL COMMENT '通知ID（幂等键，如微信的resource.id或自生成UUID）',
    
    -- 退款单关联
    refund_id           VARCHAR(64)     DEFAULT NULL COMMENT '退款单号（解析后填充）',
    refund_order_id     BIGINT          DEFAULT NULL COMMENT '退款单ID（解析后填充）',
    
    -- 回调内容
    raw_body            TEXT            NOT NULL COMMENT '原始回调报文（JSON或XML）',
    channel             VARCHAR(32)     NOT NULL COMMENT '支付渠道：WECHAT、ALIPAY',
    
    -- 处理结果
    processed           TINYINT(1)      NOT NULL DEFAULT 0 COMMENT '是否已处理：0=未处理，1=已处理',
    processed_at        DATETIME        DEFAULT NULL COMMENT '处理时间',
    result              VARCHAR(32)     DEFAULT NULL COMMENT '处理结果：SUCCESS、FAILED、IGNORED',
    error_msg           VARCHAR(512)    DEFAULT NULL COMMENT '错误信息（处理失败时填充）',
    
    -- 审计字段
    created_at          DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间（回调接收时间）',
    
    PRIMARY KEY (id),
    UNIQUE KEY uk_tenant_notify_id (tenant_id, notify_id),
    KEY idx_tenant_refund_id (tenant_id, refund_id),
    KEY idx_processed (processed, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='退款回调日志表';

-- ============================================================
-- 3. bc_order 表补充字段（取消和退款相关）
-- ============================================================

-- 3.1 取消时间（用户取消或商户拒单时填充）
SET @column_exists = (
    SELECT COUNT(*) 
    FROM INFORMATION_SCHEMA.COLUMNS 
    WHERE TABLE_SCHEMA = DATABASE() 
      AND TABLE_NAME = 'bc_order' 
      AND COLUMN_NAME = 'canceled_at'
);

SET @sql = IF(@column_exists = 0, 
    'ALTER TABLE bc_order ADD COLUMN canceled_at DATETIME NULL COMMENT ''取消时间（用户取消或商户拒单时填充）'' AFTER closed_at', 
    'SELECT ''Column canceled_at already exists'' AS status'
);

PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 3.2 取消原因码（USER_CANCEL、MERCHANT_REJECT、PAY_TIMEOUT等）
SET @column_exists = (
    SELECT COUNT(*) 
    FROM INFORMATION_SCHEMA.COLUMNS 
    WHERE TABLE_SCHEMA = DATABASE() 
      AND TABLE_NAME = 'bc_order' 
      AND COLUMN_NAME = 'cancel_reason_code'
);

SET @sql = IF(@column_exists = 0, 
    'ALTER TABLE bc_order ADD COLUMN cancel_reason_code VARCHAR(64) NULL COMMENT ''取消原因码（USER_CANCEL、MERCHANT_REJECT、PAY_TIMEOUT等）'' AFTER canceled_at', 
    'SELECT ''Column cancel_reason_code already exists'' AS status'
);

PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 3.3 取消原因描述（用户或商户填写的具体原因）
SET @column_exists = (
    SELECT COUNT(*) 
    FROM INFORMATION_SCHEMA.COLUMNS 
    WHERE TABLE_SCHEMA = DATABASE() 
      AND TABLE_NAME = 'bc_order' 
      AND COLUMN_NAME = 'cancel_reason_desc'
);

SET @sql = IF(@column_exists = 0, 
    'ALTER TABLE bc_order ADD COLUMN cancel_reason_desc VARCHAR(256) NULL COMMENT ''取消原因描述（用户或商户填写的具体原因）'' AFTER cancel_reason_code', 
    'SELECT ''Column cancel_reason_desc already exists'' AS status'
);

PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 3.4 退款时间（退款成功时填充）
SET @column_exists = (
    SELECT COUNT(*) 
    FROM INFORMATION_SCHEMA.COLUMNS 
    WHERE TABLE_SCHEMA = DATABASE() 
      AND TABLE_NAME = 'bc_order' 
      AND COLUMN_NAME = 'refunded_at'
);

SET @sql = IF(@column_exists = 0, 
    'ALTER TABLE bc_order ADD COLUMN refunded_at DATETIME NULL COMMENT ''退款时间（退款成功时填充）'' AFTER cancel_reason_desc', 
    'SELECT ''Column refunded_at already exists'' AS status'
);

PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 3.5 退款单ID（关联bc_refund_order.id，可选）
SET @column_exists = (
    SELECT COUNT(*) 
    FROM INFORMATION_SCHEMA.COLUMNS 
    WHERE TABLE_SCHEMA = DATABASE() 
      AND TABLE_NAME = 'bc_order' 
      AND COLUMN_NAME = 'refund_order_id'
);

SET @sql = IF(@column_exists = 0, 
    'ALTER TABLE bc_order ADD COLUMN refund_order_id BIGINT NULL COMMENT ''退款单ID（关联bc_refund_order.id，可选）'' AFTER refunded_at', 
    'SELECT ''Column refund_order_id already exists'' AS status'
);

PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- ============================================================
-- 4. 索引补充（如果不存在则创建）
-- ============================================================

-- bc_order 表的取消时间索引（用于查询已取消订单）
SET @index_exists = (
    SELECT COUNT(*) 
    FROM INFORMATION_SCHEMA.STATISTICS 
    WHERE TABLE_SCHEMA = DATABASE() 
      AND TABLE_NAME = 'bc_order' 
      AND INDEX_NAME = 'idx_tenant_canceled_at'
);

SET @sql = IF(@index_exists = 0, 
    'CREATE INDEX idx_tenant_canceled_at ON bc_order (tenant_id, status, canceled_at)', 
    'SELECT ''Index idx_tenant_canceled_at already exists'' AS status'
);

PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- bc_order 表的退款时间索引（用于查询已退款订单）
SET @index_exists = (
    SELECT COUNT(*) 
    FROM INFORMATION_SCHEMA.STATISTICS 
    WHERE TABLE_SCHEMA = DATABASE() 
      AND TABLE_NAME = 'bc_order' 
      AND INDEX_NAME = 'idx_tenant_refunded_at'
);

SET @sql = IF(@index_exists = 0, 
    'CREATE INDEX idx_tenant_refunded_at ON bc_order (tenant_id, status, refunded_at)', 
    'SELECT ''Index idx_tenant_refunded_at already exists'' AS status'
);

PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
