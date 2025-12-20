# 活动编排系统 V1 - 技术文档

## 目录

1. [系统概述](#系统概述)
2. [活动类型与执行通道](#活动类型与执行通道)
3. [幂等键规则](#幂等键规则)
4. [数据库设计](#数据库设计)
5. [API 接口](#api-接口)
6. [计价集成](#计价集成)
7. [事件消费](#事件消费)
8. [观测指标](#观测指标)
9. [测试清单](#测试清单)

---

## 系统概述

活动编排系统提供可配置的营销活动能力，支持：

- **同步计价类活动**：在订单计价阶段直接减免金额（无副作用）
- **异步结算类活动**：通过事件消费者发放奖励（返券、赠送）

核心设计原则：

1. **计价阶段无副作用**：PromoStage 只输出 PricingLine，不发券、不改余额
2. **异步执行幂等性**：`bc_campaign_execution_log` 唯一约束兜底
3. **租户隔离**：所有查询强制 `tenant_id` 过滤
4. **活动优先级**：多活动按 `priority DESC` 排序，取第一个匹配

---

## 活动类型与执行通道

### 1. ORDER_DISCOUNT（订单满额立减）

**执行通道**：同步计价阶段（PromoStage）

**触发时机**：订单计价时

**执行逻辑**：
1. 查询可用活动（`CampaignQueryService.queryAvailableCampaigns`）
2. 取第一个匹配活动（按优先级排序）
3. 计算折扣金额
4. 输出 `PricingLine(reasonCode=PROMO_DISCOUNT)`
5. 更新 `context.promoDiscountAmount`

**规则字段**：
```json
{
  "minAmount": 50.00,           // 最低订单金额门槛
  "firstOrderOnly": false,       // 是否限首单
  "perUserLimit": null,          // 每用户参与次数限制
  "discountAmount": 10.00,       // 固定满减金额
  "discountRate": 0.85,          // 折扣率（85折）
  "maxDiscountAmount": 20.00     // 最高优惠封顶
}
```

**示例**：
- 满50减10
- 满100享85折（最高优惠20元）

---

### 2. ORDER_REBATE_COUPON（订单完成返券）

**执行通道**：异步事件消费（CampaignEventConsumer）

**触发时机**：订单支付成功（ORDER_PAID 事件）

**执行逻辑**：
1. 消费 `ORDER_PAID` 事件
2. 查询可用返券活动
3. 为每个活动执行：
   - 幂等检查（`idempotency_key`）
   - 调用 `CouponGrantFacade.grantCoupon`
   - 写入执行日志
   - 调用 `NotifyFacade` 发通知（TODO）

**规则字段**：
```json
{
  "minAmount": 100.00,           // 最低订单金额门槛
  "firstOrderOnly": true,        // 限首单
  "perUserLimit": 1,             // 每用户限1次
  "couponTemplateIds": "123,456", // 券模板ID列表（逗号分隔）
  "couponQuantity": 2             // 每个模板发放数量
}
```

**幂等键格式**：
```
{tenantId}:ORDER_REBATE_COUPON:{orderId}:{userId}
```

**示例**：
- 首单满100返2张满50减10券
- 完成订单返1张8折券

---

### 3. RECHARGE_BONUS（充值赠送）

**执行通道**：异步事件消费（CampaignEventConsumer）

**触发时机**：充值支付成功（RECHARGE_PAID 事件）

**执行逻辑**：
1. 消费 `RECHARGE_PAID` 事件
2. 查询可用充值赠送活动
3. 为每个活动执行：
   - 幂等检查（`idempotency_key`）
   - 计算赠送金额
   - 调用 Wallet CREDIT 接口（TODO：需钱包模块提供 `addCredit` 接口）
   - 写入执行日志
   - 调用 `NotifyFacade` 发通知（TODO）

**规则字段**：
```json
{
  "minAmount": 100.00,           // 最低充值金额门槛
  "firstOrderOnly": true,        // 限首次充值
  "perUserLimit": 1,             // 每用户限1次
  "bonusAmount": 10.00,          // 固定赠送金额
  "bonusRate": 0.10,             // 赠送比例（10%）
  "maxBonusAmount": 50.00        // 最高赠送封顶
}
```

**赠送金额计算**：
```
bonus = bonusAmount + (rechargeAmount × bonusRate)
bonus = min(bonus, maxBonusAmount)
```

**幂等键格式**：
```
{tenantId}:RECHARGE_BONUS:{rechargeId}:{userId}
```

**示例**：
- 首次充值满100送10元
- 充值满500享10%赠送（最高送50元）

---

## 幂等键规则

### 格式设计

```
{tenantId}:{campaignType}:{bizOrderId}:{userId}
```

**组成部分**：
1. `tenantId`：租户ID（隔离）
2. `campaignType`：活动类型（区分不同类型活动）
3. `bizOrderId`：业务单ID（订单ID/充值ID）
4. `userId`：用户ID（区分同一订单不同用户场景，虽然当前不适用）

### 幂等保证机制

1. **数据库唯一约束**：
   ```sql
   UNIQUE KEY uk_tenant_idempotency (tenant_id, idempotency_key)
   ```

2. **执行流程**：
   ```
   1. 查询 execution_log by idempotencyKey
   2. if exists → 返回已有结果（幂等）
   3. else → 创建执行日志 → 执行活动逻辑 → 更新日志状态
   4. catch DuplicateKeyException → 查询并返回已有结果
   ```

3. **并发处理**：
   - 同一幂等键并发请求，只有一个能插入成功
   - 其他请求捕获 `DuplicateKeyException` 后查询已有记录返回

### 示例

**订单返券**：
```
tenant_id=1, order_id=100, user_id=200
幂等键：1:ORDER_REBATE_COUPON:100:200
```

**充值赠送**：
```
tenant_id=1, recharge_id=300, user_id=200
幂等键：1:RECHARGE_BONUS:300:200
```

---

## 数据库设计

### bc_campaign（活动配置表）

```sql
CREATE TABLE bc_campaign (
    id BIGINT PRIMARY KEY,
    tenant_id BIGINT NOT NULL,
    campaign_code VARCHAR(64) NOT NULL,     -- 活动编码（租户内唯一）
    campaign_name VARCHAR(128) NOT NULL,    -- 活动名称
    campaign_type VARCHAR(32) NOT NULL,     -- 活动类型
    status VARCHAR(32) NOT NULL,            -- 状态：DRAFT/ONLINE/OFFLINE/EXPIRED
    rules_json TEXT NOT NULL,               -- 活动规则（JSON）
    scope_json TEXT NOT NULL,               -- 适用范围（JSON）
    start_time DATETIME NOT NULL,           -- 开始时间
    end_time DATETIME DEFAULT NULL,         -- 结束时间（NULL=长期有效）
    priority INT NOT NULL DEFAULT 0,        -- 优先级（数字越大越优先）
    description TEXT,
    created_at DATETIME(3),
    updated_at DATETIME(3),
    UNIQUE KEY uk_tenant_code (tenant_id, campaign_code),
    INDEX idx_tenant_type_status (tenant_id, campaign_type, status),
    INDEX idx_time_range (start_time, end_time),
    INDEX idx_priority (priority)
);
```

**活动范围 scope_json**：
```json
{
  "scopeType": "STORE",          // ALL-全部, STORE-指定门店, CHANNEL-指定渠道
  "storeIds": [1, 2, 3],         // 门店ID列表（scopeType=STORE时）
  "channels": ["MINI_PROGRAM"]   // 渠道列表（scopeType=CHANNEL时，预留）
}
```

---

### bc_campaign_execution_log（执行日志表）

```sql
CREATE TABLE bc_campaign_execution_log (
    id BIGINT PRIMARY KEY,
    tenant_id BIGINT NOT NULL,
    campaign_id BIGINT NOT NULL,
    campaign_code VARCHAR(64) NOT NULL,
    campaign_type VARCHAR(32) NOT NULL,
    idempotency_key VARCHAR(256) NOT NULL,  -- 幂等键
    user_id BIGINT NOT NULL,
    biz_order_id BIGINT NOT NULL,           -- 业务单ID
    biz_order_no VARCHAR(64),               -- 业务单号
    biz_amount DECIMAL(10, 2) NOT NULL,     -- 业务金额
    execution_status VARCHAR(32) NOT NULL,  -- SUCCESS/FAILED/SKIPPED
    reward_amount DECIMAL(10, 2),           -- 奖励金额
    reward_result_id VARCHAR(256),          -- 奖励结果ID（券ID/流水ID）
    failure_reason TEXT,                    -- 失败原因
    executed_at DATETIME,                   -- 执行时间
    created_at DATETIME(3),
    updated_at DATETIME(3),
    UNIQUE KEY uk_tenant_idempotency (tenant_id, idempotency_key),
    INDEX idx_tenant_campaign (tenant_id, campaign_id),
    INDEX idx_tenant_user (tenant_id, user_id),
    INDEX idx_biz_order (biz_order_id),
    INDEX idx_execution_status (execution_status),
    INDEX idx_executed_at (executed_at)
);
```

---

## API 接口

### 后台管理接口

#### 1. 创建活动

```http
POST /admin/campaigns
Content-Type: application/json

{
  "tenantId": 1,
  "campaignCode": "FIRST_ORDER_DISCOUNT",
  "campaignName": "首单立减10元",
  "campaignType": "ORDER_DISCOUNT",
  "rules": {
    "minAmount": 50.00,
    "firstOrderOnly": true,
    "discountAmount": 10.00
  },
  "scope": {
    "scopeType": "ALL"
  },
  "startTime": "2025-01-01T00:00:00",
  "endTime": null,
  "priority": 100,
  "description": "新用户首单立减活动"
}
```

**响应**：
```json
{
  "code": 200,
  "data": 1234567890  // campaignId
}
```

---

#### 2. 更新活动

```http
PUT /admin/campaigns/{campaignId}?tenantId=1
Content-Type: application/json

{
  "campaignName": "首单立减15元",
  "rules": {
    "minAmount": 50.00,
    "discountAmount": 15.00
  }
}
```

---

#### 3. 上线活动

```http
POST /admin/campaigns/{campaignId}/online?tenantId=1&operatorId=100
```

---

#### 4. 下线活动

```http
POST /admin/campaigns/{campaignId}/offline?tenantId=1&operatorId=100
```

---

#### 5. 查询活动列表

```http
GET /admin/campaigns?tenantId=1&campaignType=ORDER_DISCOUNT
```

**响应**：
```json
{
  "code": 200,
  "data": [
    {
      "id": 1234567890,
      "tenantId": 1,
      "campaignCode": "FIRST_ORDER_DISCOUNT",
      "campaignName": "首单立减10元",
      "campaignType": "ORDER_DISCOUNT",
      "status": "ONLINE",
      "rules": {...},
      "scope": {...},
      "startTime": "2025-01-01T00:00:00",
      "endTime": null,
      "priority": 100,
      "createdAt": "2025-12-19T10:00:00"
    }
  ]
}
```

---

#### 6. 查询执行日志

```http
GET /admin/campaigns/execution-logs?tenantId=1&campaignId=123&userId=200&limit=100
```

**响应**：
```json
{
  "code": 200,
  "data": [
    {
      "id": 9876543210,
      "tenantId": 1,
      "campaignId": 123,
      "campaignCode": "FIRST_ORDER_REBATE",
      "campaignType": "ORDER_REBATE_COUPON",
      "idempotencyKey": "1:ORDER_REBATE_COUPON:100:200",
      "userId": 200,
      "bizOrderId": 100,
      "bizOrderNo": "ORD20251219001",
      "bizAmount": 150.00,
      "executionStatus": "SUCCESS",
      "rewardAmount": 0,
      "rewardResultId": "5001,5002",
      "executedAt": "2025-12-19T12:00:00",
      "createdAt": "2025-12-19T12:00:00"
    }
  ]
}
```

---

## 计价集成

### PromoStage 实现

```java
@Component
public class PromoStage implements PricingStage {
    
    @Autowired(required = false)
    private CampaignQueryFacade campaignQueryFacade;
    
    @Override
    public void execute(PricingContext context) {
        // 1. 查询可用活动
        CampaignQueryContext queryContext = CampaignQueryContext.builder()
                .tenantId(context.getRequest().getTenantId())
                .campaignType(CampaignType.ORDER_DISCOUNT)
                .storeId(context.getRequest().getStoreId())
                .userId(context.getRequest().getUserId())
                .amount(context.getCurrentAmount())
                .build();
        
        List<CampaignDTO> campaigns = campaignQueryFacade.queryAvailableCampaigns(queryContext);
        
        if (campaigns.isEmpty()) return;
        
        // 2. 应用第一个匹配的活动
        CampaignDTO campaign = campaigns.get(0);
        BigDecimal discount = calculateDiscount(context.getCurrentAmount(), campaign);
        
        // 3. 输出 PricingLine
        PricingLine line = new PricingLine(
                ReasonCode.PROMO_DISCOUNT,
                campaign.getCampaignName(),
                discount.negate(),
                campaign.getId(),
                "CAMPAIGN"
        );
        context.addBreakdownLine(line);
        context.setPromoDiscountAmount(discount);
        context.subtractAmount(discount);
    }
}
```

### 活动匹配逻辑

```java
public List<Campaign> queryAvailableCampaigns(CampaignQueryContext context) {
    // 1. 查询所有有效活动（按优先级排序）
    List<Campaign> campaigns = campaignRepository.findAvailableCampaigns(
            context.getTenantId(),
            context.getCampaignType(),
            CampaignStatus.ONLINE,
            context.getQueryTime()
    );
    
    // 2. 过滤适用范围和用户参与次数
    return campaigns.stream()
            .filter(campaign -> matchScope(campaign, context))
            .filter(campaign -> matchUserLimit(campaign, context))
            .filter(campaign -> matchAmountThreshold(campaign, context))
            .collect(Collectors.toList());
}
```

---

## 事件消费

### CampaignEventConsumer

```java
@Component
public class CampaignEventConsumer implements OutboxEventHandler {
    
    @Override
    public boolean supports(OutboxEventDO event) {
        return EventType.ORDER_PAID.getCode().equals(event.getEventType()) 
                || EventType.RECHARGE_PAID.getCode().equals(event.getEventType());
    }
    
    @Override
    public void handle(OutboxEventDO event) throws Exception {
        if (EventType.ORDER_PAID.getCode().equals(eventType)) {
            handleOrderPaid(event);
        } else if (EventType.RECHARGE_PAID.getCode().equals(eventType)) {
            handleRechargePaid(event);
        }
    }
}
```

### 事件载荷格式

**ORDER_PAID**：
```json
{
  "tenantId": 1,
  "userId": 200,
  "orderId": 100,
  "orderNo": "ORD20251219001",
  "orderAmount": 150.00,
  "storeId": 10
}
```

**RECHARGE_PAID**：
```json
{
  "tenantId": 1,
  "userId": 200,
  "rechargeId": 300,
  "rechargeNo": "RCH20251219001",
  "rechargeAmount": 500.00
}
```

---

## 观测指标

### Micrometer Metrics

#### 1. 活动应用次数（计价阶段）

```
campaign.applied.total{type="ORDER_DISCOUNT", code="FIRST_ORDER_DISCOUNT"}
```

#### 2. 活动执行成功

```
campaign.execution.success.total{type="ORDER_REBATE_COUPON", code="FIRST_ORDER_REBATE"}
```

#### 3. 活动执行失败

```
campaign.execution.failed.total{type="RECHARGE_BONUS", code="RECHARGE_BONUS_10", reason="BusinessException"}
```

#### 4. 活动跳过

```
campaign.execution.skipped.total{type="ORDER_REBATE_COUPON", code="FIRST_ORDER_REBATE", reason="AmountThreshold"}
```

---

## 测试清单

### 1. 幂等性测试

**测试场景**：同一订单重复消费 ORDER_PAID 事件

**预期结果**：
- 第一次执行：发券成功，写入执行日志
- 第二次执行：幂等返回，不重复发券

**验证 SQL**：
```sql
SELECT COUNT(*) FROM bc_campaign_execution_log 
WHERE idempotency_key = '1:ORDER_REBATE_COUPON:100:200';
-- 应该返回 1
```

---

### 2. 计价确定性测试

**测试场景**：同一订单、同一活动，多次计价

**预期结果**：
- 每次计价结果完全一致
- `promoDiscountAmount` 相同
- `PricingLine` 金额相同

---

### 3. 活动时间窗测试

**测试场景**：
1. 活动开始前查询 → 不匹配
2. 活动进行中查询 → 匹配
3. 活动结束后查询 → 不匹配

---

### 4. 活动上下线测试

**测试场景**：
1. 活动状态为 DRAFT → 不匹配
2. 活动上线（ONLINE）→ 匹配
3. 活动下线（OFFLINE）→ 不匹配

---

### 5. 多活动优先级测试

**测试场景**：
1. 创建两个活动：优先级100（满50减10），优先级200（满100减20）
2. 订单金额150元

**预期结果**：
- 应用优先级200的活动（满100减20）
- 不应用优先级100的活动

---

### 6. 用户参与次数限制测试

**测试场景**：
1. 活动规则：`perUserLimit=1`
2. 用户首次参与 → 执行成功
3. 用户再次参与 → 跳过（SKIPPED）

**验证 SQL**：
```sql
SELECT COUNT(*) FROM bc_campaign_execution_log 
WHERE tenant_id = 1 AND campaign_id = 123 AND user_id = 200 AND execution_status = 'SUCCESS';
-- 应该返回 1
```

---

## 已完成优化（2025-12-19 更新）

### V1.1 完成功能

1. **✅ 钱包赠送接口**：
   - 已实现 `WalletAssetFacade.credit()` 和 `WalletDomainService.credit()` 方法
   - 充值赠送活动（RECHARGE_BONUS）已完整接入钱包赠送
   - 支持幂等性和账本化，业务类型：`CAMPAIGN_BONUS`
   - 赠送失败会抛出异常，记录到执行日志

2. **✅ 通知集成**：
   - 已集成 `NotificationFacade`
   - 活动执行成功后自动发送通知
   - 支持模板：`CAMPAIGN_COUPON_REBATE`（返券通知）、`CAMPAIGN_RECHARGE_BONUS`（赠送通知）
   - 通知失败不影响活动执行，只记录日志

3. **✅ 活动删除**：
   - 已实现逻辑删除功能
   - 只能删除 DRAFT 或 OFFLINE 状态的活动
   - ONLINE 状态的活动需要先下线才能删除

4. **✅ 计价指标**：
   - PromoStage 新增 `campaign.applied.total{type, code}` 指标
   - 每次活动应用到计价时自动记录

### 待优化事项（V2）

1. **活动叠加**：
   - 支持多活动同时生效（当前只应用优先级最高的活动）
   
2. **活动互斥**：
   - 支持活动互斥规则配置（不同活动间的互斥关系）

3. **高级范围过滤**：
   - 支持商品/分类范围过滤（当前只支持门店和全局范围）
   - 支持渠道过滤完善（CHANNEL scope 预留但未实现）

4. **活动预算**：
   - 支持活动总预算控制（防止超支）
   - 支持单用户预算控制（每用户最高奖励金额）

5. **首单判断**：
   - 当前 `firstOrderOnly` 规则暂未实现
   - 需要接入订单统计服务判断是否为首单

---

## 附录

### 活动状态流转图

```
DRAFT → ONLINE → OFFLINE
  ↓       ↓
  ↓    EXPIRED
  ↓       ↓
  └───→ (删除)
```

### 执行流程图

```
事件消费者
  ↓
查询可用活动
  ↓
幂等检查 → 已存在？→ 返回已有结果
  ↓
执行活动逻辑
  ↓
写入执行日志
  ↓
发送通知（TODO）
```

---

## 更新日志

### V1.1 (2025-12-19)
- ✅ 完成钱包赠送接口集成
- ✅ 完成通知系统集成
- ✅ 完成活动删除功能
- ✅ 完成计价阶段指标记录
- ✅ 系统已具备完整生产环境能力

### V1.0 (2025-12-19)
- ✅ 初始实现：三种活动类型
- ✅ 计价集成（PromoStage）
- ✅ 事件消费者（ORDER_PAID、RECHARGE_PAID）
- ✅ 后台管理接口
- ✅ 幂等性和观测指标

---

**文档版本**: V1.1  
**更新时间**: 2025-12-19  
**维护人**: BlueCone Team
