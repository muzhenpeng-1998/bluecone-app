# Cache Invalidation Events（事件驱动缓存失效）

本文档描述 bluecone-app 中针对 Store/Product/Inventory/User 等上下文快照的 **事件驱动缓存失效机制**。

目标：

- 写路径（配置变更）发布 “缓存失效事件”；
- 本实例在事务提交后立刻清理 L1/L2 缓存；
- 多实例之间通过 Outbox（优先）或 Redis Pub/Sub（可选）广播，从而同步清理；
- 保留版本轮询作为兜底，但降低频率以减轻 DB 压力。

---

## 1. 为什么要事件驱动失效？

问题背景：

- Store/User/Inventory 等上下文通过 `StoreSnapshotProvider` / `UserSnapshotProvider` 等组件被缓存到：
  - L1：Caffeine（`CaffeineContextCache`）；
  - L2：Redis（`RedisContextCache`）；
  - 并通过 `VersionChecker` 周期性检查版本变化。
- 仅靠版本轮询存在以下问题：
  - DB 负载：高 QPS 场景下频繁版本查询；
  - 时效性：更新之后到下一次轮询之间存在短暂不一致；
  - 复杂性：各个 SnapshotProvider 内部均需实现自己的版本逻辑。

事件驱动的好处：

- 写路径知道 “谁被改了”，可以发出精确的失效事件（以 key 为粒度）；
- 事务提交后立刻清理本机缓存，避免读到脏数据；
- Outbox/Redis 按事件广播到其他实例，使集群内缓存尽快收敛；
- 版本轮询只作为兜底，采样率可以显著降低（例如从 0.1 降到 0.05）。

---

## 2. 事件模型

包：`com.bluecone.app.core.cacheinval.api`

### 2.1 InvalidationScope

枚举：`InvalidationScope`（`app-core/src/main/java/com/bluecone/app/core/cacheinval/api/InvalidationScope.java`）

- `STORE`：门店级快照；
- `PRODUCT`：商品级快照；
- `SKU`：SKU 级快照；
- `INVENTORY_POLICY`：库存策略；
- `USER`：用户快照。

### 2.2 CacheInvalidationEvent

记录类型：`CacheInvalidationEvent`（`app-core/src/main/java/com/bluecone/app/core/cacheinval/api/CacheInvalidationEvent.java`）

```java
public record CacheInvalidationEvent(
        String eventId,         // ULID 字符串
        long tenantId,
        InvalidationScope scope,
        String namespace,       // 与 ContextKit CacheKey.namespace 对齐，如 "store:snap"
        List<String> keys,      // 例如 "tenantId:internalId"
        long configVersion,     // 可选，用于审计/调试
        Instant occurredAt
) {}
```

说明：

- `keys` 优先使用精确键（单个或少量 key），避免大范围 “clear all”；
- 若未来有 “清理某租户全量” 等需求，可扩展额外 scope 类型（例如 `TENANT_WIDE`），本次不强制。

### 2.3 Publisher 接口

接口：`CacheInvalidationPublisher`（`app-core/src/main/java/com/bluecone/app/core/cacheinval/api/CacheInvalidationPublisher.java`）

```java
public interface CacheInvalidationPublisher {
    void publishAfterCommit(CacheInvalidationEvent event);
}
```

语义：

- 要求 “afterCommit” 执行，避免事务回滚导致误清；
- 若当前线程没有事务，则视为 best-effort，直接执行 + 广播（文档标注为不推荐但允许）。

---

## 3. 执行器：统一 L1/L2 清理

包：`com.bluecone.app.core.cacheinval.application`

### 3.1 CacheInvalidationExecutor

接口：`CacheInvalidationExecutor`（`app-core/src/main/java/com/bluecone/app/core/cacheinval/application/CacheInvalidationExecutor.java`）

```java
public interface CacheInvalidationExecutor {
    void execute(CacheInvalidationEvent event);
}
```

语义：只负责 **本实例** 的缓存失效，不关心跨实例传播。

### 3.2 DefaultCacheInvalidationExecutor

实现：`DefaultCacheInvalidationExecutor`（`app-core/src/main/java/com/bluecone/app/core/cacheinval/application/DefaultCacheInvalidationExecutor.java`）

- 注入：`ContextCache`（通常是 `TwoLevelContextCache`，L1+L2 组合）；
- 对 `event.keys` 遍历：

```java
CacheKey cacheKey = new CacheKey(event.namespace(), key);
cache.invalidate(cacheKey);
```

- 对 `TwoLevelContextCache`，`invalidate` 会同时作用于 L1/L2；
- 实现要求幂等：同一 event 多次执行不出错。

> `ContextCache.invalidate(CacheKey)` 已在 `RedisContextCache` 等实现中支持，L1 Caffeine 默认可以按需扩展。  
> `TwoLevelContextCache` 已实现同时清理 L1/L2。

---

## 4. 传输层：Outbox / Redis PubSub / InProcess

包：`com.bluecone.app.core.cacheinval.transport`

### 4.1 抽象

- 枚举：`InvalidationTransport`（OUTBOX / REDIS_PUBSUB / INPROCESS）；
- 接口：`CacheInvalidationBus`（`app-core/src/main/java/com/bluecone/app/core/cacheinval/transport/CacheInvalidationBus.java`）：

```java
public interface CacheInvalidationBus {
    void broadcast(CacheInvalidationEvent event);
    void subscribe(Consumer<CacheInvalidationEvent> consumer);
}
```

要求：

- `broadcast`：至少一次投递；重复消息必须安全（下游执行器幂等）；
- `subscribe`：注册本地消费者，具体调用线程由实现决定。

### 4.2 Outbox 实现（推荐）

实现类：`OutboxCacheInvalidationBus`（`app-application/src/main/java/com/bluecone/app/cacheinval/transport/OutboxCacheInvalidationBus.java`）

- 依赖：
  - `DomainEventPublisher`（已有 Outbox 实现）；
  - `EventRouter`、`EventPipeline`（与现有 DomainEvent 生态一致）。
- 关键点：
  - 将 `CacheInvalidationEvent` 包装为 `CacheInvalidationDomainEvent`（继承 `DomainEvent`，`eventType="cache.invalidation"`）；
  - `broadcast` 内部调用 `domainEventPublisher.publish(domainEvent)`，由 Outbox 事务写表、调度器异步分发；
  - 同时实现 `EventHandler<CacheInvalidationDomainEvent>`，在 handler 中：
    - 提取 `payload`（即 `CacheInvalidationEvent`）；
    - 依次调用本地注册的 `Consumer<CacheInvalidationEvent>`。

### 4.3 Redis Pub/Sub 实现（可选）

实现类：`RedisPubSubCacheInvalidationBus`（`app-application/src/main/java/com/bluecone/app/cacheinval/transport/RedisPubSubCacheInvalidationBus.java`）

- 依赖：
  - `StringRedisTemplate`；
  - `ObjectMapper`；
  - `CacheInvalidationProperties`（提供 `redisTopic`）。
- `broadcast`：
  - 使用 `ObjectMapper` 序列化 `CacheInvalidationEvent` 为 JSON；
  - 调用 `convertAndSend(topic, json)`。
- `subscribe`：
  - 内部维护 `List<Consumer<CacheInvalidationEvent>>`；
  - 实现 `MessageListener.onMessage`，反序列化 JSON 成 `CacheInvalidationEvent` 并推送给所有 Consumer。

> 自动装配时会创建 `RedisMessageListenerContainer` 订阅对应 topic。

### 4.4 InProcess 实现（dev / 单实例）

实现类：`InProcessCacheInvalidationBus`（`app-application/src/main/java/com/bluecone/app/cacheinval/transport/InProcessCacheInvalidationBus.java`）

- 仅在本进程内维护 `CopyOnWriteArrayList<Consumer>`；
- `broadcast` 直接遍历调用所有 consumer；
- 适合 dev / 单实例场景或没有 Outbox / Redis 的部署环境。

---

## 5. Publisher：本地即时失效 + 广播

实现类：`DefaultCacheInvalidationPublisher`（`app-core/src/main/java/com/bluecone/app/core/cacheinval/application/DefaultCacheInvalidationPublisher.java`）

- 注入：
  - `CacheInvalidationExecutor executor`；
  - `CacheInvalidationBus bus`。
- `publishAfterCommit(event)`：
  - 若当前线程中有事务（`TransactionSynchronizationManager.isSynchronizationActive()` 为 true）：
    - 注册 `TransactionSynchronization.afterCommit` 回调；
    - 在 `afterCommit` 中调用：

```java
executor.execute(event);   // 本实例立即失效
bus.broadcast(event);      // 广播给其他实例
```

  - 若当前无事务：
    - 直接执行上面的 `perform(event)` 逻辑（视为 best-effort，文档提示尽量在事务中使用）。

---

## 6. 订阅端与幂等性

### 6.1 Listener（订阅执行）

订阅方式：

- AutoConfiguration 创建 `CacheInvalidationBus` 后，业务或框架层会注册一个统一 consumer：

```java
bus.subscribe(event -> executor.execute(event));
```

- 对 Outbox/Redis/InProcess 三种 transport 来说，行为一致：收到事件 → 调用 executor → 清 L1/L2。

### 6.2 幂等与去重策略

- `ContextCache.invalidate(CacheKey)` 本身幂等：重复调用不会出错；
- Outbox/Redis 可能导致重复消息：
  - 例如重试、收敛逻辑或网络抖动；
- 因此可以不强制做全局去重，仅依赖 “重复失效不会出错” 作为安全保障。

如需进一步减少风暴，可在后续扩展：

- 在 `ContextMiddleware` 层或 executor 层加一个本地 LRU（Caffeine）保存最近 `eventId`；
- 若 `eventId` 在短时间（例如 1 分钟）内已经处理过，则跳过执行；
- 本次实现预留了 `CacheInvalidationProperties.recentEventTtl` 等配置字段，方便后续扩展。

---

## 7. 配置与 AutoConfiguration

### 7.1 配置项

类：`CacheInvalidationProperties`（`app-application/src/main/java/com/bluecone/app/config/CacheInvalidationProperties.java`）

```yaml
bluecone:
  cache:
    invalidation:
      enabled: false           # 默认关闭，避免未准备好时上线
      transport: OUTBOX        # OUTBOX / REDIS_PUBSUB / INPROCESS
      redisTopic: "bc:cache:inval"
      recentEventTtl: PT1M
      maxKeysPerEvent: 50
```

说明：

- `enabled=false` 为默认值，开启后才会注册相关 Bean；
- `transport`：
  - `OUTBOX`：首选，利用已有 Outbox 表和调度器；
  - `REDIS_PUBSUB`：轻量级广播；
  - `INPROCESS`：单机 / dev 场景。

### 7.2 AutoConfiguration

类：`CacheInvalidationAutoConfiguration`（`app-application/src/main/java/com/bluecone/app/config/CacheInvalidationAutoConfiguration.java`）

条件：

- `@ConditionalOnProperty(prefix="bluecone.cache.invalidation", name="enabled", havingValue="true")`。

提供 Bean：

- `CacheInvalidationExecutor`：
  - 默认：`new DefaultCacheInvalidationExecutor(contextKitCache)`；
- `CacheInvalidationBus`：
  - 当 `transport=OUTBOX`：
    - 需要 `DomainEventPublisher`、`EventRouter`、`EventPipeline`；
    - 创建 `OutboxCacheInvalidationBus`；
  - 当 `transport=REDIS_PUBSUB`：
    - 需要 `StringRedisTemplate` 与 `ObjectMapper`；
    - 创建 `RedisPubSubCacheInvalidationBus` + `RedisMessageListenerContainer` 订阅 topic；
  - 当 `transport=INPROCESS`：
    - 创建 `InProcessCacheInvalidationBus`。
- `CacheInvalidationPublisher`：

```java
new DefaultCacheInvalidationPublisher(executor, bus);
```

> 一旦组件启动，业务只需注入 `CacheInvalidationPublisher` 并在写路径发布事件即可。

---

## 8. 缓存键规范与 SnapshotProvider 对接

### 8.1 Namespace 约定

与 ContextKit 中 `CacheKey.namespace` 对齐，建议统一为：

- 门店快照：`"store:snap"`；
- 商品快照：`"product:snap"`；
- SKU 快照：`"sku:snap"`；
- 库存策略：`"inventory:policy"`；
- 用户快照：`"user:snap"`。

### 8.2 Key 约定

Key 采用 `"tenantId:internalId"` 形式（字符串）：

- `tenantId`：Long → `String.valueOf(tenantId)`；
- `internalId`：
  - 若使用 `Ulid128`，推荐 `Ulid128.toString()`（26 字符 ULID）；
  - 需要与 SnapshotProvider 中构建 CacheKey 的规则完全一致。

示例：

```java
String key = tenantId + ":" + storeInternalId.toString();
CacheKey cacheKey = new CacheKey("store:snap", key);
```

所有 SnapshotProvider/ContextMiddlewareKit 中的 key 生成逻辑，应统一按上述规则，保证事件驱动失效与版本轮询使用相同的命名空间与键。

---

## 9. 业务写路径接入（示例）

### 9.1 门店配置更新（示例）

在门店相关应用服务（如 `UpdateStoreCapabilitiesCommandHandler` 或 `ChangeStoreStatusService`）中：

1. 完成数据库更新，获取最新的：
   - `tenantId`；
   - `storeInternalId`（`Ulid128` 或 Long）；
   - `configVersion`（例如 `bc_store.config_version`）。
2. 构造事件：

```java
String key = tenantId + ":" + storeInternalId.toString();
CacheInvalidationEvent evt = new CacheInvalidationEvent(
    idService.nextUlidString(),         // eventId
    tenantId,
    InvalidationScope.STORE,
    "store:snap",
    List.of(key),
    newConfigVersion,
    Instant.now()
);
cacheInvalidationPublisher.publishAfterCommit(evt);
```

3. 因为 `publishAfterCommit` 使用事务同步机制：
   - 若事务回滚，事件不会执行/广播；
   - 若提交成功，本实例立刻失效 L1/L2，并通过 Outbox/Redis 广播到其他实例。

### 9.2 用户状态更新（示例）

类似地，在用户状态更新写路径（冻结/解冻/注销）中：

```java
String key = tenantId + ":" + userInternalId.toString();
CacheInvalidationEvent evt = new CacheInvalidationEvent(
    idService.nextUlidString(),
    tenantId,
    InvalidationScope.USER,
    "user:snap",
    List.of(key),
    newConfigVersion,
    Instant.now()
);
cacheInvalidationPublisher.publishAfterCommit(evt);
```

> 具体接入点请参考用户领域应用服务（如 `UserStatusChangeService`）；建议在保存实体并更新版本号之后立即发布事件。

同理可以扩展到：

- 商品上下架 / 价格变更（`PRODUCT` / `SKU`）；
- 库存策略调整（`INVENTORY_POLICY`，namespace `"inventory:policy"`）。

---

## 10. 回退策略：版本轮询兜底

事件驱动失效并不完全替代版本轮询，而是形成如下关系：

- 事件驱动：主路径，写路径可靠地发出具体 key 的失效事件，保证缓存尽快收敛；
- 版本轮询：兜底路径，在以下情况下很有价值：
  - 某些写路径尚未接入事件发布；
  - Outbox 调度器短暂不可用或 Redis 集群故障；
  - 网络抖动导致部分事件丢失。

建议调整：

- 在 `ContextKitProperties` 中将 `versionCheckSampleRate` 从原来的值适当降低（例如从 0.1 降到 0.05），减轻 DB 压力；
- 保持 `versionCheckWindow` 在合理范围（2s~5s），结合事件驱动机制，既保证最终一致，又降低轮询频率。

---

## 11. 测试建议

推荐至少覆盖以下几类测试（部分已在代码中实现）：

- `KeyInvalidationTest`：
  - 针对 `DefaultCacheInvalidationExecutor`，验证给定 keys 能调用 `ContextCache.invalidate` 对应的 `CacheKey`。
- `PublisherAfterCommitTest`：
  - 在事务内调用 `publishAfterCommit`；
  - 断言在事务提交前不会调用 `executor.execute` 和 `bus.broadcast`；
  - 提交后两者按顺序被调用（可用 spy 验证）。
- `ListenerIdempotencyTest`：
  - 对同一个 `CacheInvalidationEvent` 调用 `execute` 两次，应无错误且状态一致；
  - 如增加去重 LRU，可验证同一 `eventId` 只执行一次。
- 业务写路径集成测试（如 `StoreUpdatePublishesInvalidationTest`）：
  - 调用门店修改接口/服务；
  - 断言 `CacheInvalidationPublisher.publishAfterCommit` 被调用一次，且 `namespace` 和 `keys` 正确。

在 CI 中保留这些测试，有助于防止未来修改 SnapshotProvider / 写路径逻辑时误删事件驱动失效能力。  
