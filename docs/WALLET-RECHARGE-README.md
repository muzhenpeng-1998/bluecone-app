# 钱包充值闭环文档索引

## 文档概览

本目录包含钱包充值闭环的完整实现文档，涵盖架构设计、实施细节、快速参考和故障排查。

## 文档列表

### 1. [WALLET-RECHARGE-IMPLEMENTATION-SUMMARY.md](./WALLET-RECHARGE-IMPLEMENTATION-SUMMARY.md)
**实施总结文档** - 推荐首先阅读

- 实施概要
- 修复内容详情
- 幂等性保障机制
- 可恢复性保障
- 测试验证
- 文件清单
- 最佳实践总结

**适合人群**：项目经理、技术负责人、新加入团队成员

---

### 2. [WALLET-RECHARGE-CLOSED-LOOP.md](./WALLET-RECHARGE-CLOSED-LOOP.md)
**完整实现文档** - 深入了解技术细节

- 架构设计
- 数据库表结构
- 状态机设计
- 幂等键规则
- 核心组件详解
- 幂等性保障机制
- 常见失败场景处理
- 监控与告警
- 测试场景
- 最佳实践

**适合人群**：开发工程师、架构师、运维工程师

---

### 3. [WALLET-RECHARGE-QUICK-REFERENCE.md](./WALLET-RECHARGE-QUICK-REFERENCE.md)
**快速参考卡** - 日常开发和故障排查

- API 接口速查
- 幂等键速查
- 状态流转速查
- 核心表结构速查
- 常用 SQL 速查
- 故障排查速查
- 监控指标速查
- 测试场景速查

**适合人群**：开发工程师、运维工程师、测试工程师

---

## 快速开始

### 1. 创建充值单

```bash
curl -X POST http://localhost:8080/api/wallet/recharge/create \
  -H "Content-Type: application/json" \
  -d '{
    "tenantId": 1,
    "userId": 123,
    "rechargeAmount": 100.00,
    "payChannel": "WECHAT",
    "idempotencyKey": "uuid-123e4567-e89b-12d3-a456-426614174000"
  }'
```

### 2. 模拟微信回调

```bash
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
```

### 3. 验证充值结果

```sql
-- 查询充值单状态
SELECT * FROM bc_wallet_recharge_order WHERE recharge_id = 'wrc_01HQZX1234567890';

-- 查询账本流水
SELECT * FROM bc_wallet_ledger WHERE biz_order_no = 'wrc_01HQZX1234567890';

-- 查询账户余额
SELECT * FROM bc_wallet_account WHERE user_id = 123;
```

## 核心流程图

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

## 幂等性保障

### 三层幂等防护

| 层级 | 幂等键 | 唯一约束 | 用途 |
|-----|-------|---------|------|
| 第一层 | 客户端生成 UUID | `uk_tenant_idem_key` (recharge_order) | 防止客户端重复提交 |
| 第二层 | 渠道交易号 | `uk_tenant_pay_no` (recharge_order) | 防止微信重复回调 |
| 第三层 | `{tenantId}:{userId}:recharge:{rechargeNo}:credit` | `uk_tenant_idem_key` (wallet_ledger) | 防止 Outbox 重试导致重复入账 |

## 常见问题

### Q1: 微信回调找不到充值单？

**A**: 检查充值单是否存在，以及 `pay_no` 字段是否已填充。详见 [故障排查](./WALLET-RECHARGE-QUICK-REFERENCE.md#故障排查)。

### Q2: 账户余额未增加？

**A**: 检查 Outbox 事件是否投递，消费日志是否记录成功。详见 [故障排查](./WALLET-RECHARGE-QUICK-REFERENCE.md#故障排查)。

### Q3: Outbox 事件投递失败？

**A**: 查看 `last_error` 字段，定位错误原因，修复后重置状态。详见 [故障排查](./WALLET-RECHARGE-QUICK-REFERENCE.md#故障排查)。

### Q4: 如何验证幂等性？

**A**: 重复发送相同的微信回调，验证账户余额只增加一次。详见 [测试场景](./WALLET-RECHARGE-QUICK-REFERENCE.md#测试场景)。

## 监控指标

| 指标 | 告警阈值 | 查询 SQL |
|-----|---------|---------|
| 待投递充值事件 | > 100 | `SELECT COUNT(*) FROM bc_outbox_event WHERE status IN ('NEW', 'FAILED') AND event_type = 'recharge.paid'` |
| 充值死信事件 | > 0 | `SELECT COUNT(*) FROM bc_outbox_event WHERE status = 'DEAD' AND event_type = 'recharge.paid'` |
| 充值消费失败 | > 10 | `SELECT COUNT(*) FROM bc_event_consume_log WHERE status = 'FAILED' AND consumer_name = 'RechargeConsumer'` |

## 相关文档

- [OUTBOX-QUICK-REFERENCE.md](./OUTBOX-QUICK-REFERENCE.md)：Outbox 快速参考
- [OUTBOX-INTEGRATION-EXAMPLE.md](./OUTBOX-INTEGRATION-EXAMPLE.md)：Outbox 集成示例
- [OUTBOX-IMPLEMENTATION-SUMMARY.md](./OUTBOX-IMPLEMENTATION-SUMMARY.md)：Outbox 实现总结

## 技术栈

- **Spring Boot**：应用框架
- **MyBatis-Plus**：ORM 框架
- **MySQL**：数据库
- **Outbox Pattern**：事件驱动一致性
- **Optimistic Locking**：并发控制

## 维护信息

- **版本**：v1.0
- **最后更新**：2025-12-19
- **维护人员**：bluecone
- **联系方式**：[待补充]

## 贡献指南

如发现文档错误或有改进建议，请：

1. 提交 Issue 描述问题
2. 或直接提交 Pull Request
3. 或联系维护人员

---

**祝您使用愉快！**
