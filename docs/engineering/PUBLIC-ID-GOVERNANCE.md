# Public ID Governance Kit（Public ID 强制治理）

本文档详细说明 BlueCone 平台的 Public ID 治理体系，包括设计原理、使用方式、接入指南和常见问题。

## 目录

- [1. 为什么需要 Public ID 治理](#1-为什么需要-public-id-治理)
- [2. 核心组件](#2-核心组件)
- [3. 使用指南](#3-使用指南)
- [4. Scope Guard 策略](#4-scope-guard-策略)
- [5. 错误码与排查](#5-错误码与排查)
- [6. 新模块接入 Checklist](#6-新模块接入-checklist)
- [7. 架构规则（ArchUnit）](#7-架构规则archunit)
- [8. 性能优化](#8-性能优化)
- [9. FAQ](#9-faq)

---

## 1. 为什么需要 Public ID 治理

### 1.1 问题背景

**传统做法（不推荐）：**

```java
// ❌ 问题：暴露内部自增主键
@GetMapping("/stores/{id}")
public StoreView detail(@PathVariable Long id) {
    return storeService.getDetail(id);
}
```

**存在的风险：**

1. **枚举攻击**：攻击者可遍历 id=1,2,3... 获取所有门店信息
2. **信息泄露**：通过 id 大小推测业务规模（id=100000 说明有 10 万门店）
3. **越权访问**：租户 A 可通过猜测 id 访问租户 B 的资源
4. **耦合性强**：前端/三方系统直接依赖内部主键，数据迁移困难

### 1.2 解决方案

**Public ID 治理（推荐）：**

```java
// ✅ 推荐：使用 publicId（String）+ 自动解析 + Scope Guard
@GetMapping("/stores/{storeId}")
public StoreView detail(
    @PathVariable("storeId") @ResolvePublicId(type=STORE) Long storePk) {
    return storeService.getDetail(storePk);
}
```

**优势：**

1. **防枚举**：publicId 基于 ULID（128 位），无法枚举
2. **隐藏规模**：外部无法通过 publicId 推测业务规模
3. **自动校验**：Scope Guard 自动校验租户/门店权限，防止越权
4. **解耦**：内部主键可随时更换，不影响外部接口

---

## 2. 核心组件

### 2.1 组件架构

```
┌─────────────────────────────────────────────────────────┐
│                     Controller 层                        │
│  @ResolvePublicId 注解 + PublicIdGovernanceArgumentResolver │
└─────────────────────────────────────────────────────────┘
                            ↓
┌─────────────────────────────────────────────────────────┐
│              PublicIdGovernanceResolver                  │
│  1. 格式校验（IdService.validatePublicId）               │
│  2. 查找 Lookup（根据 ResourceType 路由）                │
│  3. 查询主键（PublicIdLookup.findInternalId）            │
│  4. Scope Guard 校验（防越权）                           │
└─────────────────────────────────────────────────────────┘
                            ↓
┌─────────────────────────────────────────────────────────┐
│                   PublicIdLookup（SPI）                  │
│  - StorePublicIdLookup：查询 bc_store 表                │
│  - ProductPublicIdLookup：查询 bc_product 表            │
│  - SkuPublicIdLookup：查询 bc_product_sku 表            │
└─────────────────────────────────────────────────────────┘
                            ↓
┌─────────────────────────────────────────────────────────┐
│                     数据库（业务表）                      │
│  索引：(tenant_id, public_id) UNIQUE                     │
└─────────────────────────────────────────────────────────┘
```

### 2.2 核心接口

#### 2.2.1 PublicIdLookup（SPI）

业务模块需实现此接口，提供 publicId -> 内部主键的映射能力。

```java
public interface PublicIdLookup {
    ResourceType type();  // 资源类型（STORE/PRODUCT/SKU）
    Optional<Object> findInternalId(long tenantId, String publicId);  // 单查
    Map<String, Object> findInternalIds(long tenantId, List<String> publicIds);  // 批量
}
```

#### 2.2.2 PublicIdGovernanceResolver

统一解析器，聚合所有 Lookup，提供格式校验 + 查询 + Scope Guard。

```java
public interface PublicIdGovernanceResolver {
    ResolvedPublicId resolve(long tenantId, ResourceType type, String publicId);
    Map<String, ResolvedPublicId> resolveBatch(long tenantId, ResourceType type, List<String> publicIds);
}
```

#### 2.2.3 ScopeGuard

权限校验器，防止越权访问。

```java
public interface ScopeGuard {
    void check(ResolvedPublicId resolved, ScopeGuardContext context);
}
```

---

## 3. 使用指南

### 3.1 Controller 层使用 @ResolvePublicId

#### 示例 1：注入 Long 主键（推荐）

```java
@GetMapping("/stores/{storeId}")
public ApiResponse<StoreView> detail(
    @PathVariable("storeId") @ResolvePublicId(type=STORE) Long storePk) {
    // storePk 已自动解析并通过 Scope Guard 校验
    Long tenantId = requireTenantId();
    StoreView view = storeFacade.getStoreBase(tenantId, storePk);
    return ApiResponse.success(view);
}
```

**执行流程：**

1. 提取 `storeId` 参数：`sto_01HN8X5K9G3QRST2VW4XYZ`
2. 校验格式：前缀 `sto_` + 26 位 ULID
3. 查询主键：`SELECT id FROM bc_store WHERE tenant_id=? AND public_id=?`
4. Scope Guard：校验 `tenantId` 和 `storePk`
5. 注入参数：`storePk = 12345`（Long）

#### 示例 2：注入完整解析结果

```java
@GetMapping("/products/{productId}")
public ApiResponse<ProductDetailResponse> detail(
    @PathVariable("productId") @ResolvePublicId(type=PRODUCT) ResolvedPublicId resolved) {
    Long productPk = resolved.asLong();
    String publicId = resolved.publicId();  // 用于日志/审计
    long tenantId = resolved.tenantId();
    
    // 业务逻辑...
    return ApiResponse.success(response);
}
```

#### 示例 3：可选参数

```java
@GetMapping("/stores")
public ApiResponse<List<StoreView>> list(
    @RequestParam(value="storeId", required=false)
    @ResolvePublicId(type=STORE, required=false) Long storePk) {
    // storePk 可能为 null
    if (storePk != null) {
        // 查询指定门店
    } else {
        // 查询所有门店
    }
}
```

#### 示例 4：嵌套路径（多个 publicId）

```java
@GetMapping("/products/{productId}/skus/{skuId}")
public ApiResponse<SkuDetailResponse> skuDetail(
    @PathVariable("productId") @ResolvePublicId(type=PRODUCT) Long productPk,
    @PathVariable("skuId") @ResolvePublicId(type=SKU) Long skuPk) {
    // 两者都会自动解析并校验
    // 可在业务层校验 SKU 是否归属于 Product
}
```

### 3.2 响应 DTO 规范

**❌ 错误示例：暴露 Long 主键**

```java
public class StoreView {
    private Long id;  // ❌ 禁止
    private String name;
}
```

**✅ 正确示例：仅暴露 publicId**

```java
public class StoreView {
    private String storePublicId;  // ✅ 推荐
    private String name;
}
```

### 3.3 批量查询（避免 N+1）

```java
@PostMapping("/products/batch")
public ApiResponse<List<ProductSummary>> batchQuery(@RequestBody BatchQueryRequest request) {
    Long tenantId = requireTenantId();
    List<String> productIds = request.productIds();
    
    // 批量解析（单次 SQL 查询）
    Map<String, ResolvedPublicId> resolvedMap = 
        governanceResolver.resolveBatch(tenantId, PRODUCT, productIds);
    
    // 提取主键列表
    List<Long> productPks = resolvedMap.values().stream()
        .map(ResolvedPublicId::asLong)
        .toList();
    
    // 批量查询商品
    List<ProductSummary> products = productService.batchQuery(productPks);
    return ApiResponse.success(products);
}
```

---

## 4. Scope Guard 策略

### 4.1 默认策略

| 资源类型 | 租户校验 | 门店校验 | 说明 |
|---------|---------|---------|------|
| STORE   | ✅ 必须 | ✅ 必须（如上下文有 storeId） | 防止跨门店访问 |
| PRODUCT | ✅ 必须 | ❌ 暂无 | 商品归属租户（后续可扩展门店级） |
| SKU     | ✅ 必须 | ❌ 暂无 | SKU 归属租户 |
| ORDER   | ✅ 必须 | ❌ 暂无 | 订单归属租户 |

### 4.2 ApiSide 策略

| ApiSide  | Scope Guard 行为 | 说明 |
|----------|-----------------|------|
| USER     | 全量校验 | 用户侧接口，强制校验租户+门店 |
| MERCHANT | 全量校验 | 商户侧接口，强制校验租户+门店 |
| PLATFORM | 跳过校验 | 平台侧接口，允许跨租户访问 |

### 4.3 禁用 Scope Guard（谨慎使用）

```java
// 仅限管理端接口，允许跨租户/门店访问
@GetMapping("/admin/stores/{storeId}")
public StoreView adminDetail(
    @PathVariable("storeId") @ResolvePublicId(type=STORE, scopeCheck=false) Long storePk) {
    // 跳过 Scope Guard 校验
    return storeService.getDetail(storePk);
}
```

---

## 5. 错误码与排查

### 5.1 错误码清单

| 错误码 | HTTP 状态码 | 说明 | 常见原因 |
|-------|------------|------|---------|
| PUBLIC_ID_INVALID | 400 | publicId 格式非法 | 前缀不匹配、ULID 长度错误 |
| PUBLIC_ID_NOT_FOUND | 404 | publicId 未找到 | 资源不存在或已删除 |
| PUBLIC_ID_FORBIDDEN | 403 | 权限不足 | 租户不匹配、门店不匹配 |
| PUBLIC_ID_LOOKUP_MISSING | 500 | 缺少 Lookup 实现 | 新资源类型未实现 Lookup |

### 5.2 排查方法

#### 5.2.1 PUBLIC_ID_INVALID

**错误示例：**

```
Public ID 格式非法，缺少分隔符 '_': invalid_format
```

**排查步骤：**

1. 检查 publicId 格式：`{prefix}_{ulid}`
2. 检查前缀是否匹配：`sto_` / `prd_` / `sku_`
3. 检查 ULID 长度：必须 26 位
4. 检查 ULID 字符：仅允许 Crockford Base32（0-9A-HJKMNP-TV-Z）

#### 5.2.2 PUBLIC_ID_NOT_FOUND

**错误示例：**

```
未找到资源：resourceType=STORE, publicId=sto_01HN8X5K9G3QRST2VW4XYZ
```

**排查步骤：**

1. 检查数据库：`SELECT * FROM bc_store WHERE tenant_id=? AND public_id=?`
2. 检查逻辑删除：`is_deleted=0`
3. 检查租户隔离：`tenant_id` 是否正确
4. 检查索引：确保 `(tenant_id, public_id)` 索引存在

#### 5.2.3 PUBLIC_ID_FORBIDDEN

**错误示例：**

```
无权访问该资源
```

**排查步骤：**

1. 检查租户 ID：`resolved.tenantId == context.tenantId`
2. 检查门店 ID：`resolved.storePk == context.storePk`（如 type=STORE）
3. 检查 ApiSide：是否为 PLATFORM 侧（可跳过校验）
4. 检查日志：查看 `ScopeGuard` 的 WARN 日志

#### 5.2.4 PUBLIC_ID_LOOKUP_MISSING

**错误示例：**

```
缺少资源类型 INVENTORY 的 PublicIdLookup 实现，请在对应模块实现该接口
```

**解决方法：**

1. 在对应模块实现 `PublicIdLookup` 接口
2. 标记为 `@Component` 注册到 Spring 容器
3. 确保 Spring 扫描路径包含该实现类

---

## 6. 新模块接入 Checklist

### 6.1 数据库准备

- [ ] **添加 public_id 字段**

```sql
ALTER TABLE bc_your_table
    ADD COLUMN internal_id BINARY(16) NULL COMMENT '内部主键 ULID128',
    ADD COLUMN public_id VARCHAR(40) NULL COMMENT '对外ID prefix_ulid';
```

- [ ] **创建唯一索引**

```sql
ALTER TABLE bc_your_table
    ADD UNIQUE KEY uk_your_table_internal_id (internal_id),
    ADD UNIQUE KEY uk_your_table_tenant_public_id (tenant_id, public_id);
```

- [ ] **填充已有数据的 public_id**

```java
// 使用 IdService 生成真实的 ULID 和 PublicId
Ulid128 internalId = idService.nextUlid();
String publicId = idService.nextPublicId(ResourceType.YOUR_TYPE);
```

### 6.2 代码实现

- [ ] **更新实体类**

```java
@Data
public class BcYourEntity {
    private Long id;  // 自增主键
    private Ulid128 internalId;  // 内部 ULID
    private String publicId;  // 对外 ID
    private Long tenantId;
    // ...
}
```

- [ ] **实现 PublicIdLookup**

```java
@Component
public class YourPublicIdLookup implements PublicIdLookup {
    @Override
    public ResourceType type() {
        return ResourceType.YOUR_TYPE;
    }
    
    @Override
    public Optional<Object> findInternalId(long tenantId, String publicId) {
        // SELECT id FROM bc_your_table WHERE tenant_id=? AND public_id=?
    }
    
    @Override
    public Map<String, Object> findInternalIds(long tenantId, List<String> publicIds) {
        // SELECT id, public_id FROM bc_your_table WHERE tenant_id=? AND public_id IN (...)
    }
}
```

- [ ] **改造 Controller**

```java
@GetMapping("/your-resources/{resourceId}")
public ApiResponse<YourView> detail(
    @PathVariable("resourceId") @ResolvePublicId(type=YOUR_TYPE) Long resourcePk) {
    // resourcePk 已自动解析并校验
}
```

- [ ] **更新 DTO/View**

```java
public class YourView {
    private String resourcePublicId;  // ✅ 使用 publicId
    // private Long id;  // ❌ 禁止暴露 Long 主键
}
```

### 6.3 测试验证

- [ ] **单元测试**

```java
@Test
void resolve_shouldReturnResolvedPublicId_whenValidPublicId() {
    // 测试 PublicIdLookup.findInternalId
}
```

- [ ] **集成测试**

```java
@Test
void detail_shouldReturn200_whenValidPublicId() {
    // 测试 Controller 接口
}
```

- [ ] **ArchUnit 测试**

```java
@Test
void shouldFollowPublicIdGovernanceRules() {
    PublicIdGovernanceRules.checkAll(classes);
}
```

---

## 7. 架构规则（ArchUnit）

### 7.1 规则清单

| 规则 | 说明 | 违规示例 |
|------|------|---------|
| CONTROLLER_PARAMS_NO_LONG_ID | Controller 参数禁止使用 Long 类型的 id | `@PathVariable Long id` |
| DTO_FIELDS_NO_LONG_ID | DTO 字段禁止暴露 Long 类型的 id | `private Long id;` |
| DTO_FIELDS_NO_LONG_RESOURCE_ID | DTO 字段禁止暴露 Long 类型的 storeId/productId | `private Long storeId;` |
| CONTROLLER_NO_DIRECT_MAPPER | Controller 禁止直接依赖 Mapper | `@Autowired XxxMapper` |

### 7.2 使用示例

```java
@Test
void shouldFollowPublicIdGovernanceRules() {
    JavaClasses classes = new ClassFileImporter()
        .importPackages("com.bluecone.app");
    
    PublicIdGovernanceRules.checkAll(classes);
}
```

### 7.3 白名单

以下包路径可豁免规则：

- `..admin..`：管理端接口
- `..platform..`：平台侧接口

---

## 8. 性能优化

### 8.1 索引优化

**必须创建的索引：**

```sql
-- 唯一索引：(tenant_id, public_id)
CREATE UNIQUE INDEX uk_bc_store_tenant_public_id ON bc_store(tenant_id, public_id);
```

**性能要求：**

- 单查响应时间：< 10ms
- 批量查询（100 条）：< 50ms

### 8.2 批量查询优化

**❌ 错误示例：N+1 查询**

```java
for (String publicId : publicIds) {
    ResolvedPublicId resolved = resolver.resolve(tenantId, PRODUCT, publicId);  // N 次查询
}
```

**✅ 正确示例：批量查询**

```java
Map<String, ResolvedPublicId> resolvedMap = 
    resolver.resolveBatch(tenantId, PRODUCT, publicIds);  // 1 次查询
```

### 8.3 监控埋点

**关键指标：**

- `public_id_resolve_hit`：解析成功次数
- `public_id_resolve_miss`：解析失败次数
- `public_id_resolve_duration`：解析耗时（按 resourceType 分组）

**告警阈值：**

- 解析耗时 > 100ms：WARN
- 解析失败率 > 1%：ERROR

---

## 9. FAQ

### 9.1 为什么不使用映射表？

**映射表方案：**

```sql
CREATE TABLE bc_public_id_map (
    tenant_id BIGINT,
    resource_type VARCHAR(32),
    public_id VARCHAR(64),
    internal_id BINARY(16),
    UNIQUE KEY (tenant_id, resource_type, public_id)
);
```

**缺点：**

1. 额外的表维护成本
2. 数据一致性问题（业务表 + 映射表双写）
3. 查询性能损耗（多一次 JOIN）

**直接查业务表方案（推荐）：**

- 索引：`(tenant_id, public_id)` 直接建在业务表
- 查询：`SELECT id FROM bc_store WHERE tenant_id=? AND public_id=?`
- 优势：无额外表、无一致性问题、性能更优

### 9.2 如何处理历史数据？

**场景：**已有 100 万条数据，`public_id` 字段为 NULL。

**方案 1：在线填充（推荐）**

```java
@Scheduled(fixedDelay = 60000)  // 每分钟执行一次
public void fillPublicId() {
    List<BcStore> stores = storeMapper.selectList(
        new LambdaQueryWrapper<BcStore>()
            .isNull(BcStore::getPublicId)
            .last("LIMIT 1000")  // 每次处理 1000 条
    );
    
    for (BcStore store : stores) {
        Ulid128 internalId = idService.nextUlid();
        String publicId = idService.nextPublicId(ResourceType.STORE);
        store.setInternalId(internalId);
        store.setPublicId(publicId);
        storeMapper.updateById(store);
    }
}
```

**方案 2：离线脚本**

```sql
-- 使用 UUID 临时填充（生产环境需使用真实 ULID）
UPDATE bc_store
SET 
    internal_id = UNHEX(REPLACE(UUID(), '-', '')),
    public_id = CONCAT('sto_', UPPER(SUBSTRING(REPLACE(UUID(), '-', ''), 1, 26)))
WHERE public_id IS NULL
LIMIT 10000;
```

### 9.3 如何支持多种 ID 类型？

**场景：**同时支持 `Long` 主键和 `Ulid128` 内部 ID。

**方案：**`PublicIdLookup.findInternalId` 返回 `Object`，由业务层决定类型。

```java
// 返回 Long 主键
@Override
public Optional<Object> findInternalId(long tenantId, String publicId) {
    Long storePk = storeMapper.findIdByPublicId(tenantId, publicId);
    return Optional.ofNullable(storePk);
}

// 返回 Ulid128
@Override
public Optional<Object> findInternalId(long tenantId, String publicId) {
    Ulid128 internalId = storeMapper.findInternalIdByPublicId(tenantId, publicId);
    return Optional.ofNullable(internalId);
}
```

### 9.4 如何处理 publicId 冲突？

**场景：**两个租户生成了相同的 publicId（理论上不可能，但需防御）。

**方案：**唯一索引 `(tenant_id, public_id)` 自动保证租户内唯一。

```sql
-- 插入冲突时抛异常
INSERT INTO bc_store (tenant_id, public_id, ...) VALUES (1001, 'sto_xxx', ...);
-- Duplicate entry '1001-sto_xxx' for key 'uk_bc_store_tenant_public_id'
```

**处理：**捕获异常，重新生成 publicId。

```java
try {
    storeMapper.insert(store);
} catch (DuplicateKeyException ex) {
    // 重新生成 publicId（极低概率）
    store.setPublicId(idService.nextPublicId(ResourceType.STORE));
    storeMapper.insert(store);
}
```

---

## 10. 参考资料

- [APP-ID-V2-IMPLEMENTATION-SUMMARY.md](./APP-ID-V2-IMPLEMENTATION-SUMMARY.md)：ID 体系设计
- [STORE-CONTEXT-MIDDLEWARE.md](./STORE-CONTEXT-MIDDLEWARE.md)：门店上下文中间件
- [PUBLIC-ID-RESOLVE-MIDDLEWARE.md](./PUBLIC-ID-RESOLVE-MIDDLEWARE.md)：Public ID 解析中间件

---

## 11. 变更记录

| 日期 | 版本 | 变更内容 | 作者 |
|------|------|---------|------|
| 2025-12-15 | v1.0 | 初始版本，完成核心功能 | AI Assistant |

---

**联系方式：**

如有问题或建议，请联系架构组或在 GitLab 提 Issue。

