# 会员 + 积分账本 M5 实现总结

## 实现时间

2025-12-18

## 概述

成功实现 `app-member-api` 和 `app-member` 两个模块，提供完整的会员与积分账本能力，支持多租户隔离、幂等操作、并发安全、可对账等核心要求。

## 新增模块

### 1. app-member-api（对外契约模块）

**位置**：`/app-member-api`

**职责**：定义对外的 DTO 和 Facade 接口，供其他业务模块调用

**核心文件**：

```
app-member-api/
├── pom.xml
└── src/main/java/com/bluecone/app/member/api/
    ├── dto/
    │   ├── MemberDTO.java                  # 会员信息 DTO
    │   ├── PointsBalanceDTO.java           # 积分余额 DTO
    │   ├── PointsOperationCommand.java     # 积分操作命令
    │   └── PointsOperationResult.java      # 积分操作结果
    └── facade/
        ├── MemberQueryFacade.java          # 会员查询门面接口
        └── PointsAssetFacade.java          # 积分资产操作门面接口
```

**关键设计**：
- 无实现依赖，保持 API 模块轻量
- 所有 DTO 不使用 Lombok，保持纯 POJO
- 所有操作命令都必须携带 `tenantId`、`idempotencyKey` 等关键参数

### 2. app-member（领域实现模块）

**位置**：`/app-member`

**职责**：实现会员和积分账本的完整业务逻辑

**核心文件**：

```
app-member/
├── pom.xml
├── README.md                               # 模块使用文档
└── src/main/java/com/bluecone/app/member/
    ├── api/impl/                           # Facade 实现层
    │   ├── MemberQueryFacadeImpl.java
    │   └── PointsAssetFacadeImpl.java
    ├── application/service/                # 应用层
    │   ├── MemberApplicationService.java   # 会员应用服务
    │   └── PointsApplicationService.java   # 积分应用服务
    ├── domain/                             # 领域层
    │   ├── model/                          # 聚合根和实体
    │   │   ├── Member.java
    │   │   ├── PointsAccount.java
    │   │   └── PointsLedger.java
    │   ├── enums/                          # 枚举
    │   │   ├── MemberStatus.java
    │   │   ├── PointsDirection.java
    │   │   └── PointsBizType.java
    │   ├── repository/                     # 仓储接口
    │   │   ├── MemberRepository.java
    │   │   ├── PointsAccountRepository.java
    │   │   └── PointsLedgerRepository.java
    │   └── service/                        # 领域服务
    │       └── PointsDomainService.java
    ├── infra/                              # 基础设施层
    │   ├── persistence/                    # 持久化
    │   │   ├── po/                         # PO（数据库实体）
    │   │   │   ├── MemberPO.java
    │   │   │   ├── PointsAccountPO.java
    │   │   │   └── PointsLedgerPO.java
    │   │   └── mapper/                     # MyBatis Mapper
    │   │       ├── MemberMapper.java
    │   │       ├── PointsAccountMapper.java
    │   │       └── PointsLedgerMapper.java
    │   ├── repository/                     # 仓储实现
    │   │   ├── MemberRepositoryImpl.java
    │   │   ├── PointsAccountRepositoryImpl.java
    │   │   └── PointsLedgerRepositoryImpl.java
    │   └── converter/                      # 转换器
    │       ├── MemberConverter.java
    │       ├── PointsAccountConverter.java
    │       └── PointsLedgerConverter.java
    ├── config/                             # 配置
    │   └── MemberAutoConfiguration.java
    └── resources/
        └── META-INF/
            └── spring.factories            # Spring 自动配置
```

## 数据库变更

### 新增迁移脚本

**位置**：`app-infra/src/main/resources/db/migration/V20251218005__create_member_tables.sql`

### 新增表

1. **bc_member（会员表）**
   - 主键：`id`（BIGINT，ULID）
   - 唯一约束：`uk_tenant_user`（租户 + 用户唯一）
   - 唯一约束：`uk_tenant_member_no`（租户 + 会员号唯一）
   - 字段：`id`, `tenant_id`, `user_id`, `member_no`, `status`, `created_at`, `updated_at`

2. **bc_points_account（积分账户表）**
   - 主键：`id`（BIGINT，ULID）
   - 唯一约束：`uk_tenant_member`（租户 + 会员唯一）
   - 乐观锁：`version`
   - 字段：`id`, `tenant_id`, `member_id`, `available_points`, `frozen_points`, `version`, `created_at`, `updated_at`

3. **bc_points_ledger（积分流水表）**
   - 主键：`id`（BIGINT，ULID）
   - 唯一约束：`uk_tenant_idem`（租户 + 幂等键唯一）
   - 索引：`idx_tenant_member_time`（会员流水查询）
   - 索引：`idx_tenant_biz`（业务流水查询）
   - 字段：`id`, `tenant_id`, `member_id`, `direction`, `delta_points`, `before_available`, `before_frozen`, `after_available`, `after_frozen`, `biz_type`, `biz_id`, `idempotency_key`, `remark`, `created_at`

## 功能特性

### 1. 会员管理

- ✅ **幂等创建会员**：`getOrCreateMember(tenantId, userId)`，多次调用返回同一个会员
- ✅ **会员查询**：支持按会员ID、用户ID查询
- ✅ **会员状态管理**：ACTIVE、INACTIVE、FROZEN

### 2. 积分账本

- ✅ **赚取积分（EARN）**：订单完成、签到奖励等，增加可用积分
- ✅ **冻结积分（FREEZE）**：下单时锁定积分，等待支付完成
- ✅ **释放积分（RELEASE）**：订单取消或支付超时，将冻结积分恢复为可用
- ✅ **回退积分（REVERT）**：退款返还积分
- ✅ **调整积分（ADJUST）**：管理员手动调整（补偿、修正等）

### 3. 幂等设计

- ✅ **幂等键格式**：`{tenantId}:{bizType}:{bizId}:{operation}`
- ✅ **数据库约束**：`uk_tenant_idem` 唯一约束保证幂等
- ✅ **业务逻辑**：操作前先查询是否已存在相同幂等键的流水记录

### 4. 并发安全

- ✅ **乐观锁**：积分账户使用 `version` 字段实现乐观锁
- ✅ **重试机制**：乐观锁冲突时自动重试（最多 3 次）
- ✅ **事务保证**：所有操作都在事务中执行，保证原子性

### 5. 可对账性

- ✅ **余额快照**：每条流水记录都包含变动前后的余额
- ✅ **完整流水**：所有积分变动都记录流水，支持审计
- ✅ **业务关联**：流水记录关联业务类型和业务ID，便于查询

## 集成示例

### 在 app-order 中的集成

**位置**：`app-order/src/main/java/com/bluecone/app/order/application/MemberPointsIntegrationExample.java`

**功能**：展示如何在订单模块中查询会员积分余额（预留接口，暂不参与计价）

### 使用示例

```java
// 1. 注入 Facade
@Autowired
private MemberQueryFacade memberQueryFacade;

@Autowired
private PointsAssetFacade pointsAssetFacade;

// 2. 获取或创建会员
MemberDTO member = memberQueryFacade.getOrCreateMember(tenantId, userId);

// 3. 查询积分余额
PointsBalanceDTO balance = memberQueryFacade.getPointsBalance(tenantId, member.getMemberId());

// 4. 赚取积分（订单完成）
PointsOperationCommand command = new PointsOperationCommand(
    tenantId, memberId, 100L,
    "ORDER_COMPLETE", orderId.toString(), 
    String.format("%d:ORDER_COMPLETE:%s:earn", tenantId, orderId)
);
command.setRemark("订单完成奖励");
PointsOperationResult result = pointsAssetFacade.commitPoints(command);
```

## 测试

### 集成测试

**位置**：`app-member/src/test/java/com/bluecone/app/member/MemberServiceIT.java`

**测试用例**：

1. **会员创建幂等性测试**：多次调用 `getOrCreateMember`，验证返回同一个会员
2. **积分赚取幂等性测试**：使用相同幂等键多次调用 `earnPoints`，验证只增加一次积分

### 运行测试

```bash
mvn test -pl app-member
```

## 文档

### 详细设计文档

**位置**：`docs/MEMBER-POINTS-LEDGER.md`

**内容**：
- 数据模型详解
- 积分操作流程
- 幂等键规则
- 与订单模块对接示例
- 并发安全机制
- 可对账性设计

### 模块使用文档

**位置**：`app-member/README.md`

**内容**：
- 功能特性
- 模块结构
- 使用方式
- API 接口
- 注意事项

## 依赖变更

### 根 pom.xml

添加了两个新模块：

```xml
<!-- Business modules -->
<module>app-member-api</module>
<module>app-member</module>
```

添加了依赖管理：

```xml
<!-- Member modules -->
<dependency>
    <groupId>com.bluecone</groupId>
    <artifactId>app-member-api</artifactId>
    <version>${project.version}</version>
</dependency>

<dependency>
    <groupId>com.bluecone</groupId>
    <artifactId>app-member</artifactId>
    <version>${project.version}</version>
</dependency>
```

### app-order/pom.xml

添加了 app-member-api 依赖：

```xml
<dependency>
    <groupId>com.bluecone</groupId>
    <artifactId>app-member-api</artifactId>
    <version>${project.version}</version>
</dependency>
```

### app-id-api/IdScope.java

添加了会员相关的 ID 作用域：

```java
/**
 * 会员作用域，对应 bc_member 表
 */
MEMBER,

/**
 * 积分账户作用域，对应 bc_points_account 表
 */
POINTS_ACCOUNT,

/**
 * 积分流水作用域，对应 bc_points_ledger 表
 */
POINTS_LEDGER;
```

## 构建验证

所有模块编译通过：

```bash
cd /Users/zhenpengmu/Desktop/code/project/bluecone-app
mvn clean compile -pl app-member-api,app-member -am -DskipTests
```

**构建结果**：✅ BUILD SUCCESS

## 核心设计原则

### 1. 低耦合

- 业务模块只依赖 `app-member-api`，不依赖实现
- 通过 Facade 接口提供服务，实现与调用方解耦

### 2. 高内聚

- DDD 分层架构：domain、application、infra、api/impl
- 领域逻辑集中在 domain 层，应用层只做编排

### 3. 幂等设计

- 所有操作都支持幂等，避免重复扣减/增加
- 幂等键 + 数据库唯一约束 + 业务逻辑三重保障

### 4. 并发安全

- 乐观锁 + 重试机制，保证高并发场景下的数据一致性
- 事务保证账户更新和流水记录的原子性

### 5. 可对账

- 完整的流水记录，包含变动前后余额快照
- 支持按业务维度查询流水，便于审计和对账

### 6. 多租户隔离

- 所有表都带 `tenant_id`，保证租户数据隔离
- 所有查询都带租户条件，防止数据泄露

## 后续扩展建议

1. **积分过期机制**：定时任务扫描并处理过期积分
2. **积分兑换**：支持积分兑换券、商品等
3. **积分转赠**：会员之间转赠积分
4. **积分等级**：根据累计积分设置会员等级
5. **储值账户**：类似积分账本的设计，实现储值卡功能
6. **优惠券系统**：实现券的发放、使用、核销等功能

## 总结

成功实现了完整的会员与积分账本体系，核心特点：

✅ **多租户隔离**：所有数据都带 `tenant_id`，保证租户数据隔离  
✅ **幂等设计**：所有操作都支持幂等，避免重复扣减/增加积分  
✅ **并发安全**：乐观锁 + 幂等键，保证高并发场景下的数据一致性  
✅ **可对账**：完整的流水记录，支持余额验证和业务审计  
✅ **低耦合**：通过 `app-member-api` 提供接口，业务模块只依赖 API，不依赖实现  
✅ **DDD 分层**：清晰的领域模型，易于维护和扩展  

本次实现为 MVP 版本，已满足基础的会员管理和积分账本需求，为后续扩展（券、储值等）打下了坚实的基础。

---

**实现者**：Cursor AI  
**实现时间**：2025-12-18  
**版本**：M5-MVP
