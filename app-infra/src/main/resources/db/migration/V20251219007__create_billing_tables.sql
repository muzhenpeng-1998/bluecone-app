-- =====================================================
-- 订阅计费 Billing V1 表结构
-- 包含：Plan SKU + Invoice + Subscription
-- =====================================================

-- =====================================================
-- bc_plan_sku: 套餐 SKU 表
-- 定义可售卖的订阅套餐（Plan + 周期 + 价格）
-- =====================================================
CREATE TABLE IF NOT EXISTS `bc_plan_sku` (
    `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    
    -- 套餐信息
    `plan_code` VARCHAR(64) NOT NULL COMMENT '套餐编码：FREE/BASIC/PRO/ENTERPRISE',
    `plan_name` VARCHAR(128) NOT NULL COMMENT '套餐名称：免费版/基础版/专业版/企业版',
    `plan_level` INT NOT NULL COMMENT '套餐等级：0=FREE, 1=BASIC, 2=PRO, 3=ENTERPRISE（用于升降级判断）',
    
    -- 计费周期
    `billing_period` VARCHAR(32) NOT NULL COMMENT '计费周期：MONTHLY/QUARTERLY/YEARLY',
    `period_months` INT NOT NULL COMMENT '周期月数：1=月付, 3=季付, 12=年付',
    
    -- 价格（单位：分）
    `price_fen` BIGINT NOT NULL COMMENT '价格（分）',
    `original_price_fen` BIGINT NULL COMMENT '原价（分，用于展示折扣）',
    
    -- 功能配额（JSON 存储，便于扩展）
    `features` JSON NOT NULL COMMENT '功能配额：{"maxStores":5, "maxUsers":10, "hasMultiWarehouse":true, ...}',
    
    -- 状态
    `status` VARCHAR(32) NOT NULL DEFAULT 'ACTIVE' COMMENT '状态：ACTIVE/INACTIVE',
    `sort_order` INT NOT NULL DEFAULT 0 COMMENT '排序（展示顺序）',
    
    -- 审计字段
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_plan_period` (`plan_code`, `billing_period`),
    KEY `idx_status_sort` (`status`, `sort_order`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='套餐 SKU 表';

-- =====================================================
-- bc_billing_invoice: 订阅账单表
-- 记录每次订阅购买/续费的账单
-- =====================================================
CREATE TABLE IF NOT EXISTS `bc_billing_invoice` (
    `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    
    -- 租户信息
    `tenant_id` BIGINT UNSIGNED NOT NULL COMMENT '租户ID',
    
    -- 账单信息
    `invoice_no` VARCHAR(64) NOT NULL COMMENT '账单号（唯一，用于展示）',
    `idempotency_key` VARCHAR(255) NOT NULL COMMENT '幂等键（创建时传入，防止重复创建）',
    
    -- 套餐信息（快照）
    `plan_sku_id` BIGINT UNSIGNED NOT NULL COMMENT '套餐 SKU ID',
    `plan_code` VARCHAR(64) NOT NULL COMMENT '套餐编码（快照）',
    `plan_name` VARCHAR(128) NOT NULL COMMENT '套餐名称（快照）',
    `billing_period` VARCHAR(32) NOT NULL COMMENT '计费周期（快照）',
    `period_months` INT NOT NULL COMMENT '周期月数（快照）',
    
    -- 金额（单位：分）
    `amount_fen` BIGINT NOT NULL COMMENT '应付金额（分）',
    `paid_amount_fen` BIGINT NULL COMMENT '实付金额（分，支付成功后填充）',
    
    -- 支付信息
    `payment_channel` VARCHAR(32) NULL COMMENT '支付渠道：WECHAT/ALIPAY',
    `channel_trade_no` VARCHAR(128) NULL COMMENT '渠道交易号（微信/支付宝交易号）',
    `paid_at` DATETIME NULL COMMENT '支付成功时间',
    
    -- 状态
    `status` VARCHAR(32) NOT NULL DEFAULT 'PENDING' COMMENT '状态：PENDING/PAID/EXPIRED/CANCELED',
    
    -- 生效周期（支付成功后填充）
    `effective_start_at` DATETIME NULL COMMENT '生效开始时间',
    `effective_end_at` DATETIME NULL COMMENT '生效结束时间',
    
    -- 审计字段
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_invoice_no` (`invoice_no`),
    UNIQUE KEY `uk_idempotency_key` (`idempotency_key`),
    UNIQUE KEY `uk_channel_trade_no` (`channel_trade_no`),
    KEY `idx_tenant_status` (`tenant_id`, `status`),
    KEY `idx_plan_sku_id` (`plan_sku_id`),
    KEY `idx_created_at` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='订阅账单表';

-- =====================================================
-- bc_tenant_subscription: 租户订阅表
-- 记录租户当前的订阅状态（每个租户一条记录）
-- =====================================================
CREATE TABLE IF NOT EXISTS `bc_tenant_subscription` (
    `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    
    -- 租户信息
    `tenant_id` BIGINT UNSIGNED NOT NULL COMMENT '租户ID',
    
    -- 当前套餐信息
    `current_plan_code` VARCHAR(64) NOT NULL COMMENT '当前套餐编码',
    `current_plan_name` VARCHAR(128) NOT NULL COMMENT '当前套餐名称',
    `current_plan_level` INT NOT NULL COMMENT '当前套餐等级',
    
    -- 功能配额（当前生效的配额快照）
    `current_features` JSON NOT NULL COMMENT '当前功能配额（快照）',
    
    -- 订阅周期
    `subscription_start_at` DATETIME NOT NULL COMMENT '订阅开始时间',
    `subscription_end_at` DATETIME NOT NULL COMMENT '订阅结束时间',
    
    -- 状态
    `status` VARCHAR(32) NOT NULL DEFAULT 'ACTIVE' COMMENT '状态：ACTIVE/EXPIRED/CANCELED',
    
    -- 最后一次账单信息（用于续费/升级判断）
    `last_invoice_id` BIGINT UNSIGNED NULL COMMENT '最后一次账单ID',
    `last_paid_at` DATETIME NULL COMMENT '最后一次支付时间',
    
    -- 审计字段
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_tenant_id` (`tenant_id`),
    KEY `idx_status_end_at` (`status`, `subscription_end_at`),
    KEY `idx_plan_code` (`current_plan_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='租户订阅表';

-- =====================================================
-- 初始化数据：插入默认套餐 SKU
-- =====================================================
INSERT INTO `bc_plan_sku` 
    (`plan_code`, `plan_name`, `plan_level`, `billing_period`, `period_months`, `price_fen`, `original_price_fen`, `features`, `status`, `sort_order`)
VALUES
    -- 免费版（永久免费）
    ('FREE', '免费版', 0, 'FOREVER', 999, 0, 0, 
     '{"maxStores": 1, "maxUsers": 2, "hasMultiWarehouse": false, "hasAdvancedReports": false, "hasPrioritySupport": false}',
     'ACTIVE', 1),
    
    -- 基础版
    ('BASIC', '基础版', 1, 'MONTHLY', 1, 9900, 9900,
     '{"maxStores": 3, "maxUsers": 5, "hasMultiWarehouse": false, "hasAdvancedReports": true, "hasPrioritySupport": false}',
     'ACTIVE', 2),
    ('BASIC', '基础版', 1, 'YEARLY', 12, 99000, 118800,
     '{"maxStores": 3, "maxUsers": 5, "hasMultiWarehouse": false, "hasAdvancedReports": true, "hasPrioritySupport": false}',
     'ACTIVE', 3),
    
    -- 专业版
    ('PRO', '专业版', 2, 'MONTHLY', 1, 29900, 29900,
     '{"maxStores": 10, "maxUsers": 20, "hasMultiWarehouse": true, "hasAdvancedReports": true, "hasPrioritySupport": true}',
     'ACTIVE', 4),
    ('PRO', '专业版', 2, 'YEARLY', 12, 299000, 358800,
     '{"maxStores": 10, "maxUsers": 20, "hasMultiWarehouse": true, "hasAdvancedReports": true, "hasPrioritySupport": true}',
     'ACTIVE', 5),
    
    -- 企业版
    ('ENTERPRISE', '企业版', 3, 'MONTHLY', 1, 99900, 99900,
     '{"maxStores": 999, "maxUsers": 999, "hasMultiWarehouse": true, "hasAdvancedReports": true, "hasPrioritySupport": true, "hasCustomIntegration": true}',
     'ACTIVE', 6),
    ('ENTERPRISE', '企业版', 3, 'YEARLY', 12, 999000, 1198800,
     '{"maxStores": 999, "maxUsers": 999, "hasMultiWarehouse": true, "hasAdvancedReports": true, "hasPrioritySupport": true, "hasCustomIntegration": true}',
     'ACTIVE', 7);

-- =====================================================
-- 设计要点：
-- 
-- bc_plan_sku:
-- 1. plan_code + billing_period 唯一索引，确保同一套餐的同一周期只有一条记录
-- 2. plan_level 用于升降级判断（数字越大等级越高）
-- 3. features JSON 字段存储功能配额，便于扩展新功能
-- 4. price_fen 使用分为单位，避免浮点数精度问题
-- 
-- bc_billing_invoice:
-- 1. idempotency_key 唯一索引，确保创建账单幂等
-- 2. channel_trade_no 唯一索引，确保支付回调幂等（同一笔支付只能成功一次）
-- 3. 账单状态机：PENDING → PAID（支付成功）或 EXPIRED（超时）或 CANCELED（取消）
-- 4. 支付成功后填充 paid_amount_fen、paid_at、effective_start_at、effective_end_at
-- 
-- bc_tenant_subscription:
-- 1. tenant_id 唯一索引，确保每个租户只有一条订阅记录
-- 2. current_features 存储当前生效的配额快照，PlanGuard 从这里读取
-- 3. 订阅状态机：ACTIVE（生效中）→ EXPIRED（到期）→ ACTIVE（续费）
-- 4. 到期后自动降级到 FREE 套餐，由定时任务 SubscriptionExpireJob 处理
-- =====================================================
