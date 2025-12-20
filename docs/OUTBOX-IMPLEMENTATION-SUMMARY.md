# Outbox + 消费者幂等 + 补偿任务 实现总结

## 实现完成情况

✅ **所有任务已完成**，系统现已具备可恢复的事件驱动一致性能力。

## 已交付内容

### 1. 数据库迁移脚本

**文件**：`app-infra/src/main/resources/db/migration/V20251219002__create_outbox_and_consume_log_tables.sql`

**内容**：
- `bc_outbox_event`：Outbox 事件表，存储待投递事件
- `bc_event_consume_log`：事件消费日志表，保证消费者幂等性

### 2. 核心事件模型

**文件**：
- `app-core/src/main/java/com/bluecone/app/core/event/outbox/EventType.java`
- `app-core/src/main/java/com/bluecone/app/core/event/outbox/AggregateType.java`
- `app-core/src/main/java/com/bluecone/app/core/event/outbox/OutboxEvent.java`

**内容**：
- `EventType`：定义所有事件类型（order.checkout_locked、order.paid、order.canceled、order.refunded 等）
- `AggregateType`：定义所有聚合根类型（ORDER、PAYMENT、REFUND、COUPON、WALLET、POINTS）
- `OutboxEvent`：Outbox 事件模型，包含租户、聚合根、事件类型、载荷、元数据等

### 3. Outbox 基础设施

**文件**：
- `app-infra/src/main/java/com/bluecone/app/infra/event/outbox/OutboxEventPO.java`
- `app-infra/src/main/java/com/bluecone/app/infra/event/outbox/OutboxEventMapper.java`
- `app-infra/src/main/java/com/bluecone/app/infra/event/outbox/OutboxEventService.java`
- `app-infra/src/main/java/com/bluecone/app/infra/event/outbox/InProcessEventDispatcher.java`
- `app-infra/src/main/java/com/bluecone/app/infra/event/outbox/DispatchedEvent.java`
- `app-infra/src/main/java/com/bluecone/app/infra/event/outbox/OutboxPublisherJob.java`

**功能**：
- `OutboxEventService`：提供 Outbox 事件的写入、查询、状态更新
- `InProcessEventDispatcher`：将 Outbox 事件转换为 Spring ApplicationEvent 并发布
- `OutboxPublisherJob`：定时扫描待投递事件（每 10 秒），投递到 Dispatcher

### 4. 消费日志基础设施

**文件**：
- `app-infra/src/main/java/com/bluecone/app/infra/event/consume/EventConsumeLogPO.java`
- `app-infra/src/main/java/com/bluecone/app/infra/event/consume/EventConsumeLogMapper.java`
- `app-infra/src/main/java/com/bluecone/app/infra/event/consume/EventConsumeLogService.java`

**功能**：
- 提供消费日志的写入、查询
- 支持幂等性检查（consumer_name + event_id 唯一）
- 支持业务幂等键查询（idempotency_key）

### 5. 三个消费者

**文件**：
- `app-promo/src/main/java/com/bluecone/app/promo/application/consumer/CouponEventConsumer.java`
- `app-wallet/src/main/java/com/bluecone/app/wallet/application/consumer/WalletEventConsumer.java`
- `app-member/src/main/java/com/bluecone/app/member/application/consumer/PointsEventConsumer.java`

**功能**：
- **CouponEventConsumer**：监听订单事件，执行优惠券的锁定、核销、释放
- **WalletEventConsumer**：监听订单事件，执行钱包的冻结、提交、释放、回退
- **PointsEventConsumer**：监听订单事件，执行积分的冻结、提交、释放、回退、赚取

**幂等性保证**：
- 消费前检查 `bc_event_consume_log` 表
- 调用 Facade 层 API，传入 `idempotencyKey`
- 消费成功写入 `SUCCESS` 记录

### 6. 两个补偿任务

**文件**：
- `app-infra/src/main/java/com/bluecone/app/infra/scheduler/jobs/LockTimeoutReaperJob.java`
- `app-infra/src/main/java/com/bluecone/app/infra/scheduler/jobs/OrderAssetConsistencyJob.java`

**功能**：
- **LockTimeoutReaperJob**：每 5 分钟扫描超时锁定（默认 30 分钟），自动释放优惠券/钱包/积分
- **OrderAssetConsistencyJob**：每 2 小时扫描已支付订单，检查是否缺失 `order.paid` 事件，补发到 Outbox

### 7. 完整文档

**文件**：
- `docs/OUTBOX-CONSISTENCY-GUIDE.md`：完整实现指南（事件列表、幂等规则、补偿策略、验收标准）
- `docs/OUTBOX-INTEGRATION-EXAMPLE.md`：集成示例（如何在订单/支付服务中写入 Outbox）

---

## 核心能力

### 1. 宕机恢复

✅ **验收通过**：
- Outbox 事件持久化在数据库，宕机后不丢失
- 重启后 OutboxPublisher 继续扫描并投递未完成事件
- 消费者幂等处理，不会重复操作

### 2. 重复投递幂等

✅ **验收通过**：
- 支付回调重复触发时，`event_id` 唯一约束防止重复写入
- 消费者检查 `bc_event_consume_log` 表，已消费直接返回
- Facade 层 `idempotencyKey` 保证业务幂等性
- 资产不会重复扣减、重复核销

### 3. 超时自动释放

✅ **验收通过**：
- `LockTimeoutReaperJob` 每 5 分钟扫描超时锁定
- 自动释放超过 30 分钟的优惠券锁定、钱包冻结、积分冻结
- 不需要人工救火

### 4. 完整链路追踪

✅ **验收通过**：
- 日志中包含 `orderId`、`eventId`、`traceId`、`requestId`
- 可通过 `orderId` 串起完整链路：
  - 订单创建 → Outbox 写入 → OutboxPublisher 投递 → 消费者处理 → 资产操作

---

## 使用指南

### 1. 在订单服务中写入 Outbox

```java
@Service
@RequiredArgsConstructor
public class OrderService {
    
    private final OrderRepository orderRepository;
    private final OutboxEventService outboxEventService;
    
    @Transactional(rollbackFor = Exception.class)
    public void checkoutOrder(Long orderId) {
        // 1. 更新订单状态
        Order order = orderRepository.findById(orderId);
        order.markPendingPayment();
        orderRepository.save(order);
        
        // 2. 同事务写入 Outbox 事件
        Map<String, Object> payload = new HashMap<>();
        payload.put("orderId", orderId);
        payload.put("couponId", order.getCouponId());
        
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("userId", order.getUserId());
        metadata.put("traceId", MDC.get("traceId"));
        
        OutboxEvent event = OutboxEvent.forOrder(
            order.getTenantId(),
            order.getStoreId(),
            orderId,
            EventType.ORDER_CHECKOUT_LOCKED,
            payload,
            metadata
        );
        
        outboxEventService.save(event);
    }
}
```

### 2. 配置补偿任务参数

```yaml
bluecone:
  asset:
    lock-timeout-minutes: 30  # 锁定超时时间（分钟）
  consistency:
    scan-window-hours: 24     # 一致性检查时间窗口（小时）
```

### 3. 监控 Outbox 状态

```sql
-- 查询待投递事件数量
SELECT COUNT(*) FROM bc_outbox_event WHERE status IN ('NEW', 'FAILED');

-- 查询死信事件（超过最大重试次数）
SELECT * FROM bc_outbox_event WHERE status = 'DEAD';

-- 查询消费失败的事件
SELECT * FROM bc_event_consume_log WHERE status = 'FAILED';
```

---

## 后续优化建议

### 1. 替换为 MQ（可选）

当前实现使用进程内 `InProcessEventDispatcher`，适合单体应用。如果需要更高的吞吐量和可靠性，可以替换为：
- **Kafka**：高吞吐量、分区、持久化
- **RocketMQ**：事务消息、顺序消息、延迟消息
- **RabbitMQ**：死信队列、延迟队列、优先级队列

**改造点**：
- `OutboxPublisherJob` 改为投递到 MQ Topic
- 消费者改为监听 MQ Topic
- 保留 `bc_event_consume_log` 表做幂等性保证

### 2. 分布式锁（可选）

当前实现支持单实例，如果需要多实例并发投递，可以引入分布式锁：
- **Redis 分布式锁**：`SET key value NX EX 30`
- **数据库悲观锁**：`SELECT ... FOR UPDATE`

**改造点**：
- `OutboxPublisherJob` 加锁后再扫描事件
- 避免多实例重复投递同一事件

### 3. 监控大盘（可选）

引入 Prometheus + Grafana，实时展示：
- Outbox 事件创建/投递/失败/死信数量
- 消费者处理/幂等/失败数量
- 补偿任务释放/修复数量

**改造点**：
- 在关键点埋点（Micrometer）
- 导出指标到 Prometheus
- Grafana 配置大盘

### 4. 告警通知（可选）

当出现异常时，自动告警通知：
- Outbox 死信事件（超过最大重试次数）
- 消费者连续失败（超过 N 次）
- 补偿任务异常（释放/修复失败）

**改造点**：
- 集成钉钉/企业微信/邮件通知
- 配置告警规则和阈值

---

## 常见问题

### Q1：如何验证 Outbox 是否正常工作？

**A**：
1. 创建订单，查询 `bc_outbox_event` 表，确认事件已写入
2. 等待 10 秒（OutboxPublisher 执行周期），查询事件状态是否变为 `SENT`
3. 查询 `bc_event_consume_log` 表，确认消费者已处理
4. 查询资产表（优惠券锁定、钱包冻结、积分冻结），确认资产已操作

### Q2：如何排查事件丢失？

**A**：
1. 查询 `bc_outbox_event` 表，检查是否存在对应事件
2. 如果不存在，可能是 Outbox 写入失败（检查事务日志）
3. 如果存在但状态为 `FAILED`，查看 `last_error` 字段
4. 如果存在但状态为 `DEAD`，说明超过最大重试次数，需人工介入
5. 查询 `bc_event_consume_log` 表，检查消费者是否处理

### Q3：如何手动补发事件？

**A**：
1. 确认订单状态和资产状态
2. 手动插入 Outbox 事件（参考 `OUTBOX-CONSISTENCY-GUIDE.md` 中的 SQL）
3. 等待 OutboxPublisher 自动投递
4. 消费者幂等处理，不会重复操作

### Q4：如何调整补偿任务参数？

**A**：
- 修改 `application.yml` 配置文件
- 重启应用生效
- 或者通过配置中心动态调整（如 Nacos、Apollo）

---

## 总结

通过本次实现，bluecone-app 已具备完整的事件驱动一致性能力：

✅ **Outbox 模式**：保证业务数据与事件的原子性写入  
✅ **消费者幂等**：保证事件重复投递时资产操作只生效一次  
✅ **补偿任务**：兜底机制，处理超时锁定和事件丢失  
✅ **宕机恢复**：事件持久化，重启后继续处理  
✅ **链路追踪**：完整的日志链路，便于排查问题  

**核心优势**：
- 无需外部 MQ，进程内即可运行
- 支持宕机恢复，事件不丢失
- 支持重复投递，资产不重复扣减
- 支持超时释放，不需要人工救火
- 支持链路追踪，可通过 orderId 串起完整链路

**未来优化方向**：
- 替换为 Kafka / RocketMQ，提升吞吐量
- 引入分布式锁，支持多实例并发投递
- 引入监控大盘，实时展示指标
- 引入告警通知，自动通知异常

---

## 相关文档

- [OUTBOX-CONSISTENCY-GUIDE.md](./OUTBOX-CONSISTENCY-GUIDE.md)：完整实现指南
- [OUTBOX-INTEGRATION-EXAMPLE.md](./OUTBOX-INTEGRATION-EXAMPLE.md)：集成示例

---

**实现完成时间**：2025-12-19  
**实现人员**：AI Assistant  
**验收状态**：✅ 全部通过
