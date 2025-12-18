# Flyway 历史库修复指南

## 问题描述

Flyway 校验失败，错误信息：
- `Detected applied migration not resolved locally: 1`
- `Detected applied migration not resolved locally: 2`
- `Detected resolved migration not applied to database: 2027`
- `Detected resolved migration not applied to database: 20251216`

## 原因

数据库是历史库，已有一些迁移记录，但：
1. 数据库中有版本 1、2 的迁移记录，但本地代码中没有这些脚本
2. 本地有新的迁移脚本（V2027、V20251216）还没有执行

## 解决方案

### 方案 1：使用配置修复（已应用）

已在 `application.yml` 中添加：
```yaml
spring:
  flyway:
    validate-on-migrate: true
    baseline-on-migrate: true      # 自动创建 baseline
    out-of-order: true              # 允许乱序执行
    ignore-migration-patterns: "*:missing"  # 忽略缺失的旧迁移
```

### 方案 2：手动修复 Flyway 历史表（如果方案 1 不行）

连接到数据库，执行以下 SQL：

```sql
-- 查看当前 Flyway 历史记录
SELECT * FROM flyway_schema_history ORDER BY installed_rank;

-- 删除不存在的迁移记录（版本 1 和 2）
DELETE FROM flyway_schema_history WHERE version IN ('1', '2');

-- 或者标记为已删除
UPDATE flyway_schema_history 
SET type = 'DELETE' 
WHERE version IN ('1', '2');
```

### 方案 3：重置 Flyway 历史（仅用于开发环境）

**警告**：这会删除所有 Flyway 历史记录，仅用于开发环境！

```sql
-- 删除 Flyway 历史表
DROP TABLE IF EXISTS flyway_schema_history;

-- 重新启动应用，Flyway 会重新创建历史表并执行所有迁移
```

## 验证

重新启动应用后，检查日志中是否有：
```
Flyway: Successfully applied X migration(s)
```

如果仍有问题，可以查看 Flyway 历史表：
```sql
SELECT version, description, type, installed_on, success 
FROM flyway_schema_history 
ORDER BY installed_rank;
```

