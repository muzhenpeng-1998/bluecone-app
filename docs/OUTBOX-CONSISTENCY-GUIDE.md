# Outbox + 消费者幂等 + 补偿任务 实现指南

## 概述

本文档描述 bluecone-app 的事件驱动一致性机制，通过 Outbox 模式 + 幂等消费者 + 补偿任务，保证订单与资产（优惠券/钱包/积分）的最终一致性。

## 核心设计

### 1. Outbox 模式

**目标**：保证业务数据与事件的原子性写入，避免消息丢失。

**实现**：
- 订单/支付状态变更时，同事务写入 `bc_outbox_event` 表
- 后台 `OutboxPublisherJob` 定时扫描待投递事件，投递到 `InProcessEventDispatcher`
- 投递成功标记为 `SENT`，失败标记为 `FAILED` 并指数退避重试
- 超过最大重试次数（默认 10 次）标记为 `DEAD`，需人工介入

**表结构**：`bc_outbox_event`
```sql
CREATE TABLE bc_outbox_event (
    id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    tenant_id BIGINT UNSIGNED NULL,
    store_id BIGINT UNSIGNED NULL,
    aggregate_type VARCHAR(64) NOT NULL,      -- ORDER/PAYMENT/REFUND
    aggregate_id VARCHAR(128) NOT NULL,       -- 订单ID/支付单ID/退款单ID
    event_type VARCHAR(128) NOT NULL,         -- order.checkout_locked/order.paid/order.canceled/order.refunded
    event_id VARCHAR(64) NOT NULL UNIQUE,     -- 事件唯一ID（UUID）
    event_payload JSON NOT NULL,              -- 事件载荷
    event_metadata JSON NULL,                 -- 事件元数据（traceId/requestId/userId）
    status VARCHAR(32) NOT NULL DEFAULT 'NEW', -- NEW/SENT/FAILED/DEAD
    retry_count INT NOT NULL DEFAULT 0,
    max_retry_count INT NOT NULL DEFAULT 10,
    next_retry_at DATETIME NULL,
    last_error TEXT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    sent_at DATETIME NULL,
    KEY idx_status_next_retry (status, next_retry_at),
    KEY idx_aggregate (aggregate_type, aggregate_id)
);
```

### 2. 消费者幂等

**目标**：保证事件重复投递时，资产操作只生效一次。

**实现**：
- 消费前检查 `bc_event_consume_log` 表（`consumer_name` + `event_id` 唯一）
- 如果已消费（状态为 `SUCCESS`），直接返回
- 调用 Facade 层 API，传入 `idempotencyKey`（业务侧幂等键）
- 消费成功写入 `SUCCESS` 记录，失败写入 `FAILED` 记录并抛异常让 Outbox 重试

**表结构**：`bc_event_consume_log`
```sql
CREATE TABLE bc_event_consume_log (
    id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    tenant_id BIGINT UNSIGNED NULL,
    consumer_name VARCHAR(128) NOT NULL,      -- CouponConsumer/WalletConsumer/PointsConsumer
    event_id VARCHAR(64) NOT NULL,            -- 对应 bc_outbox_event.event_id
    event_type VARCHAR(128) NOT NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'SUCCESS', -- SUCCESS/FAILED/PROCESSING
    idempotency_key VARCHAR(255) NULL,        -- 业务侧幂等键（如 order:123:checkout）
    consume_result JSON NULL,
    error_message TEXT NULL,
    consumed_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_consumer_event (consumer_name, event_id),
    KEY idx_idempotency_key (idempotency_key)
);
```

### 3. 补偿任务

**目标**：兜底机制，处理异常场景（超时锁定、事件丢失）。

**实现**：
- `LockTimeoutReaperJob`：每 5 分钟扫描超时锁定（默认 30 分钟），自动释放
- `OrderAssetConsistencyJob`：每 2 小时扫描已支付订单，检查是否缺失 `order.paid` 事件，补发到 Outbox

---

## 事件列表

### 订单事件

| 事件类型 | 触发时机 | 资产操作 | 幂等键格式 |
|---------|---------|---------|-----------|
| `order.checkout_locked` | 订单从 DRAFT 提交到 WAIT_PAY | 锁定优惠券、冻结钱包、冻结积分 | `order:{orderId}:checkout` |
| `order.paid` | 订单从 WAIT_PAY 流转到 PAID | 核销优惠券、扣减钱包、扣减积分、赚取积分 | `order:{orderId}:commit` / `order:{orderId}:earn` |
| `order.canceled` | 订单流转到 CANCELED | 释放优惠券、释放钱包冻结、释放积分冻结 | `order:{orderId}:release` |
| `order.refunded` | 退款单状态流转到 SUCCESS | 回退钱包、回退积分 | `refund:{refundId}:revert` |

### 支付事件

| 事件类型 | 触发时机 | 资产操作 |
|---------|---------|---------|
| `payment.success` | 支付单状态流转到 SUCCESS | 触发订单支付成功流程 |
| `payment.failed` | 支付单状态流转到 FAILED | 释放订单锁定的资产 |

### 退款事件

| 事件类型 | 触发时机 | 资产操作 |
|---------|---------|---------|
| `refund.success` | 退款单状态流转到 SUCCESS | 回退钱包、回退积分 |
| `refund.failed` | 退款单状态流转到 FAILED | 记录日志，人工介入 |

---

## 幂等规则

### 1. Outbox 层幂等

- **机制**：`event_id` 唯一索引
- **保证**：同一事件不会重复写入 Outbox
- **适用场景**：防止业务代码重复调用 `outboxEventService.save()`

### 2. 消费者层幂等

- **机制**：`bc_event_consume_log` 表 `consumer_name` + `event_id` 唯一索引
- **保证**：同一事件不会被同一消费者重复消费
- **适用场景**：防止 Outbox 重试导致资产重复操作

### 3. 业务层幂等

- **机制**：Facade 层 `idempotencyKey` 参数
- **保证**：同一业务操作（如锁定、核销、释放）不会重复执行
- **适用场景**：防止消费者重复调用 Facade API

### 幂等键设计

| 操作类型 | 幂等键格式 | 示例 |
|---------|-----------|------|
| 优惠券锁定 | `order:{orderId}:checkout` | `order:123:checkout` |
| 优惠券核销 | `order:{orderId}:commit` | `order:123:commit` |
| 优惠券释放 | `order:{orderId}:release` | `order:123:release` |
| 钱包冻结 | `wallet:{orderId}:freeze` | `wallet:123:freeze` |
| 钱包提交 | `wallet:{orderId}:commit` | `wallet:123:commit` |
| 钱包释放 | `wallet:{orderId}:release` | `wallet:123:release` |
| 钱包回退 | `wallet:{refundId}:revert` | `wallet:456:revert` |
| 积分冻结 | `points:{orderId}:freeze` | `points:123:freeze` |
| 积分提交 | `points:{orderId}:commit` | `points:123:commit` |
| 积分释放 | `points:{orderId}:release` | `points:123:release` |
| 积分回退 | `points:{refundId}:revert` | `points:456:revert` |
| 积分赚取 | `points:{orderId}:earn` | `points:123:earn` |

---

## 补偿策略

### 1. 锁定超时释放（LockTimeoutReaperJob）

**触发条件**：
- 优惠券锁定状态为 `LOCKED`，创建时间超过 30 分钟
- 钱包冻结状态为 `FROZEN`，创建时间超过 30 分钟
- 积分冻结状态为 `FROZEN`，创建时间超过 30 分钟

**补偿动作**：
- 更新状态为 `RELEASED` / `AVAILABLE`
- 恢复账户可用余额/积分
- 记录释放日志

**执行频率**：每 5 分钟

**配置项**：
```yaml
bluecone:
  asset:
    lock-timeout-minutes: 30  # 锁定超时时间（分钟）
```

### 2. 订单资产一致性检查（OrderAssetConsistencyJob）

**触发条件**：
- 订单状态为 `PAID`，支付状态为 `PAID`
- 更新时间在最近 24 小时内
- 不存在对应的 `order.paid` 事件

**补偿动作**：
- 补发 `order.paid` 事件到 Outbox
- 由 OutboxPublisher 投递事件
- 消费者幂等处理，不会重复扣减资产

**执行频率**：每 2 小时

**配置项**：
```yaml
bluecone:
  consistency:
    scan-window-hours: 24  # 扫描时间窗口（小时）
```

---

## 核心流程

### 订单下单流程（Checkout）

```
1. 用户提交订单
2. 订单服务：
   - 更新订单状态：DRAFT → WAIT_PAY
   - 同事务写入 Outbox 事件：order.checkout_locked
3. OutboxPublisher：
   - 扫描待投递事件
   - 投递到 InProcessEventDispatcher
4. CouponConsumer：
   - 检查消费日志（幂等）
   - 调用 CouponLockFacade.lock()
   - 记录消费成功
5. WalletConsumer：
   - 检查消费日志（幂等）
   - 调用 WalletAssetFacade.freeze()
   - 记录消费成功
6. PointsConsumer：
   - 检查消费日志（幂等）
   - 调用 PointsAssetFacade.freezePoints()
   - 记录消费成功
```

### 订单支付流程（Pay）

```
1. 支付回调通知
2. 支付服务：
   - 更新支付单状态：PROCESSING → SUCCESS
   - 更新订单状态：WAIT_PAY → PAID
   - 同事务写入 Outbox 事件：order.paid
3. OutboxPublisher：
   - 扫描待投递事件
   - 投递到 InProcessEventDispatcher
4. CouponConsumer：
   - 检查消费日志（幂等）
   - 调用 CouponLockFacade.commit()
   - 记录消费成功
5. WalletConsumer：
   - 检查消费日志（幂等）
   - 调用 WalletAssetFacade.commit()
   - 记录消费成功
6. PointsConsumer：
   - 检查消费日志（幂等）
   - 调用 PointsAssetFacade.commitPoints()（扣减）
   - 调用 PointsAssetFacade.commitPoints()（赚取）
   - 记录消费成功
```

### 订单取消流程（Cancel）

```
1. 用户取消订单 / 商户拒单 / 超时取消
2. 订单服务：
   - 更新订单状态：WAIT_PAY/WAIT_ACCEPT → CANCELED
   - 同事务写入 Outbox 事件：order.canceled
3. OutboxPublisher：
   - 扫描待投递事件
   - 投递到 InProcessEventDispatcher
4. CouponConsumer：
   - 检查消费日志（幂等）
   - 调用 CouponLockFacade.release()
   - 记录消费成功
5. WalletConsumer：
   - 检查消费日志（幂等）
   - 调用 WalletAssetFacade.release()
   - 记录消费成功
6. PointsConsumer：
   - 检查消费日志（幂等）
   - 调用 PointsAssetFacade.releasePoints()
   - 记录消费成功
```

### 订单退款流程（Refund）

```
1. 退款成功回调
2. 退款服务：
   - 更新退款单状态：PROCESSING → SUCCESS
   - 更新订单状态：PAID → REFUNDED
   - 同事务写入 Outbox 事件：order.refunded
3. OutboxPublisher：
   - 扫描待投递事件
   - 投递到 InProcessEventDispatcher
4. WalletConsumer：
   - 检查消费日志（幂等）
   - 调用 WalletAssetFacade.revert()
   - 记录消费成功
5. PointsConsumer：
   - 检查消费日志（幂等）
   - 调用 PointsAssetFacade.revertPoints()（回退已扣减）
   - 调用 PointsAssetFacade.revertPoints()（扣减已赚取）
   - 记录消费成功
```

---

## 异常场景处理

### 场景 1：Outbox 投递失败

**原因**：消费者抛异常（如资产余额不足、网络超时）

**处理**：
- OutboxPublisher 捕获异常，标记事件为 `FAILED`
- 计算下次重试时间（指数退避：2^retryCount 秒）
- 下次扫描时重新投递
- 超过最大重试次数（10 次）标记为 `DEAD`，告警通知人工介入

### 场景 2：消费者重复消费

**原因**：Outbox 重试、网络抖动导致重复投递

**处理**：
- 消费者检查 `bc_event_consume_log` 表
- 如果已消费（状态为 `SUCCESS`），直接返回
- 如果未消费，调用 Facade 层 API（传入 `idempotencyKey`）
- Facade 层检查幂等键，重复调用返回已有结果

### 场景 3：锁定超时未释放

**原因**：订单取消事件丢失、消费者异常

**处理**：
- `LockTimeoutReaperJob` 每 5 分钟扫描超时锁定
- 自动释放超过 30 分钟的锁定
- 记录释放日志，便于审计

### 场景 4：支付成功但事件丢失

**原因**：Outbox 写入失败、数据库异常

**处理**：
- `OrderAssetConsistencyJob` 每 2 小时扫描已支付订单
- 检查是否存在 `order.paid` 事件
- 如果缺失，补发事件到 Outbox
- 消费者幂等处理，不会重复扣减资产

### 场景 5：宕机重启

**原因**：服务器宕机、应用重启

**处理**：
- Outbox 事件持久化在数据库，不会丢失
- 重启后 OutboxPublisher 继续扫描待投递事件
- 消费者幂等处理，不会重复操作

---

## 验收标准

### 1. 宕机后重启

**测试步骤**：
1. 创建订单，写入 Outbox 事件（状态为 `NEW`）
2. 停止应用（模拟宕机）
3. 重启应用
4. 观察 OutboxPublisher 是否扫描并投递事件
5. 观察消费者是否正常处理

**预期结果**：
- Outbox 事件被投递并标记为 `SENT`
- 资产操作正常执行（优惠券锁定、钱包冻结、积分冻结）
- 日志中有完整的 traceId / requestId 链路

### 2. 支付回调重复触发

**测试步骤**：
1. 订单支付成功，写入 Outbox 事件
2. 模拟支付回调重复触发（重复写入 Outbox 事件）
3. 观察消费者是否重复扣减资产

**预期结果**：
- 第二次写入 Outbox 事件失败（`event_id` 唯一约束）
- 或者消费者检查消费日志，直接返回
- 资产只扣减一次，不会重复

### 3. 锁定超时自动释放

**测试步骤**：
1. 创建订单，锁定优惠券/冻结钱包/冻结积分
2. 等待超过 30 分钟（或修改配置为 1 分钟）
3. 观察 `LockTimeoutReaperJob` 是否自动释放

**预期结果**：
- 锁定状态从 `LOCKED` / `FROZEN` 更新为 `RELEASED` / `AVAILABLE`
- 账户可用余额/积分恢复
- 日志中有释放记录

### 4. 事件丢失自动补发

**测试步骤**：
1. 订单支付成功，但不写入 Outbox 事件（模拟丢失）
2. 等待 2 小时（或修改配置为 5 分钟）
3. 观察 `OrderAssetConsistencyJob` 是否补发事件

**预期结果**：
- 检测到缺失 `order.paid` 事件
- 补发事件到 Outbox
- 消费者幂等处理，资产正常扣减

### 5. 完整链路追踪

**测试步骤**：
1. 创建订单，记录 `orderId`
2. 查询日志，搜索 `orderId`
3. 观察是否有完整的链路日志

**预期结果**：
- 日志中包含：
  - 订单创建日志（traceId / requestId）
  - Outbox 写入日志（eventId / eventType）
  - OutboxPublisher 投递日志（eventId / traceId）
  - 消费者处理日志（consumer / eventId / traceId）
  - 资产操作日志（idempotencyKey / result）

---

## 配置项

```yaml
bluecone:
  # 资产锁定配置
  asset:
    lock-timeout-minutes: 30  # 锁定超时时间（分钟），默认 30 分钟
  
  # 一致性检查配置
  consistency:
    scan-window-hours: 24     # 扫描时间窗口（小时），默认 24 小时
  
  # Outbox 配置
  outbox:
    batch-size: 100           # 每批次处理的事件数量，默认 100
    max-retry-count: 10       # 最大重试次数，默认 10 次
    publisher-cron: "0/10 * * * * ?"  # 投递任务 cron 表达式，默认每 10 秒
  
  # 补偿任务配置
  scheduler:
    lock-timeout-reaper-cron: "0 */5 * * * ?"   # 锁定超时清理任务，默认每 5 分钟
    consistency-check-cron: "0 0 */2 * * ?"     # 一致性检查任务，默认每 2 小时
```

---

## 监控指标

### 1. Outbox 指标

- `outbox.event.created`：Outbox 事件创建数量
- `outbox.event.sent`：Outbox 事件投递成功数量
- `outbox.event.failed`：Outbox 事件投递失败数量
- `outbox.event.dead`：Outbox 事件死信数量（超过最大重试次数）
- `outbox.event.pending`：Outbox 待投递事件数量

### 2. 消费者指标

- `consumer.event.consumed`：消费者处理事件数量
- `consumer.event.idempotent`：消费者幂等返回数量
- `consumer.event.failed`：消费者处理失败数量

### 3. 补偿任务指标

- `reaper.lock.released`：锁定超时释放数量
- `consistency.event.missing`：缺失事件数量
- `consistency.event.repaired`：补发事件数量

---

## 常见问题

### Q1：Outbox 事件会不会堆积？

**A**：正常情况下不会。OutboxPublisher 每 10 秒扫描一次，批量投递 100 个事件。如果消费者处理速度跟不上，可以：
- 增加 OutboxPublisher 执行频率（如改为每 5 秒）
- 增加批量处理数量（如改为 200）
- 优化消费者性能（如异步处理、批量操作）

### Q2：消费者幂等会不会影响性能？

**A**：影响很小。幂等检查只需查询一次 `bc_event_consume_log` 表（有索引），耗时 < 10ms。相比资产操作（锁定、核销、扣减）的耗时（50-200ms），可以忽略不计。

### Q3：补偿任务会不会误释放？

**A**：不会。补偿任务只处理超时锁定（默认 30 分钟），正常订单在 15 分钟内完成支付，不会被误释放。如果订单超时未支付，释放锁定是符合预期的。

### Q4：如何排查事件丢失？

**A**：
1. 查询 `bc_outbox_event` 表，检查是否存在对应事件
2. 查询 `bc_event_consume_log` 表，检查消费者是否处理
3. 查询应用日志，搜索 `eventId` / `orderId`
4. 如果事件不存在，可能是 Outbox 写入失败（检查事务日志）
5. 如果事件存在但未消费，可能是消费者异常（检查错误日志）

### Q5：如何手动补发事件？

**A**：
1. 确认订单状态和资产状态
2. 手动插入 Outbox 事件：
   ```sql
   INSERT INTO bc_outbox_event (
       tenant_id, store_id, aggregate_type, aggregate_id, event_type, event_id,
       event_payload, event_metadata, status, retry_count, max_retry_count,
       next_retry_at, created_at, updated_at
   ) VALUES (
       1, 1, 'ORDER', '123', 'order.paid', UUID(),
       '{"orderId": 123, "couponId": 456}', '{"userId": 789}',
       'NEW', 0, 10, NOW(), NOW(), NOW()
   );
   ```
3. 等待 OutboxPublisher 自动投递
4. 消费者幂等处理，不会重复操作

---

## 总结

通过 Outbox + 消费者幂等 + 补偿任务，bluecone-app 实现了可恢复的事件驱动一致性机制：

1. **Outbox 模式**：保证业务数据与事件的原子性写入，避免消息丢失
2. **消费者幂等**：保证事件重复投递时，资产操作只生效一次
3. **补偿任务**：兜底机制，处理超时锁定和事件丢失

**核心优势**：
- ✅ 无需外部 MQ，进程内即可运行
- ✅ 支持宕机恢复，事件不丢失
- ✅ 支持重复投递，资产不重复扣减
- ✅ 支持超时释放，不需要人工救火
- ✅ 支持链路追踪，可通过 orderId 串起完整链路

**未来优化**：
- 替换为 Kafka / RocketMQ，提升吞吐量和可靠性
- 引入分布式锁，支持多实例并发投递
- 引入死信队列，自动告警和人工介入
- 引入监控大盘，实时展示 Outbox 和消费者指标
