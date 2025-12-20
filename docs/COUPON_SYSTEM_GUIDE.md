# 优惠券系统设计与订单集成指南

## 概述

本文档描述 BlueCone 优惠券系统（app-promo）的核心设计、状态机模型以及与订单系统（app-order）的集成方案。

## 1. 系统架构

### 1.1 模块划分

```
bluecone-app/
├── app-promo-api/          # 优惠券API契约（对外暴露）
│   ├── dto/                # 数据传输对象
│   ├── enums/              # 枚举定义
│   └── facade/             # 门面接口
└── app-promo/              # 优惠券实现模块
    ├── domain/             # 领域模型
    │   ├── model/          # 聚合根和实体
    │   ├── repository/     # 仓储接口
    │   └── service/        # 领域服务
    ├── application/        # 应用服务
    └── infra/              # 基础设施
        └── persistence/    # 持久化实现
```

### 1.2 核心实体

#### 优惠券模板（CouponTemplate）
- **职责**：定义优惠券的规则和属性
- **生命周期**：由运营人员创建和管理
- **关键属性**：
  - 券类型（满减券/折扣券）
  - 优惠金额/折扣率
  - 最低门槛金额
  - 适用范围（全场/指定门店/指定商品/指定分类）
  - 有效期规则

#### 优惠券实例（Coupon）
- **职责**：用户持有的具体优惠券
- **生命周期**：从发放到使用或过期
- **关键属性**：
  - 券码（唯一）
  - 持有用户
  - 当前状态
  - 有效期

#### 优惠券锁定记录（CouponLock）
- **职责**：记录优惠券的锁定状态
- **生命周期**：从锁定到释放或提交
- **关键属性**：
  - 幂等键（保证操作幂等）
  - 锁定状态
  - 关联订单
  - 过期时间

#### 优惠券核销记录（CouponRedemption）
- **职责**：记录优惠券的使用详情
- **生命周期**：订单支付成功时创建
- **关键属性**：
  - 幂等键
  - 订单金额明细
  - 实际优惠金额

## 2. 状态机设计

### 2.1 优惠券状态（CouponStatus）

```
           ┌─────────┐
    发放   │ ISSUED  │  已发放（可使用）
    ───>   │（已发放）│
           └─────────┘
                │
                │ lock()
                ▼
           ┌─────────┐
           │ LOCKED  │  已锁定（等待支付）
           │（已锁定）│
           └─────────┘
          ╱     │     ╲
  release()   commit() 超时
         ╱      │       ╲
        ▼       ▼        ▼
  ┌─────────┐ ┌─────┐ ┌────────┐
  │ ISSUED  │ │USED │ │EXPIRED │
  │（已发放）│ │（已 │ │（已过期│
  └─────────┘ │使用）│ └────────┘
              └─────┘
```

#### 状态说明

| 状态 | 描述 | 可执行操作 |
|------|------|-----------|
| ISSUED | 已发放，可以使用 | 锁定（订单下单） |
| LOCKED | 已锁定，等待支付 | 释放（订单取消）、核销（支付成功） |
| USED | 已使用，不可再用 | 无 |
| EXPIRED | 已过期，不可使用 | 无 |

### 2.2 锁定记录状态（LockStatus）

```
      lock()        release()
LOCKED ────────> RELEASED
  │
  │ commit()
  ▼
COMMITTED
```

#### 状态说明

| 状态 | 描述 | 触发时机 |
|------|------|----------|
| LOCKED | 已锁定 | 订单提交时 |
| RELEASED | 已释放 | 订单取消/超时 |
| COMMITTED | 已提交（核销） | 订单支付成功 |

## 3. 幂等性设计

### 3.1 幂等键规则

所有写操作（锁定/释放/核销）使用相同的幂等键确保操作幂等：

```
幂等键格式：ORDER:{orderId}:COUPON:{couponId}
示例：ORDER:1001:COUPON:5001
```

### 3.2 幂等实现机制

#### 数据库层面
- `bc_coupon_lock` 表的 `idempotency_key` 字段有唯一约束
- `bc_coupon_redemption` 表的 `idempotency_key` 字段有唯一约束

#### 应用层面
1. **锁定操作（lock）**：
   - 先查询锁定记录，如已存在则返回已有结果
   - 插入锁定记录（唯一约束兜底）
   - 更新券状态（带状态校验，防止并发）

2. **释放操作（release）**：
   - 查询锁定记录，如不存在或已释放/已提交，直接返回（幂等）
   - 更新锁定记录状态
   - 更新券状态

3. **核销操作（commit）**：
   - 先查询核销记录，如已存在则直接返回
   - 插入核销记录（唯一约束兜底）
   - 更新锁定记录状态
   - 更新券状态

### 3.3 并发安全

#### 券状态更新
使用 CAS（Compare-And-Set）模式更新券状态：

```sql
UPDATE bc_coupon 
SET status = 'LOCKED', order_id = ?, lock_time = ?
WHERE id = ? AND status = 'ISSUED'
```

#### 好处
- 只有状态为 ISSUED 的券才能被锁定
- 并发请求中只有一个能成功
- 数据库层面保证原子性

## 4. 订单集成

### 4.1 集成流程

#### Precheck（订单预校验）
```
用户 -> OrderController.confirm()
    -> OrderConfirmApplicationService
        -> OrderCouponPricingService.calculatePricing()
            -> CouponQueryFacade.listUsableCoupons()  # 查询可用券
            -> CouponQueryFacade.bestCoupon()         # 推荐最优券
    <- 返回定价报价（含可用券列表和最优券建议）
```

#### Checkout（订单提交）
```
用户 -> OrderController.submit()
    -> OrderSubmitApplicationService.submit()
        -> [可选] OrderCouponIntegrationService.lockCoupon()
            -> CouponLockFacade.lock()                # 锁定优惠券
        -> 创建订单（WAIT_PAY）
    <- 返回订单ID和待支付金额
```

#### Cancel（订单取消）
```
用户/系统 -> OrderCancelAppService.cancelOrder()
    -> Order.cancel()
    -> [如果有券] OrderCouponIntegrationService.releaseCoupon()
        -> CouponLockFacade.release()                 # 释放优惠券
    -> 更新订单状态（CANCELED）
```

#### Payment Success（支付成功）
```
支付回调 -> OrderPaymentStatusAppService.handlePaymentSuccess()
    -> Order.markAsPaid()
    -> [如果有券] OrderCouponIntegrationService.commitCoupon()
        -> CouponLockFacade.commit()                  # 核销优惠券
    -> 更新订单状态（WAIT_ACCEPT）
```

### 4.2 核心服务

#### OrderCouponPricingService
- **职责**：计算订单价格，查询可用优惠券
- **输入**：PricingContext（订单信息）
- **输出**：PricingQuote（包含原价、优惠金额、应付金额、可用券列表）

#### OrderCouponIntegrationService
- **职责**：处理优惠券在订单生命周期中的操作
- **方法**：
  - `lockCoupon()` - 订单提交时锁定券
  - `releaseCoupon()` - 订单取消时释放券
  - `commitCoupon()` - 支付成功时核销券

### 4.3 数据模型扩展

#### Order 领域模型
新增字段：
```java
private Long couponId;  // 关联的优惠券ID（可选）
```

#### OrderPO 持久化对象
新增字段：
```java
private Long couponId;  // 对应 bc_order.coupon_id
```

## 5. 优惠券可用性校验

### 5.1 校验规则

CouponValidationService 负责校验优惠券是否可用：

1. **状态校验**：券状态必须为 ISSUED
2. **有效期校验**：当前时间在有效期内
3. **门槛校验**：订单金额满足最低门槛
4. **适用范围校验**：
   - ALL：全场通用，无需校验
   - STORE：校验门店ID
   - SKU：校验订单中是否包含指定商品
   - CATEGORY：校验订单中是否包含指定分类商品

### 5.2 不可用原因

当优惠券不可用时，返回具体原因：
- "优惠券状态不可用，当前状态：{status}"
- "优惠券尚未生效"
- "优惠券已过期"
- "订单金额未达到使用门槛（满XX元可用）"
- "该优惠券不适用于当前门店"
- "该优惠券不适用于订单中的商品"

## 6. 数据库设计

### 6.1 核心表结构

#### bc_coupon_template（优惠券模板表）
```sql
CREATE TABLE bc_coupon_template (
    id BIGINT PRIMARY KEY,
    tenant_id BIGINT NOT NULL,
    template_code VARCHAR(64) NOT NULL,
    template_name VARCHAR(128) NOT NULL,
    coupon_type VARCHAR(32) NOT NULL,
    discount_amount DECIMAL(10, 2),
    discount_rate DECIMAL(5, 2),
    min_order_amount DECIMAL(10, 2) DEFAULT 0,
    max_discount_amount DECIMAL(10, 2),
    applicable_scope VARCHAR(32) NOT NULL,
    applicable_scope_ids TEXT,
    valid_start_time DATETIME,
    valid_end_time DATETIME,
    status VARCHAR(32) DEFAULT 'ACTIVE',
    INDEX idx_tenant_code (tenant_id, template_code)
);
```

#### bc_coupon（优惠券实例表）
```sql
CREATE TABLE bc_coupon (
    id BIGINT PRIMARY KEY,
    tenant_id BIGINT NOT NULL,
    template_id BIGINT NOT NULL,
    coupon_code VARCHAR(64) NOT NULL,
    user_id BIGINT NOT NULL,
    status VARCHAR(32) DEFAULT 'ISSUED',
    order_id BIGINT,
    UNIQUE KEY uk_coupon_code (coupon_code),
    INDEX idx_tenant_user (tenant_id, user_id),
    INDEX idx_status (status)
);
```

#### bc_coupon_lock（优惠券锁定记录表）
```sql
CREATE TABLE bc_coupon_lock (
    id BIGINT PRIMARY KEY,
    coupon_id BIGINT NOT NULL,
    order_id BIGINT NOT NULL,
    idempotency_key VARCHAR(128) NOT NULL,
    lock_status VARCHAR(32) DEFAULT 'LOCKED',
    expire_time DATETIME NOT NULL,
    UNIQUE KEY uk_idempotency (idempotency_key),
    INDEX idx_order (order_id)
);
```

#### bc_coupon_redemption（优惠券核销记录表）
```sql
CREATE TABLE bc_coupon_redemption (
    id BIGINT PRIMARY KEY,
    coupon_id BIGINT NOT NULL,
    order_id BIGINT NOT NULL,
    idempotency_key VARCHAR(128) NOT NULL,
    original_amount DECIMAL(10, 2) NOT NULL,
    discount_amount DECIMAL(10, 2) NOT NULL,
    final_amount DECIMAL(10, 2) NOT NULL,
    UNIQUE KEY uk_idempotency (idempotency_key),
    INDEX idx_order (order_id)
);
```

### 6.2 订单表扩展

#### bc_order（订单表）
新增字段：
```sql
ALTER TABLE bc_order ADD COLUMN coupon_id BIGINT DEFAULT NULL COMMENT '关联优惠券ID';
CREATE INDEX idx_coupon ON bc_order(coupon_id);
```

## 7. API 契约

### 7.1 CouponQueryFacade

#### listUsableCoupons
查询用户可用优惠券列表

**输入**：CouponQueryContext
```java
{
  "tenantId": 1,
  "userId": 1001,
  "storeId": 2001,
  "orderAmount": 100.00,
  "skuIds": [3001, 3002],
  "categoryIds": [4001]
}
```

**输出**：List<UsableCouponDTO>
```java
[
  {
    "couponId": 5001,
    "couponCode": "COUPON001",
    "couponType": "DISCOUNT_AMOUNT",
    "discountAmount": 10.00,
    "minOrderAmount": 50.00,
    "usable": true,
    "estimatedDiscount": 10.00,
    "description": "满50减10"
  },
  {
    "couponId": 5002,
    "usable": false,
    "unusableReason": "订单金额未达到使用门槛（满200元可用）"
  }
]
```

#### bestCoupon
查询最优优惠券（优惠金额最大）

**输入**：CouponQueryContext
**输出**：UsableCouponDTO（最优券，无可用券返回null）

### 7.2 CouponLockFacade

#### lock
锁定优惠券

**输入**：CouponLockCommand
```java
{
  "tenantId": 1,
  "userId": 1001,
  "couponId": 5001,
  "orderId": 6001,
  "orderAmount": 100.00,
  "idempotencyKey": "ORDER:6001:COUPON:5001",
  "lockExpireMinutes": 30
}
```

**输出**：CouponLockResult
```java
{
  "success": true,
  "message": "锁定成功",
  "lockId": 7001,
  "couponId": 5001,
  "discountAmount": 10.00,
  "lockTime": "2025-12-18T10:00:00",
  "expireTime": "2025-12-18T10:30:00"
}
```

#### release
释放优惠券

**输入**：CouponReleaseCommand
```java
{
  "tenantId": 1,
  "userId": 1001,
  "couponId": 5001,
  "orderId": 6001,
  "idempotencyKey": "ORDER:6001:COUPON:5001"
}
```

**输出**：void（幂等，重复调用不报错）

#### commit
核销优惠券

**输入**：CouponCommitCommand
```java
{
  "tenantId": 1,
  "userId": 1001,
  "couponId": 5001,
  "orderId": 6001,
  "originalAmount": 100.00,
  "discountAmount": 10.00,
  "finalAmount": 90.00,
  "idempotencyKey": "ORDER:6001:COUPON:5001"
}
```

**输出**：void（幂等，重复调用不报错）

## 8. 测试建议

### 8.1 幂等性测试

#### 测试场景
1. 同一幂等键多次调用 `lock()`，应返回相同结果
2. 同一幂等键多次调用 `commit()`，应不重复核销
3. 锁定失败后再次调用，应返回失败原因

#### 测试代码示例
```java
@Test
void testLockIdempotency() {
    CouponLockCommand command = buildLockCommand();
    
    // 第一次锁定
    CouponLockResult result1 = couponLockFacade.lock(command);
    assertThat(result1.getSuccess()).isTrue();
    
    // 第二次锁定（相同幂等键）
    CouponLockResult result2 = couponLockFacade.lock(command);
    assertThat(result2.getSuccess()).isTrue();
    assertThat(result2.getLockId()).isEqualTo(result1.getLockId());
}
```

### 8.2 并发锁券测试

#### 测试场景
同一优惠券ID并发 `lock()` 操作，只有一个请求能成功

#### 测试代码示例
```java
@Test
void testConcurrentLock() throws Exception {
    Long couponId = 5001L;
    int threadCount = 10;
    CountDownLatch latch = new CountDownLatch(threadCount);
    AtomicInteger successCount = new AtomicInteger(0);
    
    ExecutorService executor = Executors.newFixedThreadPool(threadCount);
    
    for (int i = 0; i < threadCount; i++) {
        final int orderId = 6000 + i;
        executor.submit(() -> {
            try {
                CouponLockCommand command = buildLockCommand(couponId, orderId);
                CouponLockResult result = couponLockFacade.lock(command);
                if (result.getSuccess()) {
                    successCount.incrementAndGet();
                }
            } finally {
                latch.countDown();
            }
        });
    }
    
    latch.await();
    executor.shutdown();
    
    // 只有一个请求成功
    assertThat(successCount.get()).isEqualTo(1);
}
```

### 8.3 券可用性测试

#### 测试场景
1. 过期券应返回不可用
2. 未满足门槛的券应返回不可用及原因
3. 不适用范围的券应返回不可用及原因

## 9. 运维建议

### 9.1 监控指标

- 优惠券锁定成功率
- 优惠券核销成功率
- 锁定超时释放率
- 幂等重试次数

### 9.2 告警策略

- 锁定失败率 > 5%
- 核销失败率 > 1%
- 幂等键冲突次数异常增长

### 9.3 定期清理

- 清理已过期的优惠券（状态更新为 EXPIRED）
- 清理超时未释放的锁定记录
- 归档历史核销记录

## 10. 未来扩展

### 10.1 功能扩展
- [ ] 支持券叠加使用
- [ ] 支持积分兑换券
- [ ] 支持券转赠功能
- [ ] 支持券包（多张券组合）

### 10.2 性能优化
- [ ] 券查询缓存（Redis）
- [ ] 券锁定状态缓存
- [ ] 批量券发放优化

### 10.3 运营工具
- [ ] 券模板管理后台
- [ ] 券发放规则配置
- [ ] 券使用数据分析

---

**文档版本**：v1.0  
**最后更新**：2025-12-18  
**维护者**：BlueCone Tech Team
