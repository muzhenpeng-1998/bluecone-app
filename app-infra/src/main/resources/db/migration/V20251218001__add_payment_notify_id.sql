-- 支付回调幂等优化
-- 创建时间：2025-12-18
-- 说明：为支付回调日志表添加 notify_id 唯一索引，用于幂等控制

-- 添加 notify_id 字段
-- MySQL 不支持 IF NOT EXISTS，使用存储过程或直接执行（Flyway 会处理重复执行问题）
ALTER TABLE bc_payment_notify_log 
ADD COLUMN notify_id VARCHAR(128) DEFAULT NULL COMMENT '通知ID（用于幂等，来自渠道或客户端生成）' AFTER id;

-- 创建唯一索引（幂等键）
ALTER TABLE bc_payment_notify_log 
ADD UNIQUE INDEX uk_notify_id (notify_id);

-- 为订单表添加关单原因字段
ALTER TABLE bc_order
ADD COLUMN close_reason VARCHAR(64) DEFAULT NULL COMMENT '关单原因：PAY_TIMEOUT（支付超时）、USER_CANCEL（用户取消）、MERCHANT_CANCEL（商户取消）等' AFTER status;

-- 为订单表添加关单时间字段
ALTER TABLE bc_order
ADD COLUMN closed_at DATETIME DEFAULT NULL COMMENT '关单时间' AFTER close_reason;
