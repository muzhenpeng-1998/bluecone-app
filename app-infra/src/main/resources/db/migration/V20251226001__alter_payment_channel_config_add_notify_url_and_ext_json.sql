-- 补齐支付渠道配置表字段：notify_url 和 ext_json
-- Phase 2A: 支付渠道配置持久化闭环

-- 1. 新增 notify_url 字段（如果不存在）
SET @col_exists = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS 
                   WHERE TABLE_SCHEMA = DATABASE() 
                   AND TABLE_NAME = 'bc_payment_channel_config' 
                   AND COLUMN_NAME = 'notify_url');

SET @sql = IF(@col_exists = 0, 
              'ALTER TABLE bc_payment_channel_config ADD COLUMN notify_url VARCHAR(255) NULL COMMENT ''回调地址'' AFTER channel_mode', 
              'SELECT ''Column notify_url already exists'' AS message');

PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 2. 新增 ext_json 字段（如果不存在）
SET @col_exists = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS 
                   WHERE TABLE_SCHEMA = DATABASE() 
                   AND TABLE_NAME = 'bc_payment_channel_config' 
                   AND COLUMN_NAME = 'ext_json');

SET @sql = IF(@col_exists = 0, 
              'ALTER TABLE bc_payment_channel_config ADD COLUMN ext_json TEXT NULL COMMENT ''扩展配置 JSON'' AFTER notify_url', 
              'SELECT ''Column ext_json already exists'' AS message');

PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 3. 为 channel_mode 增加默认值（避免历史插入失败）
-- 注意：MySQL 8.0+ 支持 ALTER COLUMN SET DEFAULT
ALTER TABLE bc_payment_channel_config
MODIFY COLUMN channel_mode VARCHAR(32) NOT NULL DEFAULT 'SERVICE_PROVIDER' COMMENT '接入模式：SERVICE_PROVIDER（服务商）、DIRECT（普通商户）';

-- 4. encrypt_payload 维持原字段 NOT NULL，但如果有历史空数据则填充默认值
UPDATE bc_payment_channel_config
SET encrypt_payload = '{}'
WHERE encrypt_payload IS NULL OR encrypt_payload = '';

