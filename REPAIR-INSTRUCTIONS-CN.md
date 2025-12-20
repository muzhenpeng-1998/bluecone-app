# Flyway 修复指南（中文版）

## 🚨 问题

你的 Spring Boot 应用无法启动，错误信息：
```
FlywayValidateException: Validate failed: Migrations have failed validation
Detected failed migration to version 20251218001 (add payment notify id).
```

## 📋 原因

数据库迁移脚本 `V20251218001__add_payment_notify_id.sql` 执行到一半失败了，导致：
- 数据库处于不一致状态（部分列已添加，部分未添加）
- Flyway 标记迁移为"失败"
- 应用拒绝启动（为了保护数据完整性）

## 🔧 解决方案

### 前提条件：启动 MySQL

首先确保 MySQL 服务正在运行：

#### 方法 1：使用 Homebrew（如果你用 Homebrew 安装的 MySQL）
```bash
# 启动 MySQL
brew services start mysql

# 检查 MySQL 状态
brew services list | grep mysql
```

#### 方法 2：手动启动 MySQL
```bash
# 启动 MySQL 服务器
mysql.server start

# 或者使用系统命令
sudo /usr/local/mysql/support-files/mysql.server start
```

#### 方法 3：使用 Docker（如果你用 Docker 运行 MySQL）
```bash
# 启动 MySQL 容器
docker start mysql

# 或者如果没有容器，运行一个新的
docker run -d --name mysql \
  -e MYSQL_ROOT_PASSWORD=yourpassword \
  -e MYSQL_DATABASE=bluecone \
  -p 3306:3306 \
  mysql:8
```

### 验证 MySQL 已启动

```bash
# 检查 MySQL 进程
ps aux | grep mysql

# 或者尝试连接
mysql -h localhost -P 3306 -u root -p
```

---

## 🚀 方法 1：使用 Java 修复工具（推荐）

### 步骤 1：启动 MySQL（见上文）

### 步骤 2：修改数据库密码（如果需要）

编辑文件 `SimpleFlywayRepair.java`，找到这一行：
```java
private static final String DB_PASSWORD = ""; // 根据需要修改
```

改为你的 MySQL root 密码：
```java
private static final String DB_PASSWORD = "your_mysql_password";
```

### 步骤 3：重新编译并运行

```bash
cd /Users/zhenpengmu/Desktop/code/project/bluecone-app

# 重新编译
javac -cp ".:app-application/target/bluecone-app.jar" SimpleFlywayRepair.java

# 运行修复工具
java -cp ".:app-application/target/bluecone-app.jar:/Users/zhenpengmu/.m2/repository/mysql/mysql-connector-java/8.0.26/mysql-connector-java-8.0.26.jar" SimpleFlywayRepair
```

### 期望输出

```
=================================================================
简化 Flyway 修复工具
=================================================================

✓ 数据库连接成功

[步骤 1] 检查当前数据库状态...
   ✓ 列 bc_payment_notify_log.notify_id 存在
   ✓ 列 bc_order.close_reason 存在
   ✗ 索引 bc_payment_notify_log.uk_notify_id 不存在

[步骤 2] 回滚部分变更...
   ✓ 已删除列 bc_payment_notify_log.notify_id
   ✓ 已删除列 bc_order.close_reason
   ✓ 已删除列 bc_order.closed_at

[步骤 3] 清理 Flyway 历史...
   ✓ 已从 flyway_schema_history 删除 1 条记录

[步骤 4] 验证清理结果...
   ✓ 列 bc_payment_notify_log.notify_id 已删除
   ✓ 列 bc_order.close_reason 已删除
   ✓ 列 bc_order.closed_at 已删除
   ✓ Flyway 历史已清理

=================================================================
✓ 修复完成！
=================================================================

下一步：
  重启你的 Spring Boot 应用
  Flyway 将自动重新执行迁移
```

### 步骤 4：重启应用

```bash
mvn spring-boot:run -pl app-application -am -Dspring-boot.run.profiles=local
```

---

## 🔧 方法 2：手动修复（使用 MySQL 命令行）

如果你熟悉 MySQL，可以手动执行 SQL：

### 步骤 1：连接到 MySQL

```bash
mysql -h localhost -P 3306 -u root -p bluecone
```

### 步骤 2：查看当前状态

```sql
-- 查看哪些列已经添加
SELECT COLUMN_NAME 
FROM INFORMATION_SCHEMA.COLUMNS 
WHERE TABLE_SCHEMA = 'bluecone' 
  AND TABLE_NAME = 'bc_payment_notify_log' 
  AND COLUMN_NAME = 'notify_id';

SELECT COLUMN_NAME 
FROM INFORMATION_SCHEMA.COLUMNS 
WHERE TABLE_SCHEMA = 'bluecone' 
  AND TABLE_NAME = 'bc_order' 
  AND COLUMN_NAME IN ('close_reason', 'closed_at');

-- 查看 Flyway 历史
SELECT * FROM flyway_schema_history WHERE version = '20251218001';
```

### 步骤 3：删除部分添加的列

```sql
-- 删除索引（如果存在）
ALTER TABLE bc_payment_notify_log DROP INDEX uk_notify_id;

-- 删除列
ALTER TABLE bc_payment_notify_log DROP COLUMN notify_id;
ALTER TABLE bc_order DROP COLUMN close_reason;
ALTER TABLE bc_order DROP COLUMN closed_at;
```

**注意**：如果某个列或索引不存在，MySQL 会报错，这是正常的，继续执行下一条命令即可。

### 步骤 4：清理 Flyway 历史

```sql
DELETE FROM flyway_schema_history WHERE version = '20251218001';
```

### 步骤 5：验证

```sql
-- 应该返回 0
SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS 
WHERE TABLE_SCHEMA = 'bluecone' 
  AND TABLE_NAME = 'bc_payment_notify_log' 
  AND COLUMN_NAME = 'notify_id';

-- 应该返回 0
SELECT COUNT(*) FROM flyway_schema_history WHERE version = '20251218001';
```

### 步骤 6：退出并重启应用

```sql
EXIT;
```

然后：
```bash
mvn spring-boot:run -pl app-application -am -Dspring-boot.run.profiles=local
```

---

## 🔧 方法 3：使用 Shell 脚本（需要 MySQL 客户端）

如果你已经安装了 MySQL 客户端，可以使用提供的 shell 脚本：

```bash
# 设置环境变量
export DB_HOST=localhost
export DB_PORT=3306
export DB_NAME=bluecone
export DB_USERNAME=root
export DB_PASSWORD=your_mysql_password

# 运行修复脚本
./fix-flyway.sh
```

---

## ❓ 故障排除

### 问题 1：MySQL 未启动

**错误**：`Connection refused` 或 `Communications link failure`

**解决**：
```bash
# 检查 MySQL 是否运行
ps aux | grep mysql

# 如果没运行，启动它
brew services start mysql
# 或
mysql.server start
```

### 问题 2：数据库密码错误

**错误**：`Access denied for user 'root'@'localhost'`

**解决**：
1. 修改 `SimpleFlywayRepair.java` 中的密码
2. 或者在连接 MySQL 时输入正确的密码

### 问题 3：数据库不存在

**错误**：`Unknown database 'bluecone'`

**解决**：
```bash
# 创建数据库
mysql -u root -p -e "CREATE DATABASE IF NOT EXISTS bluecone CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;"
```

### 问题 4：表不存在

**错误**：`Table 'bluecone.bc_payment_notify_log' doesn't exist`

**说明**：这说明之前的迁移脚本还没执行。解决方法：
1. 直接运行修复工具（它会优雅地处理表不存在的情况）
2. 或者先运行应用，让 Flyway 创建所有表，然后再修复

### 问题 5：修复后应用还是失败

**可能原因**：
1. 还有其他失败的迁移
2. 数据库配置不正确

**解决**：
```sql
-- 查看所有失败的迁移
SELECT * FROM flyway_schema_history WHERE success = 0;

-- 查看最近的迁移
SELECT * FROM flyway_schema_history ORDER BY installed_rank DESC LIMIT 10;
```

---

## 📚 相关文档

- **英文完整指南**：`FLYWAY-MIGRATION-REPAIR-GUIDE.md`
- **快速指南**：`QUICK-FIX-FLYWAY.md`
- **问题总结**：`FLYWAY-REPAIR-SUMMARY.md`

---

## 💡 预防措施

为了避免将来再次出现这个问题：

1. **本地开发时使用容错配置**
   - 在 `application-local.yml` 中已经配置了 `ignore-migration-patterns: "*:missing"`

2. **迁移前备份数据库**
   ```bash
   mysqldump -u root -p bluecone > backup_$(date +%Y%m%d_%H%M%S).sql
   ```

3. **测试迁移脚本**
   - 在本地数据库先测试新的迁移脚本
   - 确保没有语法错误和数据问题

4. **使用事务（注意限制）**
   - MySQL 的 DDL 语句（ALTER TABLE）不支持事务回滚
   - 建议将复杂迁移拆分成多个小迁移

---

## 🎯 总结

### 快速修复流程

1. ✅ 确保 MySQL 已启动
2. ✅ 运行修复工具：`SimpleFlywayRepair.java`
3. ✅ 重启应用

### 命令速查

```bash
# 1. 启动 MySQL
brew services start mysql

# 2. 运行修复（确保密码正确）
cd /Users/zhenpengmu/Desktop/code/project/bluecone-app
java -cp ".:app-application/target/bluecone-app.jar:/Users/zhenpengmu/.m2/repository/mysql/mysql-connector-java/8.0.26/mysql-connector-java-8.0.26.jar" SimpleFlywayRepair

# 3. 重启应用
mvn spring-boot:run -pl app-application -am -Dspring-boot.run.profiles=local
```

---

**创建时间**：2025-12-18  
**适用版本**：V20251218001  
**状态**：就绪 ✅
