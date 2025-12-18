# Epoch Keying 设计说明

## 1. 背景

在 ContextMiddlewareKit 中，原始缓存键为：

- namespace: 例如 `"store:snap"` / `"user:snap"`；
- key: `"tenantId:scopeId"`。

当单租户在短时间内触发大量配置变更（门店/商品/库存策略等）时，如果仍依赖逐 key 失效：

- Outbox / Redis Pub/Sub 会承受大量消息；
- DB 侧的版本轮询压力较大；
- 多实例之间的缓存收敛依赖足够多的事件成功投递。

因此引入 namespace 级别的 epoch 概念，通过“换版本号”替代“大规模 DEL”。

## 2. 键格式与约定

统一约定：

- namespace 使用 `CacheNamespaces` 常量定义：
  - `store:snap`
  - `product:snap`
  - `sku:snap`
  - `inventory:policy`
  - `user:snap`
- scopeId 片段：
  - 若为 ULID（`Ulid128`），一律使用 `toString()`（26 位 ULID）作为 scopeId 片段；
  - 对于复合 scope（如 `InventoryScope`），可在 `toString()` 中返回关键内部 ID 的 ULID 字符串。
- 最终缓存 key 形如：

```text
{tenantId}:{epoch}:{scopeId}
```

其中：

- `tenantId`：Long → `String.valueOf(tenantId)`；
- `epoch`：namespace 级版本戳，由 `CacheEpochProvider` 维护；
- `scopeId`：基于 ULID 或其他稳定标识。

旧格式 `"{tenantId}:{scopeId}"` 不再使用，升级后天然与旧缓存隔离。

## 3. CacheEpochProvider 行为

接口：`CacheEpochProvider`

```java
long currentEpoch(long tenantId, String namespace);
long bumpEpoch(long tenantId, String namespace);
void updateLocalEpoch(long tenantId, String namespace, long epoch);
```

默认实现：`DefaultCacheEpochProvider`

- L1：Caffeine
  - key: `"tenantId:namespace"` → 当前 epoch；
  - TTL 默认 3s（`epochL1Ttl` 可配置）。
- L2：可选 Redis
  - key: `"bc:epoch:{tenantId}:{namespace}"` → epoch（long）；
  - `currentEpoch`：
    - 先查 L1；
    - miss 则 `GET` Redis；
    - 若不存在，则 `SETNX 1`，初始化为 1；
    - L1 回填；
  - `bumpEpoch`：
    - 使用 `INCR`，保证多实例全局单调递增；
    - 结果写回 L1。
- 无 Redis 时：
  - 使用本地 `ConcurrentHashMap<String, AtomicLong>`；
  - 仅在单 JVM 内保证单调递增，多实例之间 epoch 不强一致（在文档中说明）。

`updateLocalEpoch`：

- 用于 Listener 收到来自其他实例的 `epochBump` 事件时同步本地 L1；
- 只修改当前实例的 L1，不改 Redis 中的全局值；
- 不允许把 epoch 回退（调用方应保证传入值是单调递增的）。

## 4. SnapshotProvider 与 epoch key

`SnapshotProvider.getOrLoad` 新增带 `CacheEpochProvider` 的重载：

```java
public T getOrLoad(SnapshotLoadKey loadKey,
                   SnapshotRepository<T> repository,
                   ContextCache cache,
                   VersionChecker versionChecker,
                   SnapshotSerde<T> serde,
                   ContextKitProperties props,
                   CacheEpochProvider epochProvider)
```

键生成逻辑：

- namespace = `loadKey.scopeType()`（建议使用 `CacheNamespaces` 常量）；
- 当 `epochProvider != null` 时：

```java
long epoch = epochProvider.currentEpoch(loadKey.tenantId(), namespace);
String keySuffix = loadKey.tenantId() + ":" + epoch + ":" + loadKey.scopeId();
CacheKey cacheKey = new CacheKey(namespace, keySuffix);
```

- 否则退回到旧逻辑：`keySuffix = tenantId + ":" + scopeId`。

这样做的效果：

- 每次 epoch bump 后，新请求自动命中新的 `{epoch}` 分区；
- 旧分区的 key 将自然过期（由 TTL 控制），无需集中删除；
- L2 Redis 键包含 epoch，跨实例不会读到旧版本缓存。

## 5. 失效与 epoch 的关系

### 5.1 DIRECT_KEYS 模式

写路径发布的 `CacheInvalidationEvent.keys` 一律使用“语义 key”：

```text
{tenantId}:{scopeId}
```

`DefaultCacheInvalidationExecutor` 在执行时：

- 如果注入了 `CacheEpochProvider` 且事件不是 `epochBump`：
  - 先通过 `currentEpoch(tenantId, namespace)` 获取当前 epoch；
  - 从语义 key 中提取 scopeId 片段；
  - 拼接成 `{tenantId}:{epoch}:{scopeId}` 后再调用 `ContextCache.invalidate(CacheKey)`。
- 如果没有 `CacheEpochProvider`，则退回到直接使用 `keys` 原始值。

这样，无论发布方是否感知 epoch，DIRECT_KEYS 模式都能在当前 epoch 下精确失效。

### 5.2 EPOCH_BUMP 模式

在风暴保护触发时（StormGuard 判定为 `EPOCH_BUMP`）：

- Publisher 调用 `CacheEpochProvider.bumpEpoch(tenantId, namespace)`；
- 构造一个 `epochBump=true` 的事件：
  - `keys` 可为空；
  - `newEpoch` 填入 bump 后的 epoch；
- 广播到所有实例；
- Listener 收到事件时：
  - 通过 `updateLocalEpoch` 或 `bumpEpoch` 更新本地 epoch；
  - 不再逐 key 调 `executor.execute`。

由于 SnapshotProvider 始终使用 `currentEpoch` 构造 key，新请求会直接落在新的 `{epoch}` 分区，旧 cache 将逐渐自然淘汰。  
相比大规模 DEL，这种方式：

- 单次操作复杂度为 O(1)；
- 对 Redis/Outbox 仅增加一小条事件；
- 减少对 L2/DB 的瞬时压力。

## 6. 无 Redis 场景的限制

当 Redis 不可用或关闭 `redisEpochEnabled` 时：

- epoch 完全由本地 `AtomicLong` 维护；
- 每个实例的 epoch 在初始时都为 1，并根据本实例的 EPOCH_BUMP 事件自增；
- 多实例之间不保证 epoch 一致：
  - 某个实例可能仍使用旧 epoch；
  - 该实例的缓存通过版本轮询与 DIRECT_KEYS 失效逐步收敛。

在这种场景下，建议：

- 降低 `versionCheckSampleRate`（例如 0.05），让版本轮询承担更多兜底责任；
- 适当提高 `stormThresholdPerMinute`，避免频繁的本地 epoch bump。

## 7. 与版本轮询的关系

epoch keying 与版本轮询是互补关系：

- epoch keying：
  - 用于快速收敛缓存；
  - 在事件风暴时通过 EPOCH_BUMP 换版本避免 DEL 风暴；
  - 依赖写路径发布事件和 StormGuard 的决策。
- 版本轮询（`VersionChecker`）：
  - 用于兜底，发现遗漏事件/短暂故障；
  - 在 cache 命中时按一定窗口和采样概率检查 DB 中 version 是否变化。

在开启 invalidation（事件驱动失效）后，建议：

- 将 `bluecone.contextkit.versionCheckSampleRate` 从 0.1 降至 0.02~0.05；
- 保持 `versionCheckWindow` 在 2~5 秒范围；
- 通过监控 DB QPS 与 Cache hit ratio 来进一步调优。

