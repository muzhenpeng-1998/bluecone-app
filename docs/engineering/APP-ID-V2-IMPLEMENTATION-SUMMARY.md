# BlueCone App-ID v2 实现总结

> **实施日期**: 2025-12-15  
> **状态**: 核心功能已完成，待数据库迁移

---

## 一、实施概览

本次实施完成了 BlueCone ID v2 系统的核心功能，实现了**三层 ID 体系**：

| ID 类型 | 用途 | 实现状态 | 说明 |
|---------|------|----------|------|
| **DB 主键 (long)** | 数据库物理主键 | ✅ 已完成 | 基于号段模式，高性能、无时钟依赖 |
| **ULID** | 业务内部标识 | ✅ 已完成 | 128 位全局唯一，字典序可排序 |
| **Public ID** | 对外 API 标识 | ✅ 已完成 | 格式：`prefix_ulid`，带类型前缀 |

---

## 二、已完成的工作

### 2.1 文档与契约

- ✅ **ID-CONTRACT.md**：定版三层 ID 体系规范，明确使用约束
- ✅ **bc_id_segment.sql**：号段表 DDL 及初始化脚本

### 2.2 核心类型与接口

| 组件 | 路径 | 说明 |
|------|------|------|
| `IdScope` | app-id/api | ID 作用域枚举（TENANT/STORE/ORDER 等） |
| `ResourceType` | app-id/api | 资源类型枚举（含前缀：tnt/sto/ord 等） |
| `Ulid128` | app-id/core | ULID 值对象（已存在，复用） |
| `IdService` | app-id/api | 统一 ID 门面接口（新增 nextLong/validatePublicId） |

### 2.3 号段模式实现

| 组件 | 路径 | 说明 |
|------|------|------|
| `SegmentRange` | app-id/segment | 号段范围值对象 |
| `IdSegmentRepository` | app-id/segment | 号段仓储 SPI（接口） |
| `SegmentLongIdGenerator` | app-id/segment | 号段 Long ID 生成器（本地缓存 + 并发安全） |
| `JdbcIdSegmentRepository` | app-infra/id | JDBC 号段仓储实现（事务 + FOR UPDATE） |

### 2.4 增强版 IdService

| 组件 | 路径 | 说明 |
|------|------|------|
| `EnhancedIdService` | app-id/core | 完整实现三层 ID 能力 |
| `PublicIdFactory.validate()` | app-id/core | Public ID 格式/类型/ULID 校验 |
| `IdSegmentAutoConfiguration` | app-infra/id | 自动装配号段仓储和增强版 IdService |

### 2.5 配置支持

- ✅ **BlueconeIdProperties.Segment**：号段模式配置（step、enabled）
- ✅ **META-INF/spring/...AutoConfiguration.imports**：自动装配注册

### 2.6 测试覆盖

| 测试类 | 路径 | 测试内容 |
|--------|------|----------|
| `SegmentLongIdGeneratorTest` | app-id/test | 100 线程 * 10000 次并发唯一性/单调性 |
| `EnhancedIdServiceTest` | app-id/test | ULID/Long ID/Public ID 生成与校验 |
| `JdbcIdSegmentRepositoryIT` | app-infra/test | Testcontainers MySQL 集成测试 |

**测试结果**：✅ 全部通过（mvn test）

### 2.7 业务集成

- ✅ **门店创建链路**：已更新为 `idService.nextLong(IdScope.STORE)`
  - `StoreCommandService.java`
  - `AdminStoreController.java`

---

## 三、待完成的工作

### 3.1 数据库迁移（高优先级）

#### 3.1.1 创建号段表

```sql
-- 执行 docs/sql/bc_id_segment.sql
-- 或通过 Flyway 迁移脚本
```

#### 3.1.2 为核心表添加 public_id 字段

需要为以下表添加 `public_id` 字段和索引：

```sql
-- 租户表
ALTER TABLE bc_tenant 
  ADD COLUMN public_id VARCHAR(64) NULL COMMENT '对外公开 ID（格式：tnt_ulid）',
  ADD UNIQUE KEY uk_tenant_public_id (public_id);

-- 门店表
ALTER TABLE bc_store 
  ADD COLUMN public_id VARCHAR(64) NULL COMMENT '对外公开 ID（格式：sto_ulid）',
  ADD UNIQUE KEY uk_tenant_public_id (tenant_id, public_id);

-- 订单表
ALTER TABLE bc_order 
  ADD COLUMN public_id VARCHAR(64) NULL COMMENT '对外公开 ID（格式：ord_ulid）',
  ADD UNIQUE KEY uk_tenant_public_id (tenant_id, public_id);

-- 商品表
ALTER TABLE bc_product 
  ADD COLUMN public_id VARCHAR(64) NULL COMMENT '对外公开 ID（格式：prd_ulid）',
  ADD UNIQUE KEY uk_tenant_public_id (tenant_id, public_id);

-- SKU 表
ALTER TABLE bc_sku 
  ADD COLUMN public_id VARCHAR(64) NULL COMMENT '对外公开 ID（格式：sku_ulid）',
  ADD UNIQUE KEY uk_tenant_public_id (tenant_id, public_id);

-- 用户表
ALTER TABLE bc_user 
  ADD COLUMN public_id VARCHAR(64) NULL COMMENT '对外公开 ID（格式：usr_ulid）',
  ADD UNIQUE KEY uk_tenant_public_id (tenant_id, public_id);

-- 支付表
ALTER TABLE bc_payment 
  ADD COLUMN public_id VARCHAR(64) NULL COMMENT '对外公开 ID（格式：pay_ulid）',
  ADD UNIQUE KEY uk_tenant_public_id (tenant_id, public_id);
```

#### 3.1.3 存量数据回填

```java
// 伪代码
for (Tenant tenant : allTenants) {
    if (tenant.getPublicId() == null) {
        String publicId = idService.nextPublicId(ResourceType.TENANT);
        tenant.setPublicId(publicId);
        tenantMapper.updateById(tenant);
    }
}
```

### 3.2 业务层接入（中优先级）

#### 3.2.1 租户创建链路

更新 `TenantApplicationServiceImpl.createTenant()`：

```java
@Override
@Transactional
public Long createTenant(CreateTenantCommand command) {
    Tenant tenant = new Tenant();
    
    // 生成 long 型主键
    long id = idService.nextLong(IdScope.TENANT);
    tenant.setId(id);
    
    // 生成 Public ID
    String publicId = idService.nextPublicId(ResourceType.TENANT);
    tenant.setPublicId(publicId);
    
    // ... 其他字段设置
    
    tenantService.save(tenant);
    return tenant.getId();
}
```

#### 3.2.2 其他创建链路

- 订单创建：`OrderCommandService`
- 商品创建：`ProductCommandService`
- 用户注册：`UserService`
- 支付创建：`PaymentService`

### 3.3 API 层改造（低优先级）

#### 3.3.1 请求参数改用 Public ID

```java
// 旧版
@GetMapping("/tenants/{tenantId}")
public TenantDetail getTenant(@PathVariable Long tenantId) { ... }

// 新版
@GetMapping("/tenants/{publicId}")
public TenantDetail getTenant(@PathVariable String publicId) {
    // 校验格式
    idService.validatePublicId(ResourceType.TENANT, publicId);
    
    // 查询（使用 tenant_id + public_id 索引）
    Tenant tenant = tenantService.findByPublicId(publicId);
    ...
}
```

#### 3.3.2 响应体改用 Public ID

```java
// 旧版
{
  "id": 123456,
  "tenantName": "测试租户"
}

// 新版
{
  "id": "tnt_01HN8X5K9G3QRST2VW4XYZ",
  "tenantName": "测试租户"
}
```

---

## 四、关键设计决策

### 4.1 为什么选择号段模式而非 Snowflake？

| 维度 | 号段模式 | Snowflake |
|------|----------|-----------|
| **时钟依赖** | ❌ 无 | ✅ 强依赖系统时钟 |
| **时钟回拨** | ❌ 不受影响 | ⚠️ 需特殊处理（可能阻塞或报错） |
| **数据库访问** | ✅ 每 1000 个 ID 访问一次 | ❌ 无需访问数据库 |
| **性能** | ✅ 10 万+ QPS | ✅ 100 万+ QPS |
| **运维复杂度** | ✅ 低（仅需数据库） | ⚠️ 中（需保证 nodeId 唯一） |

**结论**：号段模式性能足够（远超业务需求），且无时钟回拨风险，更适合 BlueCone 场景。

### 4.2 为什么需要 Public ID？

1. **类型安全**：前缀防止 ID 误用（如把订单 ID 传给门店接口）
2. **可读性**：一眼看出资源类型（`tnt_xxx` vs `sto_xxx`）
3. **版本演进**：未来可切换编码方式（如 Base62）而不影响 API
4. **安全性**：避免暴露 long 型主键（防止遍历攻击）

### 4.3 为什么 DB 主键不直接用 ULID？

1. **索引性能**：ULID 是 128 位，MySQL B+ 树索引效率低于 64 位 long
2. **页分裂**：ULID 的随机部分会导致 B+ 树频繁分裂，影响写入性能
3. **存储成本**：BINARY(16) 比 BIGINT 多占用 8 字节

---

## 五、性能指标

### 5.1 理论性能

- **号段分配**：单次数据库访问分配 1000 个 ID
- **本地生成**：AtomicLong.incrementAndGet()，纳秒级
- **预估 QPS**：单机 10 万+（远超业务需求）

### 5.2 测试验证

- ✅ **并发唯一性**：100 线程 * 10000 次 = 100 万个 ID 全部唯一
- ✅ **单调性**：同一 scope 内 ID 单调递增
- ✅ **线程安全**：多线程并发申请号段无重叠

---

## 六、使用示例

### 6.1 生成 Long ID

```java
@Autowired
private IdService idService;

public void createOrder() {
    long orderId = idService.nextLong(IdScope.ORDER);
    // orderId = 1, 2, 3, ... (单调递增)
}
```

### 6.2 生成 Public ID

```java
String publicId = idService.nextPublicId(ResourceType.TENANT);
// publicId = "tnt_01HN8X5K9G3QRST2VW4XYZ"
```

### 6.3 校验 Public ID

```java
try {
    idService.validatePublicId(ResourceType.STORE, "sto_01HN8X5K9G3QRST2VW4XYZ");
    // 校验通过
} catch (IllegalArgumentException e) {
    // 格式非法 / 类型不匹配 / ULID 非法
}
```

---

## 七、配置说明

### 7.1 application.yml

```yaml
bluecone:
  id:
    enabled: true  # 启用 ID 模块（默认 true）
    
    segment:
      enabled: true  # 启用号段模式（默认 true）
      step: 1000     # 号段步长（默认 1000）
    
    public-id:
      enabled: true       # 启用 Public ID（默认 true）
      separator: "_"      # 分隔符（默认 _）
      lower-case: true    # ULID 小写（默认 true）
```

### 7.2 自动装配

- **app-id**：提供 `UlidIdGenerator`、`IdService`（基础版）
- **app-infra**：提供 `JdbcIdSegmentRepository`、`EnhancedIdService`（增强版）

**优先级**：app-infra 的 `EnhancedIdService` 会覆盖 app-id 的默认 `IdService`

---

## 八、注意事项

### 8.1 禁止事项

1. ❌ **禁止**在 API 中暴露 long 型主键 `id`
2. ❌ **禁止**在 API 中暴露原始 ULID（必须包装为 Public ID）
3. ❌ **禁止**使用 `public_id` 作为 MySQL 主键
4. ❌ **禁止**跨租户查询时省略 `tenant_id`
5. ❌ **禁止**前端自行生成 Public ID（必须由后端生成）

### 8.2 最佳实践

1. ✅ **创建实体时同时生成 long ID 和 Public ID**
2. ✅ **API 查询使用 `(tenant_id, public_id)` 联合索引**
3. ✅ **Public ID 校验在 Controller 层完成**
4. ✅ **存量数据迁移时先回填 Public ID，再改 API**

---

## 九、下一步行动

### 9.1 立即执行（P0）

1. 执行 `docs/sql/bc_id_segment.sql` 创建号段表
2. 为核心表添加 `public_id` 字段和索引
3. 更新租户创建链路接入 ID v2

### 9.2 短期规划（P1）

1. 存量数据回填 Public ID
2. 更新订单/商品/用户创建链路
3. API 层逐步改造（先兼容，后切换）

### 9.3 长期规划（P2）

1. 监控号段消耗速度，优化 step 配置
2. 评估是否需要预分配（双 buffer）
3. 考虑 Public ID 编码格式演进（Base62/Base58）

---

## 十、相关文档

- [ID-CONTRACT.md](./ID-CONTRACT.md)：ID 契约规范（定版）
- [bc_id_segment.sql](../sql/bc_id_segment.sql)：号段表 DDL
- [app-id README](../../app-id/README-public-id.md)：Public ID 使用指南

---

**实施人员**: AI Assistant  
**审核状态**: 待人工审核  
**最后更新**: 2025-12-15

