# 统一计价引擎文档

## 概述

统一计价引擎（Pricing Engine）是 bluecone-app 的核心定价模块，负责订单的价格计算、优惠应用、费用计算等功能。

### 设计目标

1. **统一计价**：所有订单价格计算都通过统一的计价引擎
2. **可解释性**：每一笔价格调整都有明细行（breakdownLines）记录
3. **可追溯性**：每次计价都有版本号和快照，支持对账和审计
4. **纯计算**：计价阶段不锁定资源（券/积分/余额），只做查询和预估
5. **确定性**：相同输入必定产生相同输出，确保幂等性

## 架构设计

### 模块划分

```
app-pricing-api/          # API 接口模块（只有接口和 DTO）
├── dto/
│   ├── PricingRequest    # 计价请求
│   ├── PricingItem       # 计价商品项
│   ├── PricingQuote      # 计价报价单
│   └── PricingLine       # 计价明细行
├── enums/
│   └── ReasonCode        # 计价原因码
└── facade/
    └── PricingFacade     # 计价门面接口

app-pricing/              # 实现模块
├── application/
│   └── PricingFacadeImpl # 门面实现
├── domain/
│   ├── model/
│   │   └── PricingContext # 计价上下文
│   └── service/
│       ├── PricingStage   # 计价阶段接口
│       ├── PricingPipeline # 计价流水线
│       └── stage/         # 各个计价阶段实现
│           ├── BasePriceStage
│           ├── MemberPriceStage
│           ├── PromoStage
│           ├── CouponStage
│           ├── PointsStage
│           ├── FeeStage
│           └── RoundingStage
```

### 依赖关系

```
app-order → app-pricing-api → app-pricing
                             ↓
                    app-promo-api
                    app-member-api
                    app-wallet-api
```

**重要约束**：
- `app-order` 只依赖 `app-pricing-api`，不依赖 `app-pricing`
- `app-pricing` 只调用其他模块的 API 接口，不依赖实现模块

## 计价流程

### Pipeline 架构

计价引擎采用 Pipeline 架构，按顺序执行以下 7 个阶段：

```
Stage1: BasePriceStage      → 计算商品基价（basePrice + specSurcharge）
Stage2: MemberPriceStage     → 应用会员价/时段价（预留接口）
Stage3: PromoStage           → 应用活动折扣（预留接口）
Stage4: CouponStage          → 应用优惠券抵扣
Stage5: PointsStage          → 应用积分抵扣
Stage6: FeeStage             → 计算配送费和打包费
Stage7: RoundingStage        → 应用抹零规则
```

### 计价顺序说明

**为什么这样排序？**

1. **基价优先**：先计算商品原价，作为后续优惠的基础
2. **会员价次之**：会员价是商品级别的优惠，优先于订单级别优惠
3. **活动折扣**：活动折扣通常是订单级别的，在会员价之后
4. **优惠券**：优惠券是用户主动选择的，优先级较高
5. **积分抵扣**：积分是最后的兜底优惠方式
6. **费用计算**：在所有优惠之后计算费用，确保费用不被优惠
7. **抹零**：最后一步，对最终金额进行抹零

**重要规则**：
- 优惠券和积分可以叠加使用
- 积分抵扣不能超过订单金额的 50%
- 配送费和打包费不参与优惠计算
- 抹零只在用户明确启用时才执行

### Stage 接口设计

每个 Stage 都实现 `PricingStage` 接口：

```java
public interface PricingStage {
    void execute(PricingContext context);
    String getStageName();
    int getOrder();
}
```

**设计优势**：
- 每个 Stage 独立，易于测试
- 通过 `order` 控制执行顺序
- 所有 Stage 共享 `PricingContext`，确保数据一致性
- 新增 Stage 只需实现接口并注册为 Bean

## 计价明细行（Breakdown Lines）

### 作用

计价明细行记录了价格计算的每一步，确保：
1. **可解释性**：用户可以看到每一笔优惠的来源
2. **可追溯性**：出现价格争议时可以追溯计算过程
3. **可对账性**：财务对账时可以核对每一笔明细

### 数据结构

```java
public class PricingLine {
    private ReasonCode reasonCode;      // 原因码（枚举）
    private String description;          // 中文描述
    private BigDecimal amount;           // 金额（正数=增加，负数=减少）
    private Long relatedId;              // 关联业务ID
    private String relatedType;          // 关联业务类型
    private String extInfo;              // 扩展信息
}
```

### 示例

```json
{
  "breakdownLines": [
    {
      "reasonCode": "BASE_PRICE",
      "description": "商品A x 2",
      "amount": 20.00,
      "relatedId": 1001,
      "relatedType": "SKU"
    },
    {
      "reasonCode": "COUPON_DISCOUNT",
      "description": "优惠券抵扣: 满50减10",
      "amount": -10.00,
      "relatedId": 1001,
      "relatedType": "COUPON"
    },
    {
      "reasonCode": "DELIVERY_FEE",
      "description": "配送费 (5.0公里)",
      "amount": 10.00
    }
  ]
}
```

## 版本策略

### 计价版本号

每次计价都会生成一个唯一的 `pricingVersion`，用于：
1. **版本控制**：记录使用的计价规则版本
2. **防篡改**：下单时校验 `pricingVersion` 是否一致
3. **可追溯**：出现问题时可以追溯到具体的计价版本

### 版本号生成规则

```
pricingVersion = quoteId (32位随机字符串)
```

### 版本变更场景

当以下情况发生时，需要升级计价版本：
1. 计价规则变更（如积分兑换比例调整）
2. 费用规则变更（如配送费计算规则调整）
3. 优惠叠加规则变更
4. 新增或删除计价阶段

**版本升级流程**：
1. 在 `PricingQuote` 中更新 `pricingVersion` 字段
2. 在文档中记录版本变更内容
3. 确保新旧版本兼容，或提供迁移方案

## 对账说明

### 对账维度

1. **订单维度**：每个订单都有一条计价快照记录
2. **时间维度**：按计价时间统计每日/每月的价格数据
3. **优惠维度**：按优惠类型统计优惠金额
4. **商品维度**：按商品统计销售金额

### 对账 SQL 示例

#### 1. 查询某订单的计价明细

```sql
SELECT 
    ops.order_id,
    ops.quote_id,
    ops.original_amount,
    ops.coupon_discount_amount,
    ops.points_discount_amount,
    ops.payable_amount,
    ops.breakdown_lines
FROM order_pricing_snapshot ops
WHERE ops.order_id = ?
  AND ops.deleted = 0;
```

#### 2. 统计某日的优惠券使用情况

```sql
SELECT 
    DATE(ops.pricing_time) as pricing_date,
    COUNT(*) as coupon_usage_count,
    SUM(ops.coupon_discount_amount) as total_coupon_discount
FROM order_pricing_snapshot ops
WHERE DATE(ops.pricing_time) = ?
  AND ops.applied_coupon_id IS NOT NULL
  AND ops.deleted = 0
GROUP BY DATE(ops.pricing_time);
```

#### 3. 统计某日的积分抵扣情况

```sql
SELECT 
    DATE(ops.pricing_time) as pricing_date,
    COUNT(*) as points_usage_count,
    SUM(ops.applied_points) as total_points_used,
    SUM(ops.points_discount_amount) as total_points_discount
FROM order_pricing_snapshot ops
WHERE DATE(ops.pricing_time) = ?
  AND ops.applied_points IS NOT NULL
  AND ops.deleted = 0
GROUP BY DATE(ops.pricing_time);
```

#### 4. 对账：订单金额 vs 计价快照

```sql
SELECT 
    o.order_id,
    o.payable_amount as order_payable_amount,
    ops.payable_amount as snapshot_payable_amount,
    CASE 
        WHEN o.payable_amount = ops.payable_amount THEN '一致'
        ELSE '不一致'
    END as match_status
FROM `order` o
LEFT JOIN order_pricing_snapshot ops ON o.order_id = ops.order_id AND ops.deleted = 0
WHERE o.created_at >= ?
  AND o.created_at < ?
  AND o.deleted = 0;
```

### 对账异常处理

**异常场景**：
1. 订单金额与计价快照不一致
2. 计价快照缺失
3. 明细行金额汇总与总金额不一致

**处理流程**：
1. 记录异常日志
2. 通知运营人员
3. 人工核查原因
4. 必要时进行补偿

## 接入指南

### 1. Precheck 阶段接入

在订单确认单（Precheck）阶段调用计价引擎：

```java
@Service
public class OrderConfirmApplicationServiceImpl {
    
    private final PricingFacade pricingFacade;
    
    public OrderConfirmResponse confirm(OrderConfirmRequest request) {
        // 构建计价请求
        PricingRequest pricingRequest = buildPricingRequest(request);
        
        // 调用计价引擎
        PricingQuote quote = pricingFacade.quote(pricingRequest);
        
        // 返回报价单
        return buildResponse(quote);
    }
}
```

### 2. Checkout 阶段接入

在订单提交（Checkout）阶段再次调用计价引擎，并保存快照：

```java
@Service
public class OrderDomainServiceImpl {
    
    private final PricingFacade pricingFacade;
    private final OrderPricingSnapshotRepository snapshotRepository;
    
    public Order createOrder(ConfirmOrderRequest request) {
        // 再次计价
        PricingRequest pricingRequest = buildPricingRequest(request);
        PricingQuote quote = pricingFacade.quote(pricingRequest);
        
        // 校验金额
        if (!quote.getPayableAmount().equals(request.getPayableAmount())) {
            throw new BizException("价格已变动，请重新确认");
        }
        
        // 创建订单
        Order order = buildOrder(request, quote);
        
        // 保存计价快照
        OrderPricingSnapshot snapshot = buildSnapshot(order.getOrderId(), quote);
        snapshotRepository.save(snapshot);
        
        return order;
    }
}
```

### 3. 防篡改校验

下单时必须校验前端传入的金额与计价引擎计算的金额是否一致：

```java
// 校验金额
BigDecimal clientAmount = request.getPayableAmount();
BigDecimal serverAmount = quote.getPayableAmount();

if (clientAmount.compareTo(serverAmount) != 0) {
    log.warn("价格篡改检测：客户端金额={}, 服务端金额={}", clientAmount, serverAmount);
    throw new BizException(OrderErrorCode.PRICE_TAMPERED, "价格已变动，请重新确认");
}
```

## 扩展指南

### 新增计价阶段

1. 创建新的 Stage 类，实现 `PricingStage` 接口：

```java
@Component
public class NewStage implements PricingStage {
    
    @Override
    public void execute(PricingContext context) {
        // 实现计价逻辑
    }
    
    @Override
    public String getStageName() {
        return "NewStage";
    }
    
    @Override
    public int getOrder() {
        return 8; // 设置执行顺序
    }
}
```

2. Spring 会自动扫描并注册该 Stage
3. 编写单元测试验证功能
4. 更新文档说明新阶段的作用

### 修改计价规则

1. 修改对应 Stage 的实现代码
2. 更新 `pricingVersion` 版本号
3. 编写单元测试验证新规则
4. 在文档中记录变更内容
5. 通知相关团队（产品、运营、财务）

## 最佳实践

### 1. 确保幂等性

相同的输入必须产生相同的输出：
- 不要在计价过程中使用随机数
- 不要依赖外部可变状态
- 时间相关的计算要明确时间参数

### 2. 保持纯计算

计价阶段不要锁定资源：
- 不要锁定优惠券
- 不要冻结积分
- 不要冻结余额
- 只做查询和可用性判断

### 3. 完整的明细行

每一笔价格调整都要记录明细行：
- 使用合适的 `ReasonCode`
- 提供清晰的中文描述
- 记录关联的业务ID

### 4. 异常处理

计价失败时要提供清晰的错误信息：
- 优惠券不可用：说明原因
- 积分余额不足：说明可用积分
- 计价异常：记录详细日志

## FAQ

### Q1: 为什么计价要分 Precheck 和 Checkout 两次？

**A**: 
- **Precheck**：用户下单前的预览，展示价格和优惠
- **Checkout**：用户确认下单时的最终计价，确保价格一致

两次计价可以：
1. 防止价格篡改
2. 确保价格实时性（活动/优惠可能变化）
3. 提升用户体验（Precheck 快速返回）

### Q2: 为什么不在计价时锁定优惠券和积分？

**A**: 
1. **性能考虑**：计价是高频操作，锁定资源会影响性能
2. **用户体验**：用户可能只是预览价格，不一定下单
3. **资源浪费**：锁定后用户不下单，资源被浪费
4. **架构清晰**：计价和资源锁定分离，职责更清晰

正确的流程是：
- Precheck：只查询和预估，不锁定
- Checkout：先锁定资源，再创建订单，最后提交资源

### Q3: 如何处理计价规则变更？

**A**: 
1. 更新 `pricingVersion` 版本号
2. 在文档中记录变更内容
3. 通知相关团队
4. 必要时提供灰度发布方案
5. 监控计价异常和对账差异

### Q4: 如何保证计价的准确性？

**A**: 
1. **单元测试**：覆盖所有计价场景
2. **集成测试**：测试完整的计价流程
3. **对账机制**：定期对账，发现异常及时处理
4. **监控告警**：监控计价失败率和异常金额
5. **Code Review**：计价相关代码必须经过严格审查

## 总结

统一计价引擎是订单系统的核心模块，设计时遵循以下原则：

1. **统一入口**：所有计价都通过 `PricingFacade`
2. **可解释性**：每笔调整都有明细行
3. **可追溯性**：每次计价都有快照
4. **纯计算**：不锁定资源
5. **确定性**：相同输入产生相同输出
6. **可扩展性**：Pipeline 架构易于扩展

通过这些设计，确保计价系统的稳定性、准确性和可维护性。
