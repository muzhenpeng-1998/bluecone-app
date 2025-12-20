# Flyway 迁移失败快速修复指南

## 错误信息

```
Validate failed: Migrations have failed validation
Detected failed migration to version 20251218001 (add payment notify id).
Please remove any half-completed changes then run repair to fix the schema history.
```

## 快速修复（3个命令）

### 方法 1：使用 MySQL 命令行（推荐）

```bash
# 1. 连接数据库
mysql -h rm-bp1xld3504q1gtf6b9o.mysql.rds.aliyuncs.com -P 3306 -u root -p app

# 2. 删除失败的迁移记录
DELETE FROM flyway_schema_history WHERE version = '20251218001' AND success = 0;

# 3. 退出 MySQL
exit;

# 4. 重新启动应用
cd /Users/zhenpengmu/Desktop/code/project/bluecone-app
mvn spring-boot:run -pl app-application
```

### 方法 2：使用一行命令

```bash
mysql -h rm-bp1xld3504q1gtf6b9o.mysql.rds.aliyuncs.com -P 3306 -u root -p app -e "DELETE FROM flyway_schema_history WHERE version = '20251218001' AND success = 0;" && cd /Users/zhenpengmu/Desktop/code/project/bluecone-app && mvn spring-boot:run -pl app-application
```

### 方法 3：使用 Flyway Repair（如果上面方法不行）

```bash
cd /Users/zhenpengmu/Desktop/code/project/bluecone-app

# 使用 Maven Flyway 插件执行 repair
mvn flyway:repair -pl app-infra

# 然后重新启动应用
mvn spring-boot:run -pl app-application
```

## 详细步骤说明

### 步骤 1：查看失败的迁移记录

```sql
SELECT installed_rank, version, description, type, script, installed_on, execution_time, success 
FROM flyway_schema_history 
WHERE version = '20251218001';
```

**预期结果：**
```
+----------------+--------------+---------------------+------+------------------------------------------+---------------------+----------------+---------+
| installed_rank | version      | description         | type | script                                   | installed_on        | execution_time | success |
+----------------+--------------+---------------------+------+------------------------------------------+---------------------+----------------+---------+
|             XX | 20251218001  | add payment notify  | SQL  | V20251218001__add_payment_notify_id.sql | 2025-12-18 14:02:47 |           NULL |       0 |
+----------------+--------------+---------------------+------+------------------------------------------+---------------------+----------------+---------+
```

### 步骤 2：删除失败的记录

```sql
DELETE FROM flyway_schema_history 
WHERE version = '20251218001' AND success = 0;
```

### 步骤 3：检查是否有部分执行的表结构变更

```sql
-- 检查 bc_payment_notify_log 表
DESCRIBE bc_payment_notify_log;

-- 检查 bc_order 表
DESCRIBE bc_order;
```

**如果字段已存在，需要手动删除：**

```sql
-- 如果 notify_id 字段已存在（但索引创建失败）
ALTER TABLE bc_payment_notify_log DROP COLUMN IF EXISTS notify_id;

-- 如果 close_reason 或 closed_at 字段已存在
ALTER TABLE bc_order DROP COLUMN IF EXISTS close_reason;
ALTER TABLE bc_order DROP COLUMN IF EXISTS closed_at;
```

### 步骤 4：重新启动应用

```bash
cd /Users/zhenpengmu/Desktop/code/project/bluecone-app
mvn spring-boot:run -pl app-application
```

Flyway 会自动重新执行修复后的迁移脚本。

## 验证修复成功

启动成功后，执行以下 SQL 验证：

```sql
-- 1. 查看迁移历史（应该显示 success = 1）
SELECT version, description, success, installed_on 
FROM flyway_schema_history 
WHERE version = '20251218001';

-- 2. 验证 bc_payment_notify_log 表结构
DESCRIBE bc_payment_notify_log;
-- 应该看到 notify_id 字段

-- 3. 验证 bc_order 表结构
DESCRIBE bc_order;
-- 应该看到 close_reason 和 closed_at 字段

-- 4. 验证索引
SHOW INDEX FROM bc_payment_notify_log WHERE Key_name = 'uk_notify_id';
```

## 常见问题

### Q1: 为什么会出现这个错误？

**原因：** 之前的迁移脚本使用了 MySQL 不支持的 `IF NOT EXISTS` 语法，导致执行失败。Flyway 记录了这次失败，并在下次启动时进行校验，发现有失败的迁移记录，因此拒绝启动。

### Q2: 删除失败记录安全吗？

**安全。** 因为迁移失败，数据库结构没有变更（或只有部分变更）。删除失败记录后，Flyway 会重新执行修复后的迁移脚本。

### Q3: 如果字段已经部分创建怎么办？

需要手动删除已创建的字段，然后让 Flyway 重新执行完整的迁移：

```sql
-- 清理部分创建的字段
ALTER TABLE bc_payment_notify_log DROP COLUMN IF EXISTS notify_id;
ALTER TABLE bc_order DROP COLUMN IF EXISTS close_reason;
ALTER TABLE bc_order DROP COLUMN IF EXISTS closed_at;

-- 删除失败记录
DELETE FROM flyway_schema_history WHERE version = '20251218001';
```

### Q4: 生产环境如何处理？

生产环境更谨慎的做法：

1. **备份数据库**
2. **在测试环境验证修复方案**
3. **制定回滚计划**
4. **在维护窗口执行修复**
5. **验证数据一致性**

## 修复后的 SQL 脚本

已修复的迁移脚本（移除了 `IF NOT EXISTS`）：

```sql
-- 支付回调幂等优化
-- 创建时间：2025-12-18
-- 说明：为支付回调日志表添加 notify_id 唯一索引，用于幂等控制

-- 添加 notify_id 字段
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

## 总结

✅ **核心问题：** Flyway 检测到失败的迁移记录，拒绝启动应用

✅ **解决方案：** 删除失败记录，让 Flyway 重新执行修复后的迁移脚本

✅ **修复步骤：**
1. 连接数据库
2. `DELETE FROM flyway_schema_history WHERE version = '20251218001' AND success = 0;`
3. 重新启动应用

✅ **预防措施：** 所有迁移脚本应该在本地数据库充分测试后再提交
