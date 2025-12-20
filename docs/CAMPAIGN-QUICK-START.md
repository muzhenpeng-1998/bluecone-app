# 活动编排系统快速开始指南

## 快速概览

活动编排系统支持三种活动类型：

| 活动类型 | 执行通道 | 触发时机 | 奖励类型 |
|---------|---------|---------|---------|
| ORDER_DISCOUNT | 同步计价 | 订单计价时 | 订单直接减免 |
| ORDER_REBATE_COUPON | 异步消费 | ORDER_PAID 事件 | 发放优惠券 |
| RECHARGE_BONUS | 异步消费 | RECHARGE_PAID 事件 | 赠送余额 |

---

## 30秒快速上手

### 1. 创建订单满减活动

```bash
curl -X POST http://localhost:8080/admin/campaigns \
  -H "Content-Type: application/json" \
  -d '{
    "tenantId": 1,
    "campaignCode": "FIRST_ORDER_50_10",
    "campaignName": "首单满50减10",
    "campaignType": "ORDER_DISCOUNT",
    "rules": {
      "minAmount": 50.00,
      "firstOrderOnly": true,
      "discountAmount": 10.00
    },
    "scope": {"scopeType": "ALL"},
    "startTime": "2025-01-01T00:00:00",
    "priority": 100
  }'
```

### 2. 上线活动

```bash
curl -X POST "http://localhost:8080/admin/campaigns/{campaignId}/online?tenantId=1&operatorId=1"
```

### 3. 测试计价

创建订单时，计价引擎会自动应用活动：

```java
PricingRequest request = PricingRequest.builder()
    .tenantId(1L)
    .storeId(10L)
    .userId(200L)
    .items(...)
    .build();

PricingQuote quote = pricingFacade.calculatePrice(request);
// quote 中会包含 PROMO_DISCOUNT 明细行
```

---

## 活动配置示例

### 1. 订单满额立减

```json
{
  "campaignType": "ORDER_DISCOUNT",
  "rules": {
    "minAmount": 100.00,         // 满100元
    "discountAmount": 20.00      // 减20元
  },
  "scope": {"scopeType": "ALL"}
}
```

### 2. 订单完成返券

```json
{
  "campaignType": "ORDER_REBATE_COUPON",
  "rules": {
    "minAmount": 100.00,               // 满100元
    "firstOrderOnly": true,             // 限首单
    "perUserLimit": 1,                  // 每人限1次
    "couponTemplateIds": "123,456",     // 返券模板ID
    "couponQuantity": 2                 // 每个模板发2张
  },
  "scope": {
    "scopeType": "STORE",
    "storeIds": [1, 2, 3]               // 指定门店
  }
}
```

### 3. 充值赠送

```json
{
  "campaignType": "RECHARGE_BONUS",
  "rules": {
    "minAmount": 100.00,          // 满100元
    "bonusAmount": 10.00,         // 固定送10元
    "bonusRate": 0.10,            // 额外送10%
    "maxBonusAmount": 50.00       // 最高送50元
  },
  "scope": {"scopeType": "ALL"}
}
```

---

## 常用查询

### 查询活动列表

```bash
curl "http://localhost:8080/admin/campaigns?tenantId=1&campaignType=ORDER_DISCOUNT"
```

### 查询执行日志

```bash
# 查询用户执行记录
curl "http://localhost:8080/admin/campaigns/execution-logs?tenantId=1&userId=200&limit=50"

# 查询活动执行记录
curl "http://localhost:8080/admin/campaigns/execution-logs?tenantId=1&campaignId=123&limit=100"
```

### 数据库查询

```sql
-- 查询有效活动
SELECT * FROM bc_campaign 
WHERE tenant_id = 1 
  AND status = 'ONLINE'
  AND start_time <= NOW()
  AND (end_time IS NULL OR end_time > NOW())
ORDER BY priority DESC;

-- 查询用户执行历史
SELECT * FROM bc_campaign_execution_log
WHERE tenant_id = 1 AND user_id = 200
ORDER BY created_at DESC
LIMIT 10;

-- 检查幂等键
SELECT * FROM bc_campaign_execution_log
WHERE tenant_id = 1 
  AND idempotency_key = '1:ORDER_REBATE_COUPON:100:200';
```

---

## 监控指标

### Prometheus 查询

```promql
# 活动应用次数
sum(campaign_applied_total{type="ORDER_DISCOUNT"}) by (code)

# 活动执行成功率
sum(campaign_execution_success_total{type="ORDER_REBATE_COUPON"}) by (code)
/
(sum(campaign_execution_success_total{type="ORDER_REBATE_COUPON"}) by (code)
 + sum(campaign_execution_failed_total{type="ORDER_REBATE_COUPON"}) by (code))

# 活动执行失败数
sum(campaign_execution_failed_total) by (type, code, reason)
```

---

## 故障排查

### 1. 活动不生效

**检查清单**：
- [ ] 活动状态是 ONLINE？
- [ ] 当前时间在活动时间窗内？
- [ ] 订单金额满足门槛？
- [ ] 用户未超过参与次数限制？
- [ ] 活动范围匹配（门店/渠道）？

**查询 SQL**：
```sql
SELECT 
    id, campaign_code, status, start_time, end_time, priority,
    JSON_EXTRACT(rules_json, '$.minAmount') as min_amount,
    JSON_EXTRACT(scope_json, '$.scopeType') as scope_type
FROM bc_campaign
WHERE tenant_id = 1 
  AND campaign_type = 'ORDER_DISCOUNT'
ORDER BY priority DESC;
```

### 2. 活动重复执行

**检查幂等键**：
```sql
SELECT COUNT(*) as exec_count, idempotency_key
FROM bc_campaign_execution_log
WHERE tenant_id = 1 AND user_id = 200
GROUP BY idempotency_key
HAVING exec_count > 1;
```

如果有重复记录，说明幂等机制失效，检查：
- 唯一索引是否存在？
- 事务是否正确提交？

### 3. 计价金额不对

**检查计价明细**：
```java
PricingQuote quote = pricingFacade.calculatePrice(request);
for (PricingLine line : quote.getBreakdownLines()) {
    if (line.getReasonCode() == ReasonCode.PROMO_DISCOUNT) {
        System.out.println("活动折扣：" + line.getAmount() + "，活动ID：" + line.getRelatedId());
    }
}
```

### 4. 事件消费失败

**查询失败日志**：
```sql
SELECT * FROM bc_campaign_execution_log
WHERE tenant_id = 1 
  AND execution_status = 'FAILED'
  AND created_at >= DATE_SUB(NOW(), INTERVAL 1 DAY)
ORDER BY created_at DESC;
```

**查询 Outbox 事件**：
```sql
SELECT * FROM bc_outbox_event
WHERE event_type IN ('order.paid', 'recharge.paid')
  AND status IN ('NEW', 'FAILED')
ORDER BY created_at DESC
LIMIT 10;
```

---

## 最佳实践

### 1. 活动优先级设置

- 新用户活动：优先级 200
- 常规促销活动：优先级 100
- 兜底活动：优先级 0

### 2. 活动命名规范

```
{场景}_{条件}_{奖励}
例如：
- FIRST_ORDER_50_10（首单满50减10）
- RECHARGE_100_10PCT（充值满100送10%）
- ORDER_REBATE_COUPON_100（订单满100返券）
```

### 3. 活动测试流程

1. 创建活动（DRAFT 状态）
2. 测试环境验证（小范围门店）
3. 上线活动（ONLINE 状态）
4. 监控指标（执行成功率、失败原因）
5. 必要时下线调整（OFFLINE 状态）

---

## 常见问题

### Q1: 如何支持多活动同时生效？

**A**: V1 只应用第一个匹配活动（按优先级），V2 将支持多活动叠加。临时方案：
- 将多个优惠合并到一个活动规则中
- 或创建多个活动，用户手动选择

### Q2: 如何限制活动总预算？

**A**: V1 不支持预算控制，V2 将增加。临时方案：
- 设置活动结束时间
- 定期查询执行日志统计奖励金额
- 达到预算后手动下线

### Q3: 如何回滚活动？

**A**: 
1. 下线活动（不影响已执行记录）
2. 如需撤销已发放奖励：
   - 券：手动作废券（调用 CouponManagementFacade）
   - 余额：需钱包模块支持扣减接口（TODO）

### Q4: 如何查看活动效果？

**A**: 查询执行日志统计：
```sql
SELECT 
    campaign_code,
    execution_status,
    COUNT(*) as exec_count,
    SUM(reward_amount) as total_reward
FROM bc_campaign_execution_log
WHERE tenant_id = 1 
  AND campaign_id = 123
  AND executed_at >= '2025-01-01'
GROUP BY campaign_code, execution_status;
```

---

## 下一步

- 详细技术文档：`docs/CAMPAIGN-ORCHESTRATION-V1.md`
- 实现总结：`CAMPAIGN-E2-V1-IMPLEMENTATION-SUMMARY.md`
- 问题反馈：联系 BlueCone Team

---

**更新时间**: 2025-12-19
