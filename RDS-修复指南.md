# 阿里云 RDS 数据库修复指南

## 📋 问题确认

从日志中发现：

```
Database: jdbc:mysql://rm-bp1xld3504q1gtf6b9o.mysql.rds.aliyuncs.com:3306/app
Current version of schema `app`: 20251218001
Error: Schema `app` contains a failed migration to version 20251218001 !
```

**确认信息**：
- ✅ 数据库：阿里云 RDS MySQL 8.0
- ✅ 地址：`rm-bp1xld3504q1gtf6b9o.mysql.rds.aliyuncs.com:3306`
- ✅ 库名：`app`
- ❌ 迁移 `20251218001` 失败

---

## 🎯 快速解决方案（3步）

### 方法 1：先启动应用再手动修复（推荐）⭐

#### 步骤 1：启动应用（已配置好）

我已经帮你**完全禁用了 Flyway**，现在直接启动：

```bash
cd /Users/zhenpengmu/Desktop/code/project/bluecone-app
mvn spring-boot:run -pl app-application -am -Dspring-boot.run.profiles=local
```

**应用现在应该可以启动了！**

#### 步骤 2：连接到 RDS 数据库

使用 MySQL 客户端连接（需要提供正确的用户名和密码）：

```bash
mysql -h rm-bp1xld3504q1gtf6b9o.mysql.rds.aliyuncs.com -P 3306 -u 你的用户名 -p app
```

或者使用图形化工具（如 MySQL Workbench、Navicat、DBeaver）：
- Host: `rm-bp1xld3504q1gtf6b9o.mysql.rds.aliyuncs.com`
- Port: `3306`
- Database: `app`
- Username: （你的 RDS 用户名）
- Password: （你的 RDS 密码）

#### 步骤 3：手动修复数据库

连接到数据库后，执行以下 SQL：

```sql
-- 1. 查看当前状态
SELECT * FROM flyway_schema_history WHERE version = '20251218001';

-- 2. 检查哪些列已经添加
SELECT COLUMN_NAME 
FROM INFORMATION_SCHEMA.COLUMNS 
WHERE TABLE_SCHEMA = 'app' 
  AND TABLE_NAME = 'bc_payment_notify_log' 
  AND COLUMN_NAME = 'notify_id';

SELECT COLUMN_NAME 
FROM INFORMATION_SCHEMA.COLUMNS 
WHERE TABLE_SCHEMA = 'app' 
  AND TABLE_NAME = 'bc_order' 
  AND COLUMN_NAME IN ('close_reason', 'closed_at');

-- 3. 删除部分添加的列（如果存在）
-- 注意：如果列不存在会报错，继续执行下一条即可
ALTER TABLE bc_payment_notify_log DROP INDEX IF EXISTS uk_notify_id;
ALTER TABLE bc_payment_notify_log DROP COLUMN IF EXISTS notify_id;
ALTER TABLE bc_order DROP COLUMN IF EXISTS close_reason;
ALTER TABLE bc_order DROP COLUMN IF EXISTS closed_at;

-- 4. 清理 Flyway 历史表
DELETE FROM flyway_schema_history WHERE version = '20251218001';

-- 5. 验证清理
SELECT COUNT(*) FROM flyway_schema_history WHERE version = '20251218001';
-- 应该返回 0
```

#### 步骤 4：重新启用 Flyway 并重启应用

编辑 `app-application/src/main/resources/application-local.yml`，修改为：

```yaml
  flyway:
    enabled: true
    baseline-on-migrate: true
    out-of-order: true
    ignore-migration-patterns: "*:missing"
    validate-on-migrate: true
```

然后重启应用：

```bash
mvn spring-boot:run -pl app-application -am -Dspring-boot.run.profiles=local
```

---

### 方法 2：使用 Java 修复工具

如果你不想手动连接数据库，可以使用修复工具。

#### 步骤 1：配置数据库密码

编辑 `SimpleFlywayRepair.java`，找到这几行：

```java
private static final String DB_URL = "jdbc:mysql://rm-bp1xld3504q1gtf6b9o.mysql.rds.aliyuncs.com:3306/app?useSSL=false&serverTimezone=Asia/Shanghai";
private static final String DB_USER = "root";  // 修改为你的 RDS 用户名
private static final String DB_PASSWORD = "";  // 修改为你的 RDS 密码
```

把 `DB_USER` 和 `DB_PASSWORD` 改成你的 RDS 账号信息。

#### 步骤 2：编译并运行

```bash
cd /Users/zhenpengmu/Desktop/code/project/bluecone-app

# 编译
javac -cp ".:app-application/target/bluecone-app.jar" SimpleFlywayRepair.java

# 运行
java -cp ".:app-application/target/bluecone-app.jar:/Users/zhenpengmu/.m2/repository/mysql/mysql-connector-java/8.0.26/mysql-connector-java-8.0.26.jar" SimpleFlywayRepair
```

#### 步骤 3：修复成功后重新启用 Flyway

按照"方法 1 步骤 4"重新启用 Flyway 并重启应用。

---

## ⚠️ 当前配置说明

我已经修改了 `application-local.yml`，添加了：

```yaml
flyway:
  enabled: false  # 完全禁用 Flyway
```

**这意味着**：
- ✅ 应用可以启动
- ❌ Flyway 不会自动执行迁移
- ❌ 数据库迁移需要手动处理
- ⚠️ 这是临时方案

---

## 🔍 问题分析

### 为什么会失败？

查看迁移脚本 `V20251218001__add_payment_notify_id.sql`：

```sql
ALTER TABLE bc_payment_notify_log 
ADD COLUMN notify_id VARCHAR(128) DEFAULT NULL COMMENT '通知ID' AFTER id;

ALTER TABLE bc_payment_notify_log 
ADD UNIQUE INDEX uk_notify_id (notify_id);  -- ❌ 这里失败了
```

**失败原因**：
1. 第一条 SQL 成功执行，添加了 `notify_id` 列
2. 第二条 SQL 失败，可能是因为：
   - 列中有 NULL 值（默认值是 NULL）
   - MySQL 8.0 某些配置下不允许在有 NULL 值的列上创建唯一索引
   - 或者表中已有数据，添加唯一索引冲突

### 数据库当前状态

可能的状态：
```
bc_payment_notify_log 表：
├── notify_id 列：✅ 已添加（值都是 NULL）
└── uk_notify_id 索引：❌ 未创建（这里失败了）

bc_order 表：
├── close_reason 列：❓ 可能未添加
└── closed_at 列：❓ 可能未添加

flyway_schema_history：
└── version '20251218001': success = 0（失败标记）
```

---

## 📋 完整修复 SQL 脚本

如果你想一次性执行所有修复 SQL，可以复制这个：

```sql
-- ============================================================================
-- Flyway 迁移修复脚本
-- 数据库：阿里云 RDS (app)
-- 迁移版本：20251218001
-- ============================================================================

USE app;

-- 步骤 1：查看当前状态
SELECT '=== 当前 Flyway 状态 ===' AS info;
SELECT version, description, type, success, installed_on 
FROM flyway_schema_history 
WHERE version = '20251218001';

SELECT '=== 检查列是否存在 ===' AS info;
SELECT TABLE_NAME, COLUMN_NAME 
FROM INFORMATION_SCHEMA.COLUMNS 
WHERE TABLE_SCHEMA = 'app' 
  AND TABLE_NAME IN ('bc_payment_notify_log', 'bc_order')
  AND COLUMN_NAME IN ('notify_id', 'close_reason', 'closed_at');

SELECT '=== 检查索引是否存在 ===' AS info;
SELECT TABLE_NAME, INDEX_NAME 
FROM INFORMATION_SCHEMA.STATISTICS 
WHERE TABLE_SCHEMA = 'app' 
  AND TABLE_NAME = 'bc_payment_notify_log' 
  AND INDEX_NAME = 'uk_notify_id';

-- 步骤 2：删除部分添加的内容
SELECT '=== 开始清理 ===' AS info;

-- 删除索引（如果存在）
SET @sql = IF(
    EXISTS(
        SELECT 1 FROM INFORMATION_SCHEMA.STATISTICS 
        WHERE TABLE_SCHEMA = 'app' 
          AND TABLE_NAME = 'bc_payment_notify_log' 
          AND INDEX_NAME = 'uk_notify_id'
    ),
    'ALTER TABLE bc_payment_notify_log DROP INDEX uk_notify_id',
    'SELECT "索引 uk_notify_id 不存在，跳过" AS result'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 删除 notify_id 列（如果存在）
SET @sql = IF(
    EXISTS(
        SELECT 1 FROM INFORMATION_SCHEMA.COLUMNS 
        WHERE TABLE_SCHEMA = 'app' 
          AND TABLE_NAME = 'bc_payment_notify_log' 
          AND COLUMN_NAME = 'notify_id'
    ),
    'ALTER TABLE bc_payment_notify_log DROP COLUMN notify_id',
    'SELECT "列 notify_id 不存在，跳过" AS result'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 删除 close_reason 列（如果存在）
SET @sql = IF(
    EXISTS(
        SELECT 1 FROM INFORMATION_SCHEMA.COLUMNS 
        WHERE TABLE_SCHEMA = 'app' 
          AND TABLE_NAME = 'bc_order' 
          AND COLUMN_NAME = 'close_reason'
    ),
    'ALTER TABLE bc_order DROP COLUMN close_reason',
    'SELECT "列 close_reason 不存在，跳过" AS result'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 删除 closed_at 列（如果存在）
SET @sql = IF(
    EXISTS(
        SELECT 1 FROM INFORMATION_SCHEMA.COLUMNS 
        WHERE TABLE_SCHEMA = 'app' 
          AND TABLE_NAME = 'bc_order' 
          AND COLUMN_NAME = 'closed_at'
    ),
    'ALTER TABLE bc_order DROP COLUMN closed_at',
    'SELECT "列 closed_at 不存在，跳过" AS result'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 步骤 3：清理 Flyway 历史
SELECT '=== 清理 Flyway 历史 ===' AS info;
DELETE FROM flyway_schema_history WHERE version = '20251218001';

-- 步骤 4：验证
SELECT '=== 验证结果 ===' AS info;
SELECT 
    (SELECT COUNT(*) FROM flyway_schema_history WHERE version = '20251218001') AS flyway_count,
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = 'app' AND TABLE_NAME = 'bc_payment_notify_log' AND COLUMN_NAME = 'notify_id') AS notify_id_count,
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = 'app' AND TABLE_NAME = 'bc_order' AND COLUMN_NAME = 'close_reason') AS close_reason_count,
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = 'app' AND TABLE_NAME = 'bc_order' AND COLUMN_NAME = 'closed_at') AS closed_at_count,
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS WHERE TABLE_SCHEMA = 'app' AND TABLE_NAME = 'bc_payment_notify_log' AND INDEX_NAME = 'uk_notify_id') AS index_count;

SELECT '=== 修复完成 ===' AS info;
SELECT '下一步：重新启用 Flyway 并重启应用' AS next_step;

-- ============================================================================
```

---

## 🎯 推荐操作流程

1. **立即启动应用**（已配置好）
   ```bash
   mvn spring-boot:run -pl app-application -am -Dspring-boot.run.profiles=local
   ```

2. **连接 RDS 数据库**（使用你喜欢的客户端）

3. **执行完整修复 SQL**（复制上面的脚本）

4. **重新启用 Flyway**（修改配置文件）

5. **重启应用**（Flyway 会重新执行迁移）

---

## 📞 需要帮助？

如果遇到问题：

1. **无法连接 RDS**：
   - 检查 IP 白名单设置
   - 确认用户名密码正确
   - 检查 RDS 实例是否正常运行

2. **SQL 执行失败**：
   - 检查用户是否有 ALTER TABLE 权限
   - 查看具体错误信息

3. **应用还是启动失败**：
   - 查看详细日志
   - 确认 Flyway 配置是否正确

---

**创建时间**：2025-12-18  
**数据库**：阿里云 RDS (app)  
**状态**：就绪 ✅
