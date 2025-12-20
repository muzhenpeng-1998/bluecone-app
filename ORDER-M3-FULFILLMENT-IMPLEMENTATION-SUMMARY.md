# 订单履约流转 M3 实现总结

## 一、功能概述

本次实现了完整的订单履约流转 M3，包括：

1. **履约流转**：商家端制作流程 ACCEPTED → IN_PROGRESS → READY → COMPLETED
2. **未接单超时自动取消**：WAIT_ACCEPT 超时自动转为 CANCELED

所有操作均具备：
- ✅ 幂等保护（基于 requestId）
- ✅ 乐观锁并发保护（基于 expectedVersion）
- ✅ 状态流转约束（使用 OrderStatus.normalize() 统一判断）
- ✅ 审计追溯（bc_order_action_log 记录所有操作）

---

## 二、改动文件清单

### 1. 数据库迁移脚本

#### `app-infra/src/main/resources/db/migration/V20251218003__add_order_fulfillment_m3_fields.sql`
- 新增订单履约时间戳字段：
  - `started_at`：开始制作时间
  - `ready_at`：出餐完成时间
  - `completed_at`：订单完成时间
  - `last_state_changed_at`：最近状态变化时间（用于超时判断）
  - `operator_id`：最近操作人ID
- 新增索引：`idx_tenant_status_state_time`（用于超时扫描）

### 2. 领域模型层

#### `app-order/src/main/java/com/bluecone/app/order/domain/model/Order.java`
- 新增字段：`startedAt`, `readyAt`, `completedAt`, `lastStateChangedAt`, `operatorId`
- 新增方法：
  - `start(operatorId, now)`：开始制作（ACCEPTED → IN_PROGRESS）
  - `markReady(operatorId, now)`：出餐完成（IN_PROGRESS → READY）
  - `complete(operatorId, now)`：订单完成（READY → COMPLETED）
- 所有方法都包含完整的状态约束检查和中文 JavaDoc 说明

### 3. 基础设施层

#### `app-order/src/main/java/com/bluecone/app/order/infra/persistence/po/OrderPO.java`
- 新增字段：`startedAt`, `readyAt`, `completedAt`, `lastStateChangedAt`, `operatorId`

#### `app-order/src/main/java/com/bluecone/app/order/infra/persistence/converter/OrderConverter.java`
- 更新 `toDomain()` 方法：映射新增字段到领域模型
- 更新 `toPO()` 方法：映射领域模型到持久化对象

### 4. 应用服务层

#### `app-order/src/main/java/com/bluecone/app/order/application/command/StartOrderCommand.java`（新建）
- 开始制作命令，包含租户/门店/操作人/订单/请求ID/期望版本号

#### `app-order/src/main/java/com/bluecone/app/order/application/command/MarkReadyCommand.java`（新建）
- 出餐完成命令

#### `app-order/src/main/java/com/bluecone/app/order/application/command/CompleteOrderCommand.java`（新建）
- 订单完成命令

#### `app-order/src/main/java/com/bluecone/app/order/application/MerchantFulfillmentAppService.java`（新建）
- 商户履约流转应用服务接口，定义三个履约动作

#### `app-order/src/main/java/com/bluecone/app/order/application/impl/MerchantFulfillmentAppServiceImpl.java`（新建）
- 商户履约流转应用服务实现，完整实现幂等、乐观锁、事务保护
- 包含详细的中文注释说明设计思路

### 5. Job 定时任务

#### `app-order/src/main/java/com/bluecone/app/order/application/job/OrderAutoCancelJob.java`（新建）
- 未接单超时自动取消定时任务
- 每 1 分钟执行一次
- 支持配置超时时间（默认 10 分钟）
- 使用乐观锁防止并发冲突

### 6. API 接口层

#### `app-order/src/main/java/com/bluecone/app/order/api/dto/MerchantStartOrderRequest.java`（新建）
- 商户开始制作请求 DTO

#### `app-order/src/main/java/com/bluecone/app/order/api/dto/MerchantMarkReadyRequest.java`（新建）
- 商户出餐完成请求 DTO

#### `app-order/src/main/java/com/bluecone/app/order/api/dto/MerchantCompleteOrderRequest.java`（新建）
- 商户订单完成请求 DTO

#### `app-order/src/main/java/com/bluecone/app/order/controller/OrderController.java`
- 新增接口：
  - `POST /api/order/merchant/orders/{orderId}/start`：商户开始制作
  - `POST /api/order/merchant/orders/{orderId}/ready`：商户出餐完成
  - `POST /api/order/merchant/orders/{orderId}/complete`：商户订单完成

---

## 三、Flyway 脚本列表

```bash
app-infra/src/main/resources/db/migration/
├── V20251218__create_order_tables.sql               # 订单主表（已存在）
├── V20251218001__add_payment_notify_id.sql          # 支付通知ID（已存在）
├── V20251218002__add_order_acceptance_m2_fields.sql # 接单拒单字段（已存在）
└── V20251218003__add_order_fulfillment_m3_fields.sql # 履约流转字段（本次新增）
```

---

## 四、API 测试示例

### 前置条件

假设：
- 订单已创建且已支付：`orderId=1001`
- 当前订单状态：`WAIT_ACCEPT`
- 租户ID：`1`
- 门店ID：`101`
- 操作人ID：`201`

### 1. 商户接单（M2 已实现）

```bash
curl -X POST http://localhost:8080/api/order/merchant/orders/1001/accept \
  -H "Content-Type: application/json" \
  -d '{
    "tenantId": 1,
    "storeId": 101,
    "operatorId": 201,
    "requestId": "req-accept-1001-uuid-001",
    "expectedVersion": 1
  }'
```

**期望响应：**
```json
{
  "code": "200",
  "message": "success",
  "data": {
    "orderId": 1001,
    "orderNo": "ORD202512180001",
    "status": "ACCEPTED",
    "version": 2,
    "acceptedAt": "2025-12-18T10:00:00"
  }
}
```

---

### 2. 商户开始制作（M3 新增）

```bash
curl -X POST http://localhost:8080/api/order/merchant/orders/1001/start \
  -H "Content-Type: application/json" \
  -d '{
    "tenantId": 1,
    "storeId": 101,
    "operatorId": 201,
    "requestId": "req-start-1001-uuid-002",
    "expectedVersion": 2
  }'
```

**期望响应：**
```json
{
  "code": "200",
  "message": "success",
  "data": {
    "orderId": 1001,
    "orderNo": "ORD202512180001",
    "status": "IN_PROGRESS",
    "version": 3,
    "startedAt": "2025-12-18T10:05:00"
  }
}
```

---

### 3. 商户出餐完成（M3 新增）

```bash
curl -X POST http://localhost:8080/api/order/merchant/orders/1001/ready \
  -H "Content-Type: application/json" \
  -d '{
    "tenantId": 1,
    "storeId": 101,
    "operatorId": 201,
    "requestId": "req-ready-1001-uuid-003",
    "expectedVersion": 3
  }'
```

**期望响应：**
```json
{
  "code": "200",
  "message": "success",
  "data": {
    "orderId": 1001,
    "orderNo": "ORD202512180001",
    "status": "READY",
    "version": 4,
    "readyAt": "2025-12-18T10:20:00"
  }
}
```

---

### 4. 订单完成（M3 新增）

```bash
curl -X POST http://localhost:8080/api/order/merchant/orders/1001/complete \
  -H "Content-Type: application/json" \
  -d '{
    "tenantId": 1,
    "storeId": 101,
    "operatorId": 201,
    "requestId": "req-complete-1001-uuid-004",
    "expectedVersion": 4
  }'
```

**期望响应：**
```json
{
  "code": "200",
  "message": "success",
  "data": {
    "orderId": 1001,
    "orderNo": "ORD202512180001",
    "status": "COMPLETED",
    "version": 5,
    "completedAt": "2025-12-18T10:25:00"
  }
}
```

---

## 五、测试验证场景

### 场景 1：正向流转（Happy Path）

**步骤：**
1. 创建订单并支付（状态：WAIT_PAY → WAIT_ACCEPT）
2. 商户接单（WAIT_ACCEPT → ACCEPTED）
3. 商户开始制作（ACCEPTED → IN_PROGRESS）
4. 商户出餐完成（IN_PROGRESS → READY）
5. 订单完成（READY → COMPLETED）

**验证点：**
- 每次状态变更成功
- 时间戳正确记录（acceptedAt, startedAt, readyAt, completedAt）
- lastStateChangedAt 每次都更新
- version 每次递增

---

### 场景 2：幂等性测试

**步骤：**
1. 商户开始制作（requestId: req-001）
2. 再次调用开始制作（相同 requestId: req-001）

**验证点：**
- 第一次调用成功，status 变为 IN_PROGRESS，version 递增
- 第二次调用返回相同结果，version 不再递增
- bc_order_action_log 表只有一条记录（status=SUCCESS）

---

### 场景 3：并发冲突测试

**步骤：**
1. 两个操作员同时开始制作同一订单（不同 requestId，相同 expectedVersion）

**验证点：**
- 只有一个操作成功（version 递增）
- 另一个操作失败，返回版本冲突错误
- bc_order_action_log 表记录两条：一条 SUCCESS，一条 FAILED

---

### 场景 4：状态约束测试

**步骤：**
1. 订单状态为 WAIT_ACCEPT
2. 直接调用开始制作接口（跳过接单）

**验证点：**
- 调用失败，返回错误："订单状态不允许开始制作：当前状态=WAIT_ACCEPT，只允许 ACCEPTED 状态开始制作"
- bc_order_action_log 表记录一条 FAILED

---

### 场景 5：未接单超时自动取消

**步骤：**
1. 创建订单并支付（状态：WAIT_ACCEPT）
2. 修改数据库 last_state_changed_at 为 15 分钟前（模拟超时）
3. 等待 OrderAutoCancelJob 执行（每 1 分钟）
4. 查询订单状态

**验证点：**
- 订单状态变为 CANCELED
- close_reason = AUTO_CANCEL_NO_ACCEPT
- closedAt 记录取消时间

**手动触发测试：**
```bash
# 方式 1：修改配置文件，将超时时间改为 1 分钟
order.accept.timeout.minutes=1

# 方式 2：直接修改数据库模拟超时
UPDATE bc_order 
SET last_state_changed_at = NOW() - INTERVAL 15 MINUTE 
WHERE id = 1001 AND status = 'WAIT_ACCEPT';

# 方式 3：重启应用或等待定时任务执行
```

---

## 六、配置项说明

### application.yml

```yaml
order:
  accept:
    timeout:
      minutes: 10  # 接单超时时间（分钟），默认 10 分钟
```

**环境变量覆盖：**
```bash
export ORDER_ACCEPT_TIMEOUT_MINUTES=10
```

---

## 七、测试命令

### 单元测试

```bash
# 测试订单履约流转
mvn -pl app-order -am clean test -Dtest=OrderFulfillmentTest

# 测试自动取消
mvn -pl app-order -am clean test -Dtest=OrderAutoCancelTest
```

### 集成测试

```bash
# 完整流转测试
mvn -pl app-application -am clean test -Dtest=OrderFulfillmentIntegrationTest
```

---

## 八、核心设计亮点

### 1. 状态流转约束

所有状态判断都使用 `OrderStatus.normalize()` 统一处理，自动兼容历史重复语义状态（PENDING_ACCEPT、CANCELLED 等），避免硬编码比较。

```java
// ✅ 推荐：使用 normalize() 统一判断
OrderStatus canonical = this.status.normalize();
if (OrderStatus.IN_PROGRESS.equals(canonical)) {
    // ...
}

// ❌ 不推荐：直接比较，可能遗漏重复语义状态
if (OrderStatus.IN_PROGRESS.equals(this.status)) {
    // ...
}
```

### 2. 幂等保护策略

使用 `bc_order_action_log` 表的唯一索引（action_key）保证幂等：
- action_key 格式：`{tenantId}:{storeId}:{orderId}:{actionType}:{requestId}`
- 唯一键冲突时查询已有记录：
  - SUCCESS：直接返回已有结果
  - FAILED：提示用户换新的 requestId 重试
  - PROCESSING：并发冲突，拒绝请求

### 3. 乐观锁并发保护

订单更新使用版本号（order_version）防止并发冲突：

```sql
UPDATE bc_order 
SET status = ?, version = version + 1, ...
WHERE id = ? 
  AND tenant_id = ? 
  AND version = ?  -- 乐观锁
```

如果版本号不匹配，更新失败并抛出异常。

### 4. 审计追溯

每次履约操作都记录在 `bc_order_action_log` 表：
- 操作人（operatorId）
- 操作时间（createdAt）
- 执行结果（status: SUCCESS/FAILED）
- 错误信息（errorCode, errorMsg）

---

## 九、已知限制与后续优化

### 1. 事件发布（TODO）

当前履约流转未发布领域事件，后续可增加：
- OrderStartedEvent：开始制作事件
- OrderReadyEvent：出餐完成事件
- OrderCompletedEvent：订单完成事件
- OrderAutoCancelledEvent：自动取消事件

用于触发下游业务（如通知用户、推送商户端、更新统计数据等）。

### 2. 自动取消退款（TODO）

当前 OrderAutoCancelJob 只更新订单状态，未触发退款。后续需要：
- 发布 OrderAutoCancelledEvent
- 退款服务监听事件并发起退款

### 3. 性能优化（可选）

如果订单量很大，可考虑：
- 使用消息队列异步处理自动取消
- 分库分表后按 tenantId 分片扫描
- 增加 Redis 缓存减少数据库查询

---

## 十、验收标准

### ✅ 必须通过的测试

1. **正向流转测试**：ACCEPTED → IN_PROGRESS → READY → COMPLETED 全流程通过
2. **幂等性测试**：同一 requestId 重复调用，第二次不递增版本号
3. **并发冲突测试**：两个不同 requestId 同时操作，一个成功一个失败
4. **状态约束测试**：非法状态调用必须失败（如 WAIT_ACCEPT 直接开始制作）
5. **超时自动取消测试**：未接单超时订单自动变为 CANCELED

### ✅ 必须包含的文档

1. DDL 脚本（Flyway）
2. API 接口文档（curl 示例）
3. 测试用例清单
4. 配置项说明

---

## 十一、部署检查清单

### 部署前检查

- [ ] 确认 Flyway 脚本版本号正确（V20251218003）
- [ ] 确认 application.yml 包含配置项 `order.accept.timeout.minutes`
- [ ] 确认 OrderAutoCancelJob 定时任务已启用

### 部署后验证

- [ ] 数据库新增字段已生效：started_at, ready_at, completed_at, last_state_changed_at, operator_id
- [ ] 数据库索引已创建：idx_tenant_status_state_time
- [ ] API 接口可访问：/start, /ready, /complete
- [ ] 定时任务正常执行（查看日志："OrderAutoCancelJob 执行完成"）

---

## 十二、联系人与支持

如有问题，请联系订单团队：
- 技术负责人：[您的名字]
- 邮箱：[您的邮箱]
- 文档地址：本文件路径

---

**文档生成时间：** 2025-12-18  
**版本：** M3 v1.0  
**作者：** Claude (AI Assistant)
