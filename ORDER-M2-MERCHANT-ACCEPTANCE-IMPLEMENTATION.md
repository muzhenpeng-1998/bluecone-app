# 订单 M2：商户接单/拒单实现文档

## 一、功能概述

本次实现完成了订单"商户接单 M2"功能，打通订单状态从 `WAIT_ACCEPT`（待接单）到 `ACCEPTED`（已接单）或 `CANCELED`（已取消/拒单）的完整流程，具备以下特性：

- ✅ **并发保护**：乐观锁（订单版本号）防止并发冲突
- ✅ **幂等保护**：基于 requestId 的幂等键，防止重复接单/拒单
- ✅ **可测试性**：完整的错误码、中文注释、审计日志
- ✅ **可追溯性**：记录操作人、操作时间、拒单原因

## 二、核心设计

### 2.1 状态流转

```
支付成功 → WAIT_ACCEPT（待接单）
              ├─ 商户接单 → ACCEPTED（已接单）
              └─ 商户拒单 → CANCELED（已取消，带拒单原因）
```

### 2.2 并发与幂等策略

#### 并发保护（乐观锁）

- 订单表 `bc_order` 使用 `version` 字段作为乐观锁
- 每次更新订单时，SQL 带上 `WHERE version = ?` 条件
- 如果版本号不匹配，更新失败，抛出 `ORDER_VERSION_CONFLICT` 异常

#### 幂等保护（唯一键）

- 新增 `bc_order_action_log` 表记录每次接单/拒单操作
- `action_key` 唯一索引：`{tenantId}:{storeId}:{orderId}:{actionType}:{requestId}`
- 同一 `requestId` 重复调用：
  - 如果之前成功（SUCCESS），返回已有结果，不产生副作用
  - 如果之前失败（FAILED），抛出异常提示使用新的 requestId
  - 如果正在处理（PROCESSING），抛出并发异常

### 2.3 执行顺序（为什么要这个顺序）

1. **参数校验**：快速失败，避免无效请求进入数据库
2. **幂等检查**：先写 `action_log`，利用唯一索引防止重复执行（最早拦截点）
3. **查询订单**：获取最新订单状态和版本号
4. **业务校验**：门店归属、状态约束、版本号校验
5. **状态变更**：调用聚合根方法（领域逻辑封装）
6. **持久化**：更新订单（带乐观锁），如果失败则回滚整个事务
7. **更新日志**：标记 `action_log` 为成功，记录结果
8. **发布事件**：通知下游系统（如通知用户、推送商户端等）

## 三、改动文件清单

### 3.1 模块：app-infra（数据库）

#### Flyway 迁移脚本

- `app-infra/src/main/resources/db/migration/V20251218002__add_order_acceptance_m2_fields.sql`
  - 补充订单表字段：`reject_reason_code`、`reject_reason_desc`、`rejected_at`、`rejected_by`
  - 新增幂等动作表：`bc_order_action_log`（记录接单/拒单操作）

### 3.2 模块：app-order（领域层）

#### 领域模型

- `domain/model/Order.java`
  - 新增字段：`rejectReasonCode`、`rejectReasonDesc`、`rejectedAt`、`rejectedBy`
  - 新增方法：`reject(operatorId, reasonCode, reasonDesc)`
  - 增强方法：`accept(operatorId)` 添加详细中文注释
- `domain/model/OrderActionLog.java`（新建）
  - 幂等动作日志聚合根
  - 记录每次接单/拒单操作的执行状态和结果

#### 仓储接口

- `domain/repository/OrderActionLogRepository.java`（新建）
  - `findByActionKey(tenantId, actionKey)` - 查询幂等记录
  - `save(log)` - 保存幂等记录
  - `update(log)` - 更新幂等记录状态

#### 领域事件

- `domain/event/OrderRejectedEvent.java`（新建）
  - 商户拒单事件，触发退款、通知等下游流程

#### 错误码

- `domain/enums/OrderErrorCode.java`（新建）
  - `ORDER_NOT_FOUND` - 订单不存在
  - `ORDER_NOT_BELONG_TO_STORE` - 订单不属于当前门店
  - `ORDER_STATUS_NOT_ALLOW_ACCEPT` - 订单状态不允许接单
  - `ORDER_STATUS_NOT_ALLOW_REJECT` - 订单状态不允许拒单
  - `ORDER_VERSION_CONFLICT` - 订单版本冲突
  - `ORDER_CONCURRENT_MODIFICATION` - 订单正在被其他人操作
  - `IDEMPOTENT_ACTION_FAILED` - 该操作之前已失败

### 3.3 模块：app-order（应用层）

#### 命令对象

- `application/command/MerchantAcceptOrderCommand.java`（更新）
  - 新增字段：`requestId`、`expectedVersion`
- `application/command/MerchantRejectOrderCommand.java`（新建）
  - 拒单命令：`reasonCode`、`reasonDesc`、`requestId`、`expectedVersion`

#### 应用服务

- `application/MerchantOrderCommandAppService.java`（更新）
  - 新增方法：`rejectOrder(command)`
- `application/impl/MerchantOrderCommandAppServiceImpl.java`（重构）
  - 重写 `acceptOrder(command)`，集成幂等保护和乐观锁
  - 新增 `rejectOrder(command)`，集成幂等保护和乐观锁
  - 新增 `tryCreateActionLog()` - 幂等检查辅助方法

### 3.4 模块：app-order（接口层）

#### API 请求/响应

- `api/dto/MerchantAcceptOrderRequest.java`（更新）
  - 新增字段：`requestId`、`expectedVersion`
- `api/dto/MerchantRejectOrderRequest.java`（新建）
  - 拒单请求：`reasonCode`、`reasonDesc`、`requestId`、`expectedVersion`
- `api/dto/MerchantOrderView.java`（更新）
  - 新增字段：`version`、`rejectReasonCode`、`rejectReasonDesc`、`rejectedAt`、`rejectedBy`

#### API 端点

- `controller/OrderController.java`（更新）
  - 更新接口：`POST /api/order/merchant/orders/{orderId}/accept`
  - 新增接口：`POST /api/order/merchant/orders/{orderId}/reject`

### 3.5 模块：app-order（基础设施层）

#### 持久化对象

- `infra/persistence/po/OrderPO.java`（更新）
  - 新增字段：`rejectReasonCode`、`rejectReasonDesc`、`rejectedAt`、`rejectedBy`
- `infra/persistence/po/OrderActionLogPO.java`（新建）
  - 幂等动作表 PO

#### Mapper

- `infra/persistence/mapper/OrderActionLogMapper.java`（新建）
  - MyBatis Mapper for `bc_order_action_log`

#### 转换器

- `infra/persistence/converter/OrderConverter.java`（更新）
  - 支持新增字段的 PO ↔ Domain 转换
- `infra/persistence/converter/OrderActionLogConverter.java`（新建）
  - OrderActionLog 的 PO ↔ Domain 转换

#### 仓储实现

- `infra/persistence/repository/OrderActionLogRepositoryImpl.java`（新建）
  - 实现 `OrderActionLogRepository` 接口

## 四、API 接口文档

### 4.1 商户接单接口

**接口地址**：`POST /api/order/merchant/orders/{orderId}/accept`

**请求参数**：

```json
{
  "tenantId": 1,
  "storeId": 1001,
  "operatorId": 2001,
  "requestId": "req_uuid_12345678",  // 必填，用于幂等，建议 UUID
  "expectedVersion": 3                // 可选，用于乐观锁
}
```

**成功响应**：

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "orderId": 123456,
    "orderNo": "ord_abc123",
    "status": "ACCEPTED",
    "payStatus": "PAID",
    "payableAmount": 99.50,
    "version": 4,
    "acceptOperatorId": 2001,
    "acceptedAt": "2025-12-18T10:30:00"
  }
}
```

**错误响应**：

- `ORDER_NOT_FOUND` - 订单不存在
- `ORDER_NOT_BELONG_TO_STORE` - 订单不属于当前门店
- `ORDER_STATUS_NOT_ALLOW_ACCEPT` - 订单状态不允许接单（如已接单、已取消）
- `ORDER_VERSION_CONFLICT` - 订单版本冲突，请刷新后重试
- `ORDER_CONCURRENT_MODIFICATION` - 订单正在被其他人操作，请稍后重试
- `IDEMPOTENT_ACTION_FAILED` - 该操作之前已失败，请使用新的 requestId 重试

### 4.2 商户拒单接口

**接口地址**：`POST /api/order/merchant/orders/{orderId}/reject`

**请求参数**：

```json
{
  "tenantId": 1,
  "storeId": 1001,
  "operatorId": 2001,
  "reasonCode": "OUT_OF_STOCK",      // 必填，拒单原因码
  "reasonDesc": "商品库存不足",       // 可选，拒单原因说明
  "requestId": "req_uuid_87654321",  // 必填，用于幂等
  "expectedVersion": 3                // 可选，用于乐观锁
}
```

**拒单原因码（示例）**：

- `OUT_OF_STOCK` - 库存不足
- `BUSY` - 太忙无法接单
- `CLOSED` - 门店已打烊
- `OTHER` - 其他原因

**成功响应**：

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "orderId": 123456,
    "orderNo": "ord_abc123",
    "status": "CANCELED",
    "payStatus": "PAID",
    "payableAmount": 99.50,
    "version": 4,
    "rejectReasonCode": "OUT_OF_STOCK",
    "rejectReasonDesc": "商品库存不足",
    "rejectedAt": "2025-12-18T10:30:00",
    "rejectedBy": 2001
  }
}
```

**错误响应**：同接单接口

## 五、验证脚本

### 5.1 前置准备

1. 启动应用（确保 Flyway 迁移已执行）
2. 创建测试订单并支付成功（订单状态为 `WAIT_ACCEPT`）

### 5.2 测试用例 1：正常接单

```bash
# 假设订单 ID = 123456，版本号 = 3
curl -X POST http://localhost:8080/api/order/merchant/orders/123456/accept \
  -H "Content-Type: application/json" \
  -d '{
    "tenantId": 1,
    "storeId": 1001,
    "operatorId": 2001,
    "requestId": "req_test_accept_001",
    "expectedVersion": 3
  }'

# 期望响应：status = ACCEPTED, version = 4
```

### 5.3 测试用例 2：幂等测试（重复接单）

```bash
# 使用相同的 requestId 再次调用
curl -X POST http://localhost:8080/api/order/merchant/orders/123456/accept \
  -H "Content-Type: application/json" \
  -d '{
    "tenantId": 1,
    "storeId": 1001,
    "operatorId": 2001,
    "requestId": "req_test_accept_001",
    "expectedVersion": 3
  }'

# 期望响应：返回相同结果（status = ACCEPTED, version = 4）
# 注意：version 不再递增，说明幂等生效
```

### 5.4 测试用例 3：版本冲突（并发）

```bash
# 模拟另一个操作员同时接单（使用旧版本号）
curl -X POST http://localhost:8080/api/order/merchant/orders/123456/accept \
  -H "Content-Type: application/json" \
  -d '{
    "tenantId": 1,
    "storeId": 1001,
    "operatorId": 2002,
    "requestId": "req_test_accept_002",
    "expectedVersion": 3
  }'

# 期望响应：错误码 ORDER_VERSION_CONFLICT
# 提示："订单版本冲突，请刷新后重试"
```

### 5.5 测试用例 4：正常拒单

```bash
# 假设订单 ID = 123457，状态 = WAIT_ACCEPT，版本号 = 2
curl -X POST http://localhost:8080/api/order/merchant/orders/123457/reject \
  -H "Content-Type: application/json" \
  -d '{
    "tenantId": 1,
    "storeId": 1001,
    "operatorId": 2001,
    "reasonCode": "OUT_OF_STOCK",
    "reasonDesc": "商品库存不足，暂时无法制作",
    "requestId": "req_test_reject_001",
    "expectedVersion": 2
  }'

# 期望响应：status = CANCELED, rejectReasonCode = OUT_OF_STOCK, version = 3
```

### 5.6 测试用例 5：状态约束（已接单不能拒单）

```bash
# 尝试拒单已接单的订单（orderId = 123456，已在测试用例 1 中接单）
curl -X POST http://localhost:8080/api/order/merchant/orders/123456/reject \
  -H "Content-Type: application/json" \
  -d '{
    "tenantId": 1,
    "storeId": 1001,
    "operatorId": 2001,
    "reasonCode": "BUSY",
    "reasonDesc": "太忙",
    "requestId": "req_test_reject_002",
    "expectedVersion": 4
  }'

# 期望响应：错误码 ORDER_STATUS_NOT_ALLOW_REJECT
# 提示："订单状态不允许拒单，请刷新后重试"
```

### 5.7 测试用例 6：幂等失败后重试

```bash
# 1. 第一次拒单（使用错误的版本号，预期失败）
curl -X POST http://localhost:8080/api/order/merchant/orders/123458/reject \
  -H "Content-Type: application/json" \
  -d '{
    "tenantId": 1,
    "storeId": 1001,
    "operatorId": 2001,
    "reasonCode": "BUSY",
    "reasonDesc": "太忙",
    "requestId": "req_test_reject_003",
    "expectedVersion": 999
  }'
# 期望响应：错误码 ORDER_VERSION_CONFLICT

# 2. 使用相同的 requestId 重试
curl -X POST http://localhost:8080/api/order/merchant/orders/123458/reject \
  -H "Content-Type: application/json" \
  -d '{
    "tenantId": 1,
    "storeId": 1001,
    "operatorId": 2001,
    "reasonCode": "BUSY",
    "reasonDesc": "太忙",
    "requestId": "req_test_reject_003",
    "expectedVersion": 2
  }'
# 期望响应：错误码 IDEMPOTENT_ACTION_FAILED
# 提示："该操作之前已失败，请使用新的请求ID重试"

# 3. 使用新的 requestId 重试
curl -X POST http://localhost:8080/api/order/merchant/orders/123458/reject \
  -H "Content-Type: application/json" \
  -d '{
    "tenantId": 1,
    "storeId": 1001,
    "operatorId": 2001,
    "reasonCode": "BUSY",
    "reasonDesc": "太忙",
    "requestId": "req_test_reject_004",
    "expectedVersion": 2
  }'
# 期望响应：成功拒单
```

## 六、数据库验证

### 6.1 查询订单状态

```sql
-- 查询订单主表
SELECT id, order_no, status, version, 
       accept_operator_id, accepted_at,
       reject_reason_code, reject_reason_desc, rejected_at, rejected_by
FROM bc_order
WHERE id = 123456;
```

### 6.2 查询幂等日志

```sql
-- 查询幂等动作表
SELECT id, order_id, action_type, action_key, 
       operator_id, status, created_at, updated_at
FROM bc_order_action_log
WHERE order_id = 123456
ORDER BY created_at DESC;
```

## 七、测试清单

### 7.1 功能测试

- [x] 商户接单：WAIT_ACCEPT → ACCEPTED
- [x] 商户拒单：WAIT_ACCEPT → CANCELED（带拒单原因）
- [x] 幂等保护：同一 requestId 重复调用，返回相同结果
- [x] 版本冲突：并发接单/拒单，后者失败并提示版本冲突
- [x] 状态约束：非 WAIT_ACCEPT 状态不允许接单/拒单
- [x] 门店归属：订单不属于当前门店时拒绝操作

### 7.2 边界测试

- [x] requestId 为空：返回参数错误
- [x] reasonCode 为空（拒单）：返回参数错误
- [x] 订单不存在：返回 ORDER_NOT_FOUND
- [x] 订单已接单（重复接单）：幂等返回
- [x] 订单已拒单（重复拒单）：幂等返回
- [x] 版本号不传：不校验版本号（不推荐）

### 7.3 并发测试

- [x] 两个线程同时接单（不同 requestId）：一个成功，一个版本冲突
- [x] 一个线程接单，一个线程拒单：后者失败（状态不允许）

## 八、后续工作

### 8.1 建议优化（非本次范围）

- [ ] 接入分布式追踪（Trace ID），方便排查并发问题
- [ ] 接入消息队列，异步发送拒单通知
- [ ] 增加拒单原因码字典管理（后台配置）
- [ ] 增加接单/拒单操作日志查询接口（商户端查看历史操作）

### 8.2 监控指标（建议）

- 商户接单率（ACCEPTED / WAIT_ACCEPT）
- 商户拒单率（REJECTED / WAIT_ACCEPT）
- 拒单原因分布（TOP 5 拒单原因）
- 版本冲突次数（监控并发冲突频率）
- 幂等命中率（重复请求占比）

## 九、FAQ

### Q1：为什么要用 requestId 而不是直接用订单状态判断幂等？

**答**：订单状态只能表示"当前是否已接单"，但无法区分以下场景：
- 场景 1：第一次接单成功（应该返回成功）
- 场景 2：重复接单（应该返回成功，但不应该递增版本号）
- 场景 3：并发接单（应该返回版本冲突）

使用 `requestId` 可以精确区分"幂等返回"和"并发冲突"。

### Q2：为什么拒单后订单状态是 CANCELED 而不是 REJECTED？

**答**：为了保持状态枚举的简洁性和一致性：
- `CANCELED` 表示订单被取消（包括用户取消、商户拒单、超时取消）
- 通过 `reject_reason_code` 字段区分拒单原因
- 避免引入新的状态枚举，降低系统复杂度

### Q3：如果 expectedVersion 不传，会怎样？

**答**：不传 `expectedVersion` 时，系统不会校验版本号，仍然会使用乐观锁（自动递增版本号）。但这样无法防止"看到的是旧状态，却基于旧状态操作"的问题，**不推荐在生产环境使用**。

### Q4：如何处理"操作员 A 接单成功，但前端网络超时，操作员 A 重试时发现订单已被操作员 B 拒单"的场景？

**答**：这是典型的并发场景，系统会返回 `ORDER_VERSION_CONFLICT` 或 `ORDER_STATUS_NOT_ALLOW_ACCEPT`，前端应提示用户刷新订单列表查看最新状态。

## 十、总结

本次实现完成了订单"商户接单 M2"的核心功能，具备：

1. **并发安全**：乐观锁 + 幂等键双重保护
2. **可测试性**：完整的测试用例和验证脚本
3. **可追溯性**：审计日志 + 领域事件
4. **可维护性**：详细的中文注释 + 清晰的错误码

所有改动均遵循 DDD 分层架构，确保代码质量和可维护性。
