-- Migration: add store_no to bc_store
--
-- 迁移建议步骤（生产环境）：
-- 1) 加列：为 bc_store 增加 store_no 字段及唯一约束；
-- 2) 回填：为历史门店批量生成唯一的 Snowflake long 编号（store_no）；
-- 3) 双写：应用侧在写入/更新门店时同时维护 internal_id/public_id/store_no；
-- 4) 切换读路径：上层读模型/接口逐步改为优先使用 public_id/store_no 进行查找。

ALTER TABLE bc_store
    ADD COLUMN store_no BIGINT UNSIGNED NULL COMMENT '门店数字编号（snowflake long）',
    ADD UNIQUE KEY uk_bc_store_store_no (store_no);

