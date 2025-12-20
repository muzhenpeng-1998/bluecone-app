# 订单诊断接口使用指南

## 概述

订单诊断接口（Order Forensics API）是一个专为运维人员设计的只读诊断工具，用于快速定位订单处理过程中的问题。该接口聚合了订单全链路的数据，包括：

- 订单基本信息
- 计价快照
- 优惠券锁定与核销记录
- 钱包冻结与流水记录
- 积分流水记录
- Outbox 事件投递状态
- 消费者消费日志
- **自动诊断结论**

## 适用场景

### 何时使用订单诊断接口？

1. **用户投诉场景**
   - "我付了钱但订单没有确认"
   - "优惠券被扣了但没有生效"
   - "钱包余额扣了但订单失败了"

2. **运维巡检场景**
   - 定期检查是否有异常订单
   - 验证资产操作的一致性
   - 排查 Outbox 事件投递失败

3. **故障排查场景**
   - 订单状态与资产状态不一致
   - 消费者消费失败
   - 定时任务未正常执行

## API 参考

### 端点

```
GET /ops/api/orders/{orderId}/forensics?tenantId={tenantId}
```

### 请求参数

| 参数 | 类型 | 位置 | 必填 | 说明 |
|------|------|------|------|------|
| `orderId` | Long | Path | 是 | 订单ID |
| `tenantId` | Long | Query | 是 | 租户ID（用于租户隔离验证） |

### 认证方式

该接口受 `OpsConsoleAccessInterceptor` 保护，需要提供有效的 ops token：

**方式 1：使用 Header（推荐）**
```bash
curl -H "X-Ops-Token: your-ops-token" \
  "http://localhost:8080/ops/api/orders/123/forensics?tenantId=1"
```

**方式 2：使用 Authorization Header**
```bash
curl -H "Authorization: Bearer your-ops-token" \
  "http://localhost:8080/ops/api/orders/123/forensics?tenantId=1"
```

**方式 3：使用 Query Parameter（需配置允许）**
```bash
curl "http://localhost:8080/ops/api/orders/123/forensics?tenantId=1&token=your-ops-token"
```

### 配置 Ops Token

在 `application.yml` 中配置：

```yaml
bluecone:
  ops:
    console:
      enabled: true
      token: ${OPS_TOKEN:changeme}  # 从环境变量读取
      allow-localhost: true          # 本地开发时可绕过 token 检查
      allow-query-token: false       # 是否允许通过 query 参数传递 token
```

**生产环境建议**：
- 使用强随机字符串作为 token（至少 32 位）
- 通过环境变量注入，不要硬编码在配置文件中
- 定期轮换 token
- 禁用 `allow-query-token`（避免 token 泄露到日志）

### 响应格式

```json
{
  "orderSummary": {
    "orderId": 123,
    "tenantId": 1,
    "storeId": 10,
    "userId": 456,
    "orderNo": "ORD-20251219-001",
    "status": "PAID",
    "payStatus": "PAY_SUCCESS",
    "totalAmount": 120.00,
    "discountAmount": 20.00,
    "payableAmount": 100.00,
    "currency": "CNY",
    "couponId": 789,
    "createdAt": "2025-12-19T10:00:00",
    "updatedAt": "2025-12-19T10:05:00"
  },
  "pricingSnapshot": {
    "exists": false
  },
  "couponSection": {
    "locks": [
      {
        "id": 1,
        "couponId": 789,
        "orderId": 123,
        "lockStatus": "COMMITTED",
        "lockTime": "2025-12-19T10:00:01",
        "commitTime": "2025-12-19T10:05:01",
        "expireTime": "2025-12-19T10:30:00"
      }
    ],
    "redemptions": [
      {
        "id": 1,
        "couponId": 789,
        "orderId": 123,
        "discountAmount": 20.00,
        "redemptionTime": "2025-12-19T10:05:01"
      }
    ],
    "totalCount": 2
  },
  "walletSection": {
    "freezes": [
      {
        "id": 1,
        "freezeNo": "FRZ-001",
        "bizOrderId": 123,
        "frozenAmount": 100.00,
        "status": "COMMITTED",
        "frozenAt": "2025-12-19T10:00:02",
        "committedAt": "2025-12-19T10:05:02"
      }
    ],
    "ledgers": [
      {
        "id": 1,
        "ledgerNo": "LED-001",
        "bizOrderId": 123,
        "amount": -100.00,
        "balanceBefore": 500.00,
        "balanceAfter": 400.00,
        "remark": "订单支付扣款 - commit",
        "createdAt": "2025-12-19T10:05:02"
      }
    ],
    "totalCount": 2
  },
  "pointsSection": {
    "ledgers": [],
    "totalCount": 0
  },
  "outboxSection": {
    "events": [
      {
        "id": 1,
        "eventType": "order.checkout_locked",
        "eventId": "evt-uuid-001",
        "status": "SENT",
        "retryCount": 0,
        "createdAt": "2025-12-19T10:00:00",
        "sentAt": "2025-12-19T10:00:01"
      },
      {
        "id": 2,
        "eventType": "order.paid",
        "eventId": "evt-uuid-002",
        "status": "SENT",
        "retryCount": 0,
        "createdAt": "2025-12-19T10:05:00",
        "sentAt": "2025-12-19T10:05:01"
      }
    ],
    "totalCount": 2
  },
  "consumeSection": {
    "logs": [
      {
        "id": 1,
        "consumerName": "CouponConsumer",
        "eventId": "evt-uuid-002",
        "eventType": "order.paid",
        "status": "SUCCESS",
        "idempotencyKey": "order:123:commit",
        "consumedAt": "2025-12-19T10:05:01"
      },
      {
        "id": 2,
        "consumerName": "WalletConsumer",
        "eventId": "evt-uuid-002",
        "eventType": "order.paid",
        "status": "SUCCESS",
        "idempotencyKey": "wallet:123:commit",
        "consumedAt": "2025-12-19T10:05:02"
      }
    ],
    "totalCount": 2
  },
  "diagnosis": []
}
```

## 诊断代码说明

诊断引擎会自动分析订单数据，识别以下问题：

### 1. MISSING_WALLET_COMMIT

**严重程度**：ERROR

**含义**：订单已支付，但未找到钱包扣款流水

**可能原因**：
- `WalletEventConsumer` 未正常消费 `order.paid` 事件
- 消费过程中抛出异常
- 钱包服务不可用

**排查步骤**：
1. 查看 `consumeSection` 中是否有 `WalletConsumer` 的消费失败记录
2. 检查 `outboxSection` 中 `order.paid` 事件的状态
3. 查看 `WalletEventConsumer` 的日志，搜索 `orderId=123`
4. 检查钱包服务的健康状态

**修复建议**：
- 如果是临时故障，等待 Outbox 重试
- 如果事件已进入死信（DEAD），需要手动补发事件或直接调用钱包 Facade

### 2. MISSING_COUPON_REDEMPTION

**严重程度**：ERROR

**含义**：订单已支付且使用了优惠券，但未找到核销记录

**可能原因**：
- `CouponEventConsumer` 未正常消费 `order.paid` 事件
- 优惠券服务不可用
- 核销逻辑存在 bug

**排查步骤**：
1. 查看 `couponSection.locks` 确认优惠券已锁定
2. 检查 `consumeSection` 中 `CouponConsumer` 的消费记录
3. 查看 `CouponEventConsumer` 的日志
4. 检查优惠券服务的健康状态

**修复建议**：
- 等待 Outbox 重试或手动补发事件
- 如果优惠券已过期，需要评估是否需要补偿用户

### 3. OUTBOX_DELIVERY_FAILED

**严重程度**：ERROR（DEAD 状态）或 WARNING（FAILED 状态）

**含义**：Outbox 事件投递失败

**可能原因**：
- 消费者处理逻辑抛出异常
- 下游服务不可用
- 网络问题

**排查步骤**：
1. 查看 `outboxSection.events` 中失败事件的 `lastError` 字段
2. 检查对应消费者的日志
3. 确认下游服务是否正常

**修复建议**：
- 修复下游服务后，等待 Outbox 自动重试
- 如果已进入死信，需要手动重新发布事件

### 4. COUPON_LOCK_TIMEOUT / WALLET_FREEZE_TIMEOUT

**严重程度**：WARNING

**含义**：锁定/冻结已超时但未释放

**可能原因**：
- `LockTimeoutReaperJob` 未正常执行
- 定时任务被禁用或执行频率过低

**排查步骤**：
1. 检查 `LockTimeoutReaperJob` 的执行日志
2. 确认定时任务是否启用
3. 查看最近一次执行时间

**修复建议**：
- 确保 `LockTimeoutReaperJob` 正常运行
- 手动执行释放操作（参考 `LockTimeoutReaperJob` 代码）

### 5. DUPLICATE_CONSUMPTION

**严重程度**：WARNING

**含义**：检测到重复消费（同一个消费者对同一个事件有多条成功记录）

**可能原因**：
- 消费者幂等性实现有问题
- `consumeLogService.isConsumed()` 未被正确调用
- 并发消费导致的竞态条件

**排查步骤**：
1. 查看 `consumeSection.logs` 中重复的记录
2. 检查消费者代码，确认幂等性检查逻辑
3. 查看资产操作是否真的重复执行了

**修复建议**：
- 修复消费者的幂等性实现
- 如果资产被重复扣减，需要补偿用户

### 6. AMOUNT_INCONSISTENT

**严重程度**：WARNING

**含义**：订单应付金额与计价快照不一致

**可能原因**：
- 计价快照在订单创建后被修改
- 订单金额计算逻辑有问题
- 数据迁移导致的不一致

**排查步骤**：
1. 比对 `orderSummary.payableAmount` 和 `pricingSnapshot.payableAmount`
2. 检查订单创建流程
3. 查看订单修改日志

**修复建议**：
- 如果是历史数据问题，可以忽略
- 如果是新订单，需要修复计价逻辑

## 常见问题排查流程

### 场景 1：用户投诉"付了钱但订单没确认"

**步骤**：

1. **调用诊断接口**
   ```bash
   curl -H "X-Ops-Token: your-token" \
     "http://localhost:8080/ops/api/orders/123/forensics?tenantId=1"
   ```

2. **检查订单状态**
   - 查看 `orderSummary.status` 和 `orderSummary.payStatus`
   - 如果状态不是 `PAID`，说明支付回调可能未到达

3. **检查诊断结论**
   - 如果有 `MISSING_WALLET_COMMIT`，说明钱包扣款未执行
   - 如果有 `OUTBOX_DELIVERY_FAILED`，说明事件投递失败

4. **查看 Outbox 事件**
   - 检查 `order.paid` 事件的状态
   - 如果是 `FAILED` 或 `DEAD`，查看 `lastError`

5. **查看消费日志**
   - 检查 `WalletConsumer` 是否成功消费
   - 如果失败，查看 `errorMessage`

**解决方案**：
- 如果事件在重试中，等待自动恢复
- 如果事件已死信，手动补发或直接调用 Facade

### 场景 2：优惠券被扣但没生效

**步骤**：

1. **调用诊断接口**

2. **检查优惠券操作**
   - 查看 `couponSection.locks` 确认是否已锁定
   - 查看 `couponSection.redemptions` 确认是否已核销

3. **检查诊断结论**
   - 如果有 `MISSING_COUPON_REDEMPTION`，说明核销未执行

4. **查看消费日志**
   - 检查 `CouponConsumer` 的消费记录
   - 如果失败，查看失败原因

**解决方案**：
- 等待 Outbox 重试或手动补发事件
- 如果优惠券已过期，评估补偿方案

### 场景 3：定期巡检异常订单

**步骤**：

1. **查询最近的已支付订单**
   ```sql
   SELECT id, tenant_id, order_no, status, created_at
   FROM bc_order
   WHERE status = 'PAID'
   AND created_at > DATE_SUB(NOW(), INTERVAL 1 HOUR)
   ORDER BY created_at DESC;
   ```

2. **对每个订单调用诊断接口**

3. **汇总诊断结论**
   - 统计各类错误的数量
   - 重点关注 `ERROR` 级别的诊断

4. **生成巡检报告**

## 最佳实践

### 1. 使用脚本批量诊断

```bash
#!/bin/bash
# 批量诊断脚本

OPS_TOKEN="your-ops-token"
TENANT_ID=1
ORDER_IDS=(123 456 789)

for ORDER_ID in "${ORDER_IDS[@]}"; do
  echo "Diagnosing order: $ORDER_ID"
  
  curl -s -H "X-Ops-Token: $OPS_TOKEN" \
    "http://localhost:8080/ops/api/orders/$ORDER_ID/forensics?tenantId=$TENANT_ID" \
    | jq '.diagnosis[] | select(.severity == "ERROR")'
  
  echo "---"
done
```

### 2. 集成到监控告警

可以定期调用诊断接口，将诊断结论推送到监控系统：

```python
import requests
import json

def diagnose_order(tenant_id, order_id, ops_token):
    url = f"http://localhost:8080/ops/api/orders/{order_id}/forensics"
    headers = {"X-Ops-Token": ops_token}
    params = {"tenantId": tenant_id}
    
    response = requests.get(url, headers=headers, params=params)
    response.raise_for_status()
    
    data = response.json()
    diagnosis = data.get("diagnosis", [])
    
    # 过滤 ERROR 级别的诊断
    errors = [d for d in diagnosis if d["severity"] == "ERROR"]
    
    if errors:
        # 发送告警
        send_alert(f"Order {order_id} has {len(errors)} critical issues")
    
    return data
```

### 3. 结合日志分析

诊断接口提供了高层次的视图，但具体的错误信息需要结合日志分析：

1. 诊断接口识别问题类型
2. 根据 `eventId`、`orderId` 搜索日志
3. 定位具体的错误堆栈

### 4. 权限管理

- 只授予运维人员和高级开发人员访问权限
- 定期轮换 ops token
- 记录所有诊断接口的访问日志（用于审计）

## 限制与注意事项

1. **数据量限制**
   - 每个 section 默认最多返回 50 条记录
   - 如果数据量过大，可能被截断
   - 通过 `totalCount` 字段判断是否被截断

2. **性能考虑**
   - 诊断接口会执行多次数据库查询
   - 不建议高频调用
   - 建议在非高峰期进行批量诊断

3. **租户隔离**
   - 必须提供正确的 `tenantId`
   - 不能跨租户查询订单

4. **只读接口**
   - 诊断接口不会修改任何数据
   - 修复问题需要手动操作或等待自动恢复

## 相关文档

- [Outbox 快速参考](./OUTBOX-QUICK-REFERENCE.md)
- [Outbox 一致性指南](./OUTBOX-CONSISTENCY-GUIDE.md)
- [订单状态流转](./ORDER-STATUS-QUICK-REFERENCE.md)

## 反馈与改进

如果您在使用过程中发现问题或有改进建议，请联系平台团队。
