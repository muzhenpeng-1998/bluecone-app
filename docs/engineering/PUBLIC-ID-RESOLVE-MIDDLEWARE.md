# PublicId 解析中间件（bc_public_id_map + 多级缓存 + Controller 自动注入）

本中间件在 BlueCone 平台内提供统一的 **PublicId → InternalId(Ulid128)** 解析能力，覆盖 tenant/store/order/product 等所有资源类型。

目标：

- 统一：所有模块通过同一映射表 `bc_public_id_map` 解析 publicId，避免各自“手撸解析”导致的不一致。
- 高隔离：解析键强约束 `tenantId + resourceType + publicId`，防止跨租户串表。
- 高并发：L1 Caffeine + 可选 L2 Redis 多级缓存，避免集中打 DB。
- 高稳定：严格格式校验 + 负缓存 + 可控回退策略，防止恶意扫描与雪崩。
- 渐进迁移：迁移期可开启 fallback，允许从业务主表查回 internalId，平滑接入。

---

## 1. 为什么必须统一映射表？

### 1.1 避免重复实现与不一致

历史上各模块常见模式：

- 在订单/门店/租户等模块内自行维护 `public_id` 字段；
- Controller 中手动调用 `PublicIdCodec.decode` 将 publicId 解析为 `Ulid128`；
- 各模块对前缀校验、异常处理、幂等规则不一致。

问题：

- 重复代码：每个模块都要维护一套解析逻辑和异常分支；
- 不一致：有的模块严格校验前缀，有的直接 decode；有的返回 400，有的返回 404 或 200+错误码；
- 治理困难：跨模块排查“某个 publicId 在哪张表”时，缺少统一入口。

### 1.2 `bc_public_id_map` 带来的好处

统一映射表定义见 `docs/sql/bc_public_id_map.sql`：

```sql
CREATE TABLE IF NOT EXISTS bc_public_id_map (
  id BIGINT NOT NULL AUTO_INCREMENT COMMENT '自增主键',
  tenant_id BIGINT NOT NULL COMMENT '租户ID',
  resource_type VARCHAR(32) NOT NULL COMMENT '资源类型：TENANT/STORE/ORDER/USER/PRODUCT等',
  public_id VARCHAR(64) NOT NULL COMMENT '对外ID，prefix_ulid',
  internal_id BINARY(16) NOT NULL COMMENT '内部ID，ULID128',
  status TINYINT NOT NULL DEFAULT 1 COMMENT '1有效 0无效（预留）',
  created_at DATETIME NOT NULL,
  updated_at DATETIME NOT NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uk_tenant_type_public (tenant_id, resource_type, public_id),
  UNIQUE KEY uk_tenant_type_internal (tenant_id, resource_type, internal_id),
  KEY idx_tenant_type (tenant_id, resource_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='公共ID映射表（public_id->internal_id）';
```

约束与收益：

- **唯一性**：`uk_tenant_type_public` 确保同租户+类型下 publicId 唯一；
- **反向保护**：`uk_tenant_type_internal` 避免一个内部 ULID 被多次映射，防止污染；
- **强隔离**：所有解析都带 `tenant_id`，避免跨租户访问；
- **统一查询入口**：后续要根据 publicId 排查数据，只需查一张表即可。

---

## 2. 创建时必须写入映射（强一致）

创建资源时，必须在 **同一事务** 内写入：

1. 业务主表（如 `bc_store`）记录 `internal_id`/`public_id`；
2. 映射表 `bc_public_id_map`，写入 `(tenant_id, resource_type, public_id, internal_id)`。

### 2.1 PublicIdRegistrar：降低业务侵入

在 app-core 中提供了统一注册器：

```java
public interface PublicIdRegistrar {
    void register(long tenantId, ResourceType type, String publicId, Ulid128 internalId);
}
```

默认实现 `DefaultPublicIdRegistrar` 内部调用 `PublicIdMapRepository.insertMapping(...)`，要求由调用方保证处于正确事务中。

### 2.2 门店创建链路示例（StoreCommandService）

门店写侧应用服务 `StoreCommandService` 在 `createStoreWithPreallocatedIds(...)` 中：

1. 由 Id 模块生成 `internalId` / `publicId`；
2. 写入门店主表 `bc_store`（`internal_id`/`public_id` 字段）；
3. 在同一事务内调用：

```java
publicIdRegistrar.register(
        tenantId,
        com.bluecone.app.id.api.ResourceType.STORE,
        publicId,
        internalId
);
```

因此无论是直接调用 `createStore(...)` 还是通过 `IdempotentCreateTemplate` 幂等创建，映射都会与业务写入保持强一致。

> 其他模块（order/product/tenant/user）接入时，只需在创建应用服务中同样调用 `PublicIdRegistrar.register(...)` 即可。

---

## 3. 解析流程与多级缓存策略

解析入口接口（app-core）：

```java
public interface PublicIdResolver {
    ResolveResult resolve(ResolveKey key);
    Map<String, ResolveResult> resolveBatch(long tenantId, ResourceType type, List<String> publicIds);
}
```

对应实现类：`com.bluecone.app.core.idresolve.application.CachedPublicIdResolver`

### 3.1 校验规则（必须）

在访问缓存/DB 之前，先做 publicId 校验：

- 若 publicId 为空或全空白：判定为 `INVALID_FORMAT`；
- 使用 `PublicIdCodec.decode(publicId)` 做格式与校验和校验；
- `DecodedPublicId.type()` 必须与 `ResourceType.prefix()` 匹配，否则 `PREFIX_MISMATCH`；
- 校验失败直接返回 `ResolveResult(hit=false, exists=false, reason=INVALID_FORMAT/PREFIX_MISMATCH)`，
  **不会访问缓存或 DB**。

对应单测：`PublicIdValidationTest` 验证非法格式/前缀不匹配时仓储从未被调用。

### 3.2 L1 Caffeine 缓存（每实例）

- Key：`{tenantId}:{resourceType}:{publicId}`；
- Value：内部结构 `CacheEntry`，包含：
  - `internalId`：命中时为 ULID128；
  - `negative`：是否负缓存；
  - `expireAt`：过期时间，用于精细控制 TTL；
- TTL：
  - 正缓存：`bluecone.idresolve.cache.l1Ttl`，默认 `PT10M`；
  - 负缓存：`bluecone.idresolve.cache.negativeTtl`，默认 `PT30S`；
- 容量：`bluecone.idresolve.cache.l1MaxSize`，默认 `100000`。

查找流程中优先访问 L1：

- 若命中正缓存：直接返回 `HIT_L1`；
- 若命中负缓存：直接返回 `NOT_FOUND`，不再访问 L2/DB。

### 3.3 L2 Redis 缓存（可选）

实现类：`com.bluecone.app.infra.idresolve.RedisPublicIdL2Cache`

- 开关：`bluecone.idresolve.cache.l2Enabled`（默认 true）；
- Key：`"bc:pid:{tenantId}:{resourceType}:{publicId}"`；
- Value：
  - 正向：内部 ULID128 的 16 字节 Base64；
  - 负缓存：固定字符串 `"NULL"`；
- TTL：
  - 正缓存：`bluecone.idresolve.cache.l2Ttl`，默认 `PT30M`；
  - 负缓存：`bluecone.idresolve.cache.negativeTtl`，默认 `PT30S`。

在 L1 未命中时：

1. 调用 `PublicIdL2Cache.get(...)`：
   - 正向命中：回填 L1 并返回 `HIT_L2`；
   - 负缓存命中：写入 L1 负缓存并返回 `NOT_FOUND`；
   - 未命中：继续查 DB。

### 3.4 DB 访问与回退策略

底层仓储接口：`PublicIdMapRepository`（app-core SPI），MySQL 实现：

- DO：`com.bluecone.app.infra.idresolve.PublicIdMapDO`；
- Mapper：`com.bluecone.app.infra.idresolve.PublicIdMapMapper`；
- 实现：`com.bluecone.app.infra.idresolve.MysqlPublicIdMapRepository`。

查询逻辑：

- 单个：
  - `findInternalId(tenantId, resourceType, publicId)` 只 `select internal_id`；
  - 强制 `status = 1`。
- 批量：
  - `findInternalIds(tenantId, resourceType, publicIds)` 使用 `IN (...)` + `select public_id, internal_id`。

缓存策略：

- DB 命中：
  - 写入 L1 正缓存；
  - 若 L2 启用，写入 L2 正缓存；
  - 返回 `HIT_DB`。
- DB 未命中：
  - 若 `bluecone.idresolve.fallbackToBizTable = true`，则尝试回退查询；
  - 否则直接写入负缓存（L1 + L2）并返回 `NOT_FOUND`。

### 3.5 回退到业务主表（可选，迁移期使用）

回退 SPI：

```java
public interface PublicIdFallbackLookup {
    boolean supports(ResourceType type);
    Optional<Ulid128> findInternalId(long tenantId, ResourceType type, String publicId);
    Map<String, Ulid128> findInternalIds(long tenantId, ResourceType type, List<String> publicIds);
}
```

`CachedPublicIdResolver` 会在映射表 miss 且 `fallbackToBizTable=true` 时，
遍历所有实现该 SPI 的 Bean，选择 `supports(type)` 为 true 的实现做回退查询。

> 迁移期各业务模块（如 store/order 等）可按需提供自己的 `PublicIdFallbackLookup` 实现，
> 例如从 `bc_store` 的 `public_id` 字段推导 internalId。生产期建议关闭 fallback，避免数据分叉。

### 3.6 批量解析（resolveBatch）

批量解析流程：

1. 对每个 publicId 做校验（格式 + 前缀），非法的直接返回 `INVALID_FORMAT/PREFIX_MISMATCH`；
2. L1 批量命中：优先从 Caffeine 获取，命中的直接返回；
3. L2 批量查询：对剩余 publicId 调用 `PublicIdL2Cache.getBatch(...)`，命中回填 L1；
4. DB 分批 IN 查询：
   - 按 `bluecone.idresolve.batchMaxIn`（默认 200）拆分 `IN` 查询；
   - 命中写入 L1 + L2；
5. 回退查询（可选）：对于仍未命中的 publicId，再调用 `PublicIdFallbackLookup` 做批量查找；
6. 对最终未找到的 publicId 写入负缓存，并返回 `NOT_FOUND`。

### 3.7 指标（可选）

若注入了 `MeterRegistry`，`CachedPublicIdResolver` 会上报：

- `bluecone.idresolve.total{result=hit_l1|hit_l2|hit_db|miss|invalid}`

便于观测缓存命中率与公网上的“垃圾 publicId” 请求规模。

---

## 4. Controller 使用方式：@ResolvePublicId

### 4.1 注解与参数类型

在 app-core 中提供 Spring MVC 注解：

```java
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
public @interface ResolvePublicId {
    ResourceType type();
}
```

支持参数类型：

- `Ulid128`：直接注入内部 ULID；
- `ResolvedId`：同时注入 `internalId` 与 `publicId`。

### 4.2 使用示例（按门店 publicId 查询）

```java
@GetMapping("/store/detail")
public ApiResponse<StoreDetailView> getStore(
        @RequestParam("storeId")
        @ResolvePublicId(type = ResourceType.STORE) Ulid128 storeInternalId) {
    Long tenantId = ...; // 通常从 TenantContext 或登录态获取
    // 使用 internalId 访问仓储/领域服务
}
```

原始 publicId 从：

- `@RequestParam` 对应的 query param；
- 或 `@PathVariable` 对应的路径变量；

中获取，`PublicIdArgumentResolver` 会自动：

1. 从 `TenantContext` 读取当前租户 ID（由 `TenantWebInterceptor` 从 `X-Tenant-Id` 头中注入）；
2. 构造 `ResolveKey(tenantId, type, publicId)` 调用 `PublicIdResolver.resolve(...)`；
3. 将解析结果注入到 `Ulid128` / `ResolvedId` 参数中。

### 4.3 异常映射规范

解析过程中，如发生错误：

- publicId 格式非法 / 前缀不匹配：
  - 抛出 `PublicIdInvalidException`；
  - 由 `GlobalExceptionHandler` 映射为：
    - HTTP 400；
    - body：`ErrorCode.INVALID_PARAM` 对应的错误结构；
- publicId 未找到：
  - 抛出 `PublicIdNotFoundException`；
  - 由 `GlobalExceptionHandler` 映射为：
    - HTTP 404；
    - body：`ErrorCode.NOT_FOUND` 对应的错误结构。

对应单测：`ArgumentResolverWebMvcTest` 验证：

- 请求携带 `storeId=sto_xxx` 时能成功注入 `Ulid128`；
- Resolver 返回 `NOT_FOUND` 时，HTTP 响应为 404。

---

## 5. 配置与开关

配置前缀：`bluecone.idresolve`

```yaml
bluecone:
  idresolve:
    enabled: true                 # 总开关
    cache:
      l1Ttl: PT10M                # L1 正缓存 TTL
      negativeTtl: PT30S          # L1/L2 负缓存 TTL
      l2Enabled: true             # 是否启用 L2 Redis
      l2Ttl: PT30M                # L2 正缓存 TTL
    fallbackToBizTable: false     # 是否回退到业务主表查 internalId
    batchMaxIn: 200               # 批量解析时单次 IN 的最大条目数
```

建议：

- 本地开发 / 迁移期：
  - 可开启 `fallbackToBizTable=true`，保证老数据依然可解析；
  - 并逐步补全各模块的 `PublicIdFallbackLookup` 实现。
- 生产稳定期：
  - 建议关闭 `fallbackToBizTable`，确保所有解析都以 `bc_public_id_map` 为准；
  - 避免“业务表先写、映射表漏写”导致的歧义。

---

## 6. 渐进迁移策略与风险

### 6.1 渐进迁移步骤

1. **落地映射表**：在数据库中执行 `docs/sql/bc_public_id_map.sql`；
2. **接入创建链路**：
   - 每个模块的创建服务中调用 `PublicIdRegistrar.register(...)`；
   - 确保与业务主表写入处于同一事务；
3. **接入解析链路**：
   - 将原有 `PublicIdCodec.decode(...)` + Repository 查询替换为 `PublicIdResolver`；
   - Controller 层改用 `@ResolvePublicId` 注解，消灭样板解析代码；
4. **迁移旧数据**：
   - 按模块将存量业务表中的 `(tenant_id, public_id, internal_id)` 回填到 `bc_public_id_map`；
   - 可通过批量任务或离线脚本完成，期间可开启 `fallbackToBizTable` 兜底；
5. **关闭 fallback**：
   - 当确认映射表数据完整后，将 `fallbackToBizTable` 置为 `false`；
   - 所有解析以映射表为唯一数据源。

### 6.2 开启 fallback 的风险

当 `fallbackToBizTable=true` 时：

- 映射表与业务主表都可能成为“事实来源”，若出现写入不一致：
  - 映射表缺失，但业务表存在：解析依赖 fallback；
  - 映射表错误，但业务表正确：解析结果依赖偶然的命 中顺序；
- 迁移期间务必有监控和定期校验脚本，对比：
  - `bc_public_id_map` 与业务表的 `(tenant_id, resource_type, public_id, internal_id)`；
  - 发现不一致时，以业务表为准修正映射表。

因此建议：

- 迁移期短时间开启 fallback，用于“补洞期”；
- 长期运行时，应关闭 fallback，以 `bc_public_id_map` 为唯一可信来源。

---

## 7. 实现概要索引

- DDL：
  - `docs/sql/bc_public_id_map.sql`
- Resolver 关键代码：
  - `app-core/src/main/java/com/bluecone/app/core/idresolve/application/CachedPublicIdResolver.java`
- L2 缓存实现：
  - `app-infra/src/main/java/com/bluecone/app/infra/idresolve/RedisPublicIdL2Cache.java`
- Repository 实现：
  - `app-infra/src/main/java/com/bluecone/app/infra/idresolve/MysqlPublicIdMapRepository.java`
- Registrar 实现：
  - `app-core/src/main/java/com/bluecone/app/core/idresolve/application/DefaultPublicIdRegistrar.java`
- MVC 参数解析：
  - 注解：`app-core/src/main/java/com/bluecone/app/core/idresolve/api/ResolvePublicId.java`
  - Resolver：`app-application/src/main/java/com/bluecone/app/web/idresolve/PublicIdArgumentResolver.java`
  - WebMvc 配置：`app-application/src/main/java/com/bluecone/app/config/WebMvcConfig.java`
- 配置类：
  - `app-core/src/main/java/com/bluecone/app/core/idresolve/config/IdResolveProperties.java`
  - `app-application/src/main/java/com/bluecone/app/config/IdResolveConfiguration.java`
- 测试：
  - 校验：`app-core/src/test/java/com/bluecone/app/core/idresolve/PublicIdValidationTest.java`
  - 缓存：`app-core/src/test/java/com/bluecone/app/core/idresolve/CachedPublicIdResolverTest.java`
  - MVC：`app-application/src/test/java/com/bluecone/app/web/idresolve/ArgumentResolverWebMvcTest.java`

