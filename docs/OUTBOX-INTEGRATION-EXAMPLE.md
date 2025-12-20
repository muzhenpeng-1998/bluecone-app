# Outbox 集成示例

本文档展示如何在订单/支付服务中集成 Outbox 事件写入。

## 示例 1：订单结算锁定（Checkout）

在订单从 DRAFT 提交到 WAIT_PAY 时，同事务写入 Outbox 事件。

```java
@Service
@RequiredArgsConstructor
public class OrderCheckoutService {
    
    private final OrderRepository orderRepository;
    private final OutboxEventService outboxEventService;
    
    @Transactional(rollbackFor = Exception.class)
    public void checkoutOrder(Long orderId, Long userId) {
        // 1. 更新订单状态
        Order order = orderRepository.findById(orderId);
        order.markPendingPayment();  // DRAFT → WAIT_PAY
        orderRepository.save(order);
        
        // 2. 同事务写入 Outbox 事件
        Map<String, Object> payload = new HashMap<>();
        payload.put("orderId", orderId);
        payload.put("couponId", order.getCouponId());
        payload.put("walletAmount", order.getWalletAmount());
        payload.put("pointsUsed", order.getPointsUsed());
        
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("userId", userId);
        metadata.put("traceId", MDC.get("traceId"));
        metadata.put("requestId", MDC.get("requestId"));
        
        OutboxEvent event = OutboxEvent.forOrder(
            order.getTenantId(),
            order.getStoreId(),
            orderId,
            EventType.ORDER_CHECKOUT_LOCKED,
            payload,
            metadata
        );
        
        outboxEventService.save(event);
        
        log.info("Order checkout completed: orderId={}, eventId={}", orderId, event.getEventId());
    }
}
```

## 示例 2：订单支付成功（Pay）

在订单从 WAIT_PAY 流转到 PAID 时，同事务写入 Outbox 事件。

```java
@Service
@RequiredArgsConstructor
public class OrderPaymentService {
    
    private final OrderRepository orderRepository;
    private final OutboxEventService outboxEventService;
    
    @Transactional(rollbackFor = Exception.class)
    public void markOrderPaid(Long orderId, Long payOrderId, String payChannel, String payNo) {
        // 1. 更新订单状态
        Order order = orderRepository.findById(orderId);
        order.markPaid(payOrderId, payChannel, payNo, LocalDateTime.now());
        orderRepository.save(order);
        
        // 2. 同事务写入 Outbox 事件
        Map<String, Object> payload = new HashMap<>();
        payload.put("orderId", orderId);
        payload.put("couponId", order.getCouponId());
        payload.put("walletAmount", order.getWalletAmount());
        payload.put("pointsUsed", order.getPointsUsed());
        payload.put("pointsEarned", calculatePointsEarned(order));  // 计算赚取积分
        
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("userId", order.getUserId());
        metadata.put("payOrderId", payOrderId);
        metadata.put("payChannel", payChannel);
        metadata.put("payNo", payNo);
        metadata.put("traceId", MDC.get("traceId"));
        
        OutboxEvent event = OutboxEvent.forOrder(
            order.getTenantId(),
            order.getStoreId(),
            orderId,
            EventType.ORDER_PAID,
            payload,
            metadata
        );
        
        outboxEventService.save(event);
        
        log.info("Order paid: orderId={}, payOrderId={}, eventId={}", 
                orderId, payOrderId, event.getEventId());
    }
    
    private Integer calculatePointsEarned(Order order) {
        // 根据订单金额计算赚取积分（如：每消费 1 元赚 1 积分）
        return order.getPayableAmount().intValue();
    }
}
```

## 示例 3：订单取消（Cancel）

在订单流转到 CANCELED 时，同事务写入 Outbox 事件。

```java
@Service
@RequiredArgsConstructor
public class OrderCancelService {
    
    private final OrderRepository orderRepository;
    private final OutboxEventService outboxEventService;
    
    @Transactional(rollbackFor = Exception.class)
    public void cancelOrder(Long orderId, String reasonCode, String reasonDesc) {
        // 1. 更新订单状态
        Order order = orderRepository.findById(orderId);
        order.cancel(reasonCode, reasonDesc, LocalDateTime.now());
        orderRepository.save(order);
        
        // 2. 同事务写入 Outbox 事件
        Map<String, Object> payload = new HashMap<>();
        payload.put("orderId", orderId);
        payload.put("couponId", order.getCouponId());
        payload.put("walletAmount", order.getWalletAmount());
        payload.put("pointsUsed", order.getPointsUsed());
        payload.put("reasonCode", reasonCode);
        payload.put("reasonDesc", reasonDesc);
        
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("userId", order.getUserId());
        metadata.put("traceId", MDC.get("traceId"));
        
        OutboxEvent event = OutboxEvent.forOrder(
            order.getTenantId(),
            order.getStoreId(),
            orderId,
            EventType.ORDER_CANCELED,
            payload,
            metadata
        );
        
        outboxEventService.save(event);
        
        log.info("Order canceled: orderId={}, reasonCode={}, eventId={}", 
                orderId, reasonCode, event.getEventId());
    }
}
```

## 示例 4：订单退款成功（Refund）

在退款单状态流转到 SUCCESS 时，同事务写入 Outbox 事件。

```java
@Service
@RequiredArgsConstructor
public class RefundService {
    
    private final RefundOrderRepository refundOrderRepository;
    private final OrderRepository orderRepository;
    private final OutboxEventService outboxEventService;
    
    @Transactional(rollbackFor = Exception.class)
    public void markRefundSuccess(Long refundId, String refundNo) {
        // 1. 更新退款单状态
        RefundOrder refund = refundOrderRepository.findById(refundId);
        refund.markSuccess(refundNo, LocalDateTime.now());
        refundOrderRepository.save(refund);
        
        // 2. 更新订单状态
        Order order = orderRepository.findById(refund.getOrderId());
        order.markRefunded(refundId, LocalDateTime.now());
        orderRepository.save(order);
        
        // 3. 同事务写入 Outbox 事件
        Map<String, Object> payload = new HashMap<>();
        payload.put("orderId", refund.getOrderId());
        payload.put("refundId", refundId);
        payload.put("refundAmount", refund.getRefundAmount());
        payload.put("pointsUsed", order.getPointsUsed());
        payload.put("pointsEarned", order.getPointsEarned());
        
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("userId", order.getUserId());
        metadata.put("refundNo", refundNo);
        metadata.put("traceId", MDC.get("traceId"));
        
        OutboxEvent event = OutboxEvent.forRefund(
            refund.getTenantId(),
            refund.getStoreId(),
            refundId,
            EventType.ORDER_REFUNDED,
            payload,
            metadata
        );
        
        outboxEventService.save(event);
        
        log.info("Refund success: refundId={}, orderId={}, eventId={}", 
                refundId, refund.getOrderId(), event.getEventId());
    }
}
```

## 关键点

### 1. 事务一致性

- Outbox 写入必须与业务数据更新在同一事务中
- 使用 `@Transactional` 注解保证原子性
- 如果 Outbox 写入失败，整个事务回滚

### 2. 事件载荷设计

- `payload`：包含业务数据（orderId、couponId、walletAmount 等）
- `metadata`：包含上下文信息（userId、traceId、requestId 等）
- 保持载荷简洁，避免序列化大对象

### 3. 幂等键设计

- 消费者使用 `orderId` + 操作类型构建幂等键
- 如：`order:123:checkout`、`order:123:commit`、`order:123:release`
- 保证同一订单的同一操作只执行一次

### 4. 日志追踪

- 记录 `eventId`、`orderId`、`traceId`
- 便于排查问题和链路追踪
- 使用 MDC 传递 traceId

## 集成清单

在订单/支付服务中集成 Outbox，需要完成以下步骤：

- [ ] 引入 `OutboxEventService` 依赖
- [ ] 在关键状态变更处添加 Outbox 写入代码
- [ ] 确保 Outbox 写入与业务数据更新在同一事务
- [ ] 设计合理的事件载荷（payload + metadata）
- [ ] 添加日志记录（eventId + orderId + traceId）
- [ ] 编写单元测试，验证 Outbox 写入
- [ ] 编写集成测试，验证端到端流程

## 测试建议

### 单元测试

```java
@Test
void testCheckoutOrder_shouldWriteOutboxEvent() {
    // Given
    Long orderId = 123L;
    Long userId = 456L;
    
    // When
    orderCheckoutService.checkoutOrder(orderId, userId);
    
    // Then
    verify(outboxEventService).save(argThat(event -> 
        event.getEventType() == EventType.ORDER_CHECKOUT_LOCKED &&
        event.getAggregateId().equals(String.valueOf(orderId))
    ));
}
```

### 集成测试

```java
@Test
@Transactional
void testCheckoutOrder_shouldLockAssets() {
    // Given
    Long orderId = createTestOrder();
    Long userId = 123L;
    
    // When
    orderCheckoutService.checkoutOrder(orderId, userId);
    
    // 等待 Outbox Publisher 投递事件
    await().atMost(Duration.ofSeconds(30))
           .until(() -> isEventConsumed("CouponConsumer", orderId));
    
    // Then
    assertThat(couponLockRepository.findByOrderId(orderId))
           .isNotNull()
           .hasFieldOrPropertyWithValue("status", LockStatus.LOCKED);
}
```

## 常见问题

### Q1：Outbox 写入失败怎么办？

**A**：Outbox 写入失败会导致整个事务回滚，订单状态不会更新。需要排查：
- 数据库连接是否正常
- 表结构是否正确
- 事件载荷是否可序列化

### Q2：如何保证 Outbox 写入的性能？

**A**：Outbox 写入是简单的 INSERT 操作，性能影响很小（< 10ms）。如果性能敏感，可以：
- 使用批量写入（一次事务写入多个事件）
- 优化数据库索引
- 使用连接池

### Q3：Outbox 事件的载荷应该包含哪些信息？

**A**：
- **必须包含**：orderId、userId、tenantId、storeId
- **可选包含**：couponId、walletAmount、pointsUsed、pointsEarned
- **不建议包含**：订单完整对象（太大）、敏感信息（密码、支付密钥）

### Q4：如何测试 Outbox 集成？

**A**：
1. 单元测试：验证 Outbox 写入逻辑
2. 集成测试：验证端到端流程（订单 → Outbox → 消费者 → 资产）
3. 手动测试：查询 `bc_outbox_event` 和 `bc_event_consume_log` 表
4. 压测：模拟高并发场景，验证性能和稳定性
