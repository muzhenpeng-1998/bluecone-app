# Flyway 迁移失败修复指南

## 问题描述

Flyway 迁移脚本 `V20251218001__add_payment_notify_id.sql` 执行失败，原因是使用了 MySQL 不支持的语法 `IF NOT EXISTS`。

**错误信息：**
```
You have an error in your SQL syntax; check the manual that corresponds to your MySQL server version 
for the right syntax to use near 'IF NOT EXISTS notify_id VARCHAR(128) DEFAULT NULL COMMENT '通知ID（用于幂�' at line 7
```

## 根本原因

MySQL 的 `ALTER TABLE ADD COLUMN` 语句不支持 `IF NOT EXISTS` 子句。这是 MySQL 与其他数据库（如 PostgreSQL）的语法差异。

## 解决方案

### 步骤 1：清理失败的迁移记录

由于迁移失败，Flyway 会在 `flyway_schema_history` 表中记录失败状态，需要手动清理：

```sql
-- 连接到数据库
mysql -u root -p bluecone

-- 查看迁移历史
SELECT * FROM flyway_schema_history ORDER BY installed_rank DESC LIMIT 5;

-- 删除失败的迁移记录
DELETE FROM flyway_schema_history 
WHERE version = '20251218001' AND success = 0;
```

### 步骤 2：修复 SQL 脚本

已修复的脚本内容（移除 `IF NOT EXISTS`）：

```sql
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
```

### 步骤 3：重新编译并启动应用

```bash
# 1. 清理并重新编译
cd /Users/zhenpengmu/Desktop/code/project/bluecone-app
mvn clean compile -DskipTests

# 2. 启动应用（Flyway 会自动执行修复后的迁移脚本）
mvn spring-boot:run -pl app-application
```

## 快速修复命令（一键执行）

```bash
# 连接数据库并清理失败记录
mysql -u root -p bluecone -e "DELETE FROM flyway_schema_history WHERE version = '20251218001' AND success = 0;"

# 重新编译并启动
cd /Users/zhenpengmu/Desktop/code/project/bluecone-app
mvn clean compile -DskipTests && mvn spring-boot:run -pl app-application
```

## 验证修复

启动成功后，检查迁移是否成功：

```sql
-- 查看迁移历史
SELECT version, description, type, script, installed_on, success 
FROM flyway_schema_history 
WHERE version = '20251218001';

-- 验证字段是否添加成功
DESCRIBE bc_payment_notify_log;
DESCRIBE bc_order;
```

**预期结果：**
- `bc_payment_notify_log` 表应该有 `notify_id` 字段和 `uk_notify_id` 唯一索引
- `bc_order` 表应该有 `close_reason` 和 `closed_at` 字段

## 注意事项

### 关于 Flyway 幂等性

Flyway 迁移脚本应该是幂等的，但 MySQL 的 `ALTER TABLE ADD COLUMN` 不支持 `IF NOT EXISTS`，因此：

1. **首次执行：** 脚本会成功执行，添加字段和索引
2. **重复执行：** 如果字段已存在，会报错 `Duplicate column name`

**解决方案：**
- Flyway 的版本控制机制确保每个迁移脚本只执行一次
- 如果需要真正的幂等性，可以使用存储过程或条件判断（但会增加复杂度）

### MySQL vs PostgreSQL 语法差异

| 特性 | MySQL | PostgreSQL |
|------|-------|------------|
| ADD COLUMN IF NOT EXISTS | ❌ 不支持 | ✅ 支持 |
| CREATE INDEX IF NOT EXISTS | ❌ 不支持（5.7及以下）<br>✅ 支持（8.0及以上） | ✅ 支持 |
| 推荐做法 | 依赖 Flyway 版本控制 | 可使用 IF NOT EXISTS |

## 常见问题

### Q1: 如果字段已经存在怎么办？

如果之前的迁移部分成功（比如添加了 `notify_id` 但索引创建失败），需要手动检查并调整脚本：

```sql
-- 检查字段是否存在
SELECT COLUMN_NAME 
FROM INFORMATION_SCHEMA.COLUMNS 
WHERE TABLE_SCHEMA = 'bluecone' 
  AND TABLE_NAME = 'bc_payment_notify_log' 
  AND COLUMN_NAME = 'notify_id';

-- 如果字段已存在，只需创建索引
ALTER TABLE bc_payment_notify_log 
ADD UNIQUE INDEX uk_notify_id (notify_id);
```

### Q2: 如何避免类似问题？

1. **开发环境测试：** 在提交迁移脚本前，先在本地数据库测试
2. **使用 MySQL 8.0+：** 新版本支持更多 DDL 语法
3. **编写兼容性脚本：** 避免使用数据库特定语法
4. **使用存储过程：** 对于复杂的条件逻辑，可以使用存储过程实现幂等性

### Q3: 生产环境如何处理？

生产环境迁移失败的处理步骤：

1. **立即回滚应用：** 停止应用，避免数据不一致
2. **备份数据库：** 在修复前务必备份
3. **手动执行修复：** 清理失败记录，手动执行正确的 SQL
4. **验证数据一致性：** 确保所有表结构正确
5. **重新部署应用：** 使用修复后的迁移脚本

## 总结

- ✅ 已修复 SQL 脚本，移除 `IF NOT EXISTS` 语法
- ✅ 需要手动清理 Flyway 失败记录
- ✅ 重新编译并启动应用即可完成迁移
- ⚠️ 注意 MySQL 与 PostgreSQL 的语法差异
- ⚠️ Flyway 迁移脚本应该在开发环境充分测试后再提交
