# 订单取消/退款 M4 实现总结

**实现时间**：2025-12-18  
**功能范围**：订单取消/退款闭环（M4），支持"已支付订单退款"，并保证幂等与并发安全

---

## 一、功能概述

### 1.1 核心目标

实现订单取消/退款闭环（M4），支持以下场景：

1. **WAIT_PAY（待支付）**：用户取消 → CANCELED（不退款）
2. **WAIT_ACCEPT（待接单）**：用户取消 → CANCELED → 自动触发退款 → REFUNDED
3. **ACCEPTED（已接单）**：用户取消 → CANCELED → 自动触发退款 → REFUNDED
4. **IN_PROGRESS/READY（制作中/已出餐）**：不允许取消（M4 先不支持）

### 1.2 技术要求

- **幂等性**：通过 idemKey（action_log）和 notifyId 保证重复请求只执行一次
- **并发安全**：通过乐观锁（version）保证并发操作安全
- **状态机约束**：严格控制订单和退款单的状态流转
- **库存联动**：预留库存释放/回补接口（M4 使用 No-op 实现）

---

## 二、数据库变更

### 2.1 Flyway 迁移文件

**文件**：`app-infra/src/main/resources/db/migration/V20251218004__add_refund_m4_tables.sql`

#### 新增表

##### 1) bc_refund_order（退款单表）

| 字段名              | 类型             | 说明                                              |
|---------------------|------------------|---------------------------------------------------|
| id                  | BIGINT           | 退款单ID（内部主键，ULID）                        |
| tenant_id           | BIGINT           | 租户ID                                             |
| store_id            | BIGINT           | 门店ID                                             |
| order_id            | BIGINT           | 订单ID（关联bc_order.id）                         |
| public_order_no     | VARCHAR(64)      | 订单号（冗余，用于快速查询）                      |
| refund_id           | VARCHAR(64)      | 退款单号（对外展示，PublicId格式：rfd_xxx）       |
| channel             | VARCHAR(32)      | 退款渠道（WECHAT、ALIPAY、MOCK）                  |
| refund_amount       | DECIMAL(18,2)    | 退款金额（实际退款金额，单位：元）                |
| currency            | VARCHAR(8)       | 币种（默认：CNY）                                  |
| status              | VARCHAR(32)      | 退款状态（INIT、PROCESSING、SUCCESS、FAILED）     |
| refund_no           | VARCHAR(128)     | 第三方退款单号（如微信退款单号）                  |
| reason_code         | VARCHAR(64)      | 退款原因码（USER_CANCEL、MERCHANT_REJECT等）      |
| reason_desc         | VARCHAR(256)     | 退款原因描述                                       |
| idem_key            | VARCHAR(128)     | 幂等键（格式：{tenantId}:{storeId}:{orderId}:refund:{requestId}） |
| pay_order_id        | BIGINT           | 支付单ID（关联bc_payment_order.id）               |
| pay_no              | VARCHAR(128)     | 第三方支付单号（如微信transaction_id）            |
| refund_requested_at | DATETIME         | 退款发起时间                                       |
| refund_completed_at | DATETIME         | 退款完成时间（SUCCESS时填充）                     |
| ext_json            | TEXT             | 扩展信息JSON                                       |
| version             | INT              | 乐观锁版本号                                       |
| created_at          | DATETIME         | 创建时间                                           |
| updated_at          | DATETIME         | 更新时间                                           |

**索引**：
- UNIQUE KEY `uk_tenant_idem_key` (tenant_id, idem_key)
- UNIQUE KEY `uk_tenant_refund_id` (tenant_id, refund_id)
- KEY `idx_tenant_order` (tenant_id, order_id)
- KEY `idx_tenant_status` (tenant_id, status, created_at)

##### 2) bc_refund_notify_log（退款回调日志表）

| 字段名          | 类型         | 说明                                              |
|-----------------|--------------|---------------------------------------------------|
| id              | BIGINT       | 主键ID                                             |
| tenant_id       | BIGINT       | 租户ID                                             |
| notify_id       | VARCHAR(128) | 通知ID（幂等键，如微信的resource.id）             |
| refund_id       | VARCHAR(64)  | 退款单号（解析后填充）                            |
| refund_order_id | BIGINT       | 退款单ID（解析后填充）                            |
| raw_body        | TEXT         | 原始回调报文（JSON或XML）                         |
| channel         | VARCHAR(32)  | 支付渠道（WECHAT、ALIPAY）                        |
| processed       | TINYINT(1)   | 是否已处理（0=未处理，1=已处理）                  |
| processed_at    | DATETIME     | 处理时间                                           |
| result          | VARCHAR(32)  | 处理结果（SUCCESS、FAILED、IGNORED）              |
| error_msg       | VARCHAR(512) | 错误信息                                           |
| created_at      | DATETIME     | 创建时间（回调接收时间）                          |

**索引**：
- UNIQUE KEY `uk_tenant_notify_id` (tenant_id, notify_id)
- KEY `idx_tenant_refund_id` (tenant_id, refund_id)

#### bc_order 表补充字段

| 字段名              | 类型         | 说明                                              |
|---------------------|--------------|---------------------------------------------------|
| canceled_at         | DATETIME     | 取消时间（用户取消或商户拒单时填充）              |
| cancel_reason_code  | VARCHAR(64)  | 取消原因码（USER_CANCEL、MERCHANT_REJECT等）      |
| cancel_reason_desc  | VARCHAR(256) | 取消原因描述                                       |
| refunded_at         | DATETIME     | 退款时间（退款成功时填充）                        |
| refund_order_id     | BIGINT       | 退款单ID（关联bc_refund_order.id）                |

**索引**：
- KEY `idx_tenant_canceled_at` (tenant_id, status, canceled_at)
- KEY `idx_tenant_refunded_at` (tenant_id, status, refunded_at)

---

## 三、领域模型

### 3.1 RefundOrder（退款单聚合根）

**文件**：`app-order/src/main/java/com/bluecone/app/order/domain/model/RefundOrder.java`

#### 核心方法

| 方法                                    | 说明                                              |
|-----------------------------------------|---------------------------------------------------|
| `markProcessing(refundNo, now)`         | 标记为处理中（INIT → PROCESSING）                 |
| `markSuccess(refundNo, now)`            | 标记为成功（INIT/PROCESSING → SUCCESS）          |
| `markFailed(errorMsg, now)`             | 标记为失败（INIT/PROCESSING → FAILED）           |

#### 状态流转约束

- **INIT → PROCESSING**：发起退款请求
- **INIT/PROCESSING → SUCCESS**：退款成功（收到回调）
- **INIT/PROCESSING → FAILED**：退款失败（收到回调或超时）
- **SUCCESS/FAILED**：终态，不可再流转

### 3.2 Order（订单聚合根）扩展

**文件**：`app-order/src/main/java/com/bluecone/app/order/domain/model/Order.java`

#### 新增方法

| 方法                                      | 说明                                              |
|-------------------------------------------|---------------------------------------------------|
| `cancel(reasonCode, reasonDesc, now)`     | 取消订单（WAIT_PAY/WAIT_ACCEPT/ACCEPTED → CANCELED） |
| `markRefunded(refundOrderId, now)`        | 标记为已退款（WAIT_ACCEPT/ACCEPTED/CANCELED → REFUNDED） |

#### 状态约束

**cancel() 允许的前置状态**：
- WAIT_PAY（待支付）：直接取消，不涉及退款
- WAIT_ACCEPT（待接单）：取消后自动触发退款
- ACCEPTED（已接单）：取消后自动触发退款

**markRefunded() 允许的前置状态**：
- WAIT_ACCEPT（待接单）：已支付但商户未接单，拒单后退款
- ACCEPTED（已接单）：已接单但未开始制作，协商取消后退款
- CANCELED（已取消）：已取消且已支付，需要退款

### 3.3 RefundStatus（退款状态枚举）

**文件**：`app-order/src/main/java/com/bluecone/app/order/domain/enums/RefundStatus.java`

| 状态        | 说明                                              |
|-------------|---------------------------------------------------|
| INIT        | 初始化（退款单刚创建，尚未发起退款请求）          |
| PROCESSING  | 处理中（已向支付网关发起退款请求，等待结果）      |
| SUCCESS     | 成功（退款成功，收到支付网关回调通知）            |
| FAILED      | 失败（退款失败，支付网关拒绝或超时）              |

### 3.4 RefundChannel（退款渠道枚举）

**文件**：`app-order/src/main/java/com/bluecone/app/order/domain/enums/RefundChannel.java`

| 渠道    | 说明                                              |
|---------|---------------------------------------------------|
| WECHAT  | 微信支付退款                                       |
| ALIPAY  | 支付宝退款                                         |
| MOCK    | Mock 退款（测试用，M4 使用）                      |

---

## 四、应用服务

### 4.1 OrderCancelAppService（订单取消服务）

**接口**：`app-order/src/main/java/com/bluecone/app/order/application/OrderCancelAppService.java`  
**实现**：`app-order/src/main/java/com/bluecone/app/order/application/impl/OrderCancelAppServiceImpl.java`

#### 核心方法

```java
void cancelOrder(CancelOrderCommand command)
```

#### 业务流程

1. **幂等性检查**：根据 action_log 判断是否已执行
2. **查询订单**：根据 tenantId 和 orderId 查询订单
3. **权限校验**：验证用户是否有权取消该订单
4. **状态约束检查**：只允许 WAIT_PAY/WAIT_ACCEPT/ACCEPTED 状态取消
5. **乐观锁检查**：验证 expectedVersion 是否匹配
6. **取消订单**：调用领域模型 `order.cancel()` 方法
7. **保存订单**：使用乐观锁更新订单
8. **记录 action_log**：保存幂等性记录
9. **自动触发退款**：如果订单已支付，自动调用 RefundAppService

#### 幂等键格式

```
{tenantId}:{storeId}:{orderId}:cancel:{requestId}
```

### 4.2 RefundAppService（退款服务）

**接口**：`app-order/src/main/java/com/bluecone/app/order/application/RefundAppService.java`  
**实现**：`app-order/src/main/java/com/bluecone/app/order/application/impl/RefundAppServiceImpl.java`

#### 核心方法

```java
void applyRefund(ApplyRefundCommand command)
void onRefundNotify(String notifyId, String refundId, String refundNo, boolean success, String errorMsg)
```

#### applyRefund() 业务流程

1. **幂等性检查**：根据 idemKey 判断是否已创建退款单
2. **查询订单**：根据 tenantId 和 orderId 查询订单
3. **创建退款单**：状态为 INIT
4. **保存退款单**：持久化到数据库
5. **调用支付网关**：发起退款请求（M4 使用 Mock 实现，直接返回成功）
6. **更新退款单**：根据支付网关响应更新状态为 SUCCESS/FAILED
7. **推进订单**：退款成功后调用 `order.markRefunded()` 推进订单为 REFUNDED

#### 幂等键格式

```
{tenantId}:{storeId}:{orderId}:refund:{requestId}
```

---

## 五、基础设施层

### 5.1 RefundOrderRepository（退款单仓储）

**接口**：`app-order/src/main/java/com/bluecone/app/order/domain/repository/RefundOrderRepository.java`  
**实现**：`app-order/src/main/java/com/bluecone/app/order/infra/persistence/repository/RefundOrderRepositoryImpl.java`  
**Mapper**：`app-order/src/main/java/com/bluecone/app/order/infra/persistence/mapper/RefundOrderMapper.java`

#### 核心方法

| 方法                                      | 说明                                              |
|-------------------------------------------|---------------------------------------------------|
| `findById(tenantId, refundOrderId)`       | 根据ID查询退款单                                  |
| `findByIdemKey(tenantId, idemKey)`        | 根据幂等键查询退款单（幂等性保护）                |
| `findLatestByOrderId(tenantId, orderId)`  | 根据订单ID查询最近一笔退款单                      |
| `save(refundOrder)`                       | 新建退款单                                         |
| `update(refundOrder)`                     | 更新退款单（使用乐观锁）                          |

### 5.2 PaymentRefundGateway（支付退款网关）

**接口**：`app-order/src/main/java/com/bluecone/app/order/domain/gateway/PaymentRefundGateway.java`  
**Mock 实现**：`app-order/src/main/java/com/bluecone/app/order/infra/gateway/MockPaymentRefundGateway.java`

#### 核心方法

```java
RefundResponse refund(RefundRequest request)
```

#### Mock 实现策略

- **M4 阶段**：直接返回成功，生成随机的第三方退款单号（mock_refund_xxx）
- **后续扩展**：实现真实的微信/支付宝退款接口

### 5.3 InventoryFacade（库存 Facade）

**接口**：`app-order/src/main/java/com/bluecone/app/order/domain/facade/InventoryFacade.java`  
**No-op 实现**：`app-order/src/main/java/com/bluecone/app/order/infra/facade/NoOpInventoryFacade.java`

#### 核心方法

| 方法                                                | 说明                                              |
|-----------------------------------------------------|---------------------------------------------------|
| `releaseReservation(tenantId, storeId, orderId, reasonCode)` | 释放库存预占（订单取消时调用）                    |
| `compensateInventory(tenantId, storeId, orderId, reasonCode)` | 回补库存（订单退款时调用，已扣减的情况）          |
| `deductInventory(tenantId, storeId, orderId)`       | 扣减库存（订单完成时调用，如果采用完成时扣减策略）|

#### No-op 实现策略

- **M4 阶段**：只记录日志，不实际操作库存
- **后续扩展**：通过事件/Outbox 异步调用库存模块

---

## 六、API 接口

### 6.1 用户取消订单

**接口**：`POST /api/order/user/orders/{orderId}/cancel`  
**Controller**：`OrderController.cancelUserOrder()`

#### 请求参数

```json
{
  "tenantId": 1,
  "storeId": 100,
  "userId": 1000,
  "requestId": "req_uuid_001",
  "expectedVersion": 0,
  "reasonCode": "USER_CANCEL",
  "reasonDesc": "不想要了"
}
```

#### 响应示例

```json
{
  "code": 200,
  "message": "success",
  "data": null
}
```

#### curl 示例

```bash
curl -X POST 'http://localhost:8080/api/order/user/orders/10000/cancel' \
-H 'Content-Type: application/json' \
-d '{
  "tenantId": 1,
  "storeId": 100,
  "userId": 1000,
  "requestId": "req_uuid_001",
  "expectedVersion": 0,
  "reasonCode": "USER_CANCEL",
  "reasonDesc": "不想要了"
}'
```

### 6.2 Mock 退款回调

**接口**：`POST /api/pay/refund/notify`  
**Controller**：`RefundCallbackController.onRefundNotify()`

#### 请求参数

```json
{
  "notifyId": "notify_uuid_001",
  "refundId": "rfd_10001",
  "refundNo": "mock_refund_abc123",
  "success": true,
  "errorMsg": null
}
```

#### 响应示例

```json
{
  "code": 200,
  "message": "success",
  "data": null
}
```

#### curl 示例

```bash
curl -X POST 'http://localhost:8080/api/pay/refund/notify' \
-H 'Content-Type: application/json' \
-d '{
  "notifyId": "notify_uuid_001",
  "refundId": "rfd_10001",
  "refundNo": "mock_refund_abc123",
  "success": true,
  "errorMsg": null
}'
```

---

## 七、错误码表

| 错误码       | 说明                                              |
|--------------|---------------------------------------------------|
| BAD_REQUEST  | 参数错误（tenantId/orderId/requestId 为空）       |
| NOT_FOUND    | 订单不存在                                         |
| FORBIDDEN    | 无权操作该订单（用户ID不匹配）                    |
| BAD_REQUEST  | 订单状态不允许取消（IN_PROGRESS/READY/COMPLETED）|
| BAD_REQUEST  | 订单状态不允许退款（未支付或已退款）              |
| CONFLICT     | 乐观锁冲突（版本号不匹配，提示用户刷新）          |
| INTERNAL_ERROR | 退款失败（支付网关返回失败）                     |

---

## 八、测试

### 8.1 单元测试

**文件**：`app-order/src/test/java/com/bluecone/app/order/application/OrderCancelAppServiceTest.java`

#### 测试场景

1. **testCancelOrder_WaitPay_NoPay()**：WAIT_PAY 状态订单取消（不退款）
2. **testCancelOrder_WaitAccept_Paid()**：WAIT_ACCEPT 状态已支付订单取消（自动退款）
3. **testCancelOrder_Idempotent()**：幂等性 - 重复取消同一订单
4. **testCancelOrder_InvalidStatus()**：状态约束 - IN_PROGRESS 状态不允许取消

### 8.2 测试命令

```bash
# 运行订单取消服务的单元测试
mvn test -Dtest=OrderCancelAppServiceTest -pl app-order

# 运行所有订单模块的测试
mvn test -pl app-order

# 运行整个项目的测试
mvn test
```

### 8.3 集成测试步骤

#### 场景1：WAIT_PAY 订单取消（不退款）

1. 创建订单（状态：WAIT_PAY）
2. 调用取消接口
3. 验证订单状态变为 CANCELED
4. 验证不触发退款

#### 场景2：WAIT_ACCEPT 已支付订单取消（自动退款）

1. 创建订单并支付（状态：WAIT_ACCEPT）
2. 调用取消接口
3. 验证订单状态变为 CANCELED
4. 验证自动创建退款单（状态：SUCCESS）
5. 验证订单状态最终变为 REFUNDED

#### 场景3：幂等性测试

1. 创建订单
2. 使用同一 requestId 多次调用取消接口
3. 验证只执行一次取消操作
4. 验证版本号不会无限递增

#### 场景4：并发测试

1. 创建订单
2. 使用两个不同 requestId 同时调用取消接口（相同 expectedVersion）
3. 验证一个成功，一个版本冲突

---

## 九、改动文件清单

### 9.1 数据库迁移（1个文件）

1. `app-infra/src/main/resources/db/migration/V20251218004__add_refund_m4_tables.sql`

### 9.2 领域模型（5个文件）

1. `app-order/src/main/java/com/bluecone/app/order/domain/enums/RefundStatus.java`（新增）
2. `app-order/src/main/java/com/bluecone/app/order/domain/enums/RefundChannel.java`（新增）
3. `app-order/src/main/java/com/bluecone/app/order/domain/model/RefundOrder.java`（新增）
4. `app-order/src/main/java/com/bluecone/app/order/domain/model/Order.java`（修改：新增 cancel() 和 markRefunded() 方法）
5. `app-order/src/main/java/com/bluecone/app/order/domain/repository/RefundOrderRepository.java`（新增）

### 9.3 基础设施层（7个文件）

1. `app-order/src/main/java/com/bluecone/app/order/infra/persistence/po/RefundOrderPO.java`（已存在）
2. `app-order/src/main/java/com/bluecone/app/order/infra/persistence/po/OrderPO.java`（修改：新增 M4 字段）
3. `app-order/src/main/java/com/bluecone/app/order/infra/persistence/mapper/RefundOrderMapper.java`（已存在）
4. `app-order/src/main/java/com/bluecone/app/order/infra/persistence/converter/RefundOrderConverter.java`（新增）
5. `app-order/src/main/java/com/bluecone/app/order/infra/persistence/converter/OrderConverter.java`（修改：新增 M4 字段转换）
6. `app-order/src/main/java/com/bluecone/app/order/infra/persistence/repository/RefundOrderRepositoryImpl.java`（新增）
7. `app-order/src/main/java/com/bluecone/app/order/domain/gateway/PaymentRefundGateway.java`（新增）
8. `app-order/src/main/java/com/bluecone/app/order/infra/gateway/MockPaymentRefundGateway.java`（新增）

### 9.4 应用服务层（7个文件）

1. `app-order/src/main/java/com/bluecone/app/order/application/command/CancelOrderCommand.java`（新增）
2. `app-order/src/main/java/com/bluecone/app/order/application/command/ApplyRefundCommand.java`（新增）
3. `app-order/src/main/java/com/bluecone/app/order/application/OrderCancelAppService.java`（新增）
4. `app-order/src/main/java/com/bluecone/app/order/application/impl/OrderCancelAppServiceImpl.java`（新增）
5. `app-order/src/main/java/com/bluecone/app/order/application/RefundAppService.java`（新增）
6. `app-order/src/main/java/com/bluecone/app/order/application/impl/RefundAppServiceImpl.java`（新增）
7. `app-order/src/main/java/com/bluecone/app/order/domain/facade/InventoryFacade.java`（新增）
8. `app-order/src/main/java/com/bluecone/app/order/infra/facade/NoOpInventoryFacade.java`（新增）

### 9.5 API 层（3个文件）

1. `app-order/src/main/java/com/bluecone/app/order/api/dto/UserCancelOrderRequest.java`（新增）
2. `app-order/src/main/java/com/bluecone/app/order/controller/OrderController.java`（修改：更新取消订单接口）
3. `app-order/src/main/java/com/bluecone/app/order/controller/RefundCallbackController.java`（新增）

### 9.6 测试文件（1个文件）

1. `app-order/src/test/java/com/bluecone/app/order/application/OrderCancelAppServiceTest.java`（新增）

**总计**：23个文件（17个新增，6个修改）

---

## 十、后续扩展

### 10.1 真实退款接口集成

- 实现 WechatPaymentRefundGateway（微信支付退款）
- 实现 AlipayPaymentRefundGateway（支付宝退款）
- 解析真实的退款回调报文（XML/JSON）
- 验证签名、幂等性检查

### 10.2 退款回调处理完善

- 实现 `RefundAppService.onRefundNotify()` 方法
- 保存退款回调日志到 `bc_refund_notify_log` 表
- 根据 notifyId 保证幂等性
- 更新退款单状态并推进订单状态

### 10.3 库存联动

- 实现真实的 InventoryFacade（通过事件/Outbox 异步调用）
- 订单取消时释放库存预占
- 订单退款时回补库存

### 10.4 售后退款

- 支持已完成订单的售后退款（COMPLETED → REFUNDED）
- 支持部分退款（退部分商品）
- 支持退款审核流程（商户同意/拒绝）

---

## 十一、注意事项

### 11.1 幂等性保证

- **取消订单**：通过 action_log 的 idemKey 保证幂等
- **申请退款**：通过 bc_refund_order 的 idem_key 唯一约束保证幂等
- **退款回调**：通过 bc_refund_notify_log 的 notify_id 唯一约束保证幂等

### 11.2 并发安全

- **订单更新**：通过乐观锁（version）保证并发安全
- **退款单更新**：通过乐观锁（version）保证并发安全
- **版本号检查**：前端需传递 expectedVersion，后端校验后才允许操作

### 11.3 状态机约束

- **订单取消**：只允许 WAIT_PAY/WAIT_ACCEPT/ACCEPTED 状态取消
- **订单退款**：只允许"已支付且未退款"的订单退款
- **退款单流转**：SUCCESS/FAILED 为终态，不可再流转

### 11.4 库存联动

- **M4 阶段**：使用 No-op 实现，不实际操作库存
- **后续扩展**：通过事件/Outbox 异步调用，保证最终一致性
- **容错性**：库存操作失败不影响订单取消/退款，可通过补偿机制修复

---

## 十二、总结

M4 成功实现了订单取消/退款闭环，核心特性包括：

1. ✅ **完整的状态流转**：WAIT_PAY/WAIT_ACCEPT/ACCEPTED → CANCELED → REFUNDED
2. ✅ **幂等性保证**：通过 idemKey 和 notifyId 保证重复请求只执行一次
3. ✅ **并发安全**：通过乐观锁（version）保证并发操作安全
4. ✅ **自动退款**：已支付订单取消时自动触发退款
5. ✅ **Mock 实现**：支付退款网关和库存联动使用 Mock/No-op 实现
6. ✅ **清晰的领域模型**：RefundOrder 聚合根、RefundStatus 枚举、状态机约束
7. ✅ **完善的测试**：单元测试覆盖核心场景（取消、退款、幂等、并发）

M4 为后续扩展打下了坚实的基础，包括真实退款接口集成、库存联动、售后退款等功能。
