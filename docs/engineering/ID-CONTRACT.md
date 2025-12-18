# BlueCone ID 契约规范 v2

> **版本**: 2.0  
> **生效日期**: 2025-12-15  
> **状态**: 定版（LOCKED）

---

## 一、核心原则（不可变更）

### 1.1 三层 ID 体系

BlueCone 应用采用**三层 ID 体系**，各司其职：

| ID 类型 | 用途 | 数据类型 | 对外暴露 | 示例 |
|---------|------|----------|----------|------|
| **DB 主键 (PK)** | 数据库物理主键，性能优化 | `BIGINT` (long) | ❌ 否 | `1234567890` |
| **ULID** | 业务内部跨系统标识，可选 | `BINARY(16)` 或 `CHAR(26)` | ❌ 否 | `01HN8X5K9G3QRST2VW4XYZ` |
| **Public ID** | 对外 API 唯一标识 | `VARCHAR(64)` | ✅ 是 | `tnt_01HN8X5K9G3QRST2VW4XYZ` |

---

## 二、DB 主键（long ID）

### 2.1 硬性要求

- **数据类型**：`BIGINT NOT NULL AUTO_INCREMENT=false`（由应用层生成）
- **生成方式**：**号段模式（Segment）** 或 Snowflake（推荐号段）
- **唯一性保证**：全局唯一、单调递增（同一 scope 内）
- **对外暴露**：**严禁**在任何 API 响应或请求中使用

### 2.2 号段模式（推荐）

#### 2.2.1 核心表结构

```sql
CREATE TABLE bc_id_segment (
  scope VARCHAR(64) NOT NULL COMMENT 'ID 作用域，如 TENANT/STORE/ORDER',
  max_id BIGINT NOT NULL COMMENT '当前已分配的最大 ID',
  step INT NOT NULL DEFAULT 1000 COMMENT '每次分配的号段大小',
  updated_at DATETIME NOT NULL COMMENT '最后更新时间',
  PRIMARY KEY (scope)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='ID 号段分配表';
```

#### 2.2.2 分配算法

```java
// 伪代码
BEGIN TRANSACTION;
  SELECT max_id, step FROM bc_id_segment WHERE scope = ? FOR UPDATE;
  newMax = max_id + step;
  UPDATE bc_id_segment SET max_id = newMax, updated_at = NOW() WHERE scope = ?;
COMMIT;
// 返回区间 [max_id + 1, newMax]
```

#### 2.2.3 Scope 定义

| Scope | 说明 | 对应表 |
|-------|------|--------|
| `TENANT` | 租户 | `bc_tenant` |
| `STORE` | 门店 | `bc_store` |
| `ORDER` | 订单 | `bc_order` |
| `PRODUCT` | 商品 | `bc_product` |
| `SKU` | SKU | `bc_sku` |
| `USER` | 用户 | `bc_user` |
| `PAYMENT` | 支付 | `bc_payment` |
| `INVENTORY_RECORD` | 库存记录 | `bc_inventory_record` |

---

## 三、ULID（业务内部标识）

### 3.1 定义

- **格式**：26 字符 Crockford Base32 编码（如 `01HN8X5K9G3QRST2VW4XYZ`）
- **组成**：
  - 前 10 位：时间戳（48 位毫秒级）
  - 后 16 位：随机数（80 位熵）
- **特性**：
  - 字典序可排序
  - 全局唯一（概率极低碰撞）
  - 无需中心化协调

### 3.2 存储方式

| 存储类型 | MySQL 类型 | 长度 | 适用场景 |
|----------|-----------|------|----------|
| **二进制** | `BINARY(16)` | 16 字节 | 高性能查询、索引（推荐） |
| **字符串** | `CHAR(26)` | 26 字节 | 可读性优先、调试方便 |

### 3.3 使用场景

- 跨系统事件追踪（如订单 ID 传递到支付系统）
- 幂等性 Key（如 `request_id`）
- 分布式日志关联

### 3.4 约束

- **不作为 MySQL 主键**（性能考虑，使用 long ID）
- **不对外暴露**（使用 Public ID）

---

## 四、Public ID（对外 API 标识）

### 4.1 格式规范

```
<type_prefix>_<ulid>
```

- **type_prefix**：3-4 字符资源类型前缀（见下表）
- **分隔符**：下划线 `_`（固定）
- **ulid**：26 位 ULID 字符串

### 4.2 资源类型前缀表

| 资源类型 | 前缀 | 示例 Public ID |
|----------|------|----------------|
| 租户 (Tenant) | `tnt` | `tnt_01HN8X5K9G3QRST2VW4XYZ` |
| 门店 (Store) | `sto` | `sto_01HN8X5K9G3QRST2VW4XYZ` |
| 订单 (Order) | `ord` | `ord_01HN8X5K9G3QRST2VW4XYZ` |
| 商品 (Product) | `prd` | `prd_01HN8X5K9G3QRST2VW4XYZ` |
| SKU | `sku` | `sku_01HN8X5K9G3QRST2VW4XYZ` |
| 用户 (User) | `usr` | `usr_01HN8X5K9G3QRST2VW4XYZ` |
| 支付 (Payment) | `pay` | `pay_01HN8X5K9G3QRST2VW4XYZ` |

### 4.3 存储与索引

#### 4.3.1 表结构要求

所有核心业务表**必须**包含以下字段：

```sql
CREATE TABLE bc_xxx (
  id BIGINT NOT NULL COMMENT '主键（long ID，不对外）',
  tenant_id BIGINT NOT NULL COMMENT '租户 ID（用于分区隔离）',
  public_id VARCHAR(64) NOT NULL COMMENT '对外公开 ID',
  -- 其他业务字段...
  PRIMARY KEY (id),
  UNIQUE KEY uk_tenant_public_id (tenant_id, public_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

#### 4.3.2 索引策略

- **主键**：`id`（long，聚簇索引）
- **唯一索引**：`(tenant_id, public_id)`（API 查询必经路径）
- **禁止**：单独对 `public_id` 建唯一索引（无租户隔离）

### 4.4 API 使用规范

#### 4.4.1 请求参数

```json
POST /api/v1/stores/{store_public_id}/products
{
  "name": "商品A",
  "sku": "SKU001"
}
```

- **路径参数**：必须使用 `public_id`（如 `sto_01HN8X5K9G3QRST2VW4XYZ`）
- **请求体**：关联资源使用 `public_id`

#### 4.4.2 响应体

```json
{
  "id": "prd_01HN8X5K9G3QRST2VW4XYZ",  // 对外 public_id
  "name": "商品A",
  "store_id": "sto_01HN8X5K9G3QRST2VW4XYZ",
  "created_at": "2025-12-15T10:00:00Z"
}
```

- **禁止**：返回 `id`（long 型主键）或 ULID
- **必须**：所有 ID 字段均为 `public_id`

### 4.5 解析与校验

#### 4.5.1 格式校验

```java
// 伪代码
public void validatePublicId(ResourceType expectedType, String publicId) {
    // 1. 格式校验：prefix_ulid
    if (!publicId.matches("^[a-z]{3,4}_[0-9A-HJKMNP-TV-Z]{26}$")) {
        throw new InvalidPublicIdException("格式非法");
    }
    
    // 2. 前缀校验
    String prefix = publicId.substring(0, publicId.indexOf('_'));
    if (!prefix.equals(expectedType.prefix())) {
        throw new InvalidPublicIdException("类型不匹配");
    }
    
    // 3. ULID 合法性校验（可选，校验时间戳范围）
    String ulidPart = publicId.substring(publicId.indexOf('_') + 1);
    // ... ULID 解码校验
}
```

#### 4.5.2 查询策略

```java
// 伪代码
public Store findByPublicId(Long tenantId, String publicId) {
    // 使用 (tenant_id, public_id) 联合索引查询
    return storeRepository.findByTenantIdAndPublicId(tenantId, publicId);
}
```

---

## 五、实现清单

### 5.1 核心组件

| 组件 | 模块 | 说明 |
|------|------|------|
| `ResourceType` | app-id | 资源类型枚举（含 prefix） |
| `IdScope` | app-id | Long ID 作用域枚举 |
| `Ulid128` | app-id | ULID 值对象 |
| `IdService` | app-id | ID 生成门面接口 |
| `IdSegmentRepository` | app-id | 号段仓储 SPI |
| `SegmentLongIdGenerator` | app-id | 号段 Long ID 生成器 |
| `JdbcIdSegmentRepository` | app-infra | JDBC 号段仓储实现 |

### 5.2 DDL 脚本

- `docs/sql/bc_id_segment.sql`：号段表初始化脚本

### 5.3 配置项

```yaml
bluecone:
  id:
    enabled: true  # 启用 ID 模块（默认 true）
    segment:
      step: 1000  # 号段步长（默认 1000）
    public-id:
      separator: "_"  # 分隔符（默认 _）
```

---

## 六、迁移路径

### 6.1 新表

- 直接按本规范创建：`id BIGINT` + `public_id VARCHAR(64)` + 唯一索引

### 6.2 存量表

1. **添加字段**：`ALTER TABLE ADD COLUMN public_id VARCHAR(64)`
2. **数据回填**：为存量数据生成 `public_id`
3. **建立索引**：`CREATE UNIQUE INDEX uk_tenant_public_id ON xxx(tenant_id, public_id)`
4. **API 改造**：逐步替换为 `public_id`

---

## 七、禁止事项（NEVER）

1. ❌ **禁止**在 API 中暴露 long 型主键 `id`
2. ❌ **禁止**在 API 中暴露原始 ULID（必须包装为 Public ID）
3. ❌ **禁止**使用 `public_id` 作为 MySQL 主键
4. ❌ **禁止**跨租户查询时省略 `tenant_id`
5. ❌ **禁止**前端自行生成 Public ID（必须由后端生成）

---

## 八、FAQ

### Q1: 为什么不直接用 ULID 作为主键？

**A**: ULID 是 128 位，MySQL 索引性能不如 64 位 long；且 ULID 的随机部分会导致 B+ 树频繁分裂。

### Q2: 为什么需要 Public ID，不直接用 ULID？

**A**: 
- **类型安全**：前缀可防止 ID 误用（如把订单 ID 传给门店接口）
- **可读性**：一眼看出资源类型
- **版本演进**：未来可切换编码方式（如 Base62）而不影响 API

### Q3: 号段模式会成为瓶颈吗？

**A**: 
- 单次分配 1000 个 ID，本地缓存消耗后再申请
- 数据库仅在号段耗尽时访问一次
- 实测 QPS 可达 10 万+（远超业务需求）

### Q4: 如何保证 Public ID 全局唯一？

**A**: 
- ULID 本身全局唯一（128 位熵 + 时间戳）
- 数据库 `(tenant_id, public_id)` 唯一索引兜底

---

## 九、变更历史

| 版本 | 日期 | 变更内容 |
|------|------|----------|
| 2.0 | 2025-12-15 | 定版三层 ID 体系，明确号段模式，锁定 Public ID 格式 |
| 1.0 | 2025-11-01 | 初版，仅定义 ULID 使用规范 |

---

**本文档一经定版，任何变更需经架构评审委员会批准。**

