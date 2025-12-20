# 活动编排系统测试指南

## 目录

1. [单元测试](#单元测试)
2. [集成测试](#集成测试)
3. [手动测试场景](#手动测试场景)
4. [性能测试](#性能测试)

---

## 单元测试

### 1. 幂等性测试

**测试目标**: 验证活动执行的幂等性保证

**测试类**: `CampaignIdempotencyTest`

**测试场景**:

```java
@Test
void testExecutionIdempotency() {
    // 给定：同一订单、同一用户、同一活动
    Long tenantId = 1L;
    Long userId = 100L;
    Long orderId = 200L;
    BigDecimal orderAmount = new BigDecimal("150.00");
    
    // 当：第一次执行
    ExecutionLog log1 = campaignExecutionService.executeCampaign(
        campaign, tenantId, userId, orderId, "ORD001", orderAmount
    );
    
    // 当：第二次执行（幂等重放）
    ExecutionLog log2 = campaignExecutionService.executeCampaign(
        campaign, tenantId, userId, orderId, "ORD001", orderAmount
    );
    
    // 则：返回相同的执行日志
    assertThat(log1.getId()).isEqualTo(log2.getId());
    assertThat(log1.getRewardResultId()).isEqualTo(log2.getRewardResultId());
    
    // 则：数据库中只有一条记录
    int count = executionLogRepository.countByIdempotencyKey(
        tenantId, "1:ORDER_REBATE_COUPON:200:100"
    );
    assertThat(count).isEqualTo(1);
}
```

**验证点**:
- ✅ 幂等键格式正确
- ✅ 重复执行返回相同结果
- ✅ 数据库中只有一条记录
- ✅ 不重复发券/赠送

---

### 2. 计价确定性测试

**测试目标**: 验证订单满减计价的确定性

**测试类**: `CampaignPricingDeterministicTest`

**测试场景**:

```java
@Test
void testOrderDiscountDeterministic() {
    // 给定：固定满减活动（满100减20）
    CampaignRulesDTO rules = CampaignRulesDTO.builder()
        .minAmount(new BigDecimal("100.00"))
        .discountAmount(new BigDecimal("20.00"))
        .build();
    
    BigDecimal orderAmount = new BigDecimal("150.00");
    
    // 当：多次计算折扣
    BigDecimal discount1 = calculateDiscount(orderAmount, rules);
    BigDecimal discount2 = calculateDiscount(orderAmount, rules);
    BigDecimal discount3 = calculateDiscount(orderAmount, rules);
    
    // 则：每次计算结果完全一致
    assertThat(discount1).isEqualByComparingTo(new BigDecimal("20.00"));
    assertThat(discount2).isEqualByComparingTo(discount1);
    assertThat(discount3).isEqualByComparingTo(discount1);
}

@Test
void testOrderDiscountWithRate() {
    // 给定：折扣率活动（85折，最高优惠20元）
    CampaignRulesDTO rules = CampaignRulesDTO.builder()
        .discountRate(new BigDecimal("0.85"))
        .maxDiscountAmount(new BigDecimal("20.00"))
        .build();
    
    // 场景1：订单金额100元
    BigDecimal discount1 = calculateDiscount(new BigDecimal("100.00"), rules);
    assertThat(discount1).isEqualByComparingTo(new BigDecimal("15.00")); // 100 * (1-0.85) = 15
    
    // 场景2：订单金额200元（触发封顶）
    BigDecimal discount2 = calculateDiscount(new BigDecimal("200.00"), rules);
    assertThat(discount2).isEqualByComparingTo(new BigDecimal("20.00")); // 触发封顶
}

@Test
void testOrderDiscountNotExceedOrderAmount() {
    // 给定：满减50元的活动
    CampaignRulesDTO rules = CampaignRulesDTO.builder()
        .discountAmount(new BigDecimal("50.00"))
        .build();
    
    // 当：订单金额只有30元
    BigDecimal discount = calculateDiscount(new BigDecimal("30.00"), rules);
    
    // 则：优惠金额不超过订单金额
    assertThat(discount).isEqualByComparingTo(new BigDecimal("30.00"));
}
```

**验证点**:
- ✅ 固定满减金额计算正确
- ✅ 折扣率计算正确
- ✅ 封顶逻辑生效
- ✅ 优惠不超过订单金额
- ✅ 多次计算结果一致

---

## 集成测试

### 1. 完整活动流程测试

**测试目标**: 验证活动从创建到执行的完整流程

**测试场景**:

```java
@SpringBootTest
@Transactional
class CampaignIntegrationTest {
    
    @Autowired
    private CampaignManagementFacade campaignManagementFacade;
    
    @Autowired
    private CampaignEventConsumer campaignEventConsumer;
    
    @Test
    void testFullCampaignLifecycle() {
        // 1. 创建活动
        CampaignCreateCommand createCmd = CampaignCreateCommand.builder()
            .tenantId(1L)
            .campaignCode("FIRST_ORDER_REBATE")
            .campaignName("首单返券")
            .campaignType(CampaignType.ORDER_REBATE_COUPON)
            .rules(buildRebateRules())
            .scope(buildAllScope())
            .startTime(LocalDateTime.now())
            .priority(100)
            .build();
        
        Long campaignId = campaignManagementFacade.createCampaign(createCmd);
        assertThat(campaignId).isNotNull();
        
        // 2. 上线活动
        campaignManagementFacade.onlineCampaign(1L, campaignId, 1L);
        
        // 3. 模拟订单支付事件
        OutboxEventDO event = buildOrderPaidEvent(1L, 100L, 200L, new BigDecimal("150.00"));
        campaignEventConsumer.handle(event);
        
        // 4. 验证执行日志
        List<ExecutionLogDTO> logs = campaignManagementFacade.listExecutionLogs(1L, campaignId, null, 10);
        assertThat(logs).hasSize(1);
        assertThat(logs.get(0).getExecutionStatus()).isEqualTo("SUCCESS");
        
        // 5. 下线活动
        campaignManagementFacade.offlineCampaign(1L, campaignId, 1L);
        
        // 6. 再次触发事件（活动已下线，不应执行）
        campaignEventConsumer.handle(event);
        logs = campaignManagementFacade.listExecutionLogs(1L, campaignId, null, 10);
        assertThat(logs).hasSize(1); // 仍然只有一条记录
    }
}
```

---

### 2. 钱包赠送集成测试

**测试目标**: 验证充值赠送活动完整流程

**测试场景**:

```java
@Test
void testRechargeBonusIntegration() {
    // 1. 创建充值赠送活动（充值满100送10元）
    CampaignCreateCommand createCmd = CampaignCreateCommand.builder()
        .tenantId(1L)
        .campaignCode("RECHARGE_BONUS_100")
        .campaignName("充值满100送10")
        .campaignType(CampaignType.RECHARGE_BONUS)
        .rules(buildBonusRules())
        .scope(buildAllScope())
        .startTime(LocalDateTime.now())
        .priority(100)
        .build();
    
    Long campaignId = campaignManagementFacade.createCampaign(createCmd);
    campaignManagementFacade.onlineCampaign(1L, campaignId, 1L);
    
    // 2. 查询用户充值前余额
    WalletBalanceDTO balanceBefore = walletQueryFacade.getBalance(1L, 100L);
    
    // 3. 模拟充值支付事件
    OutboxEventDO event = buildRechargePaidEvent(1L, 100L, 300L, new BigDecimal("100.00"));
    campaignEventConsumer.handle(event);
    
    // 4. 验证余额增加
    WalletBalanceDTO balanceAfter = walletQueryFacade.getBalance(1L, 100L);
    assertThat(balanceAfter.getAvailableBalance())
        .isEqualByComparingTo(balanceBefore.getAvailableBalance().add(new BigDecimal("10.00")));
    
    // 5. 验证账本流水
    // (查询钱包账本，验证有一条 CAMPAIGN_BONUS 类型的流水)
}
```

---

## 手动测试场景

### 场景1: 订单满减计价测试

**前置条件**:
- 创建一个满100减20的活动，状态为ONLINE
- 活动开始时间为当前时间之前

**测试步骤**:

1. 创建活动
```bash
curl -X POST http://localhost:8080/admin/campaigns \
  -H "Content-Type: application/json" \
  -d '{
    "tenantId": 1,
    "campaignCode": "DISCOUNT_100_20",
    "campaignName": "满100减20",
    "campaignType": "ORDER_DISCOUNT",
    "rules": {
      "minAmount": 100.00,
      "discountAmount": 20.00
    },
    "scope": {
      "scopeType": "ALL"
    },
    "startTime": "2025-01-01T00:00:00",
    "priority": 100
  }'
```

2. 上线活动
```bash
curl -X POST "http://localhost:8080/admin/campaigns/{campaignId}/online?tenantId=1&operatorId=1"
```

3. 创建订单（金额150元）
```bash
# 通过订单接口创建订单，观察计价结果
# 预期：订单金额150元，优惠20元，实付130元
```

4. 验证计价结果
```bash
# 查看订单详情，验证 pricing_breakdown 中有活动折扣明细
# 验证 promoDiscountAmount = 20.00
```

**预期结果**:
- ✅ 订单总额150元
- ✅ 活动折扣20元
- ✅ 实付金额130元
- ✅ 计价明细中显示活动信息
- ✅ Prometheus指标 `campaign.applied.total` +1

---

### 场景2: 订单返券幂等性测试

**测试步骤**:

1. 创建返券活动
```bash
curl -X POST http://localhost:8080/admin/campaigns \
  -H "Content-Type: application/json" \
  -d '{
    "tenantId": 1,
    "campaignCode": "ORDER_REBATE",
    "campaignName": "订单完成返券",
    "campaignType": "ORDER_REBATE_COUPON",
    "rules": {
      "minAmount": 50.00,
      "couponTemplateIds": "123,456",
      "couponQuantity": 1
    },
    "scope": {
      "scopeType": "ALL"
    },
    "startTime": "2025-01-01T00:00:00",
    "priority": 100
  }'
```

2. 上线活动

3. 创建并支付订单（金额100元）

4. 等待 outbox 消费者处理事件

5. 查询执行日志
```bash
curl "http://localhost:8080/admin/campaigns/execution-logs?tenantId=1&userId=100&limit=10"
```

6. 手动重新发送 ORDER_PAID 事件（模拟重复消费）

7. 再次查询执行日志

**预期结果**:
- ✅ 第一次执行：发券成功
- ✅ 第二次执行：幂等返回，不重复发券
- ✅ 执行日志表中只有一条记录
- ✅ 用户券包中只有对应数量的券
- ✅ Prometheus指标 `campaign.execution.success.total` +1

---

### 场景3: 充值赠送测试

**测试步骤**:

1. 创建充值赠送活动（充值满100享10%赠送，最高50元）
```bash
curl -X POST http://localhost:8080/admin/campaigns \
  -H "Content-Type: application/json" \
  -d '{
    "tenantId": 1,
    "campaignCode": "RECHARGE_BONUS",
    "campaignName": "充值赠送",
    "campaignType": "RECHARGE_BONUS",
    "rules": {
      "minAmount": 100.00,
      "bonusRate": 0.10,
      "maxBonusAmount": 50.00
    },
    "scope": {
      "scopeType": "ALL"
    },
    "startTime": "2025-01-01T00:00:00",
    "priority": 100
  }'
```

2. 上线活动

3. 查询用户钱包余额（充值前）

4. 创建充值单并支付（充值200元）

5. 等待充值回调和活动消费

6. 查询用户钱包余额（充值后）

**预期结果**:
- ✅ 充值金额200元到账
- ✅ 赠送金额20元到账（200 * 10%）
- ✅ 总余额增加220元
- ✅ 钱包账本中有两条流水：RECHARGE（200元）+ CAMPAIGN_BONUS（20元）
- ✅ 用户收到赠送通知

---

### 场景4: 活动时间窗测试

**测试步骤**:

1. 创建活动（开始时间为明天，结束时间为后天）

2. 今天创建订单
   - 预期：活动不生效（未到开始时间）

3. 修改活动开始时间为昨天，结束时间为今天23:59

4. 再次创建订单
   - 预期：活动生效

5. 修改活动结束时间为昨天

6. 再次创建订单
   - 预期：活动不生效（已过结束时间）

**预期结果**:
- ✅ 未到开始时间：活动不生效
- ✅ 时间窗内：活动生效
- ✅ 超过结束时间：活动不生效

---

### 场景5: 活动优先级测试

**测试步骤**:

1. 创建两个订单满减活动：
   - 活动A：满50减5，优先级100
   - 活动B：满100减15，优先级200

2. 上线两个活动

3. 创建订单（金额150元）

**预期结果**:
- ✅ 应用活动B（优先级200更高）
- ✅ 订单优惠15元
- ✅ 活动A不被应用

---

### 场景6: 用户参与次数限制测试

**测试步骤**:

1. 创建返券活动（perUserLimit=1）

2. 上线活动

3. 用户A首次下单并支付
   - 预期：返券成功

4. 用户A再次下单并支付
   - 预期：跳过（已达参与次数上限）

5. 用户B首次下单并支付
   - 预期：返券成功（不受用户A影响）

**预期结果**:
- ✅ 用户A只能参与一次
- ✅ 用户B不受影响
- ✅ 执行日志中记录 SKIPPED 状态

---

### 场景7: 活动删除测试

**测试步骤**:

1. 创建活动（状态为DRAFT）

2. 尝试删除
   - 预期：删除成功

3. 创建活动并上线（状态为ONLINE）

4. 尝试删除
   - 预期：删除失败，提示"活动已上线，不能删除"

5. 下线活动（状态为OFFLINE）

6. 尝试删除
   - 预期：删除成功

**预期结果**:
- ✅ DRAFT状态可删除
- ✅ ONLINE状态不可删除
- ✅ OFFLINE状态可删除
- ✅ 删除后查询不到该活动

---

## 性能测试

### 1. 计价性能测试

**目标**: 验证活动计价不影响订单计价性能

**测试方法**:
- 并发100个订单计价请求
- 每个请求包含5个活动匹配

**性能指标**:
- P95 响应时间 < 100ms
- P99 响应时间 < 200ms
- 错误率 = 0%

---

### 2. 活动执行并发测试

**目标**: 验证活动执行的幂等性和并发安全

**测试方法**:
- 同一订单并发触发10次 ORDER_PAID 事件
- 验证只执行一次，不重复发券

**性能指标**:
- 只有一条执行日志记录
- 用户只收到一次奖励
- 无数据库死锁或冲突

---

### 3. 活动查询性能测试

**目标**: 验证大量活动时的查询性能

**测试数据**:
- 租户下有1000个活动
- 其中100个状态为ONLINE

**测试方法**:
- 并发查询可用活动

**性能指标**:
- P95 响应时间 < 50ms
- 数据库索引命中率 > 95%

---

## 测试数据准备

### SQL 脚本

```sql
-- 1. 创建测试租户
INSERT INTO bc_tenant (id, tenant_code, tenant_name, status, created_at, updated_at)
VALUES (1, 'TEST_TENANT', '测试租户', 'ACTIVE', NOW(), NOW());

-- 2. 创建测试用户
INSERT INTO bc_user (id, tenant_id, phone, nickname, created_at, updated_at)
VALUES (100, 1, '13800138000', '测试用户A', NOW(), NOW()),
       (101, 1, '13800138001', '测试用户B', NOW(), NOW());

-- 3. 创建测试券模板
INSERT INTO bc_coupon_template (id, tenant_id, template_code, template_name, coupon_type, 
                                 discount_amount, valid_days, status, created_at, updated_at)
VALUES (123, 1, 'COUPON_10', '满50减10券', 'DISCOUNT', 10.00, 30, 'ACTIVE', NOW(), NOW()),
       (456, 1, 'COUPON_20', '满100减20券', 'DISCOUNT', 20.00, 30, 'ACTIVE', NOW(), NOW());

-- 4. 创建测试钱包账户
INSERT INTO bc_wallet_account (id, tenant_id, user_id, available_balance, frozen_balance, 
                                 total_recharged, currency, status, version, created_at, updated_at)
VALUES (1001, 1, 100, 0.00, 0.00, 0.00, 'CNY', 'ACTIVE', 0, NOW(), NOW()),
       (1002, 1, 101, 0.00, 0.00, 0.00, 'CNY', 'ACTIVE', 0, NOW(), NOW());
```

---

## 测试检查清单

### 功能测试
- [ ] 订单满减计价正确
- [ ] 订单返券正确发放
- [ ] 充值赠送正确入账
- [ ] 活动时间窗生效
- [ ] 活动优先级生效
- [ ] 用户参与次数限制生效
- [ ] 活动上下线状态控制
- [ ] 活动删除功能

### 非功能测试
- [ ] 幂等性保证（重复事件不重复执行）
- [ ] 计价确定性（多次计价结果一致）
- [ ] 并发安全（乐观锁冲突处理）
- [ ] 租户隔离（不同租户数据隔离）
- [ ] 性能指标（计价<100ms，执行<500ms）

### 观测性测试
- [ ] Prometheus 指标正常上报
- [ ] 执行日志完整记录
- [ ] 通知正常发送
- [ ] 错误日志正确记录

---

**文档版本**: V1.1  
**更新时间**: 2025-12-19  
**维护人**: BlueCone Team
