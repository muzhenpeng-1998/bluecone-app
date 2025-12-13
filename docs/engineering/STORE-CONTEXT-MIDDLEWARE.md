# StoreContextMiddleware（门店上下文中间件）

门店上下文中间件在用户侧接口链路中统一完成：

- `storePublicId` → `storeInternalId(Ulid128)` 解析；
- 门店运行态快照（`StoreSnapshot`）加载与缓存；
- 状态校验（启用/停用、是否可接单）；
- 将门店上下文注入 `ApiContext` 与 MDC。

适用模块：

- 主要面向用户侧 API（如小程序、前台 H5），默认路径 `/api/mini/**`；
- 管理端（/api/admin/**）、运维接口（/ops/**）默认跳过。

---

## 1. storeId 传递规范

门店标识统一使用 **publicId**，并通过以下方式传入：

- Header：`X-Store-Id`（推荐）；
- Query 参数：`storeId`。

注意：

- 目前不解析 path variable 中的 storeId（如 `/stores/{storeId}`），如需支持，可在路由层补充；
- 迁移期若存在旧版以 Long storeId 传参的接口，建议逐步改为 publicId。

租户标识获取顺序：

1. `ApiContext.tenantId`（若前置 TenantMiddleware 已填充）；
2. `TenantContext`（app-infra）；
3. Header：`X-Tenant-Id`。

无法解析或非法时，返回 401/400。

---

## 2. 解析流程概览

### 2.1 PublicId 解析

中间件内部使用 `PublicIdResolver`（参见 `PUBLIC-ID-RESOLVE-MIDDLEWARE.md`）完成：

1. 从请求中解析 tenantId 与 storePublicId；
2. 调用：

```java
ResolveResult r = publicIdResolver.resolve(
        new ResolveKey(tenantId, ResourceType.STORE, storePublicId));
```

3. 根据结果：
   - `reason=INVALID_FORMAT/PREFIX_MISMATCH` → `PublicIdInvalidException` → HTTP 400；
   - `reason=NOT_FOUND` → `PublicIdNotFoundException` → HTTP 404；
   - 命中时获得 `Ulid128 storeInternalId`。

### 2.2 StoreSnapshotProvider：快照加载与缓存

核心 Provider：`com.bluecone.app.store.runtime.application.StoreSnapshotProvider`

接口：

```java
public Optional<StoreSnapshot> getOrLoad(long tenantId,
                                         Ulid128 storeInternalId,
                                         String storePublicId)
```

快照契约（`app-store/src/main/java/com/bluecone/app/store/runtime/api/StoreSnapshot.java`）：

```java
public record StoreSnapshot(
    long tenantId,
    Ulid128 storeInternalId,
    String storePublicId,
    String storeName,
    int status,            // 1=enabled,0=disabled
    boolean openForOrders, // 配置维度可接单
    String timezone,
    long configVersion,
    Instant updatedAt,
    Map<String, Object> ext
) {}
```

门店上下文（`StoreContext`）：

```java
public record StoreContext(
    long tenantId,
    Ulid128 storeInternalId,
    String storePublicId,
    StoreSnapshot snapshot
) {}
```

### 2.3 仓储实现（StoreSnapshotRepository）

SPI：`app-store/src/main/java/com/bluecone/app/store/runtime/spi/StoreSnapshotRepository.java`

```java
Optional<StoreSnapshot> loadSnapshot(long tenantId, Ulid128 storeInternalId);
Optional<Long> loadConfigVersion(long tenantId, Ulid128 storeInternalId);
```

实现类：`StoreSnapshotRepositoryImpl`（`app-store/src/main/java/com/bluecone/app/store/runtime/infrastructure/StoreSnapshotRepositoryImpl.java`）

- 仅依赖 `bc_store`：
  - 通过 `(tenant_id, internal_id, is_deleted=false)` 定位记录；
  - 将 `status` 字段映射为 `status`（1=OPEN，0=其他）；
  - `open_for_orders` → `openForOrders`；
  - `config_version` → `configVersion`；
  - `updated_at` → `updatedAt`；
  - 额外字段（如 `cityCode`、`industryType` 等）放入 `ext` 中。

版本号：

- 直接复用 `bc_store.config_version`，由写侧应用服务在每次配置更新时递增；
- 满足“单调递增”的要求，可直接用于缓存版本校验。

---

## 3. 缓存与版本失效机制

### 3.1 L1 Caffeine（必选）

内部 key：

```text
store:snap:{tenantId}:{storeInternalId}
```

实现：

- `StoreSnapshotProvider` 内部使用 `Caffeine< String, L1Entry >`；
- `L1Entry` 包含：
  - `snapshot`：StoreSnapshot 或 null；
  - `negative`：是否负缓存；
  - `configVersion`：缓存快照的版本；
  - `expireAt`：缓存过期时间；
  - `nextVersionCheck`：下一次允许做版本校验的时间。

TTL：

- 正缓存：`bluecone.store.context.cache.l1Ttl`（默认 `PT5M`）；
- 负缓存：`bluecone.store.context.cache.negativeTtl`（默认 `PT30S`）。

### 3.2 L2 Redis（可选）

Key：

```text
store:snap:{tenantId}:{storeInternalId}
```

Value：

- 正向：`StoreSnapshot` 的 JSON 序列化（通过 `ObjectMapper`）；
- 负缓存：固定字符串 `"NULL"`。

TTL：

- 正缓存：`bluecone.store.context.cache.l2Ttl`（默认 `PT30M`）；
- 负缓存：与 L1 负缓存一致（默认 `PT30S`）。

### 3.3 负缓存（CacheNegative）

当 `StoreSnapshotRepository.loadSnapshot(...)` 返回 `Optional.empty()` 时：

- L1 写入负缓存（`negative=true`，TTL=negativeTtl）；
- 若启用 L2，则 Redis 写入 `"NULL"`；
- 后续在负缓存有效期内，相同 `(tenantId, storeInternalId)` 请求不再打 DB。

单测：`StoreSnapshotProviderNegativeCacheTest` 验证：

- 首次请求 miss 时访问仓储；
- 第二次请求命中负缓存，不再访问仓储。

### 3.4 版本校验（Version Check）

为保证配置变更后的强一致，`StoreSnapshotProvider` 在 L1/L2 命中后，按一定策略做“轻量版本校验”：

1. 在 L1 有效条目存在时，根据配置：
   - `versionCheckWindow`：两次版本校验的时间间隔；
   - `versionCheckSampleRate`：采样比例（0.1 表示 10% 请求做版本校验）。
2. 满足时间窗口且命中采样时：
   - 调用 `StoreSnapshotRepository.loadConfigVersion(...)`；
   - 若 `v_db != cached.configVersion`：
     - 重新执行 `loadSnapshot(...)`；
     - 回填 L1 & L2；
3. 对高 QPS 门店：
   - `versionCheckWindow` 控制频率；
   - `versionCheckSampleRate` 控制单次窗口内的 DB 压力。

---

## 4. StoreContextMiddleware 实现与注入

### 4.1 StoreContextResolver（核心解析器）

类：`app-application/src/main/java/com/bluecone/app/application/middleware/StoreContextResolver.java`

流程：

1. 路径匹配：
   - 若 `bluecone.store.context.enabled=false` 或 path 不匹配 includePaths / 命中 excludePaths，直接返回；
2. 解析 tenantId：
   - 优先从 `ApiContextHolder.get().getTenantId()`；
   - 其次从 `TenantContext.getTenantId()`；
   - 否则从 Header `X-Tenant-Id`；
   - 为空 → 401；非法 → 400。
3. 解析 storePublicId：
   - Header `X-Store-Id`；
   - Query `storeId`；
   - 若为空：
     - 若 `requireStoreId=true` 且 path 不在 `allowMissingStoreIdPaths` 中 → 400；
     - 否则返回 null（本次请求不绑定门店）。
4. PublicId 解析：

```java
ResolveResult r = publicIdResolver.resolve(
        new ResolveKey(tenantId, ResourceType.STORE, storePublicId));
```

   - `INVALID_FORMAT/PREFIX_MISMATCH` → `PublicIdInvalidException` → 400；
   - `NOT_FOUND` → `PublicIdNotFoundException` → 404；
5. 快照加载：

```java
Optional<StoreSnapshot> snapOpt =
    snapshotProvider.getOrLoad(tenantId, internalId, storePublicId);
```

   - 空 → `BizException(STORE_NOT_FOUND)`；
   - `snapshot.status==0` → `BizException(STORE_DISABLED)`（建议映射 410）；
   - `!snapshot.openForOrders` → `BizException(STORE_CLOSED_FOR_ORDERS)`（建议映射 409/423）。
6. 生成 `StoreContext` 并注入：
   - `ApiContextHolder.get().putAttribute("STORE_CONTEXT", storeContext)`；
   - MDC 写入：
     - `tenantId`；
     - `storePublicId`；
     - `storeInternalId`（截断为前 6 + 后 4）。

### 4.2 StoreMiddleware（接入 ApiGateway）

类：`app-application/src/main/java/com/bluecone/app/gateway/middleware/StoreMiddleware.java`

- 依赖：
  - `StoreContextResolver`；
  - `StoreContextProperties`；
- 行为：
  - 根据 `includePaths` / `excludePaths` 判定是否需要门店上下文；
  - 若需要，调用 `storeContextResolver.resolve(request)`；
  - 再继续链路调用 `chain.next(ctx)`。

默认适用路径：

```yaml
bluecone:
  store:
    context:
      includePaths:
        - "/api/mini/**"
      excludePaths:
        - "/ops/**"
        - "/actuator/**"
        - "/api/admin/**"
```

---

## 5. 配置项与 AutoConfiguration

配置类：`StoreContextProperties`（`app-application/src/main/java/com/bluecone/app/config/StoreContextProperties.java`）

```yaml
bluecone:
  store:
    context:
      enabled: true
      includePaths: ["/api/mini/**"]
      excludePaths: ["/ops/**","/actuator/**","/api/admin/**"]
      cache:
        l1Ttl: PT5M
        negativeTtl: PT30S
        l2Enabled: true
        l2Ttl: PT30M
      versionCheckWindow: PT2S
      versionCheckSampleRate: 0.1
      requireStoreId: true
      allowMissingStoreIdPaths:
        - "/api/mini/home/**"
```

AutoConfiguration：`StoreContextAutoConfiguration`（`app-application/src/main/java/com/bluecone/app/config/StoreContextAutoConfiguration.java`）

- 条件：
  - `bluecone.store.context.enabled=true`（默认开启）；
  - 存在 `StoreSnapshotRepository`、`StringRedisTemplate`、`ObjectMapper`；
  - 存在 `PublicIdResolver`。
- 注册：
  - `StoreSnapshotProvider`：注入仓储、Redis、ObjectMapper 与 TTL/采样配置；
  - `StoreContextResolver`：注入 `PublicIdResolver`、`StoreSnapshotProvider`、`StoreContextProperties`。

---

## 6. 错误码对照表

门店相关错误统一通过 `BizException` + 业务错误码返回，由全局异常处理器转换为标准响应。

- `storeId` 非法 / 前缀不匹配：
  - 异常：`PublicIdInvalidException`；
  - HTTP：400；
  - 建议错误码：`STORE_ID_INVALID`（可基于现有 `StoreErrorCode` 扩展）。
- `storeId` 未找到：
  - 异常：`PublicIdNotFoundException` 或 `BizException(STORE_NOT_FOUND)`；
  - HTTP：404。
- 门店被停用：
  - 异常：`BizException(StoreErrorCode.STORE_DISABLED)`；
  - HTTP：410。
- 门店关闭接单：
  - 异常：`BizException(StoreErrorCode.STORE_CLOSED_FOR_ORDERS)`；
  - HTTP：409/423（按现有规范）。
- 租户缺失或非法：
  - 异常：`BizException(CommonErrorCode.UNAUTHORIZED / BAD_REQUEST)`；
  - HTTP：401/400。

> 注意：中间件不会将栈信息直接返回给前端，日志中会保留 traceId 与关键上下文（tenantId、storePublicId、storeInternalId 摘要）。

---

## 7. 测试覆盖

- 校验测试：`StoreContextResolverValidationTest`（`app-application/src/test/java/com/bluecone/app/application/middleware/StoreContextResolverValidationTest.java`）
  - 场景：storeId prefix 不匹配；
  - 断言：抛出 `PublicIdInvalidException`，且 `StoreSnapshotProvider` 从未被调用。

- 中间件 Happy Path：`StoreContextMiddlewareHappyPathTest`（`app-application/src/test/java/com/bluecone/app/application/middleware/StoreContextMiddlewareHappyPathTest.java`）
  - mock `PublicIdResolver` 返回内部 ULID；
  - mock `StoreSnapshotProvider` 返回可用快照；
  - 发起 `/api/mini/test-store` 请求，携带 `X-Tenant-Id` 与 `X-Store-Id`；
  - 断言：HTTP 200 且在 `ApiContextHolder` 中能获取 `StoreContext`，并校验 `ResolveKey` 中的 tenantId/type/publicId。

- 负缓存测试：`StoreSnapshotProviderNegativeCacheTest`（`app-store/src/test/java/com/bluecone/app/store/runtime/StoreSnapshotProviderNegativeCacheTest.java`）
  - 场景：仓储两次调用均返回 empty；
  - 断言：第二次调用命中负缓存，仓储 `loadSnapshot` 仅被调用一次。

---

## 8. 生产建议

- 启用 Redis L2：
  - 在用户量较大的环境中建议保持 `bluecone.store.context.cache.l2Enabled=true`；
  - 配合合理的 `l2Ttl`（30 分钟左右即可）。
- 版本字段：
  - 强烈建议所有门店配置更新链路都通过 `StoreCommandService/StoreConfigChangeService`，保证 `bc_store.config_version` 单调递增；
  - 确保 `StoreConfigChangeService` 在变更后正确失效相关缓存。
- 采样率与窗口调优：
  - 高 QPS 场景建议 `versionCheckWindow=PT2S` ~ `PT5S`；
  - `versionCheckSampleRate` 可从 0.1 逐步升高，视 DB 压力调整；
  - 若对实时性要求极高，可设为 1.0（全量校验），同时关注版本查询的索引与响应时间。

