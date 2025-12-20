# 钱包充值快速参考卡

## API 接口

### 创建充值单

**接口**：`POST /api/wallet/recharge/create`

**请求示例**：
```json
{
  "tenantId": 1,
  "userId": 123,
  "rechargeAmount": 100.00,
  "payChannel": "WECHAT",
  "idempotencyKey": "uuid-123e4567-e89b-12d3-a456-426614174000"
}
```

**响应示例**：
```json
{
  "code": 0,
  "message": "success",
  "data": {
    "rechargeNo": "wrc_01HQZX1234567890",
    "rechargeAmount": 100.00,
    "bonusAmount": 0.00,
    "totalAmount": 100.00,
    "payChannel": "WECHAT",
    "status": "INIT"
  }
}
```

### 微信支付回调

**接口**：`POST /open-api/wechat/recharge/notify`

**请求示例**（微信回调格式）：
```json
{
  "appid": "wx1234567890",
  "mchid": "1234567890",
  "out_trade_no": "wrc_01HQZX1234567890",
  "transaction_id": "4200001234567890123456789012345678",
  "trade_state": "SUCCESS",
  "bank_type": "CMB_DEBIT",
  "success_time": "2025-12-19T12:00:00+08:00",
  "amount": {
    "total": 10000,
    "currency": "CNY"
  },
  "attach": "{\"tenantId\":1}"
}
```

**响应示例**：
```json
{
  "code": "SUCCESS",
  "message": "成功"
}
```

## 幂等键速查

| 操作 | 幂等键格式 | 示例 | 用途 |
|-----|-----------|------|------|
| 充值单创建 | 客户端生成 UUID | `uuid-123e4567-e89b-12d3-a456-426614174000` | 防止重复提交 |
| 支付回调 | 渠道交易号 | `4200001234567890123456789012345678` | 防止重复回调 |
| 账本流水 | `{tenantId}:{userId}:recharge:{rechargeNo}:credit` | `1:123:recharge:wrc_01HQZX:credit` | 防止重复入账 |

## 状态流转

```
INIT（初始化）
  ↓ markAsPaying()
PAYING（支付中）
  ↓ markAsPaid()
PAID（已支付）✓

INIT/PAYING
  ↓ markAsClosed()
CLOSED（已关闭）✓
```

## 核心表结构

### bc_wallet_recharge_order

| 字段 | 类型 | 说明 |
|-----|------|------|
| id | BIGINT | 充值单ID |
| tenant_id | BIGINT | 租户ID |
| user_id | BIGINT | 用户ID |
| account_id | BIGINT | 账户ID |
| recharge_id | VARCHAR(64) | 充值单号（wrc_xxx） |
| recharge_amount | DECIMAL(18,2) | 充值金额（元） |
| total_amount | DECIMAL(18,2) | 总到账金额（元） |
| status | VARCHAR(32) | 状态：INIT/PAYING/PAID/CLOSED |
| pay_no | VARCHAR(128) | 渠道交易号（微信 transaction_id） |
| idem_key | VARCHAR(128) | 幂等键 |
| version | INT | 乐观锁版本号 |

**关键索引**：
- `uk_tenant_idem_key`：创建幂等
- `uk_tenant_pay_no`：回调幂等
- `uk_tenant_recharge_id`：充值单号唯一

### bc_wallet_ledger

| 字段 | 类型 | 说明 |
|-----|------|------|
| id | BIGINT | 流水ID |
| tenant_id | BIGINT | 租户ID |
| user_id | BIGINT | 用户ID |
| account_id | BIGINT | 账户ID |
| ledger_no | VARCHAR(64) | 流水号（wl_xxx） |
| biz_type | VARCHAR(32) | 业务类型：RECHARGE |
| biz_order_no | VARCHAR(64) | 充值单号 |
| amount | DECIMAL(18,2) | 变更金额（正数=入账） |
| balance_before | DECIMAL(18,2) | 变更前余额 |
| balance_after | DECIMAL(18,2) | 变更后余额 |
| idem_key | VARCHAR(128) | 幂等键 |

**关键索引**：
- `uk_tenant_idem_key`：入账幂等

## 常用 SQL

### 查询充值记录
```sql
SELECT 
    recharge_id,
    user_id,
    total_amount,
    status,
    pay_no,
    recharge_requested_at,
    recharge_completed_at
FROM bc_wallet_recharge_order
WHERE tenant_id = 1 AND user_id = 123
ORDER BY recharge_requested_at DESC
LIMIT 20;
```

### 查询充值账本流水
```sql
SELECT 
    l.ledger_no,
    l.biz_order_no AS recharge_no,
    l.amount,
    l.balance_before,
    l.balance_after,
    l.created_at
FROM bc_wallet_ledger l
WHERE l.tenant_id = 1 
  AND l.user_id = 123 
  AND l.biz_type = 'RECHARGE'
ORDER BY l.created_at DESC
LIMIT 20;
```

### 查询充值 Outbox 事件
```sql
SELECT 
    event_id,
    event_type,
    status,
    retry_count,
    last_error,
    created_at
FROM bc_outbox_event
WHERE aggregate_type = 'WALLET'
  AND event_type = 'recharge.paid'
ORDER BY created_at DESC
LIMIT 20;
```

### 查询充值消费日志
```sql
SELECT 
    consumer_name,
    event_id,
    status,
    idempotency_key,
    error_message,
    consumed_at
FROM bc_event_consume_log
WHERE consumer_name = 'RechargeConsumer'
ORDER BY consumed_at DESC
LIMIT 20;
```

### 查询待投递充值事件
```sql
SELECT 
    event_id,
    aggregate_id AS recharge_no,
    status,
    retry_count,
    next_retry_at,
    last_error
FROM bc_outbox_event
WHERE event_type = 'recharge.paid'
  AND status IN ('NEW', 'FAILED')
ORDER BY created_at ASC;
```

### 查询充值死信事件
```sql
SELECT 
    event_id,
    aggregate_id AS recharge_no,
    retry_count,
    last_error,
    created_at
FROM bc_outbox_event
WHERE event_type = 'recharge.paid'
  AND status = 'DEAD'
ORDER BY created_at DESC;
```

## 故障排查

### 问题1：微信回调找不到充值单

**现象**：日志显示 `未找到对应的充值单，跳过处理`

**排查步骤**：
```sql
-- 1. 检查充值单是否存在
SELECT * FROM bc_wallet_recharge_order 
WHERE tenant_id = 1 AND recharge_id = 'wrc_xxx';

-- 2. 检查 pay_no 是否已填充
SELECT * FROM bc_wallet_recharge_order 
WHERE tenant_id = 1 AND pay_no = '4200001234567890123456789012345678';
```

**解决方案**：
- 如果充值单存在但 `pay_no` 为空，手动更新：
  ```sql
  UPDATE bc_wallet_recharge_order 
  SET pay_no = '4200001234567890123456789012345678',
      status = 'PAID',
      recharge_completed_at = NOW()
  WHERE tenant_id = 1 AND recharge_id = 'wrc_xxx';
  ```
- 然后手动补发 Outbox 事件

### 问题2：Outbox 事件投递失败

**现象**：事件状态为 `FAILED`

**排查步骤**：
```sql
SELECT event_id, last_error, retry_count 
FROM bc_outbox_event 
WHERE event_id = 'xxx';
```

**解决方案**：
```sql
-- 重置状态，等待重试
UPDATE bc_outbox_event 
SET status = 'NEW', retry_count = 0, next_retry_at = NOW() 
WHERE event_id = 'xxx';
```

### 问题3：账户余额未增加

**现象**：充值单状态为 `PAID`，但账户余额未变化

**排查步骤**：
```sql
-- 1. 检查 Outbox 事件是否投递
SELECT * FROM bc_outbox_event 
WHERE aggregate_type = 'WALLET' 
  AND aggregate_id = 'wrc_xxx';

-- 2. 检查消费日志
SELECT * FROM bc_event_consume_log 
WHERE consumer_name = 'RechargeConsumer' 
  AND event_id = 'xxx';

-- 3. 检查账本流水
SELECT * FROM bc_wallet_ledger 
WHERE tenant_id = 1 
  AND biz_type = 'RECHARGE' 
  AND biz_order_no = 'wrc_xxx';
```

**解决方案**：
- 如果 Outbox 事件未投递，检查 `OutboxPublisherJob` 是否运行
- 如果消费失败，查看 `error_message`，修复后重置 Outbox 状态
- 如果账本流水不存在，手动补发事件

## 监控指标

| 指标 | SQL | 告警阈值 |
|-----|-----|---------|
| 待投递充值事件 | `SELECT COUNT(*) FROM bc_outbox_event WHERE status IN ('NEW', 'FAILED') AND event_type = 'recharge.paid'` | > 100 |
| 充值死信事件 | `SELECT COUNT(*) FROM bc_outbox_event WHERE status = 'DEAD' AND event_type = 'recharge.paid'` | > 0 |
| 充值消费失败 | `SELECT COUNT(*) FROM bc_event_consume_log WHERE status = 'FAILED' AND consumer_name = 'RechargeConsumer'` | > 10 |
| 支付中超时充值单 | `SELECT COUNT(*) FROM bc_wallet_recharge_order WHERE status = 'PAYING' AND recharge_requested_at < NOW() - INTERVAL 30 MINUTE` | > 50 |

## 测试场景

### 场景1：正常充值

```bash
# 1. 创建充值单
curl -X POST http://localhost:8080/api/wallet/recharge/create \
  -H "Content-Type: application/json" \
  -d '{
    "tenantId": 1,
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
    "out_trade_no": "wrc_01HQZX1234567890",
    "transaction_id": "4200001234567890123456789012345678",
    "trade_state": "SUCCESS",
    "success_time": "2025-12-19T12:00:00+08:00",
    "amount": {"total": 10000},
    "attach": "{\"tenantId\":1}"
  }'

# 3. 验证充值单状态
SELECT * FROM bc_wallet_recharge_order WHERE recharge_id = 'wrc_01HQZX1234567890';

# 4. 验证账本流水
SELECT * FROM bc_wallet_ledger WHERE biz_order_no = 'wrc_01HQZX1234567890';

# 5. 验证账户余额
SELECT * FROM bc_wallet_account WHERE user_id = 123;
```

### 场景2：回调重放（幂等性）

```bash
# 重复发送相同的微信回调 10 次
for i in {1..10}; do
  curl -X POST http://localhost:8080/open-api/wechat/recharge/notify \
    -H "Content-Type: application/json" \
    -d '{
      "transaction_id": "4200001234567890123456789012345678",
      "trade_state": "SUCCESS",
      ...
    }'
done

# 验证只入账一次
SELECT COUNT(*) FROM bc_wallet_ledger WHERE biz_order_no = 'wrc_01HQZX1234567890';
-- 结果应该为 1
```

## 配置项

```yaml
# application.yml
bluecone:
  wallet:
    recharge:
      # 充值单超时时间（分钟）
      timeout-minutes: 30
      # 最小充值金额（元）
      min-amount: 0.01
      # 最大充值金额（元）
      max-amount: 10000.00
```

## 相关文档

- [WALLET-RECHARGE-CLOSED-LOOP.md](./WALLET-RECHARGE-CLOSED-LOOP.md)：完整实现文档
- [OUTBOX-QUICK-REFERENCE.md](./OUTBOX-QUICK-REFERENCE.md)：Outbox 快速参考
