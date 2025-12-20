# OrderStatus 状态收口 V1 - 快速参考指南

## 🎯 核心概念

### Canonical 状态（推荐使用）
```java
// 待支付
OrderStatus.WAIT_PAY

// 待接单
OrderStatus.WAIT_ACCEPT

// 已接单
OrderStatus.ACCEPTED

// 制作中/服务中
OrderStatus.IN_PROGRESS

// 已出餐/待取货
OrderStatus.READY

// 已完成
OrderStatus.COMPLETED

// 已取消
OrderStatus.CANCELED

// 已退款
OrderStatus.REFUNDED

// 已关闭
OrderStatus.CLOSED
```

### 废弃状态（仅兼容，不推荐使用）
- ❌ `PENDING_PAYMENT` → 使用 `WAIT_PAY`
- ❌ `PENDING_ACCEPT` → 使用 `WAIT_ACCEPT`
- ❌ `CANCELLED` → 使用 `CANCELED`

### 草稿态（仅购物车，不应落订单主表）
- ⚠️ `INIT` - 初始化态
- ⚠️ `DRAFT` - 草稿态
- ⚠️ `LOCKED_FOR_CHECKOUT` - 草稿锁定态
- ⚠️ `PENDING_CONFIRM` - 待确认态

## 📖 常用方法速查

### 1. 状态归一化
```java
// 将任何状态归一化为 Canonical 状态
OrderStatus canonical = status.normalize();

// 例如
OrderStatus.PENDING_PAYMENT.normalize();  // 返回 WAIT_PAY
OrderStatus.CANCELLED.normalize();        // 返回 CANCELED
OrderStatus.WAIT_PAY.normalize();         // 返回 WAIT_PAY（自身）
```

### 2. 从 code 查找状态
```java
// ❌ 不推荐：原样返回，可能返回非 Canonical 状态
OrderStatus status = OrderStatus.fromCode("PENDING_PAYMENT");  // 返回 PENDING_PAYMENT

// ✅ 推荐：自动归一化，保证返回 Canonical 状态
OrderStatus status = OrderStatus.fromCodeNormalized("PENDING_PAYMENT");  // 返回 WAIT_PAY
```

### 3. 业务判断
```java
// 判断是否为待支付（自动兼容 PENDING_PAYMENT）
if (status.isPayPending()) {
    // 待支付逻辑
}

// 判断是否为待接单（自动兼容 PENDING_ACCEPT）
if (status.isAcceptPending()) {
    // 待接单逻辑
}

// 判断是否可接单（自动兼容 PENDING_ACCEPT）
if (status.canAccept()) {
    // 允许接单
}

// 判断是否可取消
if (status.canCancel()) {
    // 允许取消
}

// 判断是否为终态（COMPLETED/CANCELED/REFUNDED/CLOSED）
if (status.isTerminal()) {
    // 终态逻辑
}
```

## 💡 最佳实践

### ✅ DO（推荐做法）

#### 1. 新代码使用 Canonical 状态
```java
// ✅ 正确
order.setStatus(OrderStatus.WAIT_PAY);

// ❌ 错误
order.setStatus(OrderStatus.PENDING_PAYMENT);
```

#### 2. 业务判断使用专用方法
```java
// ✅ 正确：自动兼容 PENDING_ACCEPT
if (order.getStatus().canAccept()) {
    merchantService.accept(order);
}

// ❌ 错误：遗漏 PENDING_ACCEPT
if (order.getStatus() == OrderStatus.WAIT_ACCEPT) {
    merchantService.accept(order);
}
```

#### 3. 读取旧数据使用归一化
```java
// ✅ 正确：自动归一化
OrderStatus status = OrderStatus.fromCodeNormalized(dbRecord.getStatus());

// ❌ 错误：可能返回非 Canonical
OrderStatus status = OrderStatus.fromCode(dbRecord.getStatus());
```

#### 4. 状态比较前先归一化
```java
// ✅ 正确：兼容 CANCELLED
if (status.normalize() == OrderStatus.CANCELED) {
    // 已取消逻辑
}

// ❌ 错误：遗漏 CANCELLED
if (status == OrderStatus.CANCELED) {
    // 已取消逻辑
}
```

### ❌ DON'T（禁止做法）

#### 1. 不要写入草稿态到订单主表
```java
// ❌ 错误：DRAFT 不应落订单主表
orderRepository.save(order.toBuilder()
    .status(OrderStatus.DRAFT)
    .build());

// ✅ 正确：使用 WAIT_PAY
orderRepository.save(order.toBuilder()
    .status(OrderStatus.WAIT_PAY)
    .build());
```

#### 2. 不要硬编码状态判断
```java
// ❌ 错误：遗漏 PENDING_PAYMENT
if (status == OrderStatus.WAIT_PAY) {
    // 待支付逻辑
}

// ✅ 正确：使用专用方法
if (status.isPayPending()) {
    // 待支付逻辑
}
```

#### 3. 不要在多个地方重复写状态判断
```java
// ❌ 错误：重复判断逻辑
if (status == OrderStatus.COMPLETED || 
    status == OrderStatus.CANCELED || 
    status == OrderStatus.REFUNDED || 
    status == OrderStatus.CLOSED) {
    // 终态逻辑
}

// ✅ 正确：使用 isTerminal
if (status.isTerminal()) {
    // 终态逻辑
}
```

## 🚨 常见错误场景

### 场景 1：接单失败
```java
// 问题：旧代码写入了 PENDING_ACCEPT，新代码只判断 WAIT_ACCEPT
Order order = loadOrder();  // status = PENDING_ACCEPT
if (order.getStatus() == OrderStatus.WAIT_ACCEPT) {
    // ❌ 不会执行，导致无法接单
}

// 解决方案：使用 canAccept()
if (order.getStatus().canAccept()) {
    // ✅ 正确执行，自动兼容 PENDING_ACCEPT
}
```

### 场景 2：取消失败
```java
// 问题：判断逻辑遗漏 PENDING_PAYMENT
if (order.getStatus() == OrderStatus.WAIT_PAY) {
    order.cancel();  // ❌ PENDING_PAYMENT 状态无法取消
}

// 解决方案：使用 canCancel()
if (order.getStatus().canCancel()) {
    order.cancel();  // ✅ 自动兼容 PENDING_PAYMENT
}
```

### 场景 3：终态判断遗漏
```java
// 问题：忘记判断 CANCELLED
if (order.getStatus() == OrderStatus.CANCELED || 
    order.getStatus() == OrderStatus.COMPLETED) {
    // ❌ 遗漏 CANCELLED，导致已取消订单被重复处理
}

// 解决方案：使用 isTerminal()
if (order.getStatus().isTerminal()) {
    // ✅ 自动兼容 CANCELLED/CANCELED/COMPLETED/REFUNDED/CLOSED
}
```

## 📋 映射规则表

| 旧状态 | 新状态（Canonical） | 说明 |
|--------|---------------------|------|
| PENDING_PAYMENT | WAIT_PAY | 重复语义 |
| PENDING_ACCEPT | WAIT_ACCEPT | 重复语义 |
| CANCELLED | CANCELED | 重复语义 |
| INIT | WAIT_PAY | 初始化态 |
| DRAFT | WAIT_PAY | 草稿态 |
| LOCKED_FOR_CHECKOUT | WAIT_PAY | 草稿锁定态 |
| PENDING_CONFIRM | WAIT_PAY | 待确认态 |
| PAID | WAIT_ACCEPT | 瞬时态 |
| WAIT_PAY | WAIT_PAY | 自身 |
| WAIT_ACCEPT | WAIT_ACCEPT | 自身 |
| ACCEPTED | ACCEPTED | 自身 |
| IN_PROGRESS | IN_PROGRESS | 自身 |
| READY | READY | 自身 |
| COMPLETED | COMPLETED | 自身 |
| CANCELED | CANCELED | 自身 |
| REFUNDED | REFUNDED | 自身 |
| CLOSED | CLOSED | 自身 |

## 🔧 常用代码片段

### 1. 订单创建
```java
Order order = Order.builder()
    .status(OrderStatus.WAIT_PAY)  // ✅ 使用 Canonical
    .payStatus(PayStatus.UNPAID)
    .build();
```

### 2. 支付成功
```java
// 在 Order 模型中
public void markPaid() {
    this.payStatus = PayStatus.PAID;
    OrderStatus canonical = this.status != null ? this.status.normalize() : null;
    if (canonical != OrderStatus.CANCELED 
            && canonical != OrderStatus.COMPLETED 
            && canonical != OrderStatus.REFUNDED) {
        this.status = OrderStatus.WAIT_ACCEPT;  // ✅ 使用 Canonical
    }
}
```

### 3. 商户接单
```java
public void acceptOrder(Order order) {
    if (!order.getStatus().canAccept()) {  // ✅ 使用 canAccept
        throw new BizException("ORDER_STATUS_NOT_ALLOW_ACCEPT");
    }
    order.setStatus(OrderStatus.ACCEPTED);  // ✅ 使用 Canonical
}
```

### 4. 用户取消
```java
public void cancelOrder(Order order) {
    if (!order.getStatus().canCancel()) {  // ✅ 使用 canCancel
        throw new BizException("ORDER_STATUS_NOT_ALLOW_CANCEL");
    }
    order.setStatus(OrderStatus.CANCELED);  // ✅ 使用 Canonical
}
```

### 5. 查询可操作订单
```java
// 查询待处理订单（待支付 + 待接单）
List<Order> pendingOrders = orderRepository.findAll().stream()
    .filter(o -> o.getStatus().isPayPending() || o.getStatus().isAcceptPending())
    .collect(Collectors.toList());

// 查询可取消订单
List<Order> cancelableOrders = orderRepository.findAll().stream()
    .filter(o -> o.getStatus().canCancel())
    .collect(Collectors.toList());

// 排除终态订单
List<Order> activeOrders = orderRepository.findAll().stream()
    .filter(o -> !o.getStatus().isTerminal())
    .collect(Collectors.toList());
```

## 📞 FAQ

### Q1: 为什么不直接删除 PENDING_PAYMENT/PENDING_ACCEPT/CANCELLED？
**A**: 为了向后兼容，避免破坏现有代码和数据。通过 `normalize()` 和专用方法，可以平滑迁移。

### Q2: 什么时候使用 fromCode，什么时候使用 fromCodeNormalized？
**A**: 
- 数据持久化层（Repository）读取时使用 `fromCodeNormalized`
- 仅用于显示或日志记录时可以使用 `fromCode`
- 任何业务判断前必须使用 `fromCodeNormalized` 或手动调用 `normalize()`

### Q3: 草稿态可以写入订单主表吗？
**A**: 不推荐。草稿态应该：
- 存储在单独的草稿表中，或
- 使用 Redis 等缓存，或
- 确认提交后立即转为 WAIT_PAY

### Q4: 如何处理线上已有的 PENDING_PAYMENT 数据？
**A**: 
- 读取时使用 `fromCodeNormalized` 自动归一化
- 可以通过数据迁移脚本批量更新为 WAIT_PAY
- 或在业务代码中使用 `normalize()` 兼容

### Q5: 状态机配置是否已更新？
**A**: 是的，`OrderStateMachineImpl` 已全面更新：
- 所有转换规则使用 Canonical 状态
- 保留草稿态配置（仅用于购物车流程）
- 新增兼容旧状态的转换规则

## 📚 相关文档

- [详细实施总结](./ORDER-STATUS-CONSOLIDATION-V1.md)
- [测试用例说明](./app-order/src/test/java/com/bluecone/app/order/domain/enums/OrderStatusNormalizeTest.java)
- [状态机配置](./app-order/src/main/java/com/bluecone/app/order/domain/service/impl/OrderStateMachineImpl.java)

---

**版本**：V1.0  
**更新时间**：2025-12-18
