# 会员与积分账本设计文档

## 概述

`app-member` 模块实现了餐饮 SaaS 的会员与积分账本体系，支持多租户隔离、幂等操作、并发安全、可对账等核心要求。

本文档说明积分账本模型、幂等键规则、与订单/支付模块的对接建议。

## 模块结构

### app-member-api（对外契约）

提供对外的 DTO 和 Facade 接口，供其他业务模块（如 `app-order`、`app-payment`）调用。

**核心接口：**

1. **MemberQueryFacade**：会员查询
   - `getOrCreateMember(tenantId, userId)`：获取或创建会员（幂等）
   - `getMemberById(tenantId, memberId)`：根据会员ID查询
   - `getMemberByUserId(tenantId, userId)`：根据用户ID查询
   - `getPointsBalance(tenantId, memberId)`：查询积分余额

2. **PointsAssetFacade**：积分资产操作
   - `freezePoints(command)`：冻结积分（下单锁定）
   - `commitPoints(command)`：提交积分变更（支付成功后扣减/入账）
   - `releasePoints(command)`：释放冻结积分（取消/超时）
   - `revertPoints(command)`：回退积分变更（退款返还）
   - `adjustPoints(command, isIncrease)`：调整积分（管理员手动调整）

### app-member（领域实现）

采用 DDD 分层架构：

- **domain**：领域层
  - `model/`：聚合根（Member、PointsAccount、PointsLedger）
  - `enums/`：枚举（MemberStatus、PointsDirection、PointsBizType）
  - `repository/`：仓储接口
  - `service/`：领域服务（PointsDomainService）

- **application**：应用层
  - `service/`：应用服务（MemberApplicationService、PointsApplicationService）
  - 编排领域对象和服务，处理业务用例

- **infra**：基础设施层
  - `persistence/`：数据持久化（PO、Mapper、Repository 实现）
  - `converter/`：实体与 PO 转换器

- **api/impl**：Facade 实现层
  - 实现 `app-member-api` 中定义的接口

## 数据模型

### 1. bc_member（会员表）

```sql
CREATE TABLE bc_member (
    id                  BIGINT          NOT NULL COMMENT '会员ID（内部主键，ULID）',
    tenant_id           BIGINT          NOT NULL COMMENT '租户ID',
    user_id             BIGINT          NOT NULL COMMENT '平台用户ID',
    member_no           VARCHAR(64)     NOT NULL COMMENT '会员号（租户内唯一）',
    status              VARCHAR(32)     NOT NULL DEFAULT 'ACTIVE' COMMENT '会员状态',
    created_at          DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_tenant_user (tenant_id, user_id),
    UNIQUE KEY uk_tenant_member_no (tenant_id, member_no)
);
```

**设计要点：**
- `tenant_id + user_id` 唯一约束：一个用户在一个租户只能有一个会员
- `member_no`：租户内唯一的会员编号，格式：`mb_{memberId}`

### 2. bc_points_account（积分账户表）

```sql
CREATE TABLE bc_points_account (
    id                  BIGINT          NOT NULL COMMENT '账户ID（内部主键，ULID）',
    tenant_id           BIGINT          NOT NULL COMMENT '租户ID',
    member_id           BIGINT          NOT NULL COMMENT '会员ID',
    available_points    BIGINT          NOT NULL DEFAULT 0 COMMENT '可用积分',
    frozen_points       BIGINT          NOT NULL DEFAULT 0 COMMENT '冻结积分',
    version             INT             NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
    created_at          DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_tenant_member (tenant_id, member_id)
);
```

**设计要点：**
- `version`：乐观锁版本号，保证并发更新的安全性
- `available_points`：可用积分，可直接使用
- `frozen_points`：冻结积分，下单时锁定，等待支付完成或取消

### 3. bc_points_ledger（积分流水表）

```sql
CREATE TABLE bc_points_ledger (
    id                  BIGINT          NOT NULL COMMENT '流水ID（内部主键，ULID）',
    tenant_id           BIGINT          NOT NULL COMMENT '租户ID',
    member_id           BIGINT          NOT NULL COMMENT '会员ID',
    direction           VARCHAR(32)     NOT NULL COMMENT '变动方向',
    delta_points        BIGINT          NOT NULL COMMENT '变动积分值（正数）',
    before_available    BIGINT          NOT NULL DEFAULT 0 COMMENT '变动前可用积分',
    before_frozen       BIGINT          NOT NULL DEFAULT 0 COMMENT '变动前冻结积分',
    after_available     BIGINT          NOT NULL DEFAULT 0 COMMENT '变动后可用积分',
    after_frozen        BIGINT          NOT NULL DEFAULT 0 COMMENT '变动后冻结积分',
    biz_type            VARCHAR(64)     NOT NULL COMMENT '业务类型',
    biz_id              VARCHAR(128)    NOT NULL COMMENT '业务ID',
    idempotency_key     VARCHAR(256)    NOT NULL COMMENT '幂等键',
    remark              VARCHAR(512)    DEFAULT NULL COMMENT '备注说明',
    created_at          DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_tenant_idem (tenant_id, idempotency_key),
    KEY idx_tenant_member_time (tenant_id, member_id, created_at),
    KEY idx_tenant_biz (tenant_id, biz_type, biz_id)
);
```

**设计要点：**
- `idempotency_key`：幂等键，全局唯一，防止重复操作
- `before_*` 和 `after_*`：记录变动前后的余额快照，便于对账和审计
- `direction`：变动方向（EARN、SPEND、FREEZE、RELEASE、REVERT、ADJUST）

## 积分操作流程

### 1. 冻结积分（FREEZE）

**场景**：下单时锁定积分，等待支付完成

```
available_points -= delta_points
frozen_points += delta_points
```

**流水记录**：
- `direction`：FREEZE
- `delta_points`：冻结的积分值

### 2. 提交扣减（SPEND）

**场景**：支付成功后，从冻结积分中扣除

```
frozen_points -= delta_points
```

**流水记录**：
- `direction`：SPEND
- `delta_points`：扣减的积分值

### 3. 提交入账（EARN）

**场景**：订单完成、签到奖励等，增加可用积分

```
available_points += delta_points
```

**流水记录**：
- `direction`：EARN
- `delta_points`：增加的积分值

### 4. 释放积分（RELEASE）

**场景**：订单取消或支付超时，将冻结积分恢复为可用

```
frozen_points -= delta_points
available_points += delta_points
```

**流水记录**：
- `direction`：RELEASE
- `delta_points`：释放的积分值

### 5. 回退积分（REVERT）

**场景**：退款返还积分

```
available_points += delta_points
```

**流水记录**：
- `direction`：REVERT
- `delta_points`：回退的积分值

### 6. 调整积分（ADJUST）

**场景**：管理员手动调整（补偿、修正等）

```
available_points += delta_points  // 增加
available_points -= delta_points  // 减少
```

**流水记录**：
- `direction`：ADJUST
- `delta_points`：调整的积分值

## 幂等键规则

### 幂等键格式

```
{tenantId}:{bizType}:{bizId}:{operation}
```

### 示例

1. **订单完成赚取积分**
   ```
   123:ORDER_COMPLETE:order_abc123:earn
   ```

2. **退款返还积分**
   ```
   123:REFUND:refund_xyz456:revert
   ```

3. **下单冻结积分**
   ```
   123:ORDER_PAY:order_abc123:freeze
   ```

4. **订单取消释放积分**
   ```
   123:ORDER_CANCEL:order_abc123:release
   ```

### 幂等性保证

- **数据库约束**：`uk_tenant_idem (tenant_id, idempotency_key)` 唯一约束
- **业务逻辑**：操作前先查询是否已存在相同幂等键的流水记录
- **并发处理**：幂等键冲突时，直接返回已有流水记录

## 与订单模块对接

### 场景 1：订单提交时冻结积分（可选）

```java
// 在订单提交流程中（如果启用积分抵扣）
PointsOperationCommand command = new PointsOperationCommand(
    tenantId, 
    memberId, 
    pointsToUse,
    "ORDER_PAY", 
    orderId.toString(), 
    buildIdempotencyKey(tenantId, "ORDER_PAY", orderId, "freeze")
);
command.setRemark("下单冻结积分");
PointsOperationResult result = pointsAssetFacade.freezePoints(command);
```

### 场景 2：支付成功后扣减冻结积分

```java
// 在支付成功回调中
PointsOperationCommand command = new PointsOperationCommand(
    tenantId, 
    memberId, 
    frozenPoints,
    "ORDER_PAY", 
    orderId.toString(), 
    buildIdempotencyKey(tenantId, "ORDER_PAY", orderId, "commit")
);
command.setRemark("支付成功扣减积分");
PointsOperationResult result = pointsAssetFacade.commitPoints(command);
```

### 场景 3：订单完成赚取积分

```java
// 在订单完成流程中
PointsOperationCommand command = new PointsOperationCommand(
    tenantId, 
    memberId, 
    rewardPoints,
    "ORDER_COMPLETE", 
    orderId.toString(), 
    buildIdempotencyKey(tenantId, "ORDER_COMPLETE", orderId, "earn")
);
command.setRemark("订单完成奖励");
PointsOperationResult result = pointsAssetFacade.commitPoints(command);
```

### 场景 4：订单取消释放积分

```java
// 在订单取消流程中
PointsOperationCommand command = new PointsOperationCommand(
    tenantId, 
    memberId, 
    frozenPoints,
    "ORDER_CANCEL", 
    orderId.toString(), 
    buildIdempotencyKey(tenantId, "ORDER_CANCEL", orderId, "release")
);
command.setRemark("订单取消释放积分");
PointsOperationResult result = pointsAssetFacade.releasePoints(command);
```

### 场景 5：退款返还积分

```java
// 在退款成功流程中
PointsOperationCommand command = new PointsOperationCommand(
    tenantId, 
    memberId, 
    usedPoints,
    "REFUND", 
    refundId.toString(), 
    buildIdempotencyKey(tenantId, "REFUND", refundId, "revert")
);
command.setRemark("退款返还积分");
PointsOperationResult result = pointsAssetFacade.revertPoints(command);
```

## 并发安全机制

### 1. 乐观锁（账户更新）

积分账户表使用 `version` 字段实现乐观锁：

```java
UPDATE bc_points_account 
SET available_points = #{availablePoints}, 
    frozen_points = #{frozenPoints}, 
    version = version + 1,
    updated_at = NOW() 
WHERE id = #{id} AND version = #{version}
```

更新失败时，重新查询最新版本并重试（最多 3 次）。

### 2. 幂等键（流水记录）

积分流水表使用 `idempotency_key` 唯一约束：

```sql
UNIQUE KEY uk_tenant_idem (tenant_id, idempotency_key)
```

插入流水记录时，如果幂等键冲突，说明已处理过，直接返回已有记录。

### 3. 事务保证

所有积分操作都在事务中执行，保证账户更新和流水记录的原子性：

```java
@Transactional(rollbackFor = Exception.class)
public PointsLedger earnPoints(...) {
    // 1. 幂等性检查
    // 2. 查询账户
    // 3. 更新账户
    // 4. 保存流水
}
```

## 可对账性

### 1. 余额快照

每条流水记录都包含变动前后的余额快照：

- `before_available`：变动前可用积分
- `before_frozen`：变动前冻结积分
- `after_available`：变动后可用积分
- `after_frozen`：变动后冻结积分

### 2. 余额验证

通过流水记录可以验证账户余额的正确性：

```sql
-- 计算某会员的最终余额（从流水重建）
SELECT 
    member_id,
    SUM(after_available) - SUM(before_available) AS total_available_delta,
    SUM(after_frozen) - SUM(before_frozen) AS total_frozen_delta
FROM bc_points_ledger
WHERE tenant_id = ? AND member_id = ?
GROUP BY member_id;
```

### 3. 审计查询

支持按业务维度查询流水：

```sql
-- 查询某个订单相关的所有积分流水
SELECT * FROM bc_points_ledger
WHERE tenant_id = ? 
  AND biz_type IN ('ORDER_PAY', 'ORDER_COMPLETE', 'ORDER_CANCEL')
  AND biz_id = ?
ORDER BY created_at;
```

## 测试

### 集成测试

参见 `app-member/src/test/java/com/bluecone/app/member/MemberServiceIT.java`

测试用例：

1. **会员创建幂等性**：多次调用 `getOrCreateMember`，验证返回同一个会员
2. **积分赚取幂等性**：使用相同幂等键多次调用 `earnPoints`，验证只增加一次积分

### 测试运行

```bash
# 运行集成测试
mvn test -pl app-member
```

## 集成示例

参见 `app-order/src/main/java/com/bluecone/app/order/application/MemberPointsIntegrationExample.java`

展示如何在订单模块中：
1. 查询用户的会员积分余额
2. 处理会员服务未启用的情况（优雅降级）

## 后续扩展

当前实现为 MVP 版本，后续可扩展的功能：

1. **积分过期机制**：定时任务扫描并处理过期积分
2. **积分兑换**：支持积分兑换券、商品等
3. **积分转赠**：会员之间转赠积分
4. **积分等级**：根据累计积分设置会员等级
5. **储值账户**：类似积分账本的设计，实现储值卡功能
6. **优惠券系统**：实现券的发放、使用、核销等功能

## 总结

`app-member` 模块提供了完整的会员与积分账本能力，核心特点：

1. **多租户隔离**：所有数据都带 `tenant_id`，保证租户数据隔离
2. **幂等设计**：所有操作都支持幂等，避免重复扣减/增加积分
3. **并发安全**：乐观锁 + 幂等键，保证高并发场景下的数据一致性
4. **可对账**：完整的流水记录，支持余额验证和业务审计
5. **低耦合**：通过 `app-member-api` 提供接口，业务模块只依赖 API，不依赖实现

---

文档更新时间：2025-12-18
