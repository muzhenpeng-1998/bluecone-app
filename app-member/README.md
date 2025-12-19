# app-member 模块

## 简介

`app-member` 模块是 BlueCone 餐饮 SaaS 的会员与积分账本实现，提供多租户隔离的会员管理和积分资产操作能力。

## 功能特性

- ✅ **会员管理**：支持会员的创建、查询（幂等创建）
- ✅ **积分账本**：完整的积分资产操作（赚取、冻结、释放、回退、调整）
- ✅ **幂等设计**：所有操作都支持幂等，避免重复扣减/增加
- ✅ **并发安全**：乐观锁 + 幂等键，保证高并发场景下的数据一致性
- ✅ **可对账**：完整的流水记录，支持余额验证和业务审计
- ✅ **多租户隔离**：所有数据都带 `tenant_id`，保证租户数据隔离

## 模块结构

```
app-member/
├── api/impl/                   # Facade 实现层
│   ├── MemberQueryFacadeImpl.java
│   └── PointsAssetFacadeImpl.java
├── application/                # 应用层
│   └── service/
│       ├── MemberApplicationService.java
│       └── PointsApplicationService.java
├── domain/                     # 领域层
│   ├── model/                  # 聚合根和实体
│   │   ├── Member.java
│   │   ├── PointsAccount.java
│   │   └── PointsLedger.java
│   ├── enums/                  # 枚举
│   │   ├── MemberStatus.java
│   │   ├── PointsDirection.java
│   │   └── PointsBizType.java
│   ├── repository/             # 仓储接口
│   │   ├── MemberRepository.java
│   │   ├── PointsAccountRepository.java
│   │   └── PointsLedgerRepository.java
│   └── service/                # 领域服务
│       └── PointsDomainService.java
├── infra/                      # 基础设施层
│   ├── persistence/            # 持久化
│   │   ├── po/                 # PO（数据库实体）
│   │   └── mapper/             # MyBatis Mapper
│   ├── repository/             # 仓储实现
│   └── converter/              # 转换器
└── config/                     # 配置
    └── MemberAutoConfiguration.java
```

## 数据库表

### bc_member（会员表）

存储会员基础信息，`tenant_id + user_id` 唯一。

### bc_points_account（积分账户表）

存储会员的积分余额（可用积分 + 冻结积分），使用乐观锁保证并发安全。

### bc_points_ledger（积分流水表）

记录所有积分变动，包含变动前后余额快照，支持对账和审计。

## 使用方式

### 1. 添加依赖

在业务模块的 `pom.xml` 中添加：

```xml
<dependency>
    <groupId>com.bluecone</groupId>
    <artifactId>app-member-api</artifactId>
    <version>${project.version}</version>
</dependency>
```

### 2. 注入 Facade

```java
@Service
public class OrderService {
    
    @Autowired
    private MemberQueryFacade memberQueryFacade;
    
    @Autowired
    private PointsAssetFacade pointsAssetFacade;
    
    public void someMethod() {
        // 获取或创建会员
        MemberDTO member = memberQueryFacade.getOrCreateMember(tenantId, userId);
        
        // 查询积分余额
        PointsBalanceDTO balance = memberQueryFacade.getPointsBalance(tenantId, member.getMemberId());
        
        // 赚取积分
        PointsOperationCommand command = new PointsOperationCommand(
            tenantId, memberId, 100L,
            "ORDER_COMPLETE", orderId.toString(), 
            idempotencyKey
        );
        PointsOperationResult result = pointsAssetFacade.commitPoints(command);
    }
}
```

### 3. 幂等键规则

所有积分操作都必须提供幂等键，格式：

```
{tenantId}:{bizType}:{bizId}:{operation}
```

示例：

```java
String idempotencyKey = String.format("%d:ORDER_COMPLETE:%s:earn", tenantId, orderId);
```

## API 接口

### MemberQueryFacade（会员查询）

- `getOrCreateMember(tenantId, userId)`：获取或创建会员（幂等）
- `getMemberById(tenantId, memberId)`：根据会员ID查询
- `getMemberByUserId(tenantId, userId)`：根据用户ID查询
- `getPointsBalance(tenantId, memberId)`：查询积分余额

### PointsAssetFacade（积分操作）

- `freezePoints(command)`：冻结积分（下单锁定）
- `commitPoints(command)`：提交积分变更（支付成功后扣减/入账）
- `releasePoints(command)`：释放冻结积分（取消/超时）
- `revertPoints(command)`：回退积分变更（退款返还）
- `adjustPoints(command, isIncrease)`：调整积分（管理员手动调整）

## 测试

运行集成测试：

```bash
mvn test -pl app-member
```

测试用例包括：
- 会员创建幂等性测试
- 积分赚取幂等性测试

## 文档

详细设计文档：[MEMBER-POINTS-LEDGER.md](../docs/MEMBER-POINTS-LEDGER.md)

内容包括：
- 数据模型详解
- 积分操作流程
- 幂等键规则
- 与订单模块对接示例
- 并发安全机制
- 可对账性设计

## 注意事项

1. **幂等键必须唯一**：同一个业务操作必须使用相同的幂等键
2. **先创建会员**：在操作积分前，必须先调用 `getOrCreateMember` 确保会员存在
3. **检查返回结果**：所有操作都会返回 `PointsOperationResult`，需检查 `isSuccess()` 和 `isDuplicate()`
4. **异常处理**：积分操作失败时会返回失败结果，需根据 `errorMessage` 进行处理

## 后续扩展

当前为 MVP 版本，后续可扩展：

- [ ] 积分过期机制
- [ ] 积分兑换功能
- [ ] 积分转赠功能
- [ ] 会员等级体系
- [ ] 储值账户功能
- [ ] 优惠券系统

---

最后更新：2025-12-18
