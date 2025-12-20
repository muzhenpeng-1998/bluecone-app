# 优惠券系统快速开始指南

## 概述

本文档提供优惠券系统的快速上手指南，帮助开发者快速理解和使用优惠券功能。

## 系统组成

```
app-promo-api（契约）          app-promo（实现）
       │                            │
       │                            │
       ▼                            ▼
┌──────────────┐          ┌──────────────┐
│ CouponQuery  │          │   领域模型   │
│   Facade     │◄─────────┤   仓储实现   │
└──────────────┘          │   应用服务   │
                          └──────────────┘
┌──────────────┐
│ CouponLock   │
│   Facade     │
└──────────────┘
```

## 核心概念

### 优惠券状态

```
ISSUED（已发放）──lock()──> LOCKED（已锁定）──commit()──> USED（已使用）
      ↓                          │
   EXPIRED                   release()
   （已过期）                     ↓
                            ISSUED（已发放）
```

### 幂等键规则

所有操作使用相同的幂等键：`ORDER:{orderId}:COUPON:{couponId}`

## 快速使用

### 1. 查询可用优惠券

```java
@Autowired
private CouponQueryFacade couponQueryFacade;

public List<UsableCouponDTO> getAvailableCoupons(
        Long tenantId, Long userId, Long storeId, BigDecimal orderAmount) {
    
    CouponQueryContext context = new CouponQueryContext();
    context.setTenantId(tenantId);
    context.setUserId(userId);
    context.setStoreId(storeId);
    context.setOrderAmount(orderAmount);
    
    return couponQueryFacade.listUsableCoupons(context);
}
```

### 2. 获取最优优惠券

```java
public UsableCouponDTO getBestCoupon(
        Long tenantId, Long userId, Long storeId, BigDecimal orderAmount) {
    
    CouponQueryContext context = new CouponQueryContext();
    context.setTenantId(tenantId);
    context.setUserId(userId);
    context.setStoreId(storeId);
    context.setOrderAmount(orderAmount);
    
    return couponQueryFacade.bestCoupon(context);
}
```

### 3. 锁定优惠券（订单提交时）

```java
@Autowired
private OrderCouponIntegrationService orderCouponService;

public void submitOrder(Order order) {
    // 如果订单使用了优惠券
    if (order.getCouponId() != null) {
        CouponLockResult result = orderCouponService.lockCoupon(
            order.getTenantId(),
            order.getUserId(),
            order.getCouponId(),
            order.getId(),
            order.getTotalAmount()
        );
        
        if (result != null && result.getSuccess()) {
            // 锁定成功，更新订单优惠金额
            order.setDiscountAmount(result.getDiscountAmount());
            order.setPayableAmount(order.getTotalAmount().subtract(result.getDiscountAmount()));
        } else {
            // 锁定失败，记录日志或抛出异常
            log.warn("券锁定失败: {}", result != null ? result.getMessage() : "unknown");
        }
    }
    
    // 保存订单
    orderRepository.save(order);
}
```

### 4. 释放优惠券（订单取消时）

```java
public void cancelOrder(Order order) {
    // 如果订单使用了优惠券
    if (order.getCouponId() != null) {
        orderCouponService.releaseCoupon(
            order.getTenantId(),
            order.getUserId(),
            order.getCouponId(),
            order.getId()
        );
    }
    
    // 更新订单状态
    order.cancel("USER_CANCEL");
    orderRepository.update(order);
}
```

### 5. 核销优惠券（支付成功时）

```java
public void onPaymentSuccess(Order order) {
    // 如果订单使用了优惠券
    if (order.getCouponId() != null) {
        orderCouponService.commitCoupon(
            order.getTenantId(),
            order.getUserId(),
            order.getCouponId(),
            order.getId(),
            order.getTotalAmount(),
            order.getDiscountAmount(),
            order.getPayableAmount()
        );
    }
    
    // 更新订单状态
    order.markAsPaid();
    orderRepository.update(order);
}
```

## 订单集成示例

### 完整的订单提交流程

```java
@Service
@RequiredArgsConstructor
public class OrderSubmitService {
    
    private final OrderCouponPricingService pricingService;
    private final OrderCouponIntegrationService couponService;
    private final OrderRepository orderRepository;
    private final IdService idService;
    
    @Transactional
    public OrderSubmitResponse submit(OrderSubmitRequest request) {
        // 1. 计算定价（含优惠券）
        PricingContext pricingContext = buildPricingContext(request);
        PricingQuote quote = pricingService.calculatePricing(pricingContext);
        
        // 2. 创建订单
        Long orderId = idService.nextLong(IdScope.ORDER);
        Order order = Order.builder()
                .id(orderId)
                .tenantId(request.getTenantId())
                .userId(request.getUserId())
                .storeId(request.getStoreId())
                .couponId(request.getCouponId())
                .totalAmount(quote.getOriginalAmount())
                .discountAmount(quote.getDiscountAmount())
                .payableAmount(quote.getPayableAmount())
                .status(OrderStatus.WAIT_PAY)
                .build();
        
        // 3. 锁定优惠券
        if (request.getCouponId() != null) {
            CouponLockResult lockResult = couponService.lockCoupon(
                order.getTenantId(),
                order.getUserId(),
                order.getCouponId(),
                order.getId(),
                order.getTotalAmount()
            );
            
            if (lockResult == null || !lockResult.getSuccess()) {
                throw new BizException("优惠券锁定失败");
            }
        }
        
        // 4. 保存订单
        orderRepository.save(order);
        
        // 5. 返回结果
        return OrderSubmitResponse.builder()
                .orderId(order.getId())
                .payableAmount(order.getPayableAmount())
                .build();
    }
}
```

## 数据准备

### 创建优惠券模板

```sql
INSERT INTO bc_coupon_template (
    id, tenant_id, template_code, template_name, 
    coupon_type, discount_amount, min_order_amount, 
    applicable_scope, status, 
    valid_start_time, valid_end_time,
    created_at, updated_at
) VALUES (
    1, 1, 'WELCOME10', '新人专享券',
    'DISCOUNT_AMOUNT', 10.00, 50.00,
    'ALL', 'ACTIVE',
    '2025-01-01 00:00:00', '2025-12-31 23:59:59',
    NOW(), NOW()
);
```

### 发放优惠券

```sql
INSERT INTO bc_coupon (
    id, tenant_id, template_id, coupon_code, user_id,
    coupon_type, discount_amount, min_order_amount,
    applicable_scope, status,
    valid_start_time, valid_end_time,
    grant_time, created_at, updated_at
) VALUES (
    5001, 1, 1, 'COUPON001', 1001,
    'DISCOUNT_AMOUNT', 10.00, 50.00,
    'ALL', 'ISSUED',
    '2025-01-01 00:00:00', '2025-12-31 23:59:59',
    NOW(), NOW(), NOW()
);
```

## 常见问题

### Q1: 如何判断优惠券是否可用？

调用 `CouponQueryFacade.listUsableCoupons()`，查看返回的 `UsableCouponDTO.usable` 字段。
如果不可用，`unusableReason` 字段会说明原因。

### Q2: 券锁定失败怎么办？

检查返回的 `CouponLockResult.message`，常见原因：
- 券状态不是 ISSUED
- 券已过期
- 订单金额不满足门槛
- 券已被其他订单锁定

### Q3: 如何保证幂等性？

系统自动处理幂等性，使用相同的 `idempotencyKey` 重复调用会返回相同结果。
幂等键由 `OrderCouponIntegrationService` 自动生成。

### Q4: 并发锁券会有问题吗？

不会。系统使用 CAS（Compare-And-Set）模式更新券状态，只有一个请求能成功。

### Q5: 订单取消后券会自动释放吗？

需要在订单取消逻辑中显式调用 `orderCouponService.releaseCoupon()`。

## 监控指标

建议监控以下指标：

```java
// 券锁定成功率
lock_success_rate = lock_success / lock_total

// 券核销成功率  
commit_success_rate = commit_success / commit_total

// 券释放次数
release_count

// 幂等重试次数
idempotency_retry_count
```

## 告警配置

建议配置以下告警：

- 锁定失败率 > 5%
- 核销失败率 > 1%
- 幂等键冲突异常增长

## 下一步

1. 阅读完整文档：`docs/COUPON_SYSTEM_GUIDE.md`
2. 查看实施总结：`docs/COUPON_IMPLEMENTATION_SUMMARY.md`
3. 运行单元测试了解更多细节
4. 根据业务需求扩展功能

## 技术支持

如有问题，请查看：
- 系统设计文档
- 代码注释
- 单元测试用例

---

**文档版本**：v1.0  
**最后更新**：2025-12-18
