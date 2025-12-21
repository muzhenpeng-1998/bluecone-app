# app-id 最小可用指南

## 概述

`app-id` 模块提供统一的 ID 生成能力，支持：

- **ULID**（26 位字符串）：用于业务内部标识
- **long ID**（63 位正整数）：用于数据库主键
- **Public ID**（带前缀的对外 ID）：格式为 `prefix_ulid`

## 零配置启动

**默认行为：**
- ✅ ULID 生成：默认启用
- ✅ long ID 生成：默认启用（使用 SNOWFLAKE 策略）
- ✅ Public ID 生成：默认启用

**无需任何配置即可使用：**

```java
@Autowired
private IdService idService;

// 生成 ULID
String ulid = idService.nextUlidString();

// 生成 long ID（默认使用 Snowflake）
long orderId = idService.nextLong(IdScope.ORDER);

// 生成 Public ID
String publicOrderId = idService.nextPublicId(ResourceType.ORDER);
```

## 默认策略

### ULID（String ID）
- **算法**：ULID（Universally Unique Lexicographically Sortable Identifier）
- **长度**：26 位字符串
- **特性**：时间有序、线程安全、高并发下不重复

### long ID（默认：SNOWFLAKE）
- **算法**：Snowflake（Twitter 分布式 ID）
- **位布局**：time(41) | node(10) | seq(12)
- **epoch**：2024-01-01T00:00:00Z (1704067200000L)
- **nodeId**：自动派生（基于主机名/环境变量/进程信息）

## 多实例部署（必读）

### 单实例/少量实例
无需配置，系统会自动派生 nodeId。

### 多实例扩容（生产环境）
**必须显式配置 nodeId 以避免 ID 冲突：**

#### 方式 1：配置文件（推荐）
```yaml
bluecone:
  id:
    long:
      node-id: 0  # 每个实例配置不同的值（0~1023）
```

#### 方式 2：环境变量
```bash
export BLUECONE_NODE_ID=0  # 每个实例配置不同的值（0~1023）
```

#### 方式 3：容器编排（Kubernetes）
```yaml
env:
  - name: BLUECONE_NODE_ID
    valueFrom:
      fieldRef:
        fieldPath: metadata.name  # 使用 Pod 名称派生
```

## 高级配置

### 切换到号段模式（SEGMENT）

号段模式特点：
- ✅ 高性能：本地缓存号段，减少数据库访问
- ✅ 无时钟依赖：避免 Snowflake 的时钟回拨问题
- ⚠️ 需要数据库表支持

**启用条件（必须同时满足）：**

1. 配置启用号段模式：
```yaml
bluecone:
  id:
    long:
      strategy: SEGMENT  # 切换到号段模式
    segment:
      enabled: true      # 显式启用号段
      step: 1000         # 号段步长（可选，默认 1000）
```

2. 创建数据库表：
```sql
CREATE TABLE bc_id_segment (
    scope VARCHAR(64) PRIMARY KEY,
    max_id BIGINT NOT NULL,
    step INT NOT NULL,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);
```

3. 确保 `app-infra` 模块提供 `IdSegmentRepository` Bean。

### 禁用 long ID
```yaml
bluecone:
  id:
    long:
      enabled: false
```

### 自定义 epoch
```yaml
bluecone:
  id:
    long:
      epoch-millis: 1704067200000  # 2024-01-01T00:00:00Z
```

## 配置优先级

1. **配置属性**：`bluecone.id.long.node-id`
2. **环境变量**：`BLUECONE_NODE_ID`（兼容 `BLUECONE_ID_NODE_ID`）
3. **自动派生**：基于主机信息（仅适用于单实例/少量实例）

## 常见问题

### Q: 为什么多实例必须配置 nodeId？
A: Snowflake 算法依赖 nodeId 来保证多实例间 ID 不冲突。自动派生的 nodeId 基于主机信息，无法保证多实例绝对不冲突。

### Q: 如何选择 SNOWFLAKE 还是 SEGMENT？
A: 
- **SNOWFLAKE**：零配置可用，适合大多数场景
- **SEGMENT**：需要数据库表，适合对性能要求极高且可接受号段管理复杂度的场景

### Q: 时钟回拨会导致 ID 重复吗？
A: 不会。Snowflake 实现在检测到时钟回拨时会使用上一次时间戳，保证单调性，不会产生重复 ID。

### Q: 时间戳会溢出吗？
A: 41 位时间戳可用约 69 年（从 epoch 开始）。使用 2024-01-01 作为 epoch，可用到 2093 年。

## 架构治理

- ✅ 业务模块只能依赖 `app-id-api`（接口模块）
- ❌ 禁止依赖 `app-id`（实现模块）
- ✅ `app-id.internal.*` 包不得在其它模块被引用

Maven enforcer 已配置门禁，违反规则会导致编译失败。

