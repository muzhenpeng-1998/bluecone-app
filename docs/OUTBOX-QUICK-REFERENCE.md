# Outbox 快速参考卡

## 事件类型速查

| 事件类型 | 触发时机 | 资产操作 |
|---------|---------|---------|
| `order.checkout_locked` | 订单提交（DRAFT → WAIT_PAY） | 锁定优惠券、冻结钱包、冻结积分 |
| `order.paid` | 订单支付（WAIT_PAY → PAID） | 核销优惠券、扣减钱包、扣减积分、赚取积分 |
| `order.canceled` | 订单取消（→ CANCELED） | 释放优惠券、释放钱包、释放积分 |
| `order.refunded` | 退款成功（→ REFUNDED） | 回退钱包、回退积分 |

## 幂等键格式速查

| 操作 | 幂等键格式 | 示例 |
|-----|-----------|------|
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

## 代码模板

### 写入 Outbox 事件

```java
@Transactional(rollbackFor = Exception.class)
public void someBusinessMethod(Long orderId) {
    // 1. 更新业务数据
    Order order = orderRepository.findById(orderId);
    order.someStateChange();
    orderRepository.save(order);
    
    // 2. 同事务写入 Outbox
    Map<String, Object> payload = new HashMap<>();
    payload.put("orderId", orderId);
    // ... 其他业务数据
    
    Map<String, Object> metadata = new HashMap<>();
    metadata.put("userId", order.getUserId());
    metadata.put("traceId", MDC.get("traceId"));
    
    OutboxEvent event = OutboxEvent.forOrder(
        order.getTenantId(),
        order.getStoreId(),
        orderId,
        EventType.ORDER_XXX,  // 选择合适的事件类型
        payload,
        metadata
    );
    
    outboxEventService.save(event);
}
```

### 消费者监听事件

```java
@EventListener
@Transactional(rollbackFor = Exception.class)
public void onOrderEvent(DispatchedEvent event) {
    if (!"order.xxx".equals(event.getEventType())) {
        return;
    }
    
    // 1. 幂等性检查
    if (consumeLogService.isConsumed(CONSUMER_NAME, event.getEventId())) {
        log.info("Event already consumed: eventId={}", event.getEventId());
        return;
    }
    
    try {
        // 2. 解析载荷
        Long orderId = getLongField(event.getPayload().get("orderId"));
        
        // 3. 调用 Facade
        SomeCommand command = SomeCommand.builder()
            .idempotencyKey(buildIdempotencyKey(orderId, "action"))
            .build();
        
        someFacade.doSomething(command);
        
        // 4. 记录消费成功
        consumeLogService.recordSuccess(
            CONSUMER_NAME, 
            event.getEventId(), 
            event.getEventType(),
            event.getTenantId(),
            command.getIdempotencyKey(),
            "SUCCESS"
        );
        
    } catch (Exception e) {
        // 5. 记录消费失败
        consumeLogService.recordFailure(
            CONSUMER_NAME,
            event.getEventId(),
            event.getEventType(),
            event.getTenantId(),
            null,
            e.getMessage()
        );
        throw e;  // 让 Outbox 重试
    }
}
```

## 常用 SQL

### 查询待投递事件

```sql
SELECT * FROM bc_outbox_event 
WHERE status IN ('NEW', 'FAILED') 
ORDER BY created_at ASC 
LIMIT 10;
```

### 查询死信事件

```sql
SELECT * FROM bc_outbox_event 
WHERE status = 'DEAD' 
ORDER BY created_at DESC;
```

### 查询消费失败的事件

```sql
SELECT * FROM bc_event_consume_log 
WHERE status = 'FAILED' 
ORDER BY created_at DESC;
```

### 查询订单的所有事件

```sql
SELECT * FROM bc_outbox_event 
WHERE aggregate_type = 'ORDER' 
AND aggregate_id = '123' 
ORDER BY created_at ASC;
```

### 查询消费者处理记录

```sql
SELECT * FROM bc_event_consume_log 
WHERE consumer_name = 'CouponConsumer' 
AND event_id = 'xxx' 
LIMIT 1;
```

### 手动补发事件

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

## 配置项

```yaml
bluecone:
  asset:
    lock-timeout-minutes: 30      # 锁定超时时间（分钟）
  consistency:
    scan-window-hours: 24         # 一致性检查时间窗口（小时）
```

## 定时任务

| 任务 | 执行频率 | 功能 |
|-----|---------|------|
| `OutboxPublisherJob` | 每 10 秒 | 扫描并投递待投递事件 |
| `LockTimeoutReaperJob` | 每 5 分钟 | 释放超时锁定（优惠券/钱包/积分） |
| `OrderAssetConsistencyJob` | 每 2 小时 | 检查并补发缺失事件 |

## 故障排查

### 问题：事件未投递

**排查步骤**：
1. 查询 `bc_outbox_event` 表，确认事件是否存在
2. 检查事件状态（NEW/SENT/FAILED/DEAD）
3. 如果状态为 FAILED，查看 `last_error` 字段
4. 如果状态为 DEAD，说明超过最大重试次数，需人工介入

### 问题：资产重复扣减

**排查步骤**：
1. 查询 `bc_event_consume_log` 表，检查是否有多条消费记录
2. 检查消费者代码是否正确调用 `consumeLogService.isConsumed()`
3. 检查 Facade 层是否正确使用 `idempotencyKey`

### 问题：锁定未释放

**排查步骤**：
1. 查询锁定表（`bc_coupon_lock`/`bc_wallet_freeze`/`bc_points_freeze`）
2. 检查锁定创建时间是否超过 30 分钟
3. 检查 `LockTimeoutReaperJob` 是否正常执行
4. 手动执行释放 SQL（参考 `LockTimeoutReaperJob` 代码）

## 监控指标

### 关键指标

- Outbox 待投递事件数量：`SELECT COUNT(*) FROM bc_outbox_event WHERE status IN ('NEW', 'FAILED')`
- Outbox 死信事件数量：`SELECT COUNT(*) FROM bc_outbox_event WHERE status = 'DEAD'`
- 消费失败数量：`SELECT COUNT(*) FROM bc_event_consume_log WHERE status = 'FAILED'`

### 告警阈值建议

- Outbox 待投递事件数量 > 1000：告警
- Outbox 死信事件数量 > 0：告警
- 消费失败数量 > 10：告警

## 最佳实践

1. **事务一致性**：Outbox 写入必须与业务数据更新在同一事务
2. **载荷设计**：保持载荷简洁，避免序列化大对象
3. **幂等键设计**：使用 `{bizType}:{bizId}:{action}` 格式
4. **日志追踪**：记录 `eventId`、`orderId`、`traceId`
5. **错误处理**：消费失败时抛异常，让 Outbox 重试
6. **监控告警**：定期检查 Outbox 和消费日志表

## 相关文档

- [OUTBOX-CONSISTENCY-GUIDE.md](./OUTBOX-CONSISTENCY-GUIDE.md)：完整实现指南
- [OUTBOX-INTEGRATION-EXAMPLE.md](./OUTBOX-INTEGRATION-EXAMPLE.md)：集成示例
- [OUTBOX-IMPLEMENTATION-SUMMARY.md](./OUTBOX-IMPLEMENTATION-SUMMARY.md)：实现总结
