# 数据库重置指南（全新库处理）

## 说明

如果数据库是全新库或需要重置所有表，可以按照以下步骤操作。

## 方法 1：重置 Flyway 历史（推荐）

### 步骤 1：连接到数据库

```bash
mysql -h localhost -P 3306 -u root -p bluecone
```

### 步骤 2：执行重置脚本

```sql
-- 删除 Flyway 历史表
DROP TABLE IF EXISTS flyway_schema_history;

-- 可选：删除所有业务表（如果需要完全重置）
-- 注意：根据实际情况取消注释
DROP TABLE IF EXISTS bc_store CASCADE;
DROP TABLE IF EXISTS bc_store_capability CASCADE;
DROP TABLE IF EXISTS bc_store_opening_hours CASCADE;
DROP TABLE IF EXISTS bc_store_special_day CASCADE;
DROP TABLE IF EXISTS bc_store_channel CASCADE;
DROP TABLE IF EXISTS bc_store_read_model CASCADE;
DROP TABLE IF EXISTS bc_user CASCADE;
DROP TABLE IF EXISTS bc_auth_session CASCADE;
DROP TABLE IF EXISTS t_order CASCADE;
DROP TABLE IF EXISTS bc_outbox_message CASCADE;
DROP TABLE IF EXISTS bc_config_property CASCADE;
-- ... 其他表
```

### 步骤 3：重新启动应用

重新启动应用后，Flyway 会：
- 自动创建 `flyway_schema_history` 表
- 执行所有迁移脚本（从最早的版本开始）

```bash
mvn -pl app-application -am spring-boot:run -Dspring-boot.run.profiles=local
```

## 方法 2：使用 Flyway Clean（开发环境）

**警告**：此方法会删除所有表，仅用于开发环境！

在 `application-local.yml` 中临时添加：

```yaml
spring:
  flyway:
    clean-disabled: false  # 允许 Flyway clean
```

然后使用 Flyway Maven 插件清理：

```bash
mvn flyway:clean -pl app-application
```

清理后重新启动应用，Flyway 会重新执行所有迁移。

## 方法 3：手动删除并重建数据库

```sql
-- 1. 删除数据库
DROP DATABASE IF EXISTS bluecone;

-- 2. 重新创建数据库
CREATE DATABASE bluecone CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- 3. 重新启动应用，Flyway 会自动执行所有迁移
```

## 验证

重置后，检查 Flyway 历史表：

```sql
SELECT version, description, type, installed_on, success 
FROM flyway_schema_history 
ORDER BY installed_rank;
```

应该看到所有迁移脚本都已执行，包括：
- V2024010101__init_core_tables.sql
- V2024__create_bc_outbox_message.sql
- V2025__create_scheduler_tables.sql
- V2025120601__create_payment_tables.sql
- V2025120602__create_resource_tables.sql
- V2025120901__create_webhook_config_table.sql
- V20251216__create_store_tables.sql
- V2026__create_integration_tables.sql

## 注意事项

1. **生产环境禁止**：这些操作仅用于开发环境
2. **备份数据**：如果数据库中有重要数据，请先备份
3. **迁移顺序**：Flyway 会按照版本号顺序执行迁移脚本

