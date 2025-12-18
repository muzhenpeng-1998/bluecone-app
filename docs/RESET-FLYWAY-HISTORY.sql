-- Flyway 历史库重置脚本（仅用于开发环境）
-- 
-- 说明：如果数据库是全新库或需要重置 Flyway 历史，执行此脚本
-- 警告：此操作会删除所有 Flyway 历史记录，仅用于开发环境！

-- 1. 删除 Flyway 历史表（如果存在）
DROP TABLE IF EXISTS flyway_schema_history;

-- 2. 可选：删除所有业务表（如果需要完全重置）
-- 注意：根据实际情况取消注释以下语句

-- DROP TABLE IF EXISTS bc_store CASCADE;
-- DROP TABLE IF EXISTS bc_store_capability CASCADE;
-- DROP TABLE IF EXISTS bc_store_opening_hours CASCADE;
-- DROP TABLE IF EXISTS bc_store_special_day CASCADE;
-- DROP TABLE IF EXISTS bc_store_channel CASCADE;
-- DROP TABLE IF EXISTS bc_store_read_model CASCADE;
-- DROP TABLE IF EXISTS bc_user CASCADE;
-- DROP TABLE IF EXISTS bc_auth_session CASCADE;
-- DROP TABLE IF EXISTS t_order CASCADE;
-- DROP TABLE IF EXISTS bc_outbox_message CASCADE;
-- ... 其他表

-- 3. 重新启动应用后，Flyway 会：
--    - 自动创建 flyway_schema_history 表
--    - 执行所有迁移脚本（从 V2024010101 开始）

