# 缓存失效自动保护（StormGuard + Epoch Bump）

本文档说明 bluecone-app 中缓存失效自动保护机制的设计与使用方式。

## 1. 目标与总览

- 在短时间内大量缓存失效事件（storm）出现时，自动降级为 namespace epoch bump 方式失效，避免：
  - Outbox 大量消息写入；
  - Redis Pub/Sub/Broker 拥塞；
  - DB 版本轮询压力过大。
- 在正常流量下，仍优先使用精确 key 失效，不改变业务读写语义。
- 默认关闭，通过 `bluecone.cache.invalidation.protection.enabled` 显式开启。
- 与已有的 `OUTBOX` / `REDIS_PUBSUB` / `INPROCESS` 传输完全兼容。

整体链路：

1. 写路径发布 `CacheInvalidationEvent`；
2. `ProtectedCacheInvalidationPublisher` 调用 `InvalidationStormGuard` 决策：
   - `DIRECT_KEYS`：按 key 直接执行 + 广播；
   - `COALESCE`：合并事件，减少 outbox/redis 消息；
   - `EPOCH_BUMP`：对 tenant+namespace 做一次 epoch bump；
3. Listener 收到事件后按 `epochBump` 字段执行本地更新。

## 2. Namespace Epoch 机制

### 2.1 CacheEpochProvider

接口：`com.bluecone.app.core.cacheepoch.api.CacheEpochProvider`

```java
long currentEpoch(long tenantId, String namespace);
long bumpEpoch(long tenantId, String namespace);
```

默认实现：`com.bluecone.app.core.cacheepoch.application.DefaultCacheEpochProvider`

- L1：Caffeine，key=`tenantId:namespace` → epoch，TTL 默认 3s（`epochL1Ttl` 可配置）；
- L2（可选）：Redis，key=`bc:epoch:{tenantId}:{namespace}` → long；
  - 使用 `GET` / `SETNX 1` 初始化；
  - bump 时使用 `INCR`，保证多实例全局单调递增；
  - TTL 可不设置或设置为较长时间（如 7d），当前实现默认不设置 TTL。
- 无 Redis 时：
  - 使用 `ConcurrentHashMap<String, AtomicLong>`；
  - 仅保证单 JVM 内单调递增，多实例间不强制对齐（需在文档中说明）。

### 2.2 缓存键格式

SnapshotProvider 中构建 `CacheKey` 的规则升级为带 epoch：

```java
String namespace = loadKey.scopeType(); // 例如 "store:snap"
long epoch = cacheEpochProvider.currentEpoch(loadKey.tenantId(), namespace);
String keySuffix = loadKey.tenantId() + ":" + epoch + ":" + loadKey.scopeId();
CacheKey cacheKey = new CacheKey(namespace, keySuffix);
```

升级后：

- 旧 key 形如 `store:snap:{tenantId}:{scopeId}`；
- 新 key 形如 `store:snap:{tenantId}:{epoch}:{scopeId}`；
- 新老 key 不再冲突，升级过程无需清理旧缓存，天然隔离。

L2 Redis（`RedisContextCache`）的 key 也使用 `namespace + ":" + keySuffix`，自动包含 epoch，避免跨实例读取到旧版本。

### 2.3 对 ContextMiddleware 的影响

以下组件已接入 epoch 键：

- `StoreSnapshotProvider`（`store:snap`）；
- `InventoryPolicySnapshotProvider`（`inventory:policy`）；
- `UserContextResolver` / `SnapshotProvider<UserSnapshot>`（`user:snap`）。

均通过共用的 `SnapshotProvider<T>` + `CacheEpochProvider` 完成。

## 3. StormGuard：失效风暴检测与决策

StormGuard 核心类型：

- `InvalidationDecision`：`DIRECT_KEYS` / `COALESCE` / `EPOCH_BUMP`
- `GuardDecision`：包含 decision、stormMode、reason、effectiveEpoch、keysCount；
- `InvalidationStormGuard`：`GuardDecision decide(CacheInvalidationEvent evt);`
- 默认实现：`DefaultInvalidationStormGuard`

### 3.1 统计维度与窗口

- 维度：`tenantId + namespace`（必要时可以扩展 scope）；
- 统计窗口：1 分钟滑动计数（实现为按 minute bucket 的计数）。

计数器：

- Redis 启用时：使用 `StringRedisTemplate`；
  - `cntKey = "bc:storm:cnt:{tenantId}:{namespace}:{minuteEpoch}"`；
  - `INCR cntKey` 后设置 `EXPIRE 120s`；
  - `modeKey = "bc:storm:mode:{tenantId}:{namespace}"` 标记 storm 模式，`EX=stormCooldown`；
- 无 Redis 时：使用本地 Caffeine 计数 `localCounters`，只保证单实例可见。

### 3.2 阈值与决策规则

配置项参考 `CacheInvalidationProtectionProperties`：

- `coalesceThresholdPerMinute`（默认 60）；
- `stormThresholdPerMinute`（默认 300）；
- `stormCooldown`（默认 `PT2M`）；
- `maxKeysPerEvent`（默认 50）。

决策流程：

1. 若 `keysCount > maxKeysPerEvent` → `EPOCH_BUMP`（防止单事件 payload 过大）；
2. 计算当前 `cnt`：
   - Redis 模式：`INCR cntKey`；
   - 本地模式：`localCounters` 自增；
3. 若 `stormMode=true`（Redis 或本地 storm 标记存在）→ `EPOCH_BUMP`；
4. 若 `cnt >= stormThresholdPerMinute`：
   - 设置 storm 模式（写 `modeKey` / 本地标记）；
   - 决策为 `EPOCH_BUMP`；
5. 若 `cnt >= coalesceThresholdPerMinute` → `COALESCE`；
6. 否则 → `DIRECT_KEYS`。

## 4. Coalescer：事件合并与节流

接口：`InvalidationCoalescer`

- 默认实现：`DefaultInvalidationCoalescer`；
- 维度：`tenantId + namespace + scope`；
- 关键参数：
  - `debounceWindow`（默认 500ms）；
  - `maxKeysPerBatch`（默认 200）；
  - `maxKeysPerEvent`（与基础 `CacheInvalidationProperties.maxKeysPerEvent` 一致）。

实现要点：

- 使用 `ConcurrentHashMap<GroupKey, PendingBatch>` 收集待合并事件；
- `PendingBatch`：
  - `LinkedHashSet<String> keys` 做去重；
  - 记录 `firstEventTime`；
- 定时任务（单线程 `ScheduledExecutorService`）：
  - 间隔约为 `debounceWindow / 2` 检查；
  - 若 batch 已超过 `debounceWindow` 或 `keys.size()` ≥ `maxKeysPerBatch`，触发 flush。

Flush 策略：

- 若 `keys.size() > maxKeysPerBatch`：
  - 直接生成一个 `epochBump=true` 的事件，交给 publisher；
- 否则：
  - 将 keys 做去重后按 `maxKeysPerEvent` 拆分多个事件；
  - 每个合并事件带有 `protectionHint=COALESCE`，并使用特殊 `eventId` 标记避免再次被 Coalescer 合并。

## 5. Publisher 接入保护逻辑

装饰器：`ProtectedCacheInvalidationPublisher`

- 包名：`com.bluecone.app.core.cacheinval.application`；
- 依赖：
  - `DefaultCacheInvalidationPublisher` delegate；
  - `InvalidationStormGuard`；
  - `InvalidationCoalescer`；
  - `CacheEpochProvider`。

流程：

1. 写路径调用 `publishAfterCommit(evt)`；
2. `stormGuard.decide(evt)` 得到 `GuardDecision`；
3. 根据 `decision`：
   - `DIRECT_KEYS`：
     - 把 `protectionHint=DIRECT_KEYS` 写入事件；
     - 直接调用底层 `DefaultCacheInvalidationPublisher`（本地执行 + 广播）；
   - `COALESCE`：
     - 把 `protectionHint=COALESCE` 写入事件；
     - 调用 `coalescer.submit(evt)` 合并；
   - `EPOCH_BUMP`：
     - 调用 `epochProvider.bumpEpoch(tenantId, namespace)`；
     - 构造 `epochBump=true` 的事件（`keys` 可为空，`newEpoch` 填入 bump 后的 epoch，`protectionHint=EPOCH_BUMP`）；
     - 交给底层 `DefaultCacheInvalidationPublisher` 做本地广播（不再逐 key 删除）。

EPOCH_BUMP 的优势：

- 每个 namespace+tenant 只需一次 `INCR` 操作即可全量失效；
- ContextMiddleware 读取时会拿到新的 epoch，自然绕过旧缓存，无需大规模 DEL 或 L2 清理；
- 对于 Outbox / Redis 来说，消息数从 O(N keys) 降到 O(1 event)。

## 6. Listener 行为与 epochBump 兼容

Listener：`com.bluecone.app.cacheinval.CacheInvalidationListener`

当前实现保持对旧事件兼容：

- 若 `epochBump=false`：
  - 调用 `CacheInvalidationExecutor.execute(event)`，逐 key 对当前 epoch 的缓存做失效；
- 若 `epochBump=true`：
  - `executor.execute(event)` 仍为 no-op（因为 keys 通常为空）；
  - 推进中的改造可进一步在 Listener 内注入 `CacheEpochProvider` 并：
    - 调用 `epochProvider.bumpEpoch` 或根据 `newEpoch` 实现 `setTo` 语义；
    - 一般无需逐 key invalidate。

去重机制：

- Listener 内部维护 `recentEvents`（Caffeine/ConcurrentHashMap）记录最近的 eventId；
- 即使 EPOCH_BUMP 事件重复，也只是多次 `bumpEpoch` 或重复设置 epoch，不影响正确性。

## 7. 观测与 bc_cache_invalidation_log 表

DDL 变更（见 `docs/sql/migration/V20251214__cache_inval_add_guard_fields.sql`）：

```sql
ALTER TABLE bc_cache_invalidation_log
    ADD COLUMN decision VARCHAR(16) NULL COMMENT 'DIRECT/COALESCE/EPOCH_BUMP',
    ADD COLUMN storm_mode TINYINT NOT NULL DEFAULT 0 COMMENT '1 storm',
    ADD COLUMN epoch BIGINT NULL COMMENT 'new/current epoch';
```

写入逻辑：

- 在 Listener 写 `CacheInvalidationLogEntry` 时新增字段：
  - `decision`：来自 `event.protectionHint()`；
  - `stormMode`：当前实现根据事件本身难以判定，先留出字段，后续可根据 GuardDecision 扩展；
  - `epoch`：`event.newEpoch()`。
- `CacheInvalidationLogWriterImpl` 会将这些字段透传到 `CacheInvalidationLogDO` 并写入表。

这样在 Ops 控制台可以：

- 查看最近一段时间 `DIRECT/COALESCE/EPOCH_BUMP` 的比例；
- 针对某个 tenant+namespace 分析是否频繁进入 storm 模式；
- 排查某次异常失效行为时，看到 newEpoch 值和触发原因。

## 8. 配置项与 AutoConfiguration

### 8.1 配置项

基础配置：`CacheInvalidationProperties`（已有）

```yaml
bluecone:
  cache:
    invalidation:
      enabled: false
      transport: OUTBOX        # OUTBOX / REDIS_PUBSUB / INPROCESS
      redisTopic: "bc:cache:inval"
      recentEventTtl: PT1M
      maxKeysPerEvent: 50
```

保护配置：`CacheInvalidationProtectionProperties`

```yaml
bluecone:
  cache:
    invalidation:
      protection:
        enabled: false
        coalesceThresholdPerMinute: 60
        stormThresholdPerMinute: 300
        stormCooldown: PT2M
        debounceWindow: PT0.5S
        maxKeysPerEvent: 50
        maxKeysPerBatch: 200
        epochEnabled: true
        epochL1Ttl: PT3S
        redisStormEnabled: true
        redisEpochEnabled: true
```

说明：

- `enabled=false`：默认关闭保护逻辑，只有手动开启后才生效；
- `coalesceThresholdPerMinute`：达到该阈值后开始合并事件，减少 outbox/redis 压力；
- `stormThresholdPerMinute`：达到该阈值后进入 storm 模式，一段时间内使用 `EPOCH_BUMP`；
- `stormCooldown`：storm 模式保持时间；
- `debounceWindow`：Coalescer 的合并窗口；
- `maxKeysPerEvent` / `maxKeysPerBatch`：单事件/单批的 key 数上限；
- `redisStormEnabled`：
  - 开启且存在 Redis 时，StormGuard 计数与模式标记放在 Redis 中，多实例共享；
  - 关闭时，即便有 Redis 也按本地模式运行；
- `redisEpochEnabled`：
  - 开启且存在 Redis 时，epoch 使用 Redis 作为全局来源；
  - 关闭时，只使用本地 AtomicLong。

### 8.2 AutoConfiguration

1. `CacheEpochAutoConfiguration`
   - 提供 `CacheEpochProvider` Bean：
     - 有 Redis 且 `redisEpochEnabled=true`：`DefaultCacheEpochProvider(l1Ttl, redisTemplate, true, null)`；
     - 否则：`DefaultCacheEpochProvider(l1Ttl)`（单实例）。

2. `CacheInvalidationAutoConfiguration`
   - 保持原有行为：按 transport 创建 Bus 与基础 `DefaultCacheInvalidationPublisher`；
   - Listener 不变。

3. `CacheInvalidationProtectionAutoConfiguration`
   - 条件：`bluecone.cache.invalidation.protection.enabled=true`；
   - Bean：
     - `InvalidationStormGuard`：
       - 优先创建 Redis 版本（存在 `StringRedisTemplate` + `redisStormEnabled=true`）；
       - 否则创建本地版本；
     - `InvalidationCoalescer`：
       - 使用 `DefaultInvalidationCoalescer`；
     - 受保护的 Publisher：
       - 基于 `DefaultCacheInvalidationPublisher` 构造 `ProtectedCacheInvalidationPublisher`；
       - 作为主 `CacheInvalidationPublisher` Bean 使用。

## 9. 生产建议

### 9.1 Redis 开关策略

- 多实例部署强烈建议：
  - 开启 `redisEpochEnabled=true` 和 `redisStormEnabled=true`；
  - 保证 epoch 与 storm 模式在全局一致；
- 单实例 / 开发环境：
  - 可以关闭这两个开关，仅用本地 Caffeine + AtomicLong 即可。

### 9.2 阈值调优

按门店 / 商品更新频率建议：

- 低频更新（配置变更偶发）：
  - `coalesceThresholdPerMinute` 可适当调高，例如 100；
  - `stormThresholdPerMinute` 设为 300~500；
- 高频更新（如促销、库存波动明显）：
  - 建议保持默认值，观察一段时间后再调整；
  - 若 Ops 监控到频繁进入 `EPOCH_BUMP`，可：
    - 提高 `stormThresholdPerMinute`；
    - 或拆分 namespace，按更细粒度统计。

### 9.3 与版本轮询的关系

ContextMiddleware 中的版本轮询（`VersionChecker`）仍作为兜底策略：

- 缓存失效事件是主路径，保证大多数变更快速收敛；
- 当 Outbox/Redis 短暂故障、写路径尚未全部接入事件时，版本轮询可以发现遗漏的变更。

推荐：

- 在引入事件驱动失效 + 自动保护后，将 `bluecone.contextkit.versionCheckSampleRate` 从 0.1 降到 0.02~0.05：
  - 减轻 DB 负载；
  - 保持“最终一致”的兜底能力。

## 10. 总结

开启 `bluecone.cache.invalidation.protection.enabled=true` 后：

- 正常情况下：继续走精确 key 失效；
- 负载上升但未达 storm 阈值时：同一 tenant+namespace 的大量事件会在 500ms 窗口内合并，减少消息与 DB 压力；
- 真正 storm（阈值触发）时：快速切换到 `EPOCH_BUMP`，通过一次 epoch bump 实现 O(1) 级别的全量失效。

结合 Redis-based epoch 与 storm 计数，能在多实例环境下提供稳定、可观测的缓存失效自动保护机制。 

