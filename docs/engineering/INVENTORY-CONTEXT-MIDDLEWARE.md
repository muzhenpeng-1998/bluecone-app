# InventoryContextMiddleware（库存上下文中间件）

库存上下文中间件在用户下单链路中统一完成：

- 复用 `ContextMiddlewareKit` 加载并缓存门店维度的库存策略快照；
- 依赖 `StoreContextMiddleware` 解析得到的 `StoreContext`（tenantId + storeInternalId + storePublicId）；
- 在进入订单/库存核心逻辑前，为链路提供只读的“库存策略快照”；
- 将快照注入 `ApiContext`，并在 MDC 中写入关键字段（扣减模式 / 策略版本）。

> 注意：这里承载的是“库存策略快照”，**不是实时库存数量**。实时库存查询与扣减仍由库存服务的高并发链路（锁库存 / 扣减流水 / 幂等）负责。

---

## 1. 快照契约：InventoryPolicySnapshot

模块：`app-inventory`

路径：`app-inventory/src/main/java/com/bluecone/app/inventory/runtime/api/InventoryPolicySnapshot.java`

```java
public record InventoryPolicySnapshot(
        long tenantId,
        Ulid128 storeId,
        String storePublicId,
        long configVersion,
        boolean enableInventory,
        String deductMode,
        int safetyStockMode,
        Instant updatedAt,
        Map<String, Object> ext
) {}
```

语义约定：

- `tenantId`：租户 ID；
- `storeId`：门店内部主键（Ulid128，对应 `bc_store.internal_id`，通过 `StoreContext` 传入）；
- `storePublicId`：门店对外 PublicId，用于日志与跨模块透传；
- `configVersion`：
  - 优先使用库存策略表中的版本号；
  - 当前实现中以 `updated_at` 的时间戳派生（毫秒级），满足单调递增即可；
- `enableInventory`：门店是否启用库存控制（简单从策略启用状态推导）；
- `deductMode`：扣减模式字符串：
  - 与领域内 `InventoryDeductMode` 约定保持一致，示例：`ON_ORDER` / `ON_PAID` / `ON_CONFIRM`；
- `safetyStockMode`：安全库存策略模式预留：
  - `0`：不启用；
  - `1`：按门店全局配置；
  - `2`：按 SKU 维度配置；
- `updatedAt`：策略更新时间（UTC Instant）；
- `ext`：扩展字段，当前放入：
  - `oversellAllowed`：是否允许超卖；
  - `oversellLimit`：超卖上限；
  - `maxDailySold`：当日最大可售量；
  - `storeNumericId`：门店数值型 ID（对应 `bc_store.id`）。

---

## 2. Repository SPI 与基础设施实现

### 2.1 SPI：InventoryPolicyRepository

路径：`app-inventory/src/main/java/com/bluecone/app/inventory/runtime/spi/InventoryPolicyRepository.java`

```java
public interface InventoryPolicyRepository extends SnapshotRepository<InventoryPolicySnapshot> {
}
```

继承 `ContextMiddlewareKit` 的通用 `SnapshotRepository<T>` 接口，约定：

- `loadFull(SnapshotLoadKey key)`：加载完整库存策略快照；
- `loadVersion(SnapshotLoadKey key)`：仅加载轻量版本号，用于缓存版本校验。

### 2.2 Scope 封装：InventoryScope

路径：`app-inventory/src/main/java/com/bluecone/app/inventory/runtime/api/InventoryScope.java`

```java
public record InventoryScope(
        Ulid128 storeInternalId,
        String storePublicId,
        Long storeNumericId
) {
    @Override
    public String toString() {
        if (storeInternalId != null) {
            return storeInternalId.toString();
        }
        if (storeNumericId != null) {
            return String.valueOf(storeNumericId);
        }
        return "unknown";
    }
}
```

- 作为 `SnapshotLoadKey.scopeId` 传入；
- `toString()` 只使用内部 ID / 数值型 ID，保证缓存 key 稳定；
- 仓储可以通过 `scope.storeNumericId()` 直接按 `tenantId + storeId` 命中索引。

### 2.3 仓储实现：InventoryPolicyRepositoryImpl

路径：`app-inventory/src/main/java/com/bluecone/app/inventory/runtime/infrastructure/InventoryPolicyRepositoryImpl.java`

- 依赖：`InvPolicyMapper` + `bc_inv_policy`；
- `loadFull`：
  - 从 `SnapshotLoadKey` 中解析 `InventoryScope`；
  - 按 `(tenant_id, store_id, status=1)` 查询一条代表性策略，使用索引 `idx_inv_policy_tenant_store`；
  - 将字段映射为 `InventoryPolicySnapshot`：
    - `enableInventory` ← `status == 1`；
    - `deductMode` ← `deduct_mode` 字段；
    - `configVersion` ← `updated_at` 的 epochMilli；
    - `updatedAt` ← `updated_at` 转换为 UTC Instant；
    - 其它字段放入 `ext`。
- `loadVersion`：
  - 仅 `select updated_at`，同样按 `(tenant_id, store_id, status=1)` 命中索引；
  - 将 `updated_at` 转换为 epochMilli 作为版本号；
  - 未查到或 `updated_at` 为空时返回 `Optional.empty()`。

> 当前实现将“门店库存策略快照”视为门店维度的代表性策略，后续如果策略结构拆分为门店级 + SKU 级，可在不影响调用方的前提下扩展 `InventoryPolicyRepositoryImpl` 内部逻辑。

---

## 3. SnapshotProvider：InventoryPolicySnapshotProvider

路径：`app-inventory/src/main/java/com/bluecone/app/inventory/runtime/application/InventoryPolicySnapshotProvider.java`

核心接口：

```java
public Optional<InventoryPolicySnapshot> getOrLoad(long tenantId,
                                                   Ulid128 storeInternalId,
                                                   String storePublicId,
                                                   Long storeNumericId)
```

内部使用 `ContextMiddlewareKit` 的通用 `SnapshotProvider<T>` 实现：

- 构造 `SnapshotLoadKey`：

```java
InventoryScope scope = new InventoryScope(storeInternalId, storePublicId, storeNumericId);
SnapshotLoadKey loadKey = new SnapshotLoadKey(tenantId, "inventory:policy", scope);
```

- 通过 `ContextCache` + `VersionChecker` + `ContextKitProperties` 实现：
  - L1 缓存（Caffeine）；
  - 可选 L2 缓存（如果全局配置启用 Redis）；
  - 版本校验窗口 + 采样（与门店快照逻辑一致）；
  - 负缓存：`loadFull` 返回 `empty` 时写入 `NegativeValue`。

序列化适配器 `InventoryPolicySnapshotSerde`：

- L1 直接缓存 `InventoryPolicySnapshot` 对象；
- L2（JSON）通过 `ObjectMapper` 反序列化回快照对象。

### 3.1 负缓存单测

路径：`app-inventory/src/test/java/com/bluecone/app/inventory/runtime/InventoryPolicySnapshotProviderNegativeCacheTest.java`

- 场景：仓储两次调用均返回 `Optional.empty()`；
- 断言：
  - 两次 `getOrLoad` 均返回 `Optional.empty()`；
  - `InventoryPolicyRepository.loadFull` 仅被调用一次（命中负缓存）。

### 3.2 版本不一致重载单测

路径：`app-inventory/src/test/java/com/bluecone/app/inventory/runtime/InventoryPolicySnapshotProviderVersionReloadTest.java`

- 自定义 `AlwaysCheckVersionChecker`，让每次命中缓存都触发版本校验；
- 模拟：
  - 第一次 `loadFull` 返回 `snapshotV1`，`loadVersion` 返回 `1L`；
  - 第二次命中缓存时，`loadVersion` 返回 `2L`，触发 `reloadAndFill`，再次加载 `snapshotV2`；
- 断言：
  - 第一次返回扣减模式为 `ON_ORDER`；
  - 第二次返回扣减模式为 `ON_PAID`；
  - `loadFull` 总共调用两次。

---

## 4. Resolver 与 Middleware

### 4.1 InventoryContextResolver

路径：`app-application/src/main/java/com/bluecone/app/application/middleware/InventoryContextResolver.java`

职责：

- 基于 `ApiContext` 与 `StoreContext` 解析库存策略快照；
- 当路径命中 `includePaths` 且不在 `excludePaths` 时：
  1. 从 `ApiContext.tenantId` 解析 `tenantId`：
     - 为空 → `BizException(CommonErrorCode.UNAUTHORIZED)`；
     - 非法数字 → `BizException(CommonErrorCode.BAD_REQUEST)`。
  2. 从 `ApiContextHolder.get().getAttribute("STORE_CONTEXT")` 获取 `StoreContext`：
     - 缺失或类型不匹配 → `BizException(CommonErrorCode.BAD_REQUEST, "门店上下文缺失...")`；
  3. 从 `StoreContext.snapshot().ext().get("storeId")` 解析数值型 `storeId`：
     - 缺失或类型不为 `Number` → `BizException(CommonErrorCode.BAD_REQUEST, "门店上下文不包含数值型门店ID...")`；
  4. 调用：

```java
Optional<InventoryPolicySnapshot> snapshotOpt = snapshotProvider.getOrLoad(
        tenantId,
        storeContext.storeInternalId(),
        storeContext.storePublicId(),
        storeNumericId
);
```

  5. 策略未找到：
     - 抛出 `BizException(InventoryErrorCode.INVENTORY_POLICY_NOT_FOUND)`；
     - 建议映射 HTTP 404。
  6. 策略命中：
     - 将快照注入 `ApiContext`：
       - `apiCtx.setInventoryPolicySnapshot(snapshot)`；
       - `apiCtx.putAttribute("INVENTORY_POLICY_SNAPSHOT", snapshot)`；
     - MDC 写入：
       - `inventoryDeductMode` ← `snapshot.deductMode()`；
       - `inventoryPolicyVersion` ← `snapshot.configVersion()`。

> `enableInventory=false` 时 Resolver 仍然注入快照，不主动阻断请求，由下游业务（如锁库存服务）根据该标志决定是否启用库存控制。

#### 4.1.1 StoreContext 依赖与 storeId 传递

为了解决 `Ulid128` 与数值型 `storeId` 的桥接，`StoreSnapshotRepositoryImpl` 在构造 `StoreSnapshot` 时，将 `bc_store.id` 放入 `ext`：

路径：`app-store/src/main/java/com/bluecone/app/store/runtime/infrastructure/StoreSnapshotRepositoryImpl.java`

```java
Map<String, Object> ext = new HashMap<>();
if (store.getCityCode() != null) {
    ext.put("cityCode", store.getCityCode());
}
if (store.getIndustryType() != null) {
    ext.put("industryType", store.getIndustryType().name());
}
if (store.getId() != null) {
    ext.put("storeId", store.getId());
}
```

Inventory 侧仅依赖 `StoreSnapshot.ext.storeId`，不直接依赖门店模块的 DAO/实体，避免跨模块强耦合。

### 4.2 InventoryMiddleware（接入 ApiGateway）

路径：`app-application/src/main/java/com/bluecone/app/gateway/middleware/InventoryMiddleware.java`

- 依赖：
  - `InventoryContextResolver`；
  - `InventoryContextProperties`；
- 行为：
  - 根据 `includePaths` / `excludePaths` 判断是否需要库存策略上下文；
  - 若需要，调用 `inventoryContextResolver.resolve(ctx)`；
  - 再继续链路调用 `chain.next(ctx)`。

路径配置类：`InventoryContextProperties`

```yaml
bluecone:
  inventory:
    context:
      enabled: false
      includePaths:
        - "/api/mini/**"
      excludePaths:
        - "/ops/**"
        - "/actuator/**"
        - "/api/admin/**"
```

默认关闭，需要在业务环境中显式开启。

### 4.3 AutoConfiguration 与 ApiGateway 接入

AutoConfiguration：`app-application/src/main/java/com/bluecone/app/config/InventoryContextAutoConfiguration.java`

- 条件：
  - `bluecone.inventory.context.enabled=true`；
  - 存在 `InventoryPolicyRepository`、`ContextCache`（`contextKitCache`）与 `ObjectMapper`、`VersionChecker`；
- 注册：
  - `InventoryPolicySnapshotProvider`；
  - `InventoryContextResolver`；
  - `InventoryMiddleware`。

在 `ApiGateway.buildChain` 中，当 Store 中间件之后、限流之前，按需插入库存中间件：

路径：`app-application/src/main/java/com/bluecone/app/gateway/ApiGateway.java`

```java
// After tenant binding, resolve store context if needed
chain.add(storeMiddleware);
InventoryMiddleware inventoryMiddleware = null;
try {
    inventoryMiddleware = applicationContext.getBean(InventoryMiddleware.class);
} catch (BeansException ignored) {
}
if (inventoryMiddleware != null) {
    chain.add(inventoryMiddleware);
}
```

若未开启 `bluecone.inventory.context.enabled` 或未注入 `InventoryMiddleware`，网关将自动跳过库存上下文中间件。

---

## 5. 测试覆盖

### 5.1 StoreContext 缺失时返回 400

路径：`app-application/src/test/java/com/bluecone/app/application/middleware/InventoryContextResolverStoreContextMissingTest.java`

- 场景 1：`STORE_CONTEXT` 缺失：
  - 构造命中 `includePaths` 的请求；
  - 不在 `ApiContext` 中放入 `StoreContext`；
  - 断言：`resolver.resolve(ctx)` 抛出 `BizException`，错误码为 `SYS-400-000`，文案包含“门店上下文缺失”；
  - 断言：`InventoryPolicySnapshotProvider` 未被调用。
- 场景 2：策略未找到：
  - 正常注入带 `storeId` 的 `StoreContext`；
  - `InventoryPolicySnapshotProvider.getOrLoad` 返回 `Optional.empty()`；
  - 断言：抛出 `BizException(InventoryErrorCode.INVENTORY_POLICY_NOT_FOUND)`。

### 5.2 InventoryPolicySnapshotProvider 行为

见上文 3.1、3.2 的两组单测：

- `InventoryPolicySnapshotProviderNegativeCacheTest`：
  - 验证 notfound 时写负缓存，第二次请求不打 DB。
- `InventoryPolicySnapshotProviderVersionReloadTest`：
  - 验证版本不一致时触发 reload，并更新缓存中的策略快照。

---

## 6. 使用说明与注意事项

- 该中间件只负责“库存策略快照”，不参与实时库存计算；
  - 下单 / 支付 / 接单等链路仍应调用库存领域服务完成锁定/扣减；
  - 快照中的字段（扣减模式 / 安全库存模式 / 超卖配置等）仅作为决策参数使用。
- 启用步骤：
  1. 确认门店上下文中间件（`StoreContextMiddleware`）已启用；
  2. 配置 `bluecone.inventory.context.enabled=true`，并按需调整 `includePaths` / `excludePaths`；
  3. 确保 `bc_inv_policy` 数据按门店维度配置完成；
  4. 可结合埋点和 MDC 字段 `inventoryDeductMode` / `inventoryPolicyVersion` 进行日志观测。
- 对于策略变更实时性要求较高的场景：
  - 可通过调优 `ContextKitProperties` 与 `VersionChecker` 的窗口与采样率，提高版本校验频率；
  - 或在策略变更链路中主动失效 `ContextMiddlewareKit` 的缓存键（`inventory:policy:{tenantId}:{storeInternalId}`）。

> 总结：InventoryContextMiddleware 让下单链路在进入高并发库存扣减前，能够通过一次轻量快照读取掌握“当前门店的库存策略配置”，减少跨模块多次查询，同时保持与 StoreContextMiddleware 相同的缓存与版本一致性语义。

