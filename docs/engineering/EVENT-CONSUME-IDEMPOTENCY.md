# 事件消费幂等与 EventHandlerTemplate 接入指南

Outbox 保证的是“至少一次投递”（At-least-once）：同一事件可能被多次投递、重复到达不同实例。
因此消费端必须做到：

- 同一 `consumer_group + event_id` 只处理一次（成功）；
- 并发与多实例下只有一个实例获得处理权；
- 失败可记录并按退避策略重试。

本说明文档介绍在 bluecone-app 中的标准做法。

---

## 1. 数据模型：bc_event_consume_record

DDL 见：`docs/sql/bc_event_consume_record.sql`。

核心字段：

- `tenant_id`：租户 ID；
- `consumer_group`：消费者组标识，例如 `ORDER` / `INVENTORY` / `PAYMENT` 等；
- `event_id`：事件 ID（ULID128，BINARY(16)，与 Outbox 一致）；
- `event_type`：事件类型字符串，如 `ORDER_CREATED`；
- `status`：0=PROCESSING,1=SUCCEEDED,2=FAILED；
- `locked_by` / `locked_until`：锁租约（多实例并发抢占）；
- `next_retry_at` / `retry_count`：失败重试与退避；
- `error_msg`：最后一次失败原因（截断 256 字符）；
- `processed_at`：成功处理时间。

唯一键：

- `uk_group_event (consumer_group, event_id)`：保证同一消费者组对同一事件至多成功一次。

---

## 2. 核心抽象：Envelope + Repository + Template

### 2.1 EventEnvelope

位于 `com.bluecone.app.core.event.consume.api.EventEnvelope`：

```java
public record EventEnvelope(
        long tenantId,
        Ulid128 eventId,
        String eventType,
        String payloadJson,
        String headersJson,
        Instant occurredAt
) {}
```

Consumer 不再依赖 Outbox 表结构，只依赖 Envelope。

### 2.2 去重 SPI：EventDedupRepository

位于 `com.bluecone.app.core.event.consume.spi.EventDedupRepository`：

```java
public interface EventDedupRepository {
    AcquireConsumeResult tryAcquire(AcquireConsumeCommand cmd);
    Optional<ConsumeRecord> find(String consumerGroup, Ulid128 eventId);
    void markSuccess(MarkConsumeSuccessCommand cmd);
    void markFailed(MarkConsumeFailedCommand cmd);
}
```

`AcquireConsumeState`：

- `ACQUIRED`：获得处理权；
- `REPLAY_SUCCEEDED`：之前已成功处理，可直接重放；
- `IN_PROGRESS`：其他实例正在处理或锁未过期；
- `RETRYABLE_FAILED`：之前失败且到达重试时间，可由当前实例夺回处理；
- `CONFLICT`：同 group+eventId 但 event_type 不一致等数据问题。

MySQL 实现：`com.bluecone.app.infra.event.consume.MysqlEventDedupRepository`，
对应 DO/Mapper：`EventConsumeRecordDO` / `EventConsumeRecordMapper`。

### 2.3 消费模板：EventHandlerTemplate

接口位于 `com.bluecone.app.core.event.consume.api.EventHandlerTemplate`：

```java
public interface EventHandlerTemplate {
    ConsumeResult consume(String consumerGroup,
                          EventEnvelope event,
                          EventHandler handler,
                          ConsumeOptions options);
}
```

默认实现：`DefaultEventHandlerTemplate`，负责：

- 调用 `EventDedupRepository.tryAcquire` 争抢处理权；
- 根据状态决定是重放、等待还是执行业务；
- 在事务中调用业务 handler，并在同一事务内 markSuccess/markFailed；
- 根据 `baseBackoff` / `maxBackoff` 计算指数退避。

---

## 3. 标准接入模板（伪代码）

### 3.1 定义业务 handler

```java
EventHandler inventoryHandler = envelope -> {
    // 1) 从 payloadJson 反序列化业务对象
    InventoryDeductEvent payload = deserialize(envelope.payloadJson());

    // 2) 使用 eventId 做业务幂等（推荐）
    //    例如库存扣减明细表：uk(tenant_id, consumer_group, event_id)
    inventoryRepository.deductOnce(envelope.tenantId(), envelope.eventId(), payload);
};
```

### 3.2 使用模板消费事件

```java
ConsumeOptions options = new ConsumeOptions(
        Duration.ofSeconds(30),   // lockTtl
        false,                    // waitIfInProgress
        Duration.ZERO,            // waitMax
        20,                       // maxRetry
        Duration.ofSeconds(1),    // baseBackoff
        Duration.ofMinutes(5)     // maxBackoff
);

ConsumeResult result = handlerTemplate.consume(
        "INVENTORY",
        envelope,
        inventoryHandler,
        options
);

if (result.inProgress()) {
    // 可以记录日志或简单忽略，由上游调度下次再投递
}
```

在 `ACQUIRED` 状态：

- 模板在事务中执行：
  - `handler.handle(envelope)`；
  - `EventDedupRepository.markSuccess(...)`；
- 若 handler 抛异常：
  - 根据重试次数计算 `nextRetryAt`；
  - 调用 `markFailed(...)`；
  - 异常向上抛出。

在 `REPLAY_SUCCEEDED` 状态：

- 模板不会再次调用 handler，只返回 `replayed=true` 的 `ConsumeResult`。

在 `IN_PROGRESS` 状态：

- `waitIfInProgress=false`：直接返回 `inProgress=true`；
- `waitIfInProgress=true`：在 `waitMax` 窗口内轮询 `find(...)`：
  - 观察到 `SUCCEEDED` => 返回 `replayed=true`；
  - 观察到 `FAILED` => 抛 `EventConsumeFailedException`；
  - 超时 => 返回 `inProgress=true`。

---

## 4. 失败重试与退避策略

- Template 在 `markFailed` 时会根据：
  - `baseBackoff`（例如 1s）；
  - 当前 `retryCount`；
  - 计算退避：`base * 2^(retry-1)`，并与 `maxBackoff` 比较取较小值；
- `MysqlEventDedupRepository` 会持久化：
  - `status=FAILED`；
  - `retry_count` / `next_retry_at`。

上游调度器（例如 Outbox 消费调度）可以根据 `status=FAILED` 且 `next_retry_at <= now` 决定是否再次触发消费。

**maxRetry 建议**：

- 当 `retryCount` 超过 `maxRetry` 时，仍可写入失败记录，但应结合监控/告警人工介入；
- 也可在 error_msg 中标记 “exceeded maxRetry”。

---

## 5. consumer_group 的意义

`consumer_group` 用于区分不同模块对同一事件的独立消费：

- 例如同一个 `ORDER_CREATED` 事件：
  - `consumer_group = "INVENTORY"`：扣减库存；
  - `consumer_group = "NOTIFY"`：发送通知；
  - `consumer_group = "BILLING"`：记账。

表 `bc_event_consume_record` 的唯一键 `(consumer_group, event_id)` 保证：

- 每个消费者组对同一个事件最多成功处理一次；
- 不同组之间互不影响。

---

## 6. InProcessEventBus（开发验证）

位于 `com.bluecone.app.core.event.bus.InProcessEventBus`，用于在不引入 MQ 时快速接入消费模板：

```java
InProcessEventBus bus = new InProcessEventBus(eventHandlerTemplate);

bus.register("INVENTORY", "ORDER_CREATED", inventoryHandler);

bus.publish(envelope); // 内部会按 group/type 调用 handlerTemplate.consume(...)
```

生产环境接入 MQ Consumer 时，只需在 MQ 回调中调用同一 `EventHandlerTemplate.consume(...)` 即可，保证消费逻辑与幂等策略统一。

---

## 7. 生产建议

1) **消费端也要做业务幂等**：
   - 建议在业务表或辅助表中使用 `(tenant_id, consumer_group, event_id)` 建唯一索引；
   - handler 内部应以 eventId 为幂等键，防止「重复扣库存」「重复发券」等。

2) **处理时长与 lockTtl**：
   - handler 应该尽量快，只做必要业务逻辑；
   - 长耗时任务应拆分/异步化；
   - `lockTtl` 建议设置为 handler 正常处理时间的数倍（如 30s/60s），避免频繁抢占。

3) **监控与告警**：
   - 关注 `bc_event_consume_record` 中 `status=FAILED` 且 `retry_count` 接近或超过阈值的记录；
   - 将失败条数、最大 retry_count 等指标纳入监控面板；
   - 对 error_msg 中特定关键字（如 “exceeded maxRetry”）设置告警。

通过以上机制，bluecone-app 在 Outbox 至少一次投递语义之上，
为消费端提供了一套可复用、可观测的幂等处理模板，降低重复消费和并发竞态带来的风险。 

