# 钱包充值闭环实现文档

## 概述

本文档描述钱包充值闭环的完整实现，包括：创建充值单、微信支付回调、幂等入账、Outbox 可恢复机制。

## 架构设计

### 核心流程

```
用户发起充值
    ↓
创建充值单 (INIT)
    ↓
拉起微信支付 (PAYING)
    ↓
微信支付回调
    ↓
更新充值单状态 (PAID) + 写入 Outbox 事件
    ↓
Outbox 投递 recharge.paid 事件
    ↓
RechargeEventConsumer 消费事件
    ↓
写入 wallet_ledger (CREDIT) + 更新 wallet_account (乐观锁)
    ↓
充值完成
```

### 幂等性保障

系统在三个层面保障幂等性：

1. **充值单创建幂等**：通过 `idempotency_key` 唯一约束
2. **支付回调幂等**：通过 `channelTradeNo` (pay_no) 唯一约束
3. **入账幂等**：通过 `wallet_ledger.idem_key` 唯一约束

## 数据库表结构

### bc_wallet_recharge_order（充值单表）

```sql
CREATE TABLE bc_wallet_recharge_order (
    id                      BIGINT          NOT NULL COMMENT '充值单ID',
    tenant_id               BIGINT          NOT NULL COMMENT '租户ID',
    user_id                 BIGINT          NOT NULL COMMENT '用户ID',
    account_id              BIGINT          NOT NULL COMMENT '账户ID',
    
    -- 充值单号
    recharge_id             VARCHAR(64)     NOT NULL COMMENT '充值单号（PublicId：wrc_xxx）',
    
    -- 金额（单位：元）
    recharge_amount         DECIMAL(18,2)   NOT NULL COMMENT '充值金额',
    bonus_amount            DECIMAL(18,2)   NOT NULL DEFAULT 0.00 COMMENT '赠送金额',
    total_amount            DECIMAL(18,2)   NOT NULL COMMENT '总到账金额',
    currency                VARCHAR(8)      NOT NULL DEFAULT 'CNY',
    
    -- 状态
    status                  VARCHAR(32)     NOT NULL DEFAULT 'INIT' COMMENT 'INIT/PAYING/PAID/CLOSED',
    
    -- 支付信息
    pay_order_id            BIGINT          DEFAULT NULL COMMENT '支付单ID',
    pay_channel             VARCHAR(32)     DEFAULT NULL COMMENT 'WECHAT/ALIPAY',
    pay_no                  VARCHAR(128)    DEFAULT NULL COMMENT '渠道交易号（微信 transaction_id）',
    
    -- 时间
    recharge_requested_at   DATETIME        NOT NULL COMMENT '充值发起时间',
    recharge_completed_at   DATETIME        DEFAULT NULL COMMENT '充值完成时间',
    
    -- 幂等键
    idem_key                VARCHAR(128)    NOT NULL COMMENT '幂等键（客户端生成）',
    
    -- 扩展
    ext_json                TEXT            DEFAULT NULL,
    
    -- 乐观锁
    version                 INT             NOT NULL DEFAULT 0,
    
    -- 审计
    created_at              DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by              BIGINT          DEFAULT NULL,
    updated_at              DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    updated_by              BIGINT          DEFAULT NULL,
    
    PRIMARY KEY (id),
    UNIQUE KEY uk_tenant_idem_key (tenant_id, idem_key),
    UNIQUE KEY uk_tenant_recharge_id (tenant_id, recharge_id),
    UNIQUE KEY uk_tenant_pay_no (tenant_id, pay_no),  -- 回调幂等关键索引
    KEY idx_tenant_user_status (tenant_id, user_id, status, recharge_requested_at DESC)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='充值单表';
```

**关键索引说明**：
- `uk_tenant_idem_key`：创建充值单幂等
- `uk_tenant_pay_no`：支付回调幂等（同一渠道交易号只能关联一个充值单）
- `uk_tenant_recharge_id`：充值单号唯一

### bc_wallet_ledger（钱包账本表）

```sql
CREATE TABLE bc_wallet_ledger (
    id                  BIGINT          NOT NULL COMMENT '流水ID',
    tenant_id           BIGINT          NOT NULL COMMENT '租户ID',
    user_id             BIGINT          NOT NULL COMMENT '用户ID',
    account_id          BIGINT          NOT NULL COMMENT '账户ID',
    
    ledger_no           VARCHAR(64)     NOT NULL COMMENT '流水号（wl_xxx）',
    
    -- 业务关联
    biz_type            VARCHAR(32)     NOT NULL COMMENT 'RECHARGE/ORDER_PAY/REFUND/ADJUST',
    biz_order_id        BIGINT          DEFAULT NULL COMMENT '业务单ID',
    biz_order_no        VARCHAR(64)     DEFAULT NULL COMMENT '业务单号',
    
    -- 金额变更（单位：元）
    amount              DECIMAL(18,2)   NOT NULL COMMENT '变更金额（正=入账，负=出账）',
    balance_before      DECIMAL(18,2)   NOT NULL COMMENT '变更前余额',
    balance_after       DECIMAL(18,2)   NOT NULL COMMENT '变更后余额',
    currency            VARCHAR(8)      NOT NULL DEFAULT 'CNY',
    
    remark              VARCHAR(256)    DEFAULT NULL,
    
    -- 幂等键（关键）
    idem_key            VARCHAR(128)    NOT NULL COMMENT '幂等键',
    
    created_at          DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by          BIGINT          DEFAULT NULL,
    
    PRIMARY KEY (id),
    UNIQUE KEY uk_tenant_idem_key (tenant_id, idem_key),  -- 入账幂等关键索引
    UNIQUE KEY uk_tenant_ledger_no (tenant_id, ledger_no),
    KEY idx_tenant_user_created (tenant_id, user_id, created_at DESC),
    KEY idx_biz_order (tenant_id, biz_type, biz_order_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='钱包账本表';
```

## 状态机

### 充值单状态流转

```
INIT（初始化）
  ↓ markAsPaying()
PAYING（支付中）
  ↓ markAsPaid()
PAID（已支付，终态）

INIT/PAYING
  ↓ markAsClosed()
CLOSED（已关闭，终态）
```

**状态流转规则**：
- `INIT` → `PAYING` / `CLOSED`
- `PAYING` → `PAID` / `CLOSED`
- `PAID` 和 `CLOSED` 为终态，不可再变更
- 幂等：相同状态可重复设置

## 幂等键规则

### 1. 充值单创建幂等键

**格式**：客户端生成的 UUID 或业务唯一标识

**示例**：
```
uuid-123e4567-e89b-12d3-a456-426614174000
user:123:recharge:20251219120000
```

**用途**：防止客户端重复提交创建充值单

### 2. 支付回调幂等键

**格式**：渠道交易号（微信 `transaction_id`）

**示例**：
```
4200001234567890123456789012345678
```

**用途**：防止微信重复回调导致重复入账

### 3. 账本流水幂等键

**格式**：`{tenantId}:{userId}:recharge:{rechargeNo}:credit`

**示例**：
```
1:123:recharge:wrc_01HQZX1234567890:credit
```

**用途**：防止 Outbox 重试导致重复写入流水

## 核心组件

### 1. RechargeOrderDomainService（充值单领域服务）

**职责**：
- 创建充值单（幂等）
- 状态流转（INIT → PAYING → PAID）
- 乐观锁更新

**关键方法**：

```java
// 创建充值单（幂等）
RechargeOrder createRechargeOrder(
    Long tenantId, Long userId, Long accountId,
    Long rechargeAmountInCents, Long bonusAmountInCents,
    String idempotencyKey
);

// 标记为支付中
void markAsPaying(Long tenantId, String rechargeNo, Long payOrderId, String payChannel);

// 标记为已支付（根据充值单号）
RechargeOrder markAsPaid(Long tenantId, String rechargeNo, String channelTradeNo, LocalDateTime paidAt);

// 标记为已支付（根据渠道交易号，用于回调幂等）
RechargeOrder markAsPaidByChannelTradeNo(Long tenantId, String channelTradeNo, LocalDateTime paidAt);
```

### 2. WalletRechargeFacade（充值门面）

**职责**：
- 创建充值单
- 处理支付回调
- 写入 Outbox 事件

**关键方法**：

```java
// 创建充值单
RechargeCreateResult createRechargeOrder(RechargeCreateCommand command);

// 充值支付成功回调（幂等）
void onRechargePaid(Long tenantId, String channelTradeNo, LocalDateTime paidAt);
```

**回调处理流程**：
```java
@Transactional
public void onRechargePaid(Long tenantId, String channelTradeNo, LocalDateTime paidAt) {
    // 1. 根据渠道交易号查询并标记充值单为已支付（幂等）
    RechargeOrder rechargeOrder = rechargeOrderDomainService
        .markAsPaidByChannelTradeNo(tenantId, channelTradeNo, paidAt);
    
    if (rechargeOrder == null) {
        log.warn("未找到对应的充值单，跳过处理：channelTradeNo={}", channelTradeNo);
        return;
    }
    
    // 2. 写入 Outbox 事件：RECHARGE_PAID（同事务）
    OutboxEvent event = OutboxEvent.builder()
        .tenantId(tenantId)
        .aggregateType(AggregateType.WALLET)
        .aggregateId(rechargeOrder.getRechargeNo())
        .eventType(EventType.RECHARGE_PAID)
        .payload(buildPayload(rechargeOrder))
        .build();
    
    outboxEventService.save(event);
}
```

### 3. WechatRechargeCallbackController（微信回调控制器）

**职责**：
- 接收微信支付回调
- 解析回调数据
- 调用充值门面处理

**路径**：`POST /open-api/wechat/recharge/notify`

**关键逻辑**：
```java
@PostMapping("/notify")
public ResponseEntity<Map<String, String>> rechargeNotify(@RequestBody String body) {
    // 1. 解析微信回调
    RechargeCallbackData callbackData = parseCallback(body);
    
    // 2. 检查交易状态
    if (!"SUCCESS".equals(callbackData.getTradeState())) {
        return buildSuccessResponse(); // 非成功状态也返回 SUCCESS，避免重复回调
    }
    
    // 3. 调用充值门面处理回调（幂等）
    walletRechargeFacade.onRechargePaid(
        callbackData.getTenantId(),
        callbackData.getTransactionId(), // 微信交易号
        callbackData.getSuccessTime()
    );
    
    return buildSuccessResponse();
}
```

### 4. RechargeEventConsumer（充值事件消费者）

**职责**：
- 监听 `recharge.paid` 事件
- 写入钱包账本流水
- 更新钱包账户余额

**关键逻辑**：
```java
@EventListener
@Transactional
public void onRechargePaid(DispatchedEvent event) {
    if (!"recharge.paid".equals(event.getEventType())) {
        return;
    }
    
    // 1. 幂等性检查（消费日志）
    if (consumeLogService.isConsumed(CONSUMER_NAME, event.getEventId())) {
        return;
    }
    
    // 2. 解析事件载荷
    String rechargeNo = getStringField(event.getPayload().get("rechargeNo"));
    Long totalAmountInCents = getLongField(event.getPayload().get("totalAmount"));
    
    // 3. 构建幂等键
    String idempotencyKey = buildIdempotencyKey(tenantId, userId, rechargeNo);
    
    // 4. 检查账本流水是否已存在（幂等）
    WalletLedger existingLedger = walletLedgerRepository.findByIdemKey(tenantId, idempotencyKey)
        .orElse(null);
    if (existingLedger != null) {
        consumeLogService.recordSuccess(...);
        return;
    }
    
    // 5. 查询钱包账户
    WalletAccount account = walletAccountRepository.findById(tenantId, accountId);
    
    // 6. 转换金额（分 → 元）
    BigDecimal totalAmount = BigDecimal.valueOf(totalAmountInCents)
        .divide(BigDecimal.valueOf(100));
    
    // 7. 生成账本流水
    WalletLedger ledger = WalletLedger.builder()
        .bizType(BizType.RECHARGE.getCode())
        .bizOrderNo(rechargeNo)
        .amount(totalAmount) // 正数=入账
        .balanceBefore(account.getAvailableBalance())
        .balanceAfter(account.getAvailableBalance().add(totalAmount))
        .idemKey(idempotencyKey) // 幂等键
        .build();
    
    // 8. 写入账本流水（唯一约束兜底）
    try {
        walletLedgerRepository.save(ledger);
    } catch (DuplicateKeyException e) {
        // 并发冲突，说明已经入账
        consumeLogService.recordSuccess(...);
        return;
    }
    
    // 9. 更新账户余额（乐观锁）
    account.credit(totalAmount);
    account.addTotalRecharged(totalAmount);
    int updated = walletAccountRepository.updateWithVersion(account);
    if (updated == 0) {
        throw new BizException("账户余额更新失败（版本冲突），请重试");
    }
    
    // 10. 记录消费成功
    consumeLogService.recordSuccess(CONSUMER_NAME, event.getEventId(), ...);
}
```

## 幂等性保障机制

### 三层幂等防护

```
第一层：充值单创建幂等
  ↓ uk_tenant_idem_key
防止客户端重复提交

第二层：支付回调幂等
  ↓ uk_tenant_pay_no
防止微信重复回调

第三层：入账幂等
  ↓ uk_tenant_idem_key (wallet_ledger)
防止 Outbox 重试导致重复入账
```

### 并发场景处理

#### 场景1：微信回调并发

**问题**：微信可能在短时间内发送多次回调

**解决方案**：
1. 数据库唯一约束：`uk_tenant_pay_no` 保证同一 `transaction_id` 只能关联一个充值单
2. 状态检查：`markAsPaidByChannelTradeNo` 方法检查充值单状态，如果已经是 `PAID`，直接返回（幂等）

#### 场景2：Outbox 重试并发

**问题**：Outbox 投递失败后会重试，可能导致重复消费

**解决方案**：
1. 消费日志幂等：`bc_event_consume_log` 表的 `uk_consumer_event` 保证同一事件只消费一次
2. 账本流水幂等：`bc_wallet_ledger` 表的 `uk_tenant_idem_key` 保证同一幂等键只写入一次
3. 乐观锁：账户余额更新使用乐观锁，版本冲突时抛异常由 Outbox 重试

#### 场景3：服务宕机恢复

**问题**：支付回调写入 Outbox 后，服务宕机，事件未投递

**解决方案**：
1. Outbox 持久化：事件已写入数据库，服务重启后不会丢失
2. 定时扫描：`OutboxPublisherJob` 每 10 秒扫描待投递事件（`status = NEW/FAILED`）
3. 重试机制：失败事件按指数退避重试，最多重试 10 次
4. 死信处理：超过最大重试次数的事件标记为 `DEAD`，需人工介入

## 常见失败场景处理

### 1. 微信回调找不到充值单

**现象**：
```
log.warn("未找到对应的充值单，跳过处理：channelTradeNo={}", channelTradeNo);
```

**原因**：
- 充值单未创建成功
- 充值单未关联 `pay_no`（微信交易号）
- `transaction_id` 解析错误

**处理方式**：
1. 检查充值单是否存在：`SELECT * FROM bc_wallet_recharge_order WHERE tenant_id = ? AND recharge_id = ?`
2. 检查 `pay_no` 字段是否已填充：可能在 `markAsPaying` 时未正确设置
3. 手动补发 Outbox 事件（如果充值单状态已是 `PAID`）

### 2. 账本流水写入失败（唯一约束冲突）

**现象**：
```
DuplicateKeyException: Duplicate entry '1-1:123:recharge:wrc_xxx:credit' for key 'uk_tenant_idem_key'
```

**原因**：
- 幂等键重复，说明已经入账过

**处理方式**：
- 正常情况，代码已捕获并记录消费成功
- 检查账户余额是否已增加：`SELECT * FROM bc_wallet_account WHERE id = ?`

### 3. 账户余额更新失败（乐观锁冲突）

**现象**：
```
BizException: 账户余额更新失败（版本冲突），请重试
```

**原因**：
- 并发更新账户余额，版本号冲突

**处理方式**：
- Outbox 会自动重试
- 检查重试次数：`SELECT * FROM bc_outbox_event WHERE event_id = ?`
- 如果超过最大重试次数（10次），需人工介入

### 4. Outbox 事件投递失败

**现象**：
```
SELECT * FROM bc_outbox_event WHERE status = 'FAILED' AND event_type = 'recharge.paid';
```

**原因**：
- 消费者抛异常
- 数据库连接超时
- 业务逻辑错误

**处理方式**：
1. 查看 `last_error` 字段，定位错误原因
2. 修复问题后，手动重置状态：
   ```sql
   UPDATE bc_outbox_event 
   SET status = 'NEW', retry_count = 0, next_retry_at = NOW() 
   WHERE event_id = ?;
   ```
3. 等待 `OutboxPublisherJob` 自动重试

### 5. Outbox 事件死信

**现象**：
```
SELECT * FROM bc_outbox_event WHERE status = 'DEAD' AND event_type = 'recharge.paid';
```

**原因**：
- 超过最大重试次数（10次）
- 业务逻辑持续失败

**处理方式**：
1. 查看 `last_error` 字段，定位根因
2. 修复问题后，手动补发事件：
   ```sql
   -- 方式1：重置状态
   UPDATE bc_outbox_event 
   SET status = 'NEW', retry_count = 0, next_retry_at = NOW() 
   WHERE event_id = ?;
   
   -- 方式2：手动调用消费者
   -- 从 event_payload 中提取数据，调用 RechargeEventConsumer
   ```

## 监控与告警

### 关键指标

| 指标 | SQL | 告警阈值 |
|-----|-----|---------|
| 待投递充值事件数量 | `SELECT COUNT(*) FROM bc_outbox_event WHERE status IN ('NEW', 'FAILED') AND event_type = 'recharge.paid'` | > 100 |
| 充值死信事件数量 | `SELECT COUNT(*) FROM bc_outbox_event WHERE status = 'DEAD' AND event_type = 'recharge.paid'` | > 0 |
| 充值消费失败数量 | `SELECT COUNT(*) FROM bc_event_consume_log WHERE status = 'FAILED' AND consumer_name = 'RechargeConsumer'` | > 10 |
| 支付中超时充值单 | `SELECT COUNT(*) FROM bc_wallet_recharge_order WHERE status = 'PAYING' AND recharge_requested_at < NOW() - INTERVAL 30 MINUTE` | > 50 |

### 查询语句

#### 查询最近充值记录
```sql
SELECT 
    r.recharge_id,
    r.user_id,
    r.total_amount,
    r.status,
    r.pay_no,
    r.recharge_requested_at,
    r.recharge_completed_at,
    l.ledger_no,
    l.amount AS ledger_amount,
    l.balance_after
FROM bc_wallet_recharge_order r
LEFT JOIN bc_wallet_ledger l ON l.tenant_id = r.tenant_id AND l.biz_order_no = r.recharge_id
WHERE r.tenant_id = 1
ORDER BY r.recharge_requested_at DESC
LIMIT 20;
```

#### 查询充值单对应的 Outbox 事件
```sql
SELECT 
    e.event_id,
    e.event_type,
    e.status,
    e.retry_count,
    e.last_error,
    e.created_at,
    e.sent_at
FROM bc_outbox_event e
WHERE e.aggregate_type = 'WALLET'
  AND e.aggregate_id = 'wrc_01HQZX1234567890'
ORDER BY e.created_at DESC;
```

#### 查询充值事件消费日志
```sql
SELECT 
    c.consumer_name,
    c.event_id,
    c.status,
    c.idempotency_key,
    c.error_message,
    c.consumed_at
FROM bc_event_consume_log c
WHERE c.consumer_name = 'RechargeConsumer'
  AND c.event_type = 'recharge.paid'
ORDER BY c.consumed_at DESC
LIMIT 20;
```

## 测试场景

### 1. 正常充值流程

**步骤**：
1. 调用 `POST /api/wallet/recharge/create` 创建充值单
2. 模拟微信回调 `POST /open-api/wechat/recharge/notify`
3. 验证充值单状态变为 `PAID`
4. 验证 Outbox 事件已写入
5. 验证账本流水已写入
6. 验证账户余额已增加

### 2. 回调重放（幂等性）

**步骤**：
1. 完成一次正常充值
2. 重复发送相同的微信回调（相同 `transaction_id`）
3. 验证充值单状态仍为 `PAID`（未重复更新）
4. 验证账本流水只有一条（未重复入账）
5. 验证账户余额未重复增加

### 3. 宕机恢复

**步骤**：
1. 创建充值单并发送微信回调
2. 在 Outbox 事件写入后、消费前，停止服务
3. 重启服务
4. 验证 `OutboxPublisherJob` 自动投递事件
5. 验证账本流水已写入
6. 验证账户余额已增加

### 4. 并发回调

**步骤**：
1. 创建充值单
2. 并发发送 10 次相同的微信回调
3. 验证充值单状态只更新一次
4. 验证 Outbox 事件只写入一次
5. 验证账本流水只有一条
6. 验证账户余额只增加一次

## 最佳实践

### 1. 金额单位统一

- **数据库**：使用 `DECIMAL(18,2)` 存储元
- **领域模型**：使用 `Long` 存储分（避免浮点数精度问题）
- **API 接口**：使用 `BigDecimal` 传输元

### 2. 幂等键设计

- **客户端生成**：使用 UUID 或业务唯一标识
- **服务端生成**：使用 `{tenantId}:{userId}:{bizType}:{bizId}:{action}` 格式
- **唯一约束**：数据库必须有唯一索引兜底

### 3. 状态机严格

- 定义清晰的状态流转规则
- 终态不可变更
- 幂等：相同状态可重复设置

### 4. 乐观锁使用

- 账户余额更新必须使用乐观锁
- 版本冲突时抛异常，由 Outbox 重试

### 5. 日志追踪

- 记录关键字段：`rechargeNo`、`channelTradeNo`、`eventId`、`userId`
- 使用结构化日志：便于检索和分析
- 记录每个步骤的执行结果

### 6. 监控告警

- 定期检查 Outbox 待投递事件数量
- 定期检查死信事件数量
- 定期检查消费失败数量
- 定期检查超时充值单数量

## 相关文档

- [OUTBOX-QUICK-REFERENCE.md](./OUTBOX-QUICK-REFERENCE.md)：Outbox 快速参考
- [OUTBOX-INTEGRATION-EXAMPLE.md](./OUTBOX-INTEGRATION-EXAMPLE.md)：Outbox 集成示例
- [OUTBOX-IMPLEMENTATION-SUMMARY.md](./OUTBOX-IMPLEMENTATION-SUMMARY.md)：Outbox 实现总结

## 附录

### 幂等键格式汇总

| 操作 | 幂等键格式 | 示例 |
|-----|-----------|------|
| 充值单创建 | 客户端生成 UUID | `uuid-123e4567-e89b-12d3-a456-426614174000` |
| 支付回调 | 渠道交易号 | `4200001234567890123456789012345678` |
| 账本流水 | `{tenantId}:{userId}:recharge:{rechargeNo}:credit` | `1:123:recharge:wrc_01HQZX:credit` |

### 状态码说明

| 状态 | 说明 | 是否终态 |
|-----|------|---------|
| INIT | 初始化（充值单已创建，等待拉起支付） | 否 |
| PAYING | 支付中（已拉起支付，等待支付回调） | 否 |
| PAID | 已支付（支付成功，已入账） | 是 |
| CLOSED | 已关闭（超时关闭或用户取消） | 是 |

### 事件类型说明

| 事件类型 | 触发时机 | 消费者 | 操作 |
|---------|---------|--------|------|
| `recharge.paid` | 充值单支付回调成功，状态流转到 PAID | `RechargeConsumer` | 写入账本流水（CREDIT），增加可用余额 |
