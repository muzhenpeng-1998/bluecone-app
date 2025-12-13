# Outbox 事件发布基础设施（Transactional Event Publishing）

本说明文档描述 bluecone-app 中基于 Outbox 模式的“事务事件发布”能力，
以及如何与幂等创建模板（IdempotentCreateTemplate）配合使用。

目标：

- 业务表写入与 Outbox 事件写入处于同一数据库事务；
- 事件异步投递，可重试且至少一次（At-least-once）；
- 支持多实例并发消费，消费端通过 eventId 幂等；
- 接入成本低：业务只依赖 `DomainEvent` + `DomainEventPublisher`。

---

## 1. 表结构与状态机

当前实现使用表 `bc_outbox_message`（定义见 `app-infra/src/main/resources/db/migration/V2024__create_bc_outbox_message.sql`）。

规范版 DDL 见：`docs/sql/bc_outbox_event.sql`，推荐字段：

- `tenant_id`：租户 ID（多租户隔离与清理）；
- `event_id`：事件内部 ID（ULID128，BINARY(16)），用于消费端幂等；
- `event_type`：事件语义，如 `ORDER_CREATED`；
- `aggregate_type` / `aggregate_id` / `public_aggregate_id`：标识来源聚合；
- `payload_json` / `headers_json`：事件载荷与头（traceId/requestId/operator 等）；
- `status`：0=NEW,1=PROCESSING,2=SENT,3=FAILED；
- `locked_by` / `locked_until`：多实例竞争的租约；
- `next_retry_at` / `retry_count`：重试调度；
- `error_msg`：最后一次失败原因（截断版）。

当前代码中的 `bc_outbox_message` 使用 `event_key` + JSON payload/headers 存储，语义与上述字段对齐，
未来可以按需要迁移到 `bc_outbox_event` 结构。

---

## 2. 核心抽象与实现

### 2.1 领域事件与发布入口

- 核心接口：
  - `com.bluecone.app.core.event.DomainEvent`：所有领域事件的抽象基类；
  - `com.bluecone.app.core.event.DomainEventPublisher`：领域层唯一依赖的发布入口。
- 生产环境实现：
  - `com.bluecone.app.infra.outbox.core.TransactionalOutboxEventPublisher`：
    - `@Transactional`，在业务事务内将事件序列化后写入 Outbox 表；
    - 使用 `OutboxStoreService.persist` 落库；
    - headers 中写入 `traceId`、`tenantId`、`eventId` 等信息。
- 开发/测试环境：
  - `InMemoryEventPublisher`：直接在当前线程触发 `EventOrchestrator`，绕过 Outbox，便于本地调试。

业务代码只依赖 `DomainEvent` + `DomainEventPublisher` 即可，不需要了解 Outbox 表详情。

### 2.2 Outbox 持久化与调度

- 持久化：
  - `OutboxMessageEntity`：映射表 `bc_outbox_message`；
  - `OutboxMessageRepository`：封装 MyBatis-Plus 的持久化操作；
  - `OutboxStoreService`：事务内将事件持久化为一条 Outbox 记录（status=NEW）。
- 调度与投递：
  - `OutboxDispatchService`：扫描待投递记录并调用 `OutboxEventRouter` 分发到各个 `EventHandler`；
  - `OutboxPublisherJob`：`@Scheduled` 定时触发 `dispatchDueMessages()`；
  - 重试策略由 `RetryPolicy` / `SimpleExponentialBackoffRetryPolicy` 决定；
  - 消费幂等由 `EventConsumptionTracker`（默认 `RedisEventConsumptionTracker`）确保同一 handler + eventId 只处理一次。

投递语义：**At-least-once**，消费端必须以 eventId 为幂等键。

---

## 3. 与 IdempotentCreateTemplate 的协作

在 Step 10 中，我们为所有 create 接口提供了 `IdempotentCreateTemplate`。

为方便在“创建成功”时登记领域事件，本仓库在 `app-core` 中增加了：

- `CreateWorkWithEvents<T>`：创建逻辑回调，返回业务结果与待发布事件列表；
- `CreateWorkWithEventsResult<T>`：包含 `value` 与 `List<DomainEvent>`；
- `IdempotentCreateTemplate#create(CreateRequest, CreateWorkWithEvents<T>)`：带事件的 create 重载；
- `DefaultIdempotentCreateTemplate` 中集成了对 `DomainEventPublisher` 的调用：
  - 在同一事务（REQUIRES_NEW/REQUIRED）中执行：
    1. 业务写入；
    2. 幂等 `markSuccess`；
    3. 调用 `DomainEventPublisher.publish` 注册事件（事务内写 Outbox）。

这样可以保证：

> 业务表写入 + 幂等记录 + Outbox 事件写入处于同一事务，提交即同时成功，回滚即同时回滚。

### 3.1 标准用法示例（结合创建模板）

```java
IdempotentCreateResult<OrderCreatedDTO> result =
        idempotentCreateTemplate.create(req, (internalId, publicId) -> {
            // 1) 落库：订单表使用 internalId（BINARY(16)）+ publicId（VARCHAR UNIQUE）
            orderRepository.insert(internalId, publicId, tenantId, ...);

            // 2) 构造领域事件（继承 DomainEvent）
            OrderCreatedEvent event = new OrderCreatedEvent(
                    publicId,
                    tenantId,
                    // 其他字段...
            );

            // 3) 返回业务结果 + 事件列表
            return CreateWorkWithEvents.result(
                    new OrderCreatedDTO(publicId),
                    List.of(event)
            );
        });
```

在 `TxMode.REQUIRES_NEW` 下：

- `DefaultIdempotentCreateTemplate` 会在一个新事务中：
  - 执行上述 `insert`；
  - `markSuccess` 幂等记录（result_ref=publicId）；
  - 调用 `DomainEventPublisher.publish(event)` 将事件写入 Outbox；
- 事务提交成功后，Outbox 才会逐步被 `OutboxDispatchService` 异步消费。

如果 `work` 抛异常，事务整体回滚，Outbox 中不会插入事件，也不会 `markSuccess`，而是通过 `markFailed` 记录失败。

---

## 4. 多实例与重试策略

### 4.1 多实例并发

- `OutboxDispatchService.dispatchDueMessages()` 使用数据库查询 `status IN (NEW, FAILED)` 且 `next_retry_at <= now` 的记录；
- 当前实现通过状态机与 `retryCount/nextRetryAt` 保证：
  - 多实例下不会无限重复处理同一条记录；
  - 消费出错时会按指数退避重试；
  - 超过阈值后标记为 DEAD，以便人工介入。

在消费侧，`RedisEventConsumptionTracker` 为每个 handler + eventId 建立 Redis 键，实现幂等消费。

### 4.2 退避与失败处理

- 退避策略由 `RetryPolicy` 决定，默认实现为简单指数退避：
  - 第一次失败：延迟较短；
  - 随着 `retryCount` 增加，延迟逐步加大，直到上限；
- 当策略判断“应该放弃”时，记录会进入 DEAD 状态，`OutboxMetricsRecorder` 负责打点，运维可结合监控报警。

---

## 5. 接入与配置

### 5.1 Outbox 自动装配

Outbox 相关自动装配位于：

- `com.bluecone.app.infra.outbox.config.OutboxConfiguration`
  - 注册 `EventSerializer`、`RetryPolicy`、`EventConsumptionTracker`；
  - 根据 profile 决定使用 `TransactionalOutboxEventPublisher` 或 `InMemoryEventPublisher`。

典型配置：

```yaml
bluecone:
  outbox:
    enabled: true
    dispatch-batch-size: 100
    publish-cron: "0/10 * * * * ?"
    # 其他与重试、消费去重相关的属性见 OutboxProperties
```

### 5.2 建议的生产参数

- `dispatch-batch-size`：100–500 之间，根据事件量与 DB 压力调整；
- 重试退避上限：5–10 分钟；
- 消费去重 TTL：按业务保留时间设置（如 7–30 天），确保重复投递不会重复执行业务。

---

## 6. 总结

通过：

- 事务内 Outbox 写入（TransactionalOutboxEventPublisher）；
- 幂等创建模板 + 带事件的 `CreateWorkWithEvents`；
- OutboxDispatchService + RetryPolicy + EventConsumptionTracker；

bluecone-app 形成了一套可复用的“领域事件发布基础设施”，满足：

- 业务写入与事件写入同事务，保证强一致；
- 至少一次投递语义，消费端可通过 eventId 幂等；
- 多实例部署下安全抢占事件，避免重复处理；
- 低侵入：业务只需构造 DomainEvent 并通过模板或 DomainEventPublisher 发布即可。 

