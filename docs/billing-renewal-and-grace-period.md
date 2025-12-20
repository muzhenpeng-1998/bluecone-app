# Billing D3: 续费与到期管理

## 概述

Billing D3 在现有 Billing V1 基础上实现了完整的续费与到期管理功能，包括：

- **到期提醒任务**：提前 7/3/1/0 天自动生成提醒任务
- **一键续费**：后台管理接口支持一键生成续费账单
- **宽限期策略**：到期后进入 7 天宽限期，限制部分功能但不立即降级
- **Dunning 重试**：提醒发送支持指数退避重试（1min, 5min, 30min）
- **PlanGuard 限制**：宽限期内统一限制写操作和高成本功能
- **指标与审计**：完整的业务指标和 Dunning 日志

## 架构设计

### 订阅状态机

```
ACTIVE（生效中）
    ↓ 到期
GRACE（宽限期，7天）
    ↓ 宽限期结束
EXPIRED（已过期，降级到免费版）
```

**状态转换规则：**

1. **ACTIVE → GRACE**：订阅到期时，由 `SubscriptionExpireJob` 自动转为 GRACE 状态
2. **GRACE → ACTIVE**：宽限期内续费成功，恢复 ACTIVE 状态
3. **GRACE → EXPIRED**：宽限期结束（7天后），由 `SubscriptionExpireJob` 降级到免费版

### 提醒任务生成

**提醒时机：**

- `EXPIRE_7D`：到期前 7 天
- `EXPIRE_3D`：到期前 3 天
- `EXPIRE_1D`：到期前 1 天
- `EXPIRE_0D`：到期当天
- `GRACE_3D`：宽限期第 3 天（最后警告）

**幂等保证：**

- 提醒任务通过 `subscription_id + remind_at` 唯一索引保证幂等
- 重复生成时会跳过已存在的任务

### Dunning 重试策略

**指数退避重试：**

- 第 1 次重试：1 分钟后
- 第 2 次重试：5 分钟后
- 第 3 次重试：30 分钟后
- 重试耗尽：标记为 FAILED

**发送渠道：**

- `IN_APP`：站内通知（默认）
- `EMAIL`：邮件通知（默认）
- `SMS`：短信通知（可选，暂未实现）

## 核心组件

### 1. 数据库表

#### bc_billing_reminder_task

提醒任务表，记录每个订阅的提醒任务。

**关键字段：**

- `subscription_id + remind_at`：唯一索引，保证幂等
- `status`：PENDING/SENT/FAILED
- `retry_count`：当前重试次数
- `next_retry_at`：下次重试时间

#### bc_billing_dunning_log

Dunning 发送日志表，记录每次提醒发送的详细日志。

**关键字段：**

- `reminder_task_id`：关联提醒任务
- `notification_channel`：通知渠道
- `send_result`：SUCCESS/FAILED
- `error_message`：错误信息

### 2. 调度任务

#### ReminderScheduleJob

**职责：** 扫描即将到期的订阅，生成提醒任务

**执行频率：** 每小时执行一次

**逻辑：**

1. 扫描未来 8 天内到期的 ACTIVE 订阅
2. 为每个订阅生成 7/3/1/0 天提醒任务（幂等）
3. 扫描 GRACE 状态的订阅，生成宽限期提醒任务

#### ReminderDispatcherJob

**职责：** 扫描待发送的提醒任务，发送通知

**执行频率：** 每分钟执行一次

**逻辑：**

1. 查询 `status=PENDING` 且 `next_retry_at <= now` 的任务
2. 解析通知渠道，发送通知（站内 + 邮件）
3. 记录 Dunning 日志
4. 更新任务状态：
   - 全部成功 → SENT
   - 部分失败 → 重试（指数退避）
   - 重试耗尽 → FAILED

#### SubscriptionExpireJob

**职责：** 处理订阅到期和宽限期逻辑

**执行频率：** 每小时执行一次

**逻辑：**

1. **ACTIVE → GRACE**：扫描已到期的 ACTIVE 订阅，转为 GRACE 状态
2. **GRACE → EXPIRED**：扫描宽限期已结束的订阅（到期 + 7天），降级到免费版

### 3. 续费接口

#### POST /api/admin/billing/subscription/renew

**请求参数：**

```json
{
  "planSkuId": 123,  // 可选，不传则使用当前套餐
  "paymentChannel": "WECHAT",  // WECHAT/ALIPAY
  "idempotencyKey": "RENEW-xxx"  // 可选，不传则自动生成
}
```

**响应：**

```json
{
  "invoiceId": 456,
  "invoiceNo": "INV1234567890",
  "amountFen": 29900,
  "paymentParams": {
    "invoiceId": 456,
    "invoiceNo": "INV1234567890",
    "amountFen": 29900,
    "paymentChannel": "WECHAT"
  }
}
```

**逻辑：**

1. 查询当前订阅
2. 确定续费套餐（不传则使用当前套餐）
3. 创建续费账单（幂等）
4. 返回支付参数

### 4. PlanGuard

**职责：** 根据订阅状态限制功能访问

**限制策略：**

| 状态 | 读操作 | 写操作 | 高级功能 | 创建资源 |
|------|--------|--------|----------|----------|
| ACTIVE | ✅ | ✅ | ✅ | ✅ |
| GRACE | ✅ | ❌ | ❌ | ❌ |
| EXPIRED | ✅ | ❌ | ❌ | ❌ |

**使用方式：**

```java
// 方式1：直接调用
@Autowired
private PlanGuard planGuard;

public void createStore(Long tenantId) {
    planGuard.checkWritePermission(tenantId, "createStore");
    // ... 业务逻辑
}

// 方式2：注解方式
@RequireWritePermission("createStore")
public void createStore(Long tenantId) {
    // ... 业务逻辑
}

@RequireAdvancedFeature("多仓库管理")
public void enableMultiWarehouse(Long tenantId) {
    // ... 业务逻辑
}
```

### 5. 通知渠道

#### InAppNotificationSender

站内通知发送器（当前为简化实现，仅记录日志）

**TODO：** 集成实际的站内通知服务（如消息中心）

#### EmailNotificationSender

邮件通知发送器（当前为简化实现，仅记录日志）

**TODO：** 集成实际的邮件服务（如 SMTP 服务）

## 业务指标

### 提醒任务指标

- `billing.reminder.task.created`：提醒任务创建数量
- `billing.reminder.task.sent`：提醒任务发送成功数量
- `billing.reminder.task.failed`：提醒任务发送失败数量

### 续费指标

- `billing.renewal.invoice.created`：续费账单创建数量
- `billing.renewal.payment.success`：续费支付成功数量
- `billing.renewal.payment.failed`：续费支付失败数量

### 宽限期指标

- `billing.grace.entered`：进入宽限期数量
- `billing.grace.exited`：退出宽限期数量（续费成功）
- `billing.grace.expired`：宽限期过期数量（降级）

### 订阅状态指标

- `billing.subscription.activated`：订阅激活数量
- `billing.subscription.expired`：订阅到期数量
- `billing.subscription.downgraded`：订阅降级数量

### Dunning 指标

- `billing.dunning.log.created`：Dunning 日志创建数量
- `billing.dunning.success`：Dunning 发送成功数量
- `billing.dunning.failed`：Dunning 发送失败数量

## 测试场景

### 1. 提醒任务幂等性测试

**场景：** 重复生成提醒任务

**预期：** 相同 `subscription_id + remind_at` 的任务只会创建一次

**验证：**

```sql
SELECT COUNT(*) FROM bc_billing_reminder_task 
WHERE subscription_id = 1 AND remind_at = '2025-12-26 00:00:00';
-- 应该返回 1
```

### 2. 提醒发送重试测试

**场景：** 提醒发送失败，触发重试

**预期：** 按照指数退避策略重试（1min, 5min, 30min）

**验证：**

```sql
SELECT retry_count, next_retry_at, last_error 
FROM bc_billing_reminder_task 
WHERE id = 1;
```

### 3. 到期 → 宽限期 → 降级测试

**场景：** 订阅到期后进入宽限期，宽限期结束后降级

**步骤：**

1. 创建一个即将到期的订阅（`subscription_end_at = now + 1 hour`）
2. 等待 `SubscriptionExpireJob` 执行，验证状态变为 GRACE
3. 修改 `subscription_end_at` 为 8 天前
4. 等待 `SubscriptionExpireJob` 执行，验证状态变为 EXPIRED，套餐降级为 FREE

**验证：**

```sql
-- 步骤2后
SELECT status FROM bc_tenant_subscription WHERE id = 1;
-- 应该返回 'GRACE'

-- 步骤4后
SELECT status, current_plan_code FROM bc_tenant_subscription WHERE id = 1;
-- 应该返回 status='EXPIRED', current_plan_code='FREE'
```

### 4. 续费回调重放测试

**场景：** 支付回调重复通知

**预期：** 相同 `channel_trade_no` 的回调只会生效一次

**验证：**

```sql
SELECT COUNT(*) FROM bc_billing_invoice 
WHERE channel_trade_no = 'wx_trade_123';
-- 应该返回 1
```

### 5. PlanGuard 限制测试

**场景：** 宽限期内尝试写操作

**预期：** 抛出 `GRACE_PERIOD_WRITE_RESTRICTED` 异常

**代码：**

```java
@Test
public void testGracePeriodWriteRestricted() {
    // 设置订阅为 GRACE 状态
    subscription.setStatus("GRACE");
    subscriptionMapper.updateById(subscription);
    
    // 尝试写操作
    assertThrows(BizException.class, () -> {
        planGuard.checkWritePermission(tenantId, "createStore");
    });
}
```

## 配置项

### 宽限期天数

默认：7 天

修改位置：`SubscriptionExpireJob.GRACE_PERIOD_DAYS`

### 提醒任务最大重试次数

默认：3 次

修改位置：`ReminderScheduleJob.createReminderTask()` 中的 `maxRetryCount`

### 重试间隔

默认：1min, 5min, 30min

修改位置：`ReminderDispatcherJob.RETRY_INTERVALS_MINUTES`

### 通知渠道

默认：IN_APP + EMAIL

修改位置：`ReminderScheduleJob.createReminderTask()` 中的 `notificationChannels`

## 运维建议

### 1. 监控告警

**关键指标：**

- `billing.reminder.task.failed`：提醒发送失败率 > 10% 告警
- `billing.grace.expired`：宽限期过期数量异常增长告警
- `billing.dunning.failed`：Dunning 发送失败率 > 20% 告警

### 2. 日志查询

**查询提醒任务：**

```sql
-- 查询待发送的提醒任务
SELECT * FROM bc_billing_reminder_task 
WHERE status = 'PENDING' 
ORDER BY next_retry_at ASC 
LIMIT 10;

-- 查询发送失败的提醒任务
SELECT * FROM bc_billing_reminder_task 
WHERE status = 'FAILED' 
ORDER BY updated_at DESC 
LIMIT 10;
```

**查询 Dunning 日志：**

```sql
-- 查询某个租户的 Dunning 日志
SELECT * FROM bc_billing_dunning_log 
WHERE tenant_id = 1 
ORDER BY created_at DESC 
LIMIT 10;

-- 统计 Dunning 发送成功率
SELECT 
    notification_channel,
    COUNT(*) as total,
    SUM(CASE WHEN send_result = 'SUCCESS' THEN 1 ELSE 0 END) as success_count,
    ROUND(SUM(CASE WHEN send_result = 'SUCCESS' THEN 1 ELSE 0 END) * 100.0 / COUNT(*), 2) as success_rate
FROM bc_billing_dunning_log 
WHERE created_at >= DATE_SUB(NOW(), INTERVAL 7 DAY)
GROUP BY notification_channel;
```

### 3. 手动干预

**手动重试提醒任务：**

```sql
-- 重置失败的提醒任务，使其重新发送
UPDATE bc_billing_reminder_task 
SET status = 'PENDING', 
    retry_count = 0, 
    next_retry_at = NOW() 
WHERE id = 1;
```

**手动延长宽限期：**

```sql
-- 将订阅从 GRACE 恢复为 ACTIVE，延长宽限期
UPDATE bc_tenant_subscription 
SET status = 'ACTIVE', 
    subscription_end_at = DATE_ADD(subscription_end_at, INTERVAL 7 DAY) 
WHERE id = 1;
```

## 未来优化

### 1. 短信通知

当前短信通知暂未实现，未来可以集成短信服务（如阿里云短信、腾讯云短信）。

### 2. 站内通知集成

当前站内通知为简化实现，未来应该集成实际的消息中心服务。

### 3. 邮件服务集成

当前邮件通知为简化实现，未来应该集成实际的 SMTP 服务或第三方邮件服务。

### 4. 动态配置

将宽限期天数、重试次数、通知渠道等配置项移到配置中心，支持动态调整。

### 5. 提醒模板

支持自定义提醒模板，允许运营人员配置提醒文案。

### 6. A/B 测试

支持不同的提醒策略 A/B 测试，优化续费转化率。

## 总结

Billing D3 实现了完整的续费与到期管理功能，通过提醒任务、宽限期策略、PlanGuard 限制等机制，有效提升了用户续费体验，同时保证了系统的稳定性和可维护性。

所有关键操作都保证了幂等性，支持重试和审计，为后续的业务优化和运营分析提供了坚实的基础。
