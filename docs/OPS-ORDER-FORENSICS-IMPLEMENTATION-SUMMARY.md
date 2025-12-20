# Order Forensics 实现总结

## 概述

Order Forensics（订单诊断）功能已完整实现，提供了一个强大的运维诊断工具，用于快速定位订单处理过程中的问题。

## 实现内容

### 1. DTO 层（16 个类）

**主视图**：
- `OrderForensicsView` - 主响应 DTO

**数据段 DTOs**：
- `OrderSummarySection` - 订单摘要
- `PricingSnapshotSection` - 计价快照
- `CouponSection` - 优惠券操作汇总
- `WalletSection` - 钱包操作汇总
- `PointsSection` - 积分操作汇总
- `OutboxSection` - Outbox 事件汇总
- `ConsumeSection` - 消费日志汇总
- `DiagnosisItem` - 诊断结论

**明细 DTOs**：
- `CouponLockItem` - 优惠券锁定记录
- `CouponRedemptionItem` - 优惠券核销记录
- `WalletFreezeItem` - 钱包冻结记录
- `WalletLedgerItem` - 钱包流水记录
- `PointsLedgerItem` - 积分流水记录
- `OutboxEventItem` - Outbox 事件记录
- `ConsumeLogItem` - 消费日志记录

### 2. Mapper 扩展（7 个方法）

为以下 Mapper 添加了诊断查询方法：

1. **OutboxEventMapper**
   - `selectByOrderId()` - 查询订单的所有 Outbox 事件

2. **EventConsumeLogMapper**
   - `selectByEventIds()` - 批量查询消费日志

3. **CouponLockMapper**
   - `selectByOrderId()` - 查询订单的优惠券锁定记录

4. **CouponRedemptionMapper**
   - `selectByOrderId()` - 查询订单的优惠券核销记录

5. **WalletFreezeMapper**
   - `selectByBizOrderIdForForensics()` - 查询订单的钱包冻结记录

6. **WalletLedgerMapper**
   - `selectByBizOrderIdForForensics()` - 查询订单的钱包流水记录

7. **PointsLedgerMapper**
   - `selectByBizId()` - 查询订单的积分流水记录

### 3. 服务层（2 个类）

**OrderDiagnosisEngine**
- 实现了 6 条诊断规则：
  1. `MISSING_WALLET_COMMIT` - 已支付但缺失钱包提交流水
  2. `MISSING_COUPON_REDEMPTION` - 已支付但缺失优惠券核销记录
  3. `OUTBOX_DELIVERY_FAILED` - Outbox 事件投递失败
  4. `COUPON_LOCK_TIMEOUT` / `WALLET_FREEZE_TIMEOUT` - 锁定/冻结超时
  5. `DUPLICATE_CONSUMPTION` - 重复消费检测
  6. `AMOUNT_INCONSISTENT` - 金额不一致检测

**OrderForensicsQueryService**
- 聚合订单全链路数据
- 调用诊断引擎生成诊断结论
- 实现租户隔离验证
- 支持数据量限制（默认每个 section 最多 50 条）

### 4. 控制器层（1 个类）

**OrderForensicsController**
- 端点：`GET /ops/api/orders/{orderId}/forensics?tenantId={tenantId}`
- 受 `OpsConsoleAccessInterceptor` 保护（token 认证）
- 参数验证和异常处理
- 只读接口，不修改任何业务状态

### 5. 测试（2 个测试类）

**OrderDiagnosisEngineTest**
- 12 个单元测试，覆盖所有诊断规则
- 测试正常场景和异常场景
- 验证诊断代码、严重程度、消息内容

**OrderForensicsControllerTest**
- 6 个集成测试
- 测试正常请求、参数验证、异常处理
- 使用 MockMvc 模拟 HTTP 请求

### 6. 文档（2 个文档）

**OPS-ORDER-FORENSICS-GUIDE.md**
- API 参考（端点、参数、响应格式）
- 认证方式说明
- 诊断代码详细说明
- 常见问题排查流程
- 最佳实践和使用示例

**OPS-ORDER-FORENSICS-IMPLEMENTATION-SUMMARY.md**（本文档）
- 实现内容总结
- 文件清单
- 使用示例

## 文件清单

### 新增文件（20 个）

```
app-ops/src/main/java/com/bluecone/app/ops/
├── api/dto/forensics/
│   ├── OrderForensicsView.java
│   ├── OrderSummarySection.java
│   ├── PricingSnapshotSection.java
│   ├── CouponSection.java
│   ├── CouponLockItem.java
│   ├── CouponRedemptionItem.java
│   ├── WalletSection.java
│   ├── WalletFreezeItem.java
│   ├── WalletLedgerItem.java
│   ├── PointsSection.java
│   ├── PointsLedgerItem.java
│   ├── OutboxSection.java
│   ├── OutboxEventItem.java
│   ├── ConsumeSection.java
│   ├── ConsumeLogItem.java
│   └── DiagnosisItem.java
├── service/
│   ├── OrderForensicsQueryService.java
│   └── diagnosis/
│       └── OrderDiagnosisEngine.java
└── web/
    └── OrderForensicsController.java

app-ops/src/test/java/com/bluecone/app/ops/
├── service/diagnosis/
│   └── OrderDiagnosisEngineTest.java
└── web/
    └── OrderForensicsControllerTest.java

docs/
├── OPS-ORDER-FORENSICS-GUIDE.md
└── OPS-ORDER-FORENSICS-IMPLEMENTATION-SUMMARY.md
```

### 修改文件（7 个）

```
app-infra/src/main/java/com/bluecone/app/infra/event/
├── outbox/OutboxEventMapper.java
└── consume/EventConsumeLogMapper.java

app-promo/src/main/java/com/bluecone/app/promo/infra/persistence/mapper/
├── CouponLockMapper.java
└── CouponRedemptionMapper.java

app-wallet/src/main/java/com/bluecone/app/wallet/infra/persistence/mapper/
├── WalletFreezeMapper.java
└── WalletLedgerMapper.java

app-member/src/main/java/com/bluecone/app/member/infra/persistence/mapper/
└── PointsLedgerMapper.java
```

## 使用示例

### 1. 基本查询

```bash
curl -H "X-Ops-Token: your-ops-token" \
  "http://localhost:8080/ops/api/orders/123/forensics?tenantId=1"
```

### 2. 提取诊断结论

```bash
curl -H "X-Ops-Token: your-ops-token" \
  "http://localhost:8080/ops/api/orders/123/forensics?tenantId=1" \
  | jq '.diagnosis'
```

### 3. 只查看错误级别的诊断

```bash
curl -H "X-Ops-Token: your-ops-token" \
  "http://localhost:8080/ops/api/orders/123/forensics?tenantId=1" \
  | jq '.diagnosis[] | select(.severity == "ERROR")'
```

### 4. 批量诊断脚本

```bash
#!/bin/bash
OPS_TOKEN="your-ops-token"
TENANT_ID=1

# 从数据库查询最近的订单
ORDER_IDS=$(mysql -u user -p -D bluecone -N -e \
  "SELECT id FROM bc_order WHERE status = 'PAID' AND created_at > DATE_SUB(NOW(), INTERVAL 1 HOUR)")

for ORDER_ID in $ORDER_IDS; do
  echo "Diagnosing order: $ORDER_ID"
  
  ERRORS=$(curl -s -H "X-Ops-Token: $OPS_TOKEN" \
    "http://localhost:8080/ops/api/orders/$ORDER_ID/forensics?tenantId=$TENANT_ID" \
    | jq -r '.diagnosis[] | select(.severity == "ERROR") | .message')
  
  if [ -n "$ERRORS" ]; then
    echo "  ERRORS FOUND:"
    echo "$ERRORS" | sed 's/^/    /'
  else
    echo "  No errors"
  fi
  
  echo "---"
done
```

## 配置

在 `application.yml` 中配置（如果需要）：

```yaml
bluecone:
  ops:
    console:
      enabled: true
      token: ${OPS_TOKEN:changeme}
      allow-localhost: true
      max-page-size: 100
      expose-payload: false  # 是否暴露事件载荷（敏感信息）
      max-error-msg-len: 200
      max-payload-len: 2000
```

## 安全性

1. **认证**：所有请求必须提供有效的 ops token
2. **租户隔离**：强制验证 tenantId，不能跨租户查询
3. **只读**：不修改任何业务状态
4. **数据脱敏**：可配置是否暴露事件载荷（默认不暴露）
5. **访问日志**：所有请求会被 `OpsConsoleAccessInterceptor` 记录

## 性能考虑

1. **数据量限制**：每个 section 默认最多返回 50 条记录
2. **查询优化**：所有查询都带 tenant_id 索引过滤
3. **并发控制**：建议不要高频调用，适合按需诊断
4. **缓存策略**：当前未实现缓存，每次都是实时查询

## 后续优化建议

1. **计价快照集成**
   - 当前 `PricingSnapshotSection` 返回空数据
   - 如果项目有计价快照表，需要补充查询逻辑

2. **分页支持**
   - 当前每个 section 固定返回最多 50 条
   - 可以考虑添加分页参数

3. **缓存优化**
   - 对于历史订单，可以考虑缓存诊断结果
   - 减少重复查询的数据库压力

4. **诊断规则扩展**
   - 根据实际运维经验，持续添加新的诊断规则
   - 例如：积分扣减异常、退款流程异常等

5. **监控集成**
   - 将诊断结果推送到监控系统
   - 自动告警严重问题

6. **批量诊断接口**
   - 提供批量查询接口，一次诊断多个订单
   - 返回汇总统计信息

## 测试建议

### 单元测试

```bash
mvn test -Dtest=OrderDiagnosisEngineTest
mvn test -Dtest=OrderForensicsControllerTest
```

### 集成测试

1. 创建测试订单
2. 模拟各种异常场景（缺失流水、事件失败等）
3. 调用诊断接口验证诊断结论

### 手动测试清单

- [ ] 正常订单（已支付，所有资产操作成功）
- [ ] 缺失钱包提交流水
- [ ] 缺失优惠券核销记录
- [ ] Outbox 事件失败
- [ ] 锁定/冻结超时
- [ ] 重复消费
- [ ] 租户隔离验证
- [ ] 权限验证（无效 token）

## 相关文档

- [OPS-ORDER-FORENSICS-GUIDE.md](./OPS-ORDER-FORENSICS-GUIDE.md) - 使用指南
- [OUTBOX-QUICK-REFERENCE.md](./OUTBOX-QUICK-REFERENCE.md) - Outbox 快速参考
- [OUTBOX-CONSISTENCY-GUIDE.md](./OUTBOX-CONSISTENCY-GUIDE.md) - 一致性指南

## 贡献者

- 实现日期：2025-12-19
- 实现模块：app-ops
- 涉及模块：app-infra, app-promo, app-wallet, app-member

## 版本历史

- v1.0 (2025-12-19)：初始实现
  - 16 个 DTO 类
  - 6 条诊断规则
  - 完整的测试覆盖
  - 详细的使用文档
