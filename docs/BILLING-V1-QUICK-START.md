# 订阅计费 Billing V1 快速开始

## 快速开始指南

### 1. 编译项目

```bash
cd /Users/zhenpengmu/Desktop/code/project/bluecone-app
mvn clean install -DskipTests
```

### 2. 运行数据库迁移

迁移脚本会自动执行（Flyway）：
- `V20251219007__create_billing_tables.sql`

检查迁移状态：
```bash
mvn flyway:info
```

### 3. 验证表结构

登录数据库，检查以下表是否创建成功：
```sql
SHOW TABLES LIKE 'bc_plan_sku';
SHOW TABLES LIKE 'bc_billing_invoice';
SHOW TABLES LIKE 'bc_tenant_subscription';

-- 检查初始数据
SELECT * FROM bc_plan_sku;
```

应该看到 7 条套餐 SKU 记录（FREE、BASIC、PRO、ENTERPRISE 的不同周期）。

### 4. 启动应用

```bash
java -jar app-application/target/app-application-1.0.0-SNAPSHOT.jar
```

### 5. 测试 API

#### 5.1 获取套餐列表

```bash
curl -X GET http://localhost:8080/api/admin/billing/plans \
  -H "X-Tenant-Id: 1001"
```

#### 5.2 创建账单

```bash
curl -X POST http://localhost:8080/api/admin/billing/invoices \
  -H "X-Tenant-Id: 1001" \
  -H "Content-Type: application/json" \
  -d '{
    "planSkuId": 2,
    "paymentChannel": "WECHAT",
    "idempotencyKey": "test-invoice-001"
  }'
```

#### 5.3 查询账单列表

```bash
curl -X GET "http://localhost:8080/api/admin/billing/invoices?pageNum=1&pageSize=20" \
  -H "X-Tenant-Id: 1001"
```

#### 5.4 查询当前订阅

```bash
curl -X GET http://localhost:8080/api/admin/billing/subscription \
  -H "X-Tenant-Id: 1001"
```

### 6. 模拟支付回调（开发环境）

在开发环境，你可以直接调用 `BillingPaymentCallbackService` 来模拟支付成功：

```java
@Autowired
private BillingPaymentCallbackService billingPaymentCallbackService;

// 模拟支付成功
Long invoiceId = 1L;
String channelTradeNo = "WX-TEST-" + System.currentTimeMillis();
Long paidAmountFen = 9900L;
LocalDateTime paidAt = LocalDateTime.now();

billingPaymentCallbackService.handleInvoicePaid(invoiceId, channelTradeNo, paidAmountFen, paidAt);
```

或者直接更新数据库：

```sql
-- 模拟支付成功
UPDATE bc_billing_invoice 
SET status = 'PAID',
    channel_trade_no = 'WX-TEST-12345',
    paid_amount_fen = 9900,
    paid_at = NOW(),
    effective_start_at = NOW(),
    effective_end_at = DATE_ADD(NOW(), INTERVAL 1 MONTH)
WHERE id = 1;

-- 手动写入 Outbox 事件
INSERT INTO bc_outbox_event (
    tenant_id, aggregate_type, aggregate_id, event_type, event_id, 
    event_payload, status, next_retry_at
) VALUES (
    1001, 'INVOICE', '1', 'invoice.paid', UUID(),
    '{"invoiceId": 1, "tenantId": 1001, "planSkuId": 2, "planCode": "BASIC"}',
    'NEW', NOW()
);
```

### 7. 验证订阅生效

查询订阅表：

```sql
SELECT * FROM bc_tenant_subscription WHERE tenant_id = 1001;
```

应该看到订阅记录已创建，状态为 `ACTIVE`。

### 8. 测试定时任务

#### 8.1 测试到期降级

```sql
-- 创建一个已过期的订阅
INSERT INTO bc_tenant_subscription (
    tenant_id, current_plan_code, current_plan_name, current_plan_level,
    current_features, subscription_start_at, subscription_end_at, status
) VALUES (
    2001, 'BASIC', '基础版', 1,
    '{"maxStores": 3, "maxUsers": 5}',
    DATE_SUB(NOW(), INTERVAL 2 MONTH),
    DATE_SUB(NOW(), INTERVAL 1 DAY),
    'ACTIVE'
);

-- 等待 SubscriptionExpireJob 执行（每小时一次）
-- 或者手动触发（在代码中调用）
```

验证降级：

```sql
SELECT * FROM bc_tenant_subscription WHERE tenant_id = 2001;
-- 应该看到 current_plan_code = 'FREE', status = 'EXPIRED'
```

#### 8.2 测试对账补偿

```sql
-- 创建一个已支付但未生效的账单
INSERT INTO bc_billing_invoice (
    tenant_id, invoice_no, idempotency_key, plan_sku_id, plan_code, plan_name,
    billing_period, period_months, amount_fen, paid_amount_fen, payment_channel,
    channel_trade_no, paid_at, status, effective_start_at, effective_end_at
) VALUES (
    3001, 'INV-TEST-001', 'test-reconcile-001', 2, 'BASIC', '基础版',
    'MONTHLY', 1, 9900, 9900, 'WECHAT',
    'WX-TEST-001', NOW(), 'PAID', NOW(), DATE_ADD(NOW(), INTERVAL 1 MONTH)
);

-- 等待 BillingReconcileJob 执行（每 30 分钟一次）
-- 应该会补发 INVOICE_PAID 事件，订阅最终生效
```

### 9. 查看日志

关键日志关键字：
- `[billing-callback]`: 支付回调处理
- `[billing-consumer]`: 订阅激活消费
- `[billing-reconcile-job]`: 对账补偿任务
- `[subscription-expire-job]`: 到期降级任务
- `[subscription-plan]`: 配额查询

### 10. 常见问题排查

#### Q1: 账单创建失败
检查：
- `bc_plan_sku` 表是否有数据
- `planSkuId` 是否存在
- `idempotencyKey` 是否重复

#### Q2: 支付回调后订阅未生效
检查：
1. `bc_outbox_event` 表是否有 `INVOICE_PAID` 事件
2. 事件状态是否为 `SENT`
3. `bc_event_consume_log` 表是否有消费记录
4. 日志中是否有 `[billing-consumer]` 相关错误

#### Q3: 定时任务未执行
检查：
1. `@EnableScheduling` 注解是否生效
2. 日志中是否有定时任务执行记录
3. 应用配置中 `spring.task.scheduling.enabled` 是否为 `true`

---

## 下一步

1. **集成真实支付**: 对接微信/支付宝支付 SDK，生成真实的预支付参数
2. **PlanGuard 集成**: 将现有的 PlanGuard 改为从 `SubscriptionPlanService` 读取配额
3. **初始化订阅**: 为现有租户初始化免费版订阅记录
4. **监控告警**: 配置关键指标的监控和告警

---

## 参考文档

- [状态机与对账补偿说明](./billing-v1-state-machine-and-reconciliation.md)
- [实现总结](./BILLING-V1-IMPLEMENTATION-SUMMARY.md)

---

**最后更新**: 2025-12-19
