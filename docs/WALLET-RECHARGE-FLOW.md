# 钱包充值闭环实现文档

## 目标

实现完整的钱包充值闭环：创建充值单 → 拉起微信支付 → 小程序支付回调 → 回调幂等入账（wallet_ledger CREDIT）→ Outbox 保障可恢复。

---

## 核心约束

### 1. 回调幂等性

- **幂等键规则**：以 `(tenantId + channelTradeNo)` 作为幂等键
- **数据库兜底**：`bc_wallet_recharge_order.uk_tenant_pay_no` 唯一约束
- **消费日志**：`bc_event_consume_log` 记录消费状态（consumer_name + event_id 唯一）
- **账本幂等**：`bc_wallet_ledger.uk_tenant_idem_key` 唯一约束

### 2. 充值单状态机

```
INIT (初始化)
  ↓
PAYING (支付中)
  ↓
PAID (已支付) ← 终态，不可回退
  ↓
CLOSED (已关闭) ← 终态
```

**状态流转规则**：
- `INIT` → `PAYING` / `CLOSED`
- `PAYING` → `PAID` / `CLOSED`
- `PAID` 和 `CLOSED` 为终态，不可再变更
- 相同状态可重复设置（幂等）

### 3. 充值回调处理

**原则**：先落库更新状态 + 写 outbox，再由消费者入账（不要在回调事务里直接改余额）

**流程**：
1. 回调接收：更新充值单状态为 `PAID`
2. 写入 Outbox：事件类型 `RECHARGE_PAID`
3. 消费者入账：写入 `wallet_ledger`（CREDIT）+ 更新 `wallet_account` 余额

### 4. 金额单位

- **数据库存储**：元（DECIMAL(18,2)）
- **领域模型**：分（Long）
- **API 接口**：元（BigDecimal）

---

## 架构设计

### 模块依赖

```
app-application (Controller)
    ↓
app-wallet (Facade + Consumer)
    ↓
app-wallet-api (API 接口)
    ↓
app-wallet (Domain Service + Repository)
```

### 核心组件

| 组件 | 职责 |
|------|------|
| `RechargeController` | 接收充值请求，创建充值单 |
| `WechatRechargeCallbackController` | 接收微信支付回调 |
| `WalletRechargeFacade` | 充值门面，处理充值业务逻辑 |
| `RechargeOrderDomainService` | 充值单领域服务，管理状态机 |
| `RechargeEventConsumer` | 消费 `RECHARGE_PAID` 事件，执行入账 |
| `OutboxEventService` | Outbox 事件服务，保证事件可靠投递 |

---

## 数据库设计

### 1. 充值单表 (bc_wallet_recharge_order)

```sql
CREATE TABLE IF NOT EXISTS bc_wallet_recharge_order (
    id                  BIGINT          NOT NULL COMMENT '充值单ID（内部主键，ULID）',
    tenant_id           BIGINT          NOT NULL COMMENT '租户ID',
    user_id             BIGINT          NOT NULL COMMENT '用户ID',
    account_id          BIGINT          NOT NULL COMMENT '账户ID',
    
    recharge_id         VARCHAR(64)     NOT NULL COMMENT '充值单号（PublicId格式：wrc_xxx）',
    
    recharge_amount     DECIMAL(18,2)   NOT NULL COMMENT '充值金额（单位：元）',
    bonus_amount        DECIMAL(18,2)   NOT NULL DEFAULT 0.00 COMMENT '赠送金额',
    total_amount        DECIMAL(18,2)   NOT NULL COMMENT '总到账金额',
    currency            VARCHAR(8)      NOT NULL DEFAULT 'CNY' COMMENT '币种',
    
    status              VARCHAR(32)     NOT NULL DEFAULT 'INIT' COMMENT '充值状态：INIT、PAYING、PAID、CLOSED',
    
    pay_order_id        BIGINT          DEFAULT NULL COMMENT '支付单ID',
    pay_channel         VARCHAR(32)     DEFAULT NULL COMMENT '支付渠道：WECHAT、ALIPAY',
    pay_no              VARCHAR(128)    DEFAULT NULL COMMENT '第三方支付单号（渠道交易号）',
    
    recharge_requested_at DATETIME      NOT NULL COMMENT '充值发起时间',
    recharge_completed_at DATETIME      DEFAULT NULL COMMENT '充值完成时间',
    
    idem_key            VARCHAR(128)    NOT NULL COMMENT '幂等键',
    ext_json            TEXT            DEFAULT NULL COMMENT '扩展信息JSON',
    
    version             INT             NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
    created_at          DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by          BIGINT          DEFAULT NULL,
    updated_at          DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    updated_by          BIGINT          DEFAULT NULL,
    
    PRIMARY KEY (id),
    UNIQUE KEY uk_tenant_idem_key (tenant_id, idem_key),
    UNIQUE KEY uk_tenant_recharge_id (tenant_id, recharge_id),
    UNIQUE KEY uk_tenant_pay_no (tenant_id, pay_no), -- 支付回调幂等
    KEY idx_tenant_user_status (tenant_id, user_id, status, recharge_requested_at DESC),
    KEY idx_tenant_account (tenant_id, account_id),
    KEY idx_status_requested (status, recharge_requested_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='充值单表';
```

### 2. 索引说明

| 索引 | 用途 |
|------|------|
| `uk_tenant_idem_key` | 创建充值单幂等（基于客户端幂等键） |
| `uk_tenant_recharge_id` | 充值单号唯一约束 |
| `uk_tenant_pay_no` | **支付回调幂等**（基于渠道交易号） |
| `idx_tenant_user_status` | 用户充值记录查询 |
| `idx_tenant_account` | 账户充值查询 |
| `idx_status_requested` | 充值单状态查询 |

---

## 业务流程

### 完整充值流程

```
1. 用户发起充值
   ↓
2. POST /api/wallet/recharge/create
   - 创建充值单（状态：INIT）
   - 返回充值单号 + 支付参数
   ↓
3. 小程序拉起微信支付
   ↓
4. 用户完成支付
   ↓
5. 微信回调 POST /open-api/wechat/recharge/notify
   - 更新充值单状态：INIT/PAYING → PAID
   - 写入 Outbox 事件：RECHARGE_PAID
   ↓
6. OutboxPublisherJob 投递事件
   ↓
7. RechargeEventConsumer 消费事件
   - 写入 wallet_ledger（CREDIT）
   - 更新 wallet_account 余额（乐观锁）
   ↓
8. 充值完成
```

### 幂等性保证

#### 场景 1：创建充值单重复调用

```
请求 1：idempotencyKey = "uuid-123"
  → 创建充值单：rechargeNo = "wrc_xxx"

请求 2：idempotencyKey = "uuid-123"（相同）
  → 查询到已存在充值单，直接返回（幂等）
```

#### 场景 2：支付回调重复推送

```
回调 1：channelTradeNo = "wx_transaction_123"
  → 更新充值单状态为 PAID
  → 写入 Outbox 事件

回调 2：channelTradeNo = "wx_transaction_123"（相同）
  → 查询到充值单已 PAID，直接返回（幂等）
  → 不再写入 Outbox 事件
```

#### 场景 3：消费者重复消费

```
消费 1：eventId = "event-123"
  → 写入 wallet_ledger（idemKey = "1:100:recharge:wrc_xxx:credit"）
  → 更新 wallet_account 余额
  → 记录消费日志

消费 2：eventId = "event-123"（相同）
  → 查询消费日志，已消费，直接返回（幂等）
```

#### 场景 4：消费者并发消费

```
线程 1：写入 wallet_ledger（idemKey = "1:100:recharge:wrc_xxx:credit"）
线程 2：写入 wallet_ledger（idemKey = "1:100:recharge:wrc_xxx:credit"）
  → 唯一约束冲突（DuplicateKeyException）
  → 线程 2 捕获异常，记录消费成功，返回
```

---

## 幂等键规则

| 操作 | 幂等键格式 | 示例 | 唯一约束表 |
|------|-----------|------|-----------|
| 创建充值单 | `{tenantId}:{userId}:recharge:{requestId}` | `1:100:recharge:uuid-123` | `bc_wallet_recharge_order.uk_tenant_idem_key` |
| 支付回调 | `{tenantId}:{channelTradeNo}` | `1:wx_transaction_123` | `bc_wallet_recharge_order.uk_tenant_pay_no` |
| 入账流水 | `{tenantId}:{userId}:recharge:{rechargeNo}:credit` | `1:100:recharge:wrc_xxx:credit` | `bc_wallet_ledger.uk_tenant_idem_key` |

---

## 接口定义

### 1. 创建充值单

**接口**：`POST /api/wallet/recharge/create`

**请求**：
```json
{
  "userId": 123,
  "rechargeAmount": 100.00,
  "payChannel": "WECHAT",
  "idempotencyKey": "uuid-123456"
}
```

**响应**：
```json
{
  "code": 0,
  "message": "success",
  "data": {
    "rechargeNo": "wrc_01HN8X5K9G3QRST2VW4XYZ",
    "rechargeAmount": 100.00,
    "bonusAmount": 0.00,
    "totalAmount": 100.00,
    "payChannel": "WECHAT",
    "status": "INIT",
    "payParams": {
      "timeStamp": "1234567890",
      "nonceStr": "abc123",
      "package": "prepay_id=wx123",
      "signType": "RSA",
      "paySign": "signature"
    }
  }
}
```

### 2. 微信充值回调

**接口**：`POST /open-api/wechat/recharge/notify`

**请求**（微信回调格式）：
```json
{
  "appid": "wx1234567890",
  "mchid": "1234567890",
  "out_trade_no": "wrc_01HN8X5K9G3QRST2VW4XYZ",
  "transaction_id": "wx_transaction_123456",
  "trade_state": "SUCCESS",
  "bank_type": "CMB_DEBIT",
  "success_time": "2025-12-19T10:30:00+08:00",
  "amount": {
    "total": 10000,
    "currency": "CNY"
  },
  "attach": "{\"tenantId\": 1}"
}
```

**响应**：
```json
{
  "code": "SUCCESS",
  "message": "成功"
}
```

---

## 事件定义

### RECHARGE_PAID 事件

**事件类型**：`recharge.paid`

**聚合根类型**：`WALLET`

**聚合根ID**：充值单号（rechargeNo）

**载荷 (Payload)**：
```json
{
  "rechargeNo": "wrc_01HN8X5K9G3QRST2VW4XYZ",
  "rechargeId": 123456789,
  "userId": 100,
  "accountId": 200,
  "rechargeAmount": 10000,
  "bonusAmount": 0,
  "totalAmount": 10000,
  "channelTradeNo": "wx_transaction_123456",
  "paidAt": "2025-12-19T10:30:00"
}
```

**元数据 (Metadata)**：
```json
{
  "userId": 100,
  "source": "recharge_callback"
}
```

---

## 故障恢复场景

### 场景 1：回调写 Outbox 后服务宕机

**问题**：回调已更新充值单状态为 PAID，并写入 Outbox，但服务宕机，消费者未消费

**恢复**：
1. 服务重启后，`OutboxPublisherJob` 扫描待投递事件
2. 投递 `RECHARGE_PAID` 事件
3. `RechargeEventConsumer` 消费事件，完成入账

### 场景 2：消费者入账失败

**问题**：消费者写入账本流水时数据库异常

**恢复**：
1. 消费者抛出异常，Outbox 记录失败
2. `OutboxPublisherJob` 重试投递（指数退避）
3. 消费者重新消费，成功入账

### 场景 3：回调重放

**问题**：微信多次推送同一笔支付回调

**恢复**：
1. 第一次回调：更新充值单状态为 PAID，写入 Outbox
2. 第二次回调：查询到充值单已 PAID，直接返回（幂等）
3. 不会重复写入 Outbox，不会重复入账

### 场景 4：消费者并发消费

**问题**：Outbox 重试导致同一事件被多个消费者并发消费

**恢复**：
1. 消费者 A 和 B 同时消费同一事件
2. 消费者 A 写入账本流水成功
3. 消费者 B 写入账本流水时唯一约束冲突（DuplicateKeyException）
4. 消费者 B 捕获异常，记录消费成功，返回

---

## 测试场景

### 1. 正常充值流程

```bash
# 1. 创建充值单
curl -X POST http://localhost:8080/api/wallet/recharge/create \
  -H "Content-Type: application/json" \
  -d '{
    "userId": 123,
    "rechargeAmount": 100.00,
    "payChannel": "WECHAT",
    "idempotencyKey": "test-uuid-001"
  }'

# 2. 模拟微信回调
curl -X POST http://localhost:8080/open-api/wechat/recharge/notify \
  -H "Content-Type: application/json" \
  -d '{
    "appid": "wx1234567890",
    "mchid": "1234567890",
    "out_trade_no": "wrc_01HN8X5K9G3QRST2VW4XYZ",
    "transaction_id": "wx_test_001",
    "trade_state": "SUCCESS",
    "success_time": "2025-12-19T10:30:00+08:00",
    "amount": {"total": 10000},
    "attach": "{\"tenantId\": 1}"
  }'

# 3. 查询钱包余额
curl -X GET "http://localhost:8080/api/wallet/balance?tenantId=1&userId=123"

# 4. 查询账本流水
curl -X GET "http://localhost:8080/api/wallet/ledger?tenantId=1&userId=123"
```

### 2. 幂等性测试

#### 测试 1：重复创建充值单

```bash
# 第一次创建
curl -X POST http://localhost:8080/api/wallet/recharge/create \
  -d '{"userId": 123, "rechargeAmount": 100.00, "payChannel": "WECHAT", "idempotencyKey": "test-uuid-002"}'

# 第二次创建（相同 idempotencyKey）
curl -X POST http://localhost:8080/api/wallet/recharge/create \
  -d '{"userId": 123, "rechargeAmount": 100.00, "payChannel": "WECHAT", "idempotencyKey": "test-uuid-002"}'

# 预期：返回相同的充值单号
```

#### 测试 2：重复支付回调

```bash
# 第一次回调
curl -X POST http://localhost:8080/open-api/wechat/recharge/notify \
  -d '{"transaction_id": "wx_test_002", "trade_state": "SUCCESS", ...}'

# 第二次回调（相同 transaction_id）
curl -X POST http://localhost:8080/open-api/wechat/recharge/notify \
  -d '{"transaction_id": "wx_test_002", "trade_state": "SUCCESS", ...}'

# 预期：第二次回调直接返回成功，不重复入账
```

### 3. 宕机恢复测试

```bash
# 1. 创建充值单
curl -X POST http://localhost:8080/api/wallet/recharge/create \
  -d '{"userId": 123, "rechargeAmount": 100.00, "payChannel": "WECHAT", "idempotencyKey": "test-uuid-003"}'

# 2. 模拟回调（写入 Outbox）
curl -X POST http://localhost:8080/open-api/wechat/recharge/notify \
  -d '{"transaction_id": "wx_test_003", "trade_state": "SUCCESS", ...}'

# 3. 立即停止服务（模拟宕机）
kill -9 <pid>

# 4. 重启服务
java -jar app.jar

# 5. 等待 OutboxPublisherJob 执行（10秒）

# 6. 查询钱包余额（预期：已入账）
curl -X GET "http://localhost:8080/api/wallet/balance?tenantId=1&userId=123"
```

---

## 监控与告警

### 关键指标

| 指标 | SQL | 告警阈值 |
|------|-----|---------|
| 待投递 Outbox 事件数 | `SELECT COUNT(*) FROM bc_outbox_event WHERE status IN ('NEW', 'FAILED')` | > 100 |
| 死信 Outbox 事件数 | `SELECT COUNT(*) FROM bc_outbox_event WHERE status = 'DEAD'` | > 0 |
| 消费失败数 | `SELECT COUNT(*) FROM bc_event_consume_log WHERE status = 'FAILED' AND consumer_name = 'RechargeConsumer'` | > 10 |
| 充值单 PAYING 超时数 | `SELECT COUNT(*) FROM bc_wallet_recharge_order WHERE status = 'PAYING' AND recharge_requested_at < NOW() - INTERVAL 30 MINUTE` | > 10 |

### 日志追踪

**关键日志**：
- `[RechargeConsumer] Received recharge.paid event: eventId={eventId}`
- `[RechargeConsumer] Processing recharge: rechargeNo={rechargeNo}, userId={userId}, amount={amount}`
- `[RechargeConsumer] Recharge credited successfully: rechargeNo={rechargeNo}, ledgerNo={ledgerNo}`

**故障排查**：
```sql
-- 1. 查询充值单状态
SELECT * FROM bc_wallet_recharge_order WHERE recharge_id = 'wrc_xxx';

-- 2. 查询 Outbox 事件
SELECT * FROM bc_outbox_event 
WHERE aggregate_type = 'WALLET' 
AND aggregate_id = 'wrc_xxx' 
ORDER BY created_at DESC;

-- 3. 查询消费日志
SELECT * FROM bc_event_consume_log 
WHERE consumer_name = 'RechargeConsumer' 
AND event_id = 'xxx';

-- 4. 查询账本流水
SELECT * FROM bc_wallet_ledger 
WHERE biz_type = 'RECHARGE' 
AND biz_order_no = 'wrc_xxx';
```

---

## 常见问题

### Q1: 回调多次推送，会重复入账吗？

**A**: 不会。通过以下三层幂等保证：
1. 充值单表 `uk_tenant_pay_no` 唯一约束（基于 channelTradeNo）
2. 消费日志表 `bc_event_consume_log` 唯一约束（consumer_name + event_id）
3. 账本流水表 `uk_tenant_idem_key` 唯一约束（基于充值单号）

### Q2: 服务宕机后，充值会丢失吗？

**A**: 不会。回调已写入 Outbox 事件，服务重启后 `OutboxPublisherJob` 会自动重试投递，最终完成入账。

### Q3: 消费者入账失败怎么办？

**A**: Outbox 会自动重试（指数退避），最多重试 10 次。如果仍然失败，事件状态变为 `DEAD`，需要人工介入。

### Q4: 如何查询充值记录？

**A**: 
```sql
-- 查询用户充值记录
SELECT * FROM bc_wallet_recharge_order 
WHERE tenant_id = 1 AND user_id = 123 
ORDER BY recharge_requested_at DESC;

-- 查询充值账本流水
SELECT * FROM bc_wallet_ledger 
WHERE tenant_id = 1 AND user_id = 123 AND biz_type = 'RECHARGE' 
ORDER BY created_at DESC;
```

### Q5: 充值金额单位是什么？

**A**: 
- 数据库存储：元（DECIMAL(18,2)）
- 领域模型：分（Long）
- API 接口：元（BigDecimal）
- 微信回调：分（Long）

---

## 相关文档

- [WALLET-IMPLEMENTATION-SUMMARY.md](./WALLET-IMPLEMENTATION-SUMMARY.md)：钱包实现总结
- [WALLET-ORDER-INTEGRATION.md](./WALLET-ORDER-INTEGRATION.md)：钱包-订单集成
- [OUTBOX-QUICK-REFERENCE.md](./OUTBOX-QUICK-REFERENCE.md)：Outbox 快速参考
- [OUTBOX-CONSISTENCY-GUIDE.md](./OUTBOX-CONSISTENCY-GUIDE.md)：Outbox 一致性指南

---

## 总结

本次实现完成了钱包充值闭环，核心亮点：

1. **三层幂等保证**：充值单 + 消费日志 + 账本流水
2. **状态机设计**：严格的状态流转规则，PAID 后不可回退
3. **Outbox 解耦**：回调只写 Outbox，消费者异步入账
4. **故障可恢复**：服务宕机、消费失败均可自动恢复
5. **金额单位统一**：分（领域模型）、元（API/数据库）

**下一步优化**：
- 集成真实支付网关（微信支付 SDK）
- 支持充值赠送活动
- 充值单超时自动关闭
- 充值对账与补偿
