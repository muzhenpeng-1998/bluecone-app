# 统一计价引擎快速开始

## 快速概览

统一计价引擎（Pricing Engine）是 bluecone-app 的核心定价模块，提供：
- ✅ 商品基价计算
- ✅ 优惠券抵扣
- ✅ 积分抵扣
- ✅ 配送费和打包费计算
- ✅ 抹零功能
- 🔄 会员价（预留接口）
- 🔄 活动折扣（预留接口）

## 5分钟快速上手

### 1. 添加依赖

在你的模块 `pom.xml` 中添加：

```xml
<dependency>
    <groupId>com.bluecone</groupId>
    <artifactId>app-pricing-api</artifactId>
    <version>${project.version}</version>
</dependency>
```

### 2. 注入 PricingFacade

```java
@Service
public class YourService {
    
    @Autowired
    private PricingFacade pricingFacade;
}
```

### 3. 调用计价引擎

```java
// 构建计价请求
PricingRequest request = new PricingRequest();
request.setTenantId(1L);
request.setStoreId(100L);
request.setUserId(1000L);
request.setDeliveryMode("DELIVERY");
request.setDeliveryDistance(new BigDecimal("5.0"));

// 添加商品
List<PricingItem> items = new ArrayList<>();
PricingItem item = new PricingItem();
item.setSkuId(1001L);
item.setSkuName("商品A");
item.setQuantity(2);
item.setBasePrice(new BigDecimal("10.00"));
items.add(item);
request.setItems(items);

// 调用计价
PricingQuote quote = pricingFacade.quote(request);

// 获取结果
BigDecimal payableAmount = quote.getPayableAmount();
```

## 核心概念

### PricingRequest（计价请求）
包含订单计价所需的所有信息：
- 租户、门店、用户信息
- 商品列表
- 优惠券ID（可选）
- 使用积分（可选）
- 配送方式和距离
- 是否启用抹零

### PricingQuote（计价报价单）
包含完整的计价结果：
- 商品原价
- 各类优惠金额
- 配送费、打包费
- 应付金额
- **breakdownLines**：完整的计价明细行

### PricingLine（计价明细行）
记录每一步价格调整：
- `reasonCode`：原因码（枚举）
- `description`：中文描述
- `amount`：金额（正数=增加，负数=减少）
- `relatedId`：关联业务ID

## 常见场景

### 场景1：基础计价（无优惠）

```java
PricingRequest request = new PricingRequest();
request.setTenantId(1L);
request.setStoreId(100L);
request.setUserId(1000L);
request.setDeliveryMode("PICKUP"); // 自提，无配送费

List<PricingItem> items = new ArrayList<>();
PricingItem item = new PricingItem();
item.setSkuId(1001L);
item.setSkuName("咖啡");
item.setQuantity(1);
item.setBasePrice(new BigDecimal("25.00"));
items.add(item);
request.setItems(items);

PricingQuote quote = pricingFacade.quote(request);
// payableAmount = 25.00 + 1.00(打包费) = 26.00
```

### 场景2：使用优惠券

```java
PricingRequest request = createBasicRequest();
request.setCouponId(1001L); // 指定优惠券ID

PricingQuote quote = pricingFacade.quote(request);
// 如果优惠券可用，会自动抵扣
// quote.getCouponDiscountAmount() 可获取抵扣金额
```

### 场景3：使用积分

```java
PricingRequest request = createBasicRequest();
request.setMemberId(2001L);     // 必须是会员
request.setUsePoints(500);       // 使用500积分

PricingQuote quote = pricingFacade.quote(request);
// 500积分 = 5元（100积分=1元）
// quote.getPointsDiscountAmount() 可获取抵扣金额
```

### 场景4：优惠券+积分叠加

```java
PricingRequest request = createBasicRequest();
request.setCouponId(1001L);      // 优惠券
request.setMemberId(2001L);
request.setUsePoints(500);       // 积分

PricingQuote quote = pricingFacade.quote(request);
// 优惠券和积分可以叠加使用
```

### 场景5：配送费计算

```java
PricingRequest request = createBasicRequest();
request.setDeliveryMode("DELIVERY");
request.setDeliveryDistance(new BigDecimal("5.5"));

PricingQuote quote = pricingFacade.quote(request);
// 配送费 = 5元(起步价) + (5.5-3)*2 = 10元
// quote.getDeliveryFee() 可获取配送费
```

### 场景6：抹零

```java
PricingRequest request = createBasicRequest();
request.setEnableRounding(true); // 启用抹零

PricingQuote quote = pricingFacade.quote(request);
// 最终金额会四舍五入到角（保留1位小数）
```

## 计价明细行示例

```json
{
  "quoteId": "abc123...",
  "pricingVersion": "1.0.0",
  "originalAmount": 50.00,
  "couponDiscountAmount": 10.00,
  "pointsDiscountAmount": 5.00,
  "deliveryFee": 10.00,
  "packingFee": 1.00,
  "payableAmount": 46.00,
  "breakdownLines": [
    {
      "reasonCode": "BASE_PRICE",
      "description": "商品A x 2",
      "amount": 20.00,
      "relatedId": 1001,
      "relatedType": "SKU"
    },
    {
      "reasonCode": "BASE_PRICE",
      "description": "商品B x 1",
      "amount": 30.00,
      "relatedId": 1002,
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
      "reasonCode": "POINTS_DISCOUNT",
      "description": "积分抵扣: 500积分",
      "amount": -5.00,
      "relatedId": 2001,
      "relatedType": "POINTS"
    },
    {
      "reasonCode": "DELIVERY_FEE",
      "description": "配送费 (5.5公里)",
      "amount": 10.00
    },
    {
      "reasonCode": "PACKING_FEE",
      "description": "打包费",
      "amount": 1.00
    }
  ]
}
```

## 重要规则

### 积分抵扣规则
- **兑换比例**：100积分 = 1元
- **抵扣上限**：订单金额的50%
- **余额校验**：必须有足够的积分余额

### 配送费规则
- **3公里内**：起步价5元
- **超过3公里**：5元 + (距离-3) × 2元/公里
- **自提订单**：无配送费

### 打包费规则
- **固定**：1元

### 抹零规则
- **方式**：四舍五入到角（保留1位小数）
- **触发**：仅在 `enableRounding=true` 时执行

## 最佳实践

### 1. Precheck + Checkout 模式

```java
// Precheck：用户下单前预览价格
PricingQuote precheckQuote = pricingFacade.quote(request);
// 展示给用户

// Checkout：用户确认下单
PricingQuote checkoutQuote = pricingFacade.quote(request);
// 校验金额是否一致
if (!checkoutQuote.getPayableAmount().equals(clientAmount)) {
    throw new BizException("价格已变动，请重新确认");
}
```

### 2. 保存计价快照

```java
// 下单时保存计价快照
OrderPricingSnapshot snapshot = new OrderPricingSnapshot();
snapshot.setOrderId(order.getOrderId());
snapshot.setQuoteId(quote.getQuoteId());
snapshot.setPricingVersion(quote.getPricingVersion());
snapshot.setOriginalAmount(quote.getOriginalAmount());
snapshot.setPayableAmount(quote.getPayableAmount());
snapshot.setBreakdownLines(quote.getBreakdownLines());
// ... 设置其他字段
snapshotRepository.save(snapshot);
```

### 3. 防篡改校验

```java
// 校验前端传入的金额
BigDecimal clientAmount = request.getPayableAmount();
BigDecimal serverAmount = quote.getPayableAmount();

if (clientAmount.compareTo(serverAmount) != 0) {
    log.warn("价格篡改检测：客户端={}, 服务端={}", clientAmount, serverAmount);
    throw new BizException("价格已变动，请重新确认");
}
```

## 错误处理

### 优惠券不可用

```java
PricingQuote quote = pricingFacade.quote(request);
if (quote.getCouponDiscountAmount().compareTo(BigDecimal.ZERO) == 0) {
    // 优惠券未生效，可能原因：
    // - 优惠券不存在
    // - 优惠券已使用
    // - 订单金额不满足最低使用条件
    // 可以从 context.getContextData("coupon_unavailable_reason") 获取原因
}
```

### 积分余额不足

```java
PricingQuote quote = pricingFacade.quote(request);
if (quote.getPointsDiscountAmount().compareTo(BigDecimal.ZERO) == 0) {
    // 积分未生效，可能原因：
    // - 积分余额不足
    // - 非会员用户
    // 可以从 context.getContextData("points_unavailable_reason") 获取原因
}
```

## 性能优化建议

1. **缓存商品价格**：避免每次都查询数据库
2. **批量查询**：一次性查询所有需要的数据
3. **异步日志**：计价日志异步记录
4. **监控告警**：监控计价失败率和耗时

## 下一步

- 📖 阅读完整文档：[pricing-engine.md](./pricing-engine.md)
- 🧪 查看测试用例：`app-pricing/src/test/java/`
- 📊 查看对账SQL：[pricing-engine.md#对账说明](./pricing-engine.md#对账说明)

## 常见问题

**Q: 为什么计价要调用两次（Precheck + Checkout）？**

A: Precheck 用于用户下单前预览价格，Checkout 用于确认下单时的最终计价。两次计价可以防止价格篡改，确保价格实时性。

**Q: 计价时会锁定优惠券和积分吗？**

A: 不会。计价阶段只做查询和可用性判断，不锁定资源。资源锁定在订单创建时进行。

**Q: 如何新增计价规则？**

A: 实现 `PricingStage` 接口，注册为 Spring Bean 即可。详见[扩展指南](./pricing-engine.md#扩展指南)。

**Q: 如何对账？**

A: 查询 `order_pricing_snapshot` 表，使用 `breakdown_lines` 字段进行明细对账。详见[对账说明](./pricing-engine.md#对账说明)。
