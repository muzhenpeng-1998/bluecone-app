-- Migration: ensure infra tables use ULID128/BINARY(16) where appropriate
--
-- 1) bc_event_consume_record:
--    - event_id 已为 BINARY(16)，本迁移文件仅作为文档化占位，无需变更。
--
-- 2) bc_outbox_message:
--    - 当前表结构未在 docs/sql 中显式给出；
--    - 建议对照 docs/sql/bc_outbox_event.sql，将 event_id / aggregate_id
--      迁移为 BINARY(16) 并引入 public_aggregate_id / 索引等。
--    - 具体迁移步骤需结合现网数据量与回放策略评估后实施。
--
-- 3) bc_idempotency_record:
--    - 目前仅使用 BIGINT 自增主键 + result_ref(public_id)，若未来需要内部 ULID128 主键，
--      可参考业务主表的 internal_id/public_id 方案追加字段。

-- 占位文件：本迁移不执行任何 DDL，仅用于记录 ULID128 在基础设施表中的现状与后续建议。

