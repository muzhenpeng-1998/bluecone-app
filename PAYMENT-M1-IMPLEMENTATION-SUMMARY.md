# 支付推进 M1 实现总结

## 目标
实现支付推进 M1，打通 WAIT_PAY -> PAID，并提供最小验证。

## 一、改动文件清单（按模块）

### 1. app-infra（数据库迁移）
- ✅ `V20251218001__add_payment_notify_id.sql` - 新增支付回调幂等字段和订单关单字段

### 2. app-order（订单模块）

#### 领域模型
- ✅ `Order.java` - 新增方法：
  - `markPaid(Long payOrderId, String payChannel, String payNo, LocalDateTime paidAt)` - 支付成功推进（含状态机约束）
  - `markCancelledWithReason(String closeReason)` - 带原因关单（含幂等）
  - 新增字段：`closeReason`、`closedAt`

#### API层
- ✅ `OrderPaymentFacade.java` - 新建，对外暴露订单支付能力
- ✅ `OrderPaymentFacadeImpl.java` - 新建，实现订单支付门面

#### 持久化层
- ✅ `OrderPO.java` - 新增字段：`closeReason`、`closedAt`

### 3. app-payment（支付模块）

#### API DTO
- ✅ `PaymentCreateRequest.java` - 新建，支付创建请求
- ✅ `PaymentCreateResponse.java` - 新建，支付创建响应
- ✅ `PaymentNotifyRequest.java` - 新建，支付回调请求
- ✅ `PaymentNotifyResponse.java` - 新建，支付回调响应

#### 应用服务（待实现）
- ⏳ `PaymentM1ApplicationService.java` - 待创建，M1简化支付服务
- ⏳ `PaymentNotifyApplicationService.java` - 待创建，支付回调服务

#### 控制器（待实现）
- ⏳ `PaymentController.java` - 待创建，支付接口控制器

### 4. app-application（集成测试）
- ⏳ `PaymentFlowM1IntegrationTest.java` - 待创建，支付流程集成测试
- ⏳ `OrderTimeoutCloseJobTest.java` - 待创建，超时关单测试

### 5. app-order（超时关单Job）
- ⏳ `OrderTimeoutCloseJob.java` - 待创建，订单超时关单定时任务

## 二、核心实现说明

### 1. 订单状态机约束

#### markPaid 方法
```java
/**
 * 由支付链路调用，记录支付单信息并流转状态。
 * <p>状态机约束：只允许从 WAIT_PAY 状态流转到 PAID 状态。</p>
 * <p>幂等性：如果订单已经是 PAID 状态，则直接返回，不抛异常（允许重复回调）。</p>
 */
public void markPaid(Long payOrderId, String payChannel, String payNo, LocalDateTime paidAt) {
    // 幂等性：如果订单已经是 PAID 状态，直接返回（允许重复回调）
    if (OrderStatus.PAID.equals(this.status)) {
        log.debug("订单已支付，幂等返回：orderId={}, status={}", this.id, this.status);
        return;
    }
    
    // 状态机约束：只允许从 WAIT_PAY 状态流转到 PAID 状态
    if (!OrderStatus.WAIT_PAY.equals(this.status)) {
        String msg = String.format("订单状态不允许支付：当前状态=%s，只允许 WAIT_PAY 状态支付", 
                this.status != null ? this.status.getCode() : "NULL");
        log.warn("订单状态不允许支付：orderId={}, currentStatus={}", this.id, this.status);
        throw new BizException(CommonErrorCode.BAD_REQUEST, msg);
    }
    
    // 流转状态：WAIT_PAY -> PAID
    this.status = OrderStatus.PAID;
    this.payStatus = PayStatus.PAID;
    
    // 记录支付信息到扩展字段
    updateExt("payOrderId", payOrderId);
    updateExt("payChannel", payChannel);
    updateExt("payNo", payNo);
    updateExt("paidAt", paidAt != null ? paidAt.toString() : null);
    
    this.updatedAt = LocalDateTime.now();
}
```

#### markCancelledWithReason 方法
```java
/**
 * 标记为已取消（带关单原因）。
 * <p>用于超时关单、用户取消、商户取消等场景。</p>
 * <p>幂等性：如果订单已经是 CANCELLED 状态，则直接返回。</p>
 */
public void markCancelledWithReason(String closeReason) {
    // 幂等性：如果订单已经是 CANCELLED 状态，直接返回
    if (OrderStatus.CANCELLED.equals(this.status)) {
        log.debug("订单已取消，幂等返回：orderId={}, closeReason={}", this.id, this.closeReason);
        return;
    }
    
    // 状态约束：只允许从 WAIT_PAY、INIT、PENDING_PAYMENT 等未支付状态取消
    if (OrderStatus.PAID.equals(this.status) || OrderStatus.COMPLETED.equals(this.status)) {
        String msg = String.format("订单状态不允许取消：当前状态=%s", 
                this.status != null ? this.status.getCode() : "NULL");
        log.warn("订单状态不允许取消：orderId={}, currentStatus={}", this.id, this.status);
        throw new BizException(CommonErrorCode.BAD_REQUEST, msg);
    }
    
    // 流转状态
    this.status = OrderStatus.CANCELLED;
    this.closeReason = closeReason;
    this.closedAt = LocalDateTime.now();
    this.updatedAt = LocalDateTime.now();
}
```

### 2. 模块边界说明

#### OrderPaymentFacade（订单侧）
```java
/**
 * 订单支付门面接口。
 * <p>对外暴露订单侧的支付相关能力，供支付模块调用。</p>
 * <p>隔离原则：支付模块不得直接操作订单表，只能通过此 Facade 调用订单应用服务。</p>
 */
public interface OrderPaymentFacade {
    /**
     * 标记订单为已支付。
     * <p>状态机约束：只允许从 WAIT_PAY 状态流转到 PAID 状态。</p>
     * <p>幂等性：如果订单已经是 PAID 状态，则直接返回成功，不抛异常（允许重复回调）。</p>
     */
    void markOrderPaid(Long tenantId, Long orderId, Long payOrderId, String payChannel, String payNo, LocalDateTime paidAt);

    /**
     * 标记订单为已取消（带关单原因）。
     * <p>用于超时关单、用户取消、商户取消等场景。</p>
     * <p>幂等性：如果订单已经是 CANCELLED 状态，则直接返回成功。</p>
     */
    void markOrderCancelled(Long tenantId, Long orderId, String closeReason);
}
```

### 3. 数据库变更

#### V20251218001__add_payment_notify_id.sql
```sql
-- 支付回调幂等优化
-- 为支付回调日志表添加 notify_id 唯一索引，用于幂等控制
ALTER TABLE bc_payment_notify_log 
ADD COLUMN IF NOT EXISTS notify_id VARCHAR(128) DEFAULT NULL COMMENT '通知ID（用于幂等，来自渠道或客户端生成）' AFTER id;

ALTER TABLE bc_payment_notify_log 
ADD UNIQUE INDEX IF NOT EXISTS uk_notify_id (notify_id);

-- 为订单表添加关单原因字段
ALTER TABLE bc_order
ADD COLUMN IF NOT EXISTS close_reason VARCHAR(64) DEFAULT NULL COMMENT '关单原因：PAY_TIMEOUT（支付超时）、USER_CANCEL（用户取消）、MERCHANT_CANCEL（商户取消）等' AFTER status;

ALTER TABLE bc_order
ADD COLUMN IF NOT EXISTS closed_at DATETIME DEFAULT NULL COMMENT '关单时间' AFTER close_reason;
```

## 三、待实现功能

### 1. 支付创建接口 POST /api/pay/create

**功能说明：**
- 校验订单状态必须是 WAIT_PAY
- 创建支付单（若已存在未支付的支付单，可复用返回）
- 返回 mockPayToken 或预支付信息

**请求示例：**
```bash
curl -X POST http://localhost:8080/api/pay/create \
  -H "Content-Type: application/json" \
  -d '{
    "tenantId": 1,
    "storeId": 1,
    "orderId": 123456,
    "channel": "MOCK",
    "userId": 1,
    "clientIp": "127.0.0.1"
  }'
```

**响应示例：**
```json
{
  "code": 0,
  "message": "success",
  "data": {
    "paymentId": 789012,
    "paymentNo": "pay_01234567890123456789",
    "orderId": 123456,
    "orderNo": "ord_01234567890123456789",
    "payableAmount": 40.00,
    "currency": "CNY",
    "channel": "MOCK",
    "mockPayToken": "mock_token_abc123",
    "reused": false
  }
}
```

### 2. 支付回调接口 POST /api/pay/notify

**功能说明：**
- 首先写入 notify_log（notifyId 唯一），若重复则直接返回成功（幂等）
- 校验 amount 与订单应付一致
- 更新 payment_order 状态为 SUCCESS
- 调用订单侧 OrderPaymentFacade.markOrderPaid 推进订单：WAIT_PAY -> PAID

**请求示例：**
```bash
curl -X POST http://localhost:8080/api/pay/notify \
  -H "Content-Type: application/json" \
  -d '{
    "notifyId": "notify_abc123",
    "payNo": "wx_transaction_123456",
    "orderId": 123456,
    "amount": 40.00,
    "paidAt": "2025-12-18T12:00:00",
    "channel": "WECHAT_JSAPI",
    "tenantId": 1
  }'
```

**响应示例：**
```json
{
  "code": 0,
  "message": "success",
  "data": {
    "success": true,
    "message": "支付回调处理成功",
    "idempotent": false
  }
}
```

**幂等验证（重复通知）：**
```bash
# 再次发送相同的 notifyId
curl -X POST http://localhost:8080/api/pay/notify \
  -H "Content-Type: application/json" \
  -d '{
    "notifyId": "notify_abc123",
    "payNo": "wx_transaction_123456",
    "orderId": 123456,
    "amount": 40.00,
    "paidAt": "2025-12-18T12:00:00",
    "channel": "WECHAT_JSAPI",
    "tenantId": 1
  }'
```

**响应示例（幂等返回）：**
```json
{
  "code": 0,
  "message": "success",
  "data": {
    "success": true,
    "message": "支付回调已处理（幂等返回）",
    "idempotent": true
  }
}
```

### 3. 订单超时关单 Job

**功能说明：**
- 每分钟扫描一次超时订单（status=WAIT_PAY 且 created_at < now - TTL）
- 逐笔推进为 CANCELLED，并记录 closeReason=PAY_TIMEOUT
- 幂等要求：同一订单重复扫描不会重复执行副作用
- 若存在库存预占：调用库存模块释放（预留接口）

**实现要点：**
```java
@Scheduled(cron = "0 * * * * ?") // 每分钟执行一次
public void closeTimeoutOrders() {
    // 1. 查询超时订单（created_at < now - 15分钟 且 status=WAIT_PAY）
    List<Order> timeoutOrders = orderRepository.findTimeoutOrders(15);
    
    // 2. 逐笔关单
    for (Order order : timeoutOrders) {
        try {
            // 调用领域模型方法（含幂等和状态机校验）
            order.markCancelledWithReason("PAY_TIMEOUT");
            orderRepository.update(order);
            
            // 3. 释放库存（预留接口）
            // inventoryFacade.releaseReservation(order.getId());
            
            log.info("订单超时关单成功：orderId={}, closeReason=PAY_TIMEOUT", order.getId());
        } catch (Exception e) {
            log.error("订单超时关单失败：orderId={}", order.getId(), e);
        }
    }
}
```

## 四、错误码表

| 错误码 | reasonCode | 说明 | 场景 |
|--------|-----------|------|------|
| BAD_REQUEST | ORDER_STATUS_NOT_ALLOW_PAY | 订单状态不允许支付 | 订单不是 WAIT_PAY 状态时尝试支付 |
| BAD_REQUEST | ORDER_STATUS_NOT_ALLOW_CANCEL | 订单状态不允许取消 | 订单已支付或已完成时尝试取消 |
| BAD_REQUEST | PAYMENT_AMOUNT_MISMATCH | 支付金额不一致 | 回调金额与订单应付金额不一致 |
| NOT_FOUND | ORDER_NOT_FOUND | 订单不存在 | 查询订单时订单不存在 |
| NOT_FOUND | PAYMENT_NOT_FOUND | 支付单不存在 | 查询支付单时支付单不存在 |

## 五、验收标准

### 1. 集成测试要求
- ✅ 创建订单到 WAIT_PAY（复用现有 submit 流程）
- ⏳ 调用 createPay 创建支付单
- ⏳ 调用 notify 一次，订单变 PAID
- ⏳ 再次调用 notify，仍返回成功且状态不变、无重复副作用
- ⏳ 构造超时订单，运行 Job 后状态变 CANCELLED

### 2. 验收命令
```bash
mvn -pl app-application -am clean test
```

## 六、关键分支中文注释说明

### 1. 为什么 markPaid 方法要做幂等处理？
**原因：** 支付回调可能因网络问题重复通知，如果不做幂等处理，会导致订单状态重复推进或抛出异常。通过检查订单当前状态，如果已经是 PAID 状态，直接返回成功，避免重复处理。

### 2. 为什么 markPaid 方法只允许从 WAIT_PAY 状态流转？
**原因：** 这是订单状态机的核心约束。订单只有在待支付状态时才能接收支付成功的通知。如果订单已取消、已完成或其他状态，说明业务流程异常，需要抛出异常阻止非法状态流转。

### 3. 为什么支付回调要先写入 notify_log？
**原因：** notify_log 表的 notify_id 字段有唯一索引，利用数据库的唯一约束实现幂等。如果重复通知，插入会失败（或通过 INSERT IGNORE 跳过），从而快速判断是否为重复请求，避免重复处理业务逻辑。

### 4. 为什么超时关单 Job 要逐笔处理而不是批量更新？
**原因：** 逐笔处理可以：
- 利用领域模型的状态机校验和幂等逻辑
- 记录每笔订单的关单日志
- 在某笔订单处理失败时不影响其他订单
- 后续扩展库存释放等副作用时更容易控制

### 5. 为什么订单模块要提供 OrderPaymentFacade 而不是让支付模块直接操作订单表？
**原因：** 这是 DDD 的模块边界隔离原则：
- 订单状态机逻辑封装在订单领域模型中，支付模块不应该了解订单的状态流转规则
- 通过 Facade 接口，订单模块可以控制对外暴露的能力，隐藏内部实现细节
- 避免跨模块直接操作数据库，降低耦合度，提高可维护性

## 七、下一步工作

1. ⏳ 实现 PaymentM1ApplicationService（支付创建服务）
2. ⏳ 实现 PaymentNotifyApplicationService（支付回调服务）
3. ⏳ 实现 PaymentController（支付接口控制器）
4. ⏳ 实现 OrderTimeoutCloseJob（订单超时关单定时任务）
5. ⏳ 编写 PaymentFlowM1IntegrationTest（支付流程集成测试）
6. ⏳ 编写 OrderTimeoutCloseJobTest（超时关单测试）
7. ⏳ 运行测试并验证

## 八、当前进度

- ✅ 数据库迁移脚本已创建
- ✅ 订单领域模型已更新（markPaid、markCancelledWithReason）
- ✅ OrderPaymentFacade 已实现并编译通过
- ✅ 支付 API DTO 已创建
- ⏳ 支付应用服务待实现
- ⏳ 支付控制器待实现
- ⏳ 超时关单 Job 待实现
- ⏳ 集成测试待编写

**编译状态：** ✅ app-order 模块编译成功

**预计完成时间：** 需要继续实现剩余功能（约30-40分钟）

**建议：** 由于实现内容较多，建议分步骤验证：
1. 先实现支付创建和回调接口
2. 验证支付流程和幂等性
3. 再实现超时关单 Job
4. 最后编写完整的集成测试

## 九、编译问题修复记录

### 问题：CommonErrorCode.NOT_FOUND 不存在
**错误信息：**
```
java: 找不到符号
  符号:   变量 NOT_FOUND
  位置: 类 com.bluecone.app.core.error.CommonErrorCode
```

**原因：** `CommonErrorCode` 枚举中没有定义 `NOT_FOUND` 常量，只有 `SYSTEM_ERROR`、`BAD_REQUEST`、`UNAUTHORIZED`、`FORBIDDEN`、`CONFLICT`。

**解决方案：** 使用订单模块专属的错误码 `OrderErrorCode.ORDER_NOT_FOUND` 替代。

**修复代码：**
```java
// 修改前
throw new BizException(CommonErrorCode.NOT_FOUND, "订单不存在");

// 修改后
throw new BizException(OrderErrorCode.ORDER_NOT_FOUND);
```
