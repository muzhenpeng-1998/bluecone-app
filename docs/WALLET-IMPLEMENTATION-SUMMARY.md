# 储值钱包（app-wallet）实现总结

## 实施概览

本文档总结 **app-wallet（储值钱包）** 模块的实现，包括核心功能、技术架构、集成点和后续工作。

**实施日期**: 2025-12-19  
**版本**: M5 (Milestone 5)

---

## 已完成功能

### ✅ 1. 模块与 POM 配置

**新增模块**:
- `app-wallet-api`: 钱包 API 接口定义（无实现依赖）
- `app-wallet`: 钱包实现模块

**依赖配置**:
- `app-order` 依赖 `app-wallet-api`
- `app-payment` 依赖 `app-wallet-api`
- 遵循模块隔离原则：业务模块只依赖 API 模块

**POM 文件**:
- `app-wallet/pom.xml`
- `app-wallet-api/pom.xml`
- `app-order/pom.xml` (已添加 wallet-api 依赖)
- `app-payment/pom.xml` (已添加 wallet-api 依赖)

---

### ✅ 2. 数据库迁移脚本

**迁移脚本**: `V20251218007__create_wallet_tables.sql`

**创建的表**:

#### bc_wallet_account (钱包账户表)
- 可用余额 (available_balance)
- 冻结余额 (frozen_balance)
- 累计充值/消费金额
- 乐观锁版本号 (version)
- 唯一约束: `uk_tenant_user (tenant_id, user_id)`

#### bc_wallet_ledger (钱包账本流水表)
- 流水号 (ledger_no, PublicId 格式: wl_xxx)
- 业务类型 (biz_type: RECHARGE/ORDER_PAY/REFUND/ADJUST)
- 变更金额 (amount: 正数=入账，负数=出账)
- 变更前后余额 (balance_before, balance_after)
- 幂等键 (idem_key)
- 唯一约束: `uk_tenant_idem_key (tenant_id, idem_key)`

#### bc_wallet_freeze (钱包冻结记录表)
- 冻结单号 (freeze_no, PublicId 格式: wfz_xxx)
- 冻结金额 (frozen_amount)
- 冻结状态 (status: FROZEN/COMMITTED/RELEASED/REVERTED)
- 过期时间 (expires_at, 默认30分钟)
- 幂等键 (idem_key)
- 唯一约束: `uk_tenant_idem_key (tenant_id, idem_key)`

#### bc_wallet_recharge_order (充值单表)
- 充值单号 (recharge_id, PublicId 格式: wrc_xxx)
- 充值金额 (recharge_amount)
- 赠送金额 (bonus_amount)
- 充值状态 (status: INIT/PAYING/SUCCESS/FAILED/CLOSED)
- 幂等键 (idem_key)
- 唯一约束: `uk_tenant_idem_key (tenant_id, idem_key)`

**索引设计**:
- 租户隔离索引 (tenant_id)
- 用户查询索引 (tenant_id, user_id)
- 业务单关联索引 (tenant_id, biz_type, biz_order_id)
- 超时扫描索引 (status, expires_at)

---

### ✅ 3. API 接口定义 (app-wallet-api)

#### WalletQueryFacade (查询门面)
```java
// 查询用户钱包余额
WalletBalanceDTO getBalance(Long tenantId, Long userId);

// 查询或创建用户钱包账户（如果不存在则创建）
WalletBalanceDTO getOrCreateBalance(Long tenantId, Long userId);

// 检查用户余额是否足够
boolean hasEnoughBalance(Long tenantId, Long userId, BigDecimal requiredAmount);
```

#### WalletAssetFacade (资产操作门面)
```java
// 冻结余额（下单锁定）
WalletAssetResult freeze(WalletAssetCommand command);

// 提交余额变更（支付成功后提交扣减）
WalletAssetResult commit(WalletAssetCommand command);

// 释放冻结余额（取消订单/超时）
WalletAssetResult release(WalletAssetCommand command);

// 回退余额变更（退款返还）
WalletAssetResult revert(WalletAssetCommand command);
```

#### WalletRechargeFacade (充值门面，预留)
```java
// 创建充值单（预留，先空实现）
String createRechargeOrder(Long tenantId, Long userId, BigDecimal rechargeAmount, String idempotencyKey);

// 充值支付成功回调（预留，先空实现）
void onRechargePaid(String rechargeId, Long payOrderId, String payNo);
```

**DTO 定义**:
- `WalletAssetCommand`: 钱包资产操作命令
- `WalletAssetResult`: 钱包资产操作结果
- `WalletBalanceDTO`: 钱包余额 DTO

---

### ✅ 4. 领域服务实现 (app-wallet)

#### WalletDomainService (核心业务逻辑)

**freeze (冻结余额)**:
- 检查可用余额是否足够
- 账户：`available` → `frozen`
- 写入冻结记录（`bc_wallet_freeze`）
- 幂等：重复调用返回已冻结结果
- 并发控制：乐观锁 (version)

**commit (提交冻结)**:
- 检查冻结记录状态必须是 `FROZEN`
- 冻结记录：`FROZEN` → `COMMITTED`
- 账户：`frozen` → 扣减
- 写入账本流水（`bc_wallet_ledger`）：`ORDER_PAY`（出账）
- 幂等：重复调用返回已提交结果

**release (释放冻结)**:
- 检查冻结记录状态必须是 `FROZEN`
- 冻结记录：`FROZEN` → `RELEASED`
- 账户：`frozen` → `available`
- 不写入账本流水（只是状态恢复）
- 幂等：重复调用不报错，直接返回

**revert (回退余额)**:
- 账户：`available` 增加
- 写入账本流水（`bc_wallet_ledger`）：`REFUND`（入账）
- 幂等：重复调用返回已回退结果

#### 仓储实现

**WalletAccountRepositoryImpl**:
- 查询账户（by userId）
- 创建账户（with ID generation）
- 乐观锁更新账户（updateWithVersion）
- 获取或创建账户（getOrCreate）

**WalletFreezeRepositoryImpl**:
- 查询冻结记录（by idemKey / by bizOrderId）
- 创建冻结记录（with ID generation）
- 乐观锁更新冻结记录（updateWithVersion）
- 查询过期冻结记录（for 补偿机制）

**WalletLedgerRepositoryImpl**:
- 查询账本流水（by idemKey / by userId / by accountId）
- 创建账本流水（with ID generation）

---

### ✅ 5. Facade 实现 (app-wallet)

#### WalletAssetFacadeImpl
- 实现 `WalletAssetFacade` 接口
- 调用 `WalletDomainService` 执行业务逻辑
- 参数校验、异常处理、日志记录
- 返回统一的 `WalletAssetResult`

#### WalletQueryFacadeImpl
- 实现 `WalletQueryFacade` 接口
- 调用 `WalletDomainService` 查询余额
- 转换领域模型为 DTO

#### WalletRechargeFacadeImpl
- 实现 `WalletRechargeFacade` 接口
- 空实现（抛出 `UnsupportedOperationException`）
- 预留充值功能接口

---

### ✅ 6. 订单集成

#### Precheck (订单预览)

**集成点**: `UserOrderPreviewAppServiceImpl.preview()`

**功能**:
- 查询用户钱包余额
- 判断余额是否足够支付本订单
- 返回给前端展示

**响应扩展**:
```java
response.setWalletBalance(WalletBalanceInfo.builder()
    .availableBalance(balance.getAvailableBalance())
    .frozenBalance(balance.getFrozenBalance())
    .totalBalance(balance.getTotalBalance())
    .sufficient(sufficient) // 余额是否足够
    .currency("CNY")
    .build());
```

#### Checkout (订单下单)

**集成点**: `OrderConfirmAppServiceImpl.confirmOrder()`

**功能**:
- 用户选择钱包余额支付时（`useWalletBalance = true`）
- 冻结订单金额（`freeze`）
- 立即完成钱包支付（`commit`）
- 标记订单为已支付（`WAIT_PAY` → `PAID` → `WAIT_ACCEPT`）

**请求扩展**:
```java
// ConfirmOrderRequest 新增字段
private Boolean useWalletBalance = Boolean.FALSE;
```

**流程**:
```
1. 创建订单（状态：WAIT_PAY）
2. 冻结余额（available → frozen）
3. 提交冻结（frozen → 扣减，写账本流水）
4. 标记订单已支付（WAIT_PAY → PAID → WAIT_ACCEPT）
```

#### Cancel (订单取消)

**集成点**: `OrderCancelAppServiceImpl.cancelOrder()`

**功能**:
- 订单未支付时（`WAIT_PAY`），释放冻结余额
- 冻结记录：`FROZEN` → `RELEASED`
- 账户：`frozen` → `available`

**流程**:
```
1. 取消订单（WAIT_PAY → CANCELED）
2. 释放冻结余额（frozen → available）
```

#### Refund (订单退款)

**集成点**: `RefundAppServiceImpl.applyRefund()`

**功能**:
- 钱包支付订单退款时，回退余额变更
- 账户：`available` 增加
- 写入账本流水：`REFUND`（入账）

**流程**:
```
1. 创建退款单
2. 调用支付网关退款（Mock）
3. 回退钱包余额（available 增加，写账本流水）
4. 标记订单已退款（PAID → REFUNDED）
```

---

### ✅ 7. 幂等性设计

#### 幂等键规则

| 操作 | 幂等键格式 | 唯一约束表 |
|------|-----------|-----------|
| freeze | `{tenantId}:{userId}:{orderId}:freeze` | `bc_wallet_freeze.uk_tenant_idem_key` |
| commit | `{tenantId}:{userId}:{orderId}:commit` | `bc_wallet_ledger.uk_tenant_idem_key` |
| release | `{tenantId}:{userId}:{orderId}:release` | 无（状态检查） |
| revert | `{tenantId}:{userId}:{orderId}:refund` | `bc_wallet_ledger.uk_tenant_idem_key` |

#### 幂等实现策略

1. **数据库唯一约束兜底**
   - 所有写操作都有唯一约束（`idem_key`）
   - 并发情况下，唯一约束冲突时重新查询返回

2. **业务层幂等检查**
   - 写入前先查询是否已存在
   - 已存在时直接返回（避免不必要的数据库操作）

3. **状态幂等**
   - 订单状态变更支持幂等（如已支付时重复支付直接返回）
   - 冻结记录状态变更支持幂等（如已提交时重复提交直接返回）

---

### ✅ 8. 并发控制

#### 账户并发更新

**策略**: 乐观锁 (version)

```java
// 1. 查询账户（带版本号）
WalletAccount account = accountRepository.findByUserId(tenantId, userId);

// 2. 修改余额
account.freeze(amount);

// 3. 乐观锁更新（WHERE version = expectedVersion）
int updated = accountRepository.updateWithVersion(account);
if (updated == 0) {
    throw new BizException("账户余额变更冲突，请重试");
}
```

**为什么选择乐观锁**:
- 钱包余额变更频率较低，冲突概率小
- 乐观锁性能优于悲观锁（无需加锁等待）
- 冲突时抛异常，前端重试即可

#### 冻结记录并发控制

**策略**: 唯一约束 + 乐观锁

```java
// 1. 幂等性检查（查询是否已存在）
WalletFreeze existingFreeze = freezeRepository.findByIdemKey(tenantId, idemKey);
if (existingFreeze != null) {
    return existingFreeze; // 幂等返回
}

// 2. 创建冻结记录（唯一约束兜底）
try {
    freezeRepository.insert(freeze);
} catch (DuplicateKeyException e) {
    // 并发情况下，唯一约束冲突，重新查询返回
    return freezeRepository.findByIdemKey(tenantId, idemKey);
}
```

---

### ✅ 9. 账本化要求

#### 必须写账本的操作

| 操作 | 业务类型 | 金额符号 | 说明 |
|------|---------|---------|------|
| 充值 | `RECHARGE` | 正数 | 充值入账 |
| 订单支付 | `ORDER_PAY` | 负数 | 订单支付出账 |
| 退款 | `REFUND` | 正数 | 退款返还入账 |
| 管理员调整 | `ADJUST` | 正/负 | 人工调整（预留） |

#### 不写账本的操作

| 操作 | 说明 |
|------|------|
| 释放冻结 | 只是状态恢复，非资金流水 |

#### 账本流水字段

- `amount`: 变更金额（正数=入账，负数=出账）
- `balance_before`: 变更前可用余额
- `balance_after`: 变更后可用余额
- `idem_key`: 幂等键（唯一约束）

---

### ✅ 10. ID 生成扩展

**新增 IdScope**:
```java
WALLET_ACCOUNT,   // 钱包账户
WALLET_LEDGER,    // 钱包账本流水
WALLET_FREEZE,    // 钱包冻结记录
WALLET_RECHARGE   // 钱包充值单
```

**新增 ResourceType**:
```java
WALLET_ACCOUNT("wac"),   // 钱包账户 PublicId: wac_xxx
WALLET_LEDGER("wl"),     // 钱包流水 PublicId: wl_xxx
WALLET_FREEZE("wfz"),    // 钱包冻结 PublicId: wfz_xxx
WALLET_RECHARGE("wrc")   // 钱包充值 PublicId: wrc_xxx
```

---

### ✅ 11. 文档输出

#### WALLET-ORDER-INTEGRATION.md (钱包-订单集成文档)

**包含内容**:
- 架构设计与模块依赖
- 核心概念（账户、冻结、账本）
- 状态机与流转图
- 幂等性设计详解
- 账本化要求说明
- 并发控制策略
- 集成点详细说明（Precheck/Checkout/Cancel/Refund）
- 测试策略（单元测试、集成测试、补偿机制）
- 常见问题 (FAQ)

---

## 技术亮点

### 1. 模块隔离设计

- API 模块与实现模块分离
- 业务模块只依赖 API 模块
- 符合依赖倒置原则（DIP）

### 2. 幂等性保证

- 数据库唯一约束兜底
- 业务层幂等检查
- 状态幂等设计
- 支持重复调用不报错

### 3. 账本化设计

- 所有资金变更必须写账本
- 记录变更前后余额
- 支持审计和对账

### 4. 并发控制

- 乐观锁保证账户并发安全
- 唯一约束保证冻结记录幂等
- 冲突时抛异常，前端重试

### 5. 状态机设计

- 清晰的状态流转规则
- 支持幂等性（已完成状态重复调用直接返回）
- 支持补偿机制（超时自动释放）

---

## 待完成工作

### 🔲 1. 单元测试 (TODO 10)

**需要测试的场景**:
- 幂等性测试（重复调用返回相同结果）
- 并发测试（并发冻结同一账户余额不会超扣）
- 冻结超时补偿测试（扫描释放逻辑）
- 余额不足测试
- 乐观锁冲突测试

**测试文件位置**:
- `app-wallet/src/test/java/com/bluecone/app/wallet/domain/service/WalletDomainServiceTest.java`
- `app-wallet/src/test/java/com/bluecone/app/wallet/application/facade/WalletAssetFacadeTest.java`

### 🔲 2. 集成测试 (TODO 11)

**需要测试的场景**:
- 完整支付流程测试（充值 → 下单 → 支付 → 验证余额和账本）
- 取消订单流程测试（下单 → 取消 → 验证余额恢复）
- 退款流程测试（下单 → 支付 → 退款 → 验证余额返还）
- 并发下单测试（多个订单并发冻结余额）

**测试文件位置**:
- `app-order/src/test/java/com/bluecone/app/order/integration/WalletPaymentIntegrationTest.java`

### 🔲 3. 冻结超时补偿机制

**实现方式**:
```java
@Scheduled(fixedDelay = 60000) // 每分钟执行一次
public void releaseExpiredFreezes() {
    List<WalletFreeze> expiredFreezes = freezeRepository.findExpiredFreezes(
        LocalDateTime.now(), 100
    );
    
    for (WalletFreeze freeze : expiredFreezes) {
        walletPaymentService.releaseWalletFreeze(
            freeze.getTenantId(), 
            freeze.getUserId(), 
            freeze.getBizOrderId()
        );
    }
}
```

**实现位置**:
- `app-wallet/src/main/java/com/bluecone/app/wallet/application/scheduler/WalletFreezeCompensationScheduler.java`

### 🔲 4. 充值功能实现

**需要实现**:
- `WalletRechargeFacadeImpl.createRechargeOrder()`
- `WalletRechargeFacadeImpl.onRechargePaid()`
- 充值单状态机（INIT → PAYING → SUCCESS/FAILED）
- 充值成功后写入账本流水（RECHARGE）

### 🔲 5. 监控与告警

**需要监控的指标**:
- 钱包余额变更 QPS
- 冻结超时数量
- 乐观锁冲突次数
- 账本流水写入延迟

**告警规则**:
- 冻结超时数量 > 阈值
- 乐观锁冲突率 > 阈值
- 账本流水写入失败

---

## 部署清单

### 1. 数据库迁移

```bash
# 执行 Flyway 迁移
mvn flyway:migrate
```

**迁移脚本**:
- `V20251218007__create_wallet_tables.sql`

### 2. 应用部署

```bash
# 编译打包
mvn clean package -DskipTests

# 启动应用
java -jar app-application/target/app-application-1.0.0-SNAPSHOT.jar
```

### 3. 验证

```bash
# 1. 查询钱包余额（应返回 null 或 0）
curl -X GET http://localhost:8080/api/wallet/balance?tenantId=1&userId=1

# 2. 下单（选择钱包支付）
curl -X POST http://localhost:8080/api/order/user/orders/submit \
  -H "Content-Type: application/json" \
  -d '{
    "useWalletBalance": true,
    "items": [{"skuId": 1, "quantity": 1}],
    ...
  }'

# 3. 查询账本流水
curl -X GET http://localhost:8080/api/wallet/ledger?tenantId=1&userId=1
```

---

## 风险与注意事项

### 1. 余额不足处理

**风险**: 用户余额不足时下单失败，体验不佳

**建议**:
- 前端预览时展示余额是否足够
- 下单时前端校验余额
- 后端冻结时返回友好错误信息

### 2. 并发冲突处理

**风险**: 高并发场景下乐观锁冲突率较高

**建议**:
- 前端重试机制（指数退避）
- 监控乐观锁冲突率
- 必要时考虑悲观锁或分布式锁

### 3. 冻结超时处理

**风险**: 用户下单后长时间未支付，余额被冻结

**建议**:
- 设置合理的过期时间（默认30分钟）
- 实现定时任务自动释放过期冻结
- 通知用户余额已释放

### 4. 账本流水一致性

**风险**: 账本流水写入失败导致余额与流水不一致

**建议**:
- 使用事务保证原子性
- 实现补偿机制（定时任务对账）
- 监控账本流水写入失败次数

---

## 后续优化方向

### 1. 性能优化

- 账户余额缓存（Redis）
- 账本流水异步写入（MQ）
- 冻结记录分库分表

### 2. 功能扩展

- 支持多币种
- 支持赠送金额（充值活动）
- 支持余额提现
- 支持余额转账

### 3. 监控与运维

- 实时监控余额变更
- 账本流水对账工具
- 冻结超时告警
- 异常订单补偿工具

---

## 总结

本次实现完成了 **app-wallet（储值钱包）** 模块的核心功能，并成功集成到订单支付流程中。主要亮点包括：

1. **模块化设计**: API 与实现分离，符合依赖倒置原则
2. **幂等性保证**: 数据库唯一约束 + 业务层幂等检查
3. **账本化设计**: 所有资金变更必须写账本，支持审计和对账
4. **并发控制**: 乐观锁保证账户并发安全
5. **状态机设计**: 清晰的状态流转规则，支持幂等和补偿

后续需要完成单元测试、集成测试和补偿机制，确保系统稳定性和可靠性。

---

## 附录

### 相关文档

- [钱包-订单集成文档](WALLET-ORDER-INTEGRATION.md)
- [订单状态机文档](ORDER-STATUS-CONSOLIDATION-V1.md)
- [订单快速开始](ORDER-M0-QUICK-START.md)
- [支付实现总结](PAYMENT-M1-IMPLEMENTATION-SUMMARY.md)

### 代码位置

- **API 模块**: `app-wallet-api/src/main/java/com/bluecone/app/wallet/api/`
- **实现模块**: `app-wallet/src/main/java/com/bluecone/app/wallet/`
- **订单集成**: `app-order/src/main/java/com/bluecone/app/order/application/`
- **迁移脚本**: `app-infra/src/main/resources/db/migration/V20251218007__create_wallet_tables.sql`
- **文档**: `docs/WALLET-ORDER-INTEGRATION.md`
