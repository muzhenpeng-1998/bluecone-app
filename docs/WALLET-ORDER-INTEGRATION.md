# 钱包账本与订单对接说明

## 概述

本文档描述 **app-wallet（储值钱包）** 与 **app-order（订单）** 的集成方案，包括状态机流转、幂等规则、账本化要求和并发控制策略。

## 目录

- [架构设计](#架构设计)
- [核心概念](#核心概念)
- [状态机与流转](#状态机与流转)
- [幂等性设计](#幂等性设计)
- [账本化要求](#账本化要求)
- [并发控制](#并发控制)
- [集成点说明](#集成点说明)
- [测试策略](#测试策略)

---

## 架构设计

### 模块依赖关系

```
app-order  ──depends──>  app-wallet-api
app-payment ──depends──>  app-wallet-api
app-wallet-api (接口定义，无实现)
app-wallet (实现模块，依赖 app-wallet-api)
```

**约束：**
- `app-order` 和 `app-payment` **只能依赖** `app-wallet-api`，不能依赖 `app-wallet` 实现模块
- 所有钱包操作通过 Facade 接口调用，保持模块隔离

### 核心组件

| 组件 | 职责 |
|------|------|
| `WalletQueryFacade` | 查询钱包余额、检查余额是否足够 |
| `WalletAssetFacade` | 冻结、提交、释放、回退余额操作 |
| `WalletDomainService` | 钱包领域服务，实现核心业务逻辑 |
| `WalletPaymentService` | 订单侧钱包支付服务，封装钱包支付流程 |

---

## 核心概念

### 钱包账户 (bc_wallet_account)

- **可用余额 (available_balance)**: 用户可以使用的余额
- **冻结余额 (frozen_balance)**: 下单时锁定的余额，等待支付完成或取消
- **总余额**: `available_balance + frozen_balance`
- **乐观锁 (version)**: 每次余额变更时版本号+1，保证并发安全

### 钱包冻结记录 (bc_wallet_freeze)

- **状态流转**: `FROZEN` → `COMMITTED` / `RELEASED` / `REVERTED`
- **业务类型**: `ORDER_CHECKOUT` (订单下单冻结)
- **幂等键 (idem_key)**: `{tenantId}:{userId}:{orderId}:freeze`
- **过期时间 (expires_at)**: 默认30分钟，超时自动释放

### 钱包账本流水 (bc_wallet_ledger)

- **业务类型**:
  - `RECHARGE`: 充值入账
  - `ORDER_PAY`: 订单支付出账（负数）
  - `REFUND`: 退款入账（正数）
  - `ADJUST`: 管理员调整
- **幂等键 (idem_key)**: `{tenantId}:{userId}:{bizType}:{bizOrderId}:{operationType}`
- **账本化要求**: 所有资金变更必须写入账本流水，记录变更前后余额

---

## 状态机与流转

### 订单-钱包联动状态机

```
┌─────────────────────────────────────────────────────────────────┐
│                        订单状态流转                              │
└─────────────────────────────────────────────────────────────────┘

用户下单（选择钱包支付）
    │
    ├──> [1] 冻结余额 (freeze)
    │    - 订单状态: INIT → WAIT_PAY
    │    - 钱包状态: available → frozen
    │    - 冻结记录: FROZEN
    │
    ├──> [2a] 立即支付 (commit)
    │    - 订单状态: WAIT_PAY → PAID → WAIT_ACCEPT
    │    - 钱包状态: frozen → 扣减
    │    - 冻结记录: FROZEN → COMMITTED
    │    - 账本流水: ORDER_PAY (出账)
    │
    └──> [2b] 取消订单 (release)
         - 订单状态: WAIT_PAY → CANCELED
         - 钱包状态: frozen → available
         - 冻结记录: FROZEN → RELEASED
         - 账本流水: 无（只是状态恢复）

商户接单后用户申请退款
    │
    └──> [3] 退款返还 (revert)
         - 订单状态: PAID/WAIT_ACCEPT → REFUNDED
         - 钱包状态: available 增加
         - 账本流水: REFUND (入账)
```

### 冻结记录状态机

```
FROZEN (已冻结)
  │
  ├──> COMMITTED (已提交) - 支付成功，余额已扣除
  ├──> RELEASED (已释放) - 取消订单，余额已恢复
  └──> REVERTED (已回退) - 退款返还（暂未使用）
```

---

## 幂等性设计

### 幂等键规则

| 操作 | 幂等键格式 | 唯一约束表 | 幂等行为 |
|------|-----------|-----------|---------|
| **freeze** | `{tenantId}:{userId}:{orderId}:freeze` | `bc_wallet_freeze.uk_tenant_idem_key` | 重复调用返回已冻结结果 |
| **commit** | `{tenantId}:{userId}:{orderId}:commit` | `bc_wallet_ledger.uk_tenant_idem_key` | 重复调用返回已提交结果 |
| **release** | `{tenantId}:{userId}:{orderId}:release` | 无（状态检查） | 重复调用不报错，直接返回 |
| **revert** | `{tenantId}:{userId}:{orderId}:refund` | `bc_wallet_ledger.uk_tenant_idem_key` | 重复调用返回已回退结果 |

### 幂等实现策略

1. **数据库唯一约束兜底**
   - 所有写操作都有唯一约束（`idem_key`）
   - 并发情况下，唯一约束冲突时重新查询返回

2. **业务层幂等检查**
   - 写入前先查询是否已存在
   - 已存在时直接返回（避免不必要的数据库操作）

3. **状态幂等**
   - 订单状态变更支持幂等（如已支付时重复支付直接返回）
   - 冻结记录状态变更支持幂等（如已提交时重复提交直接返回）

---

## 账本化要求

### 必须写账本的操作

| 操作 | 业务类型 | 金额符号 | 说明 |
|------|---------|---------|------|
| **充值** | `RECHARGE` | 正数 | 充值入账 |
| **订单支付** | `ORDER_PAY` | 负数 | 订单支付出账 |
| **退款** | `REFUND` | 正数 | 退款返还入账 |
| **管理员调整** | `ADJUST` | 正/负 | 人工调整（预留） |

### 不写账本的操作

| 操作 | 说明 |
|------|------|
| **释放冻结** | 只是状态恢复，非资金流水 |

### 账本流水字段说明

```sql
CREATE TABLE bc_wallet_ledger (
    id                  BIGINT          NOT NULL COMMENT '流水ID',
    ledger_no           VARCHAR(64)     NOT NULL COMMENT '流水号（PublicId格式：wl_xxx）',
    biz_type            VARCHAR(32)     NOT NULL COMMENT '业务类型：RECHARGE、ORDER_PAY、REFUND、ADJUST',
    biz_order_id        BIGINT          DEFAULT NULL COMMENT '关联业务单ID（订单ID、充值单ID等）',
    amount              DECIMAL(18,2)   NOT NULL COMMENT '变更金额（正数=入账，负数=出账）',
    balance_before      DECIMAL(18,2)   NOT NULL COMMENT '变更前可用余额',
    balance_after       DECIMAL(18,2)   NOT NULL COMMENT '变更后可用余额',
    idem_key            VARCHAR(128)    NOT NULL COMMENT '幂等键',
    ...
    UNIQUE KEY uk_tenant_idem_key (tenant_id, idem_key)
);
```

---

## 并发控制

### 账户并发更新

**策略：乐观锁 (version)**

```java
// 1. 查询账户（带版本号）
WalletAccount account = accountRepository.findByUserId(tenantId, userId);
int expectedVersion = account.getVersion();

// 2. 修改余额
account.freeze(amount);

// 3. 乐观锁更新（WHERE version = expectedVersion）
int updated = accountRepository.updateWithVersion(account);
if (updated == 0) {
    throw new BizException("账户余额变更冲突，请重试");
}
```

**为什么选择乐观锁：**
- 钱包余额变更频率较低（相比库存），冲突概率小
- 乐观锁性能优于悲观锁（无需加锁等待）
- 冲突时抛异常，前端重试即可

### 冻结记录并发控制

**策略：唯一约束 + 乐观锁**

```java
// 1. 幂等性检查（查询是否已存在）
WalletFreeze existingFreeze = freezeRepository.findByIdemKey(tenantId, idemKey);
if (existingFreeze != null) {
    return existingFreeze; // 幂等返回
}

// 2. 创建冻结记录（唯一约束兜底）
try {
    freezeRepository.insert(freeze);
} catch (DuplicateKeyException e) {
    // 并发情况下，唯一约束冲突，重新查询返回
    return freezeRepository.findByIdemKey(tenantId, idemKey);
}
```

### 账本流水并发控制

**策略：唯一约束 (idem_key)**

- 账本流水只写不改，无需乐观锁
- 通过唯一约束保证同一幂等键只写入一次
- 并发冲突时重新查询返回

---

## 集成点说明

### 1. 订单预览 (Precheck)

**接口**: `POST /api/order/user/orders/preview`

**集成点**: `UserOrderPreviewAppServiceImpl.preview()`

**钱包操作**:
```java
// 查询钱包余额
WalletBalanceDTO balance = walletQueryFacade.getBalance(tenantId, userId);

// 判断余额是否足够
boolean sufficient = balance.getAvailableBalance().compareTo(payableAmount) >= 0;

// 返回给前端
response.setWalletBalance(WalletBalanceInfo.builder()
    .availableBalance(balance.getAvailableBalance())
    .sufficient(sufficient)
    .build());
```

**前端展示**:
- 显示用户当前可用余额
- 显示余额是否足够支付本订单
- 用户可选择是否使用钱包余额支付

---

### 2. 订单下单 (Checkout)

**接口**: `POST /api/order/user/orders/submit`

**集成点**: `OrderConfirmAppServiceImpl.confirmOrder()`

**钱包操作**:
```java
// 如果用户选择钱包余额支付
if (Boolean.TRUE.equals(request.getUseWalletBalance())) {
    // 1. 冻结余额
    String idemKey = String.format("%d:%d:%d:freeze", tenantId, userId, orderId);
    WalletAssetCommand freezeCommand = new WalletAssetCommand(
        tenantId, userId, order.getPayableAmount(),
        "ORDER_CHECKOUT", orderId, idemKey
    );
    WalletAssetResult result = walletAssetFacade.freeze(freezeCommand);
    
    // 2. 立即完成钱包支付（提交冻结）
    walletPaymentService.payWithWallet(tenantId, userId, orderId);
}
```

**状态流转**:
- 订单状态: `INIT` → `WAIT_PAY` → `PAID` → `WAIT_ACCEPT`
- 钱包状态: `available` → `frozen` → 扣减
- 冻结记录: `FROZEN` → `COMMITTED`
- 账本流水: 写入 `ORDER_PAY` 出账记录

---

### 3. 订单取消 (Cancel)

**接口**: `POST /api/order/user/orders/{orderId}/cancel`

**集成点**: `OrderCancelAppServiceImpl.cancelOrder()`

**钱包操作**:
```java
// 如果订单未支付（WAIT_PAY），释放冻结余额
if (OrderStatus.WAIT_PAY.equals(order.getStatus())) {
    walletPaymentService.releaseWalletFreeze(tenantId, userId, orderId);
}
```

**状态流转**:
- 订单状态: `WAIT_PAY` → `CANCELED`
- 钱包状态: `frozen` → `available`
- 冻结记录: `FROZEN` → `RELEASED`
- 账本流水: 无（只是状态恢复）

---

### 4. 订单退款 (Refund)

**接口**: 自动触发（订单取消时）或手动申请退款

**集成点**: `RefundAppServiceImpl.applyRefund()`

**钱包操作**:
```java
// 如果是钱包支付订单，回退余额变更
if (isWalletPayment) {
    String idemKey = String.format("%d:%d:%d:refund", tenantId, userId, orderId);
    walletPaymentService.revertWalletPayment(
        tenantId, userId, orderId, refundAmount, orderNo
    );
}
```

**状态流转**:
- 订单状态: `PAID/WAIT_ACCEPT` → `REFUNDED`
- 钱包状态: `available` 增加
- 账本流水: 写入 `REFUND` 入账记录

---

## 测试策略

### 单元测试

#### 1. 幂等性测试

```java
@Test
void testFreezeIdempotency() {
    // 第一次冻结
    WalletAssetResult result1 = walletAssetFacade.freeze(command);
    assertThat(result1.isSuccess()).isTrue();
    assertThat(result1.isIdempotent()).isFalse();
    
    // 第二次冻结（相同幂等键）
    WalletAssetResult result2 = walletAssetFacade.freeze(command);
    assertThat(result2.isSuccess()).isTrue();
    assertThat(result2.isIdempotent()).isTrue(); // 幂等返回
    assertThat(result2.getFreezeNo()).isEqualTo(result1.getFreezeNo());
}
```

#### 2. 并发测试

```java
@Test
void testConcurrentFreeze() throws Exception {
    // 并发冻结同一账户余额
    int threadCount = 10;
    CountDownLatch latch = new CountDownLatch(threadCount);
    AtomicInteger successCount = new AtomicInteger(0);
    
    for (int i = 0; i < threadCount; i++) {
        executor.submit(() -> {
            try {
                WalletAssetCommand command = buildFreezeCommand(orderId + i);
                WalletAssetResult result = walletAssetFacade.freeze(command);
                if (result.isSuccess()) {
                    successCount.incrementAndGet();
                }
            } finally {
                latch.countDown();
            }
        });
    }
    
    latch.await();
    
    // 验证：只有足够余额的订单能冻结成功
    assertThat(successCount.get()).isLessThanOrEqualTo(expectedSuccessCount);
}
```

#### 3. 冻结超时补偿测试

```java
@Test
void testFreezeExpiration() {
    // 1. 冻结余额（设置过期时间）
    WalletFreeze freeze = createFreezeWithExpiry(orderId, 1); // 1分钟后过期
    
    // 2. 等待过期
    Thread.sleep(61000);
    
    // 3. 扫描并释放过期冻结记录
    walletFreezeCompensationService.releaseExpiredFreezes();
    
    // 4. 验证冻结记录已释放
    WalletFreeze updated = freezeRepository.findById(freeze.getId());
    assertThat(updated.getStatus()).isEqualTo(FreezeStatus.RELEASED);
}
```

### 集成测试

#### 1. 完整支付流程测试

```java
@Test
void testWalletPaymentFlow() {
    // 1. 充值（准备余额）
    rechargeWallet(userId, new BigDecimal("100.00"));
    
    // 2. 下单（冻结余额）
    ConfirmOrderResponse orderResp = submitOrder(useWalletBalance = true);
    assertThat(orderResp.getStatus()).isEqualTo("PAID");
    
    // 3. 验证余额变更
    WalletBalanceDTO balance = walletQueryFacade.getBalance(tenantId, userId);
    assertThat(balance.getAvailableBalance()).isEqualTo(new BigDecimal("50.00"));
    assertThat(balance.getFrozenBalance()).isEqualTo(BigDecimal.ZERO);
    
    // 4. 验证账本流水
    List<WalletLedger> ledgers = ledgerRepository.findByUserId(tenantId, userId);
    assertThat(ledgers).hasSize(2); // 充值 + 支付
    assertThat(ledgers.get(1).getBizType()).isEqualTo("ORDER_PAY");
}
```

#### 2. 取消订单流程测试

```java
@Test
void testCancelOrderWithWalletFreeze() {
    // 1. 下单（冻结余额）
    ConfirmOrderResponse orderResp = submitOrder(useWalletBalance = true);
    
    // 2. 取消订单（释放冻结）
    cancelOrder(orderResp.getOrderId());
    
    // 3. 验证余额恢复
    WalletBalanceDTO balance = walletQueryFacade.getBalance(tenantId, userId);
    assertThat(balance.getAvailableBalance()).isEqualTo(new BigDecimal("100.00"));
    assertThat(balance.getFrozenBalance()).isEqualTo(BigDecimal.ZERO);
    
    // 4. 验证冻结记录已释放
    WalletFreeze freeze = freezeRepository.findByBizOrderId(tenantId, "ORDER_CHECKOUT", orderId);
    assertThat(freeze.getStatus()).isEqualTo(FreezeStatus.RELEASED);
}
```

#### 3. 退款流程测试

```java
@Test
void testRefundWithWalletRevert() {
    // 1. 下单并支付（钱包支付）
    ConfirmOrderResponse orderResp = submitOrder(useWalletBalance = true);
    
    // 2. 申请退款
    applyRefund(orderResp.getOrderId(), refundAmount);
    
    // 3. 验证余额返还
    WalletBalanceDTO balance = walletQueryFacade.getBalance(tenantId, userId);
    assertThat(balance.getAvailableBalance()).isEqualTo(new BigDecimal("100.00"));
    
    // 4. 验证账本流水
    List<WalletLedger> ledgers = ledgerRepository.findByUserId(tenantId, userId);
    assertThat(ledgers).hasSize(3); // 充值 + 支付 + 退款
    assertThat(ledgers.get(2).getBizType()).isEqualTo("REFUND");
    assertThat(ledgers.get(2).getAmount()).isEqualTo(refundAmount); // 正数（入账）
}
```

---

## 补偿机制（预留）

### 冻结超时释放

**场景**: 用户下单后长时间未支付，冻结记录超时

**补偿策略**:
```java
@Scheduled(fixedDelay = 60000) // 每分钟执行一次
public void releaseExpiredFreezes() {
    List<WalletFreeze> expiredFreezes = freezeRepository.findExpiredFreezes(
        LocalDateTime.now(), 100
    );
    
    for (WalletFreeze freeze : expiredFreezes) {
        try {
            walletPaymentService.releaseWalletFreeze(
                freeze.getTenantId(), 
                freeze.getUserId(), 
                freeze.getBizOrderId()
            );
        } catch (Exception e) {
            log.error("释放超时冻结记录失败：freezeId={}", freeze.getId(), e);
        }
    }
}
```

---

## 常见问题 (FAQ)

### Q1: 为什么冻结和提交要分两步？

**A**: 
- **冻结 (freeze)**: 下单时锁定余额，防止用户重复使用
- **提交 (commit)**: 支付成功后实际扣减余额，写入账本流水
- **分离原因**: 
  1. 支持取消订单（释放冻结余额）
  2. 支持超时自动释放（防止余额长期冻结）
  3. 账本化要求（只有实际扣减时才写流水）

### Q2: 为什么释放操作不写账本流水？

**A**: 
- 释放操作只是状态恢复（`frozen` → `available`），非资金流水
- 账本流水只记录实际的资金变更（充值、支付、退款）
- 如果需要审计释放操作，可通过冻结记录表（`bc_wallet_freeze`）查询

### Q3: 如何处理并发冻结导致的超扣？

**A**: 
- 使用乐观锁（`version`）保证账户并发安全
- 冻结前检查可用余额是否足够
- 乐观锁冲突时抛异常，前端重试

### Q4: 如何保证幂等性？

**A**: 
- 数据库唯一约束兜底（`idem_key`）
- 业务层幂等检查（写入前先查询）
- 状态幂等（已完成的操作重复调用直接返回）

---

## 版本历史

| 版本 | 日期 | 作者 | 变更说明 |
|------|------|------|---------|
| v1.0 | 2025-12-19 | bluecone | 初版：完成钱包-订单集成 |

---

## 附录

### 数据库表结构

详见迁移脚本：`app-infra/src/main/resources/db/migration/V20251218007__create_wallet_tables.sql`

### API 接口文档

详见 Swagger 文档：`http://localhost:8080/swagger-ui.html`

### 相关文档

- [订单状态机文档](ORDER-STATUS-CONSOLIDATION-V1.md)
- [订单快速开始](ORDER-M0-QUICK-START.md)
- [支付实现总结](PAYMENT-M1-IMPLEMENTATION-SUMMARY.md)
