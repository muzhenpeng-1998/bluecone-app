# 订单 M2：Flyway 迁移失败修复指南

## 问题描述

启动应用时遇到以下错误：

```
Migration V20251218002__add_order_acceptance_m2_fields.sql failed
SQL State  : 42000
Error Code : 1064
Message    : You have an error in your SQL syntax; 
             check the manual that corresponds to your MySQL server version 
             for the right syntax to use near 'IF NOT EXISTS reject_reason_code ...'
```

**根本原因**：MySQL 的 `ALTER TABLE ADD COLUMN` 语法不支持 `IF NOT EXISTS`。

## 解决方案

### 步骤 1：修复 Flyway 历史记录

执行以下 SQL 删除失败的迁移记录：

```bash
# 连接数据库
mysql -u root -p

# 切换到你的数据库
USE bluecone_db;

# 执行修复脚本
source /Users/zhenpengmu/Desktop/code/project/bluecone-app/FIX-M2-FLYWAY-FAILED.sql
```

或者直接执行：

```sql
-- 查看失败记录
SELECT installed_rank, version, description, script, success
FROM flyway_schema_history
WHERE version = '20251218002';

-- 删除失败记录
DELETE FROM flyway_schema_history 
WHERE version = '20251218002' 
  AND script = 'V20251218002__add_order_acceptance_m2_fields.sql'
  AND success = 0;
```

### 步骤 2：重新编译项目

```bash
cd /Users/zhenpengmu/Desktop/code/project/bluecone-app
mvn clean compile
```

### 步骤 3：重启应用

```bash
mvn spring-boot:run
```

Flyway 会自动检测到 V20251218002 脚本尚未成功执行，并重新执行修复后的版本。

## 修复后的脚本说明

新的迁移脚本使用动态 SQL 实现幂等性：

```sql
-- 检查字段是否存在
SET @col_exists = 0;
SELECT COUNT(*) INTO @col_exists 
FROM information_schema.COLUMNS 
WHERE TABLE_SCHEMA = DATABASE() 
  AND TABLE_NAME = 'bc_order' 
  AND COLUMN_NAME = 'reject_reason_code';

-- 如果不存在则添加
SET @sql = IF(@col_exists = 0, 
    'ALTER TABLE bc_order ADD COLUMN reject_reason_code ...', 
    'SELECT ''Column already exists'' AS msg');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
```

这种方式支持：
- ✅ 幂等性：重复执行不会报错
- ✅ 兼容性：适用于 MySQL 5.7/8.0
- ✅ 可维护性：清晰的错误处理

## 验证步骤

### 1. 确认 Flyway 迁移成功

```sql
SELECT installed_rank, version, description, type, script, 
       installed_on, execution_time, success
FROM flyway_schema_history
WHERE version = '20251218002';
```

期望结果：`success = 1`

### 2. 确认订单表字段已添加

```sql
DESCRIBE bc_order;
```

应该看到以下新字段：
- `reject_reason_code`
- `reject_reason_desc`
- `rejected_at`
- `rejected_by`

### 3. 确认幂等动作表已创建

```sql
DESCRIBE bc_order_action_log;
```

应该看到表结构，包含：
- `id`
- `tenant_id`
- `store_id`
- `order_id`
- `action_type`
- `action_key`（唯一索引）
- 等

## 常见问题

### Q1：删除 Flyway 记录后，如何确保不会影响其他迁移？

**答**：只删除失败的记录（`success = 0`），不影响已成功的迁移。Flyway 会根据版本号顺序执行未完成的迁移。

### Q2：如果订单表字段已经存在（手动添加），会怎样？

**答**：修复后的脚本使用动态 SQL 检查字段是否存在，如果已存在则跳过，不会报错。

### Q3：如果不想修复 Flyway 记录，能否直接手动创建表结构？

**答**：可以，但不推荐。手动创建后，需要在 `flyway_schema_history` 表中插入成功记录：

```sql
INSERT INTO flyway_schema_history (
    installed_rank, version, description, type, script, 
    checksum, installed_by, installed_on, execution_time, success
)
VALUES (
    (SELECT MAX(installed_rank) + 1 FROM flyway_schema_history AS fsh), 
    '20251218002', 
    'add order acceptance m2 fields', 
    'SQL', 
    'V20251218002__add_order_acceptance_m2_fields.sql', 
    NULL, 
    'manual', 
    NOW(), 
    0, 
    1
);
```

但这样做会破坏 Flyway 的校验机制，不推荐使用。

## 总结

1. **原因**：MySQL 不支持 `ALTER TABLE ADD COLUMN IF NOT EXISTS`
2. **修复**：使用动态 SQL 实现幂等性
3. **步骤**：删除失败记录 → 重新编译 → 重启应用
4. **验证**：检查 Flyway 历史记录和表结构

修复完成后，订单 M2 功能即可正常使用！
