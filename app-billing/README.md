# app-billing - 订阅计费模块

## 模块概述

`app-billing` 是 bluecone-app 的订阅计费核心模块，负责：
- 套餐 SKU 管理
- 订阅账单创建与支付
- 租户订阅状态管理
- 支付回调处理
- 订阅生效与到期管理
- 对账补偿

## 模块结构

```
app-billing/
├── src/main/java/com/bluecone/app/billing/
│   ├── domain/                           # 领域层
│   │   ├── enums/                        # 枚举
│   │   │   ├── PlanCode.java            # 套餐编码
│   │   │   ├── BillingPeriod.java       # 计费周期
│   │   │   ├── InvoiceStatus.java       # 账单状态
│   │   │   └── SubscriptionStatus.java  # 订阅状态
│   │   └── service/
│   │       └── BillingDomainService.java # 领域服务
│   ├── dao/                              # 数据访问层
│   │   ├── entity/                       # 实体
│   │   │   ├── PlanSkuDO.java
│   │   │   ├── BillingInvoiceDO.java
│   │   │   └── TenantSubscriptionDO.java
│   │   └── mapper/                       # MyBatis Mapper
│   │       ├── PlanSkuMapper.java
│   │       ├── BillingInvoiceMapper.java
│   │       └── TenantSubscriptionMapper.java
│   ├── application/                      # 应用层
│   │   ├── BillingApplicationService.java        # 应用服务
│   │   ├── BillingPaymentCallbackService.java    # 支付回调服务
│   │   ├── BillingEventConsumer.java             # 事件消费者
│   │   └── SubscriptionPlanService.java          # 订阅套餐服务
│   ├── scheduler/                        # 定时任务
│   │   ├── SubscriptionExpireJob.java    # 订阅到期任务
│   │   └── BillingReconcileJob.java      # 对账补偿任务
│   └── config/
│       └── BillingSchedulerConfig.java   # 定时任务配置
└── src/test/java/
    └── com/bluecone/app/billing/
        └── BillingDomainServiceTest.java # 单元测试
```

## 核心功能

### 1. 套餐管理

支持 4 种套餐等级：
- **FREE**: 免费版（永久免费）
- **BASIC**: 基础版（月付/年付）
- **PRO**: 专业版（月付/年付）
- **ENTERPRISE**: 企业版（月付/年付）

每个套餐包含：
- 价格（分）
- 计费周期
- 功能配额（JSON）

### 2. 账单管理

- 创建账单（幂等）
- 支付回调处理（幂等）
- 账单状态流转（PENDING → PAID）
- 生效周期计算

### 3. 订阅管理

- 首次订阅激活
- 续费延长周期
- 到期自动降级
- 配额快照管理

### 4. 事件驱动

- 支付成功写入 Outbox 事件
- 消费者异步激活订阅
- 支持重试与补偿

### 5. 定时任务

- **SubscriptionExpireJob**: 每小时扫描到期订阅，自动降级
- **BillingReconcileJob**: 每 30 分钟扫描未生效的已支付账单，补发事件

## 依赖

### 必需依赖
- `app-billing-api`: API 契约
- `app-core`: 核心模块（事件、异常）
- `app-infra`: 基础设施（Outbox、数据库）
- `app-id-api`: ID 生成服务

### 可选依赖
- `app-payment`: 支付回调集成

## 使用示例

### 1. 查询套餐列表

```java
@Autowired
private BillingApplicationService billingApplicationService;

List<PlanSkuDTO> plans = billingApplicationService.listPlans();
```

### 2. 创建账单

```java
CreateInvoiceCommand command = CreateInvoiceCommand.builder()
    .tenantId(1001L)
    .planSkuId(2L)
    .idempotencyKey("INV-" + UUID.randomUUID())
    .paymentChannel("WECHAT")
    .build();

CreateInvoiceResult result = billingApplicationService.createInvoice(command);
```

### 3. 处理支付回调

```java
@Autowired
private BillingPaymentCallbackService callbackService;

callbackService.handleInvoicePaid(
    invoiceId,
    channelTradeNo,
    paidAmountFen,
    paidAt
);
```

### 4. 查询租户订阅

```java
SubscriptionDTO subscription = billingApplicationService.getSubscription(tenantId);
```

### 5. 获取租户配额（供 PlanGuard 使用）

```java
@Autowired
private SubscriptionPlanService subscriptionPlanService;

TenantPlanConfig config = subscriptionPlanService.getTenantPlanConfig(tenantId);
int maxStores = config.maxStores();
boolean hasMultiWarehouse = config.hasFeature(Feature.MULTI_WAREHOUSE);
```

## 配置

### 定时任务配置

```yaml
spring:
  task:
    scheduling:
      enabled: true  # 启用定时任务
```

### 定时任务执行频率

- `SubscriptionExpireJob`: `0 0 * * * ?` (每小时)
- `BillingReconcileJob`: `0 */30 * * * ?` (每 30 分钟)

可根据需要修改 `@Scheduled` 注解的 cron 表达式。

## 测试

### 运行单元测试

```bash
mvn test -pl app-billing
```

### 测试覆盖

- 账单创建幂等性
- 支付回调幂等性
- 首次订阅激活
- 续费延长周期
- 到期降级

## 监控

### 关键日志

| 日志关键字 | 说明 |
|-----------|------|
| `[billing-callback]` | 支付回调处理 |
| `[billing-consumer]` | 订阅激活消费 |
| `[billing-reconcile-job]` | 对账补偿任务 |
| `[subscription-expire-job]` | 到期降级任务 |
| `[subscription-plan]` | 配额查询 |

### 关键指标

- 账单支付成功率
- 订阅激活延迟
- 对账补偿次数
- 到期未降级数量

## 常见问题

### Q1: 账单创建失败
- 检查 `planSkuId` 是否存在
- 检查 `idempotencyKey` 是否重复

### Q2: 支付回调后订阅未生效
- 检查 `bc_outbox_event` 表是否有事件
- 检查 `bc_event_consume_log` 表是否有消费记录
- 查看日志中是否有错误

### Q3: 定时任务未执行
- 检查 `@EnableScheduling` 是否生效
- 检查配置中 `spring.task.scheduling.enabled` 是否为 `true`

## 相关文档

- [状态机与对账补偿说明](../docs/billing-v1-state-machine-and-reconciliation.md)
- [实现总结](../docs/BILLING-V1-IMPLEMENTATION-SUMMARY.md)
- [快速开始](../docs/BILLING-V1-QUICK-START.md)

## 版本历史

- **V1.0** (2025-12-19): 初始版本，支持基础订阅计费功能

## 许可证

Copyright © 2025 Bluecone Team
