-- ============================================================================
-- BlueCone ID 号段表（Segment-based Long ID Generation）
-- ============================================================================
-- 用途：为 long 型主键提供号段分配服务，避免 Snowflake 的时钟回拨问题
-- 特性：
--   1. 高性能：应用本地缓存号段，减少数据库访问
--   2. 全局唯一：同一 scope 内单调递增
--   3. 无时钟依赖：纯号段递增，不依赖系统时钟
-- ============================================================================

CREATE TABLE IF NOT EXISTS bc_id_segment (
    scope VARCHAR(64) NOT NULL COMMENT 'ID 作用域（如 TENANT/STORE/ORDER），对应业务表或领域',
    max_id BIGINT NOT NULL COMMENT '当前已分配的最大 ID（下次分配从 max_id+1 开始）',
    step INT NOT NULL DEFAULT 1000 COMMENT '每次分配的号段大小（建议 1000）',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '最后更新时间',
    PRIMARY KEY (scope)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='ID 号段分配表';

-- ============================================================================
-- 初始化数据：为各业务作用域预分配记录
-- ============================================================================
-- 说明：
--   - max_id=0 表示尚未分配任何 ID，首次分配将返回 [1, 1000]
--   - step=1000 表示每次分配 1000 个 ID
--   - 可根据业务 QPS 调整 step（高并发场景可设为 5000 或 10000）
-- ============================================================================

INSERT INTO bc_id_segment (scope, max_id, step, updated_at) VALUES
('TENANT', 0, 1000, NOW()),
('STORE', 0, 1000, NOW()),
('ORDER', 0, 1000, NOW()),
('PRODUCT', 0, 1000, NOW()),
('SKU', 0, 1000, NOW()),
('USER', 0, 1000, NOW()),
('PAYMENT', 0, 1000, NOW()),
('INVENTORY_RECORD', 0, 1000, NOW())
ON DUPLICATE KEY UPDATE updated_at = updated_at; -- 幂等：已存在则不更新

-- ============================================================================
-- 使用示例（应用层伪代码）
-- ============================================================================
-- BEGIN;
--   SELECT max_id, step FROM bc_id_segment WHERE scope = 'ORDER' FOR UPDATE;
--   -- 假设查询结果：max_id=0, step=1000
--   newMax = 0 + 1000 = 1000;
--   UPDATE bc_id_segment SET max_id = 1000, updated_at = NOW() WHERE scope = 'ORDER';
-- COMMIT;
-- -- 返回号段：[1, 1000]
-- ============================================================================

-- ============================================================================
-- 维护说明
-- ============================================================================
-- 1. 监控 max_id 增长速度，预估 ID 耗尽时间（BIGINT 最大值 9223372036854775807）
-- 2. 如需重置某个 scope（仅测试环境），可执行：
--    UPDATE bc_id_segment SET max_id = 0 WHERE scope = 'xxx';
-- 3. 生产环境禁止手动修改 max_id，避免 ID 重复
-- ============================================================================

