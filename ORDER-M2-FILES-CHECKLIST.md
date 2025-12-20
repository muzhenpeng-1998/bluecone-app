# 订单 M2：商户接单/拒单 - 改动文件清单

## 一、改动概览

| 模块 | 新增文件 | 修改文件 | 总计 |
|------|---------|---------|------|
| app-infra（数据库） | 1 | 0 | 1 |
| app-order（领域层） | 4 | 2 | 6 |
| app-order（应用层） | 2 | 2 | 4 |
| app-order（接口层） | 1 | 2 | 3 |
| app-order（基础设施） | 4 | 2 | 6 |
| 文档 | 3 | 0 | 3 |
| **总计** | **15** | **8** | **23** |

## 二、详细文件清单

### 2.1 app-infra（数据库迁移）

#### 新增文件（1个）

```
app-infra/src/main/resources/db/migration/
  └─ V20251218002__add_order_acceptance_m2_fields.sql    [新增] Flyway 迁移脚本
```

**说明**：
- 补充订单表字段：`reject_reason_code`、`reject_reason_desc`、`rejected_at`、`rejected_by`
- 新增幂等动作表：`bc_order_action_log`

### 2.2 app-order（领域层）

#### 新增文件（4个）

```
app-order/src/main/java/com/bluecone/app/order/domain/
  ├─ model/
  │   └─ OrderActionLog.java                              [新增] 幂等动作日志聚合根
  ├─ repository/
  │   └─ OrderActionLogRepository.java                    [新增] 幂等日志仓储接口
  ├─ event/
  │   └─ OrderRejectedEvent.java                          [新增] 商户拒单领域事件
  └─ enums/
      └─ OrderErrorCode.java                              [新增] 订单业务错误码枚举
```

#### 修改文件（2个）

```
app-order/src/main/java/com/bluecone/app/order/domain/
  ├─ model/
  │   └─ Order.java                                       [修改] 新增 reject() 方法及字段
  └─ enums/
      └─ OrderStatus.java                                 [无需修改] 已有 canAccept() 方法
```

**修改内容**：
- `Order.java`：
  - 新增字段：`rejectReasonCode`、`rejectReasonDesc`、`rejectedAt`、`rejectedBy`
  - 新增方法：`reject(operatorId, reasonCode, reasonDesc)`
  - 增强方法：`accept(operatorId)` 添加详细中文注释

### 2.3 app-order（应用层）

#### 新增文件（2个）

```
app-order/src/main/java/com/bluecone/app/order/application/
  └─ command/
      └─ MerchantRejectOrderCommand.java                  [新增] 商户拒单命令
```

#### 修改文件（2个）

```
app-order/src/main/java/com/bluecone/app/order/application/
  ├─ command/
  │   └─ MerchantAcceptOrderCommand.java                  [修改] 新增 requestId、expectedVersion 字段
  ├─ MerchantOrderCommandAppService.java                  [修改] 新增 rejectOrder() 方法签名
  └─ impl/
      └─ MerchantOrderCommandAppServiceImpl.java          [修改] 重写 acceptOrder()、新增 rejectOrder()
```

**修改内容**：
- `MerchantAcceptOrderCommand.java`：新增 `requestId`、`expectedVersion` 字段
- `MerchantOrderCommandAppService.java`：新增 `rejectOrder(command)` 方法签名
- `MerchantOrderCommandAppServiceImpl.java`：
  - 重写 `acceptOrder()`，集成幂等保护和乐观锁
  - 新增 `rejectOrder()`，集成幂等保护和乐观锁
  - 新增 `tryCreateActionLog()` - 幂等检查辅助方法

### 2.4 app-order（接口层）

#### 新增文件（1个）

```
app-order/src/main/java/com/bluecone/app/order/api/dto/
  └─ MerchantRejectOrderRequest.java                      [新增] 商户拒单请求 DTO
```

#### 修改文件（2个）

```
app-order/src/main/java/com/bluecone/app/order/
  ├─ api/dto/
  │   ├─ MerchantAcceptOrderRequest.java                  [修改] 新增 requestId、expectedVersion 字段
  │   └─ MerchantOrderView.java                           [修改] 新增拒单相关字段
  └─ controller/
      └─ OrderController.java                             [修改] 新增 rejectMerchantOrder() 接口
```

**修改内容**：
- `MerchantAcceptOrderRequest.java`：新增 `requestId`、`expectedVersion` 字段
- `MerchantOrderView.java`：新增 `version`、`rejectReasonCode`、`rejectReasonDesc`、`rejectedAt`、`rejectedBy` 字段
- `OrderController.java`：新增 `POST /api/order/merchant/orders/{orderId}/reject` 接口

### 2.5 app-order（基础设施层）

#### 新增文件（4个）

```
app-order/src/main/java/com/bluecone/app/order/infra/persistence/
  ├─ po/
  │   └─ OrderActionLogPO.java                            [新增] 幂等动作表 PO
  ├─ mapper/
  │   └─ OrderActionLogMapper.java                        [新增] MyBatis Mapper
  ├─ converter/
  │   └─ OrderActionLogConverter.java                     [新增] OrderActionLog 转换器
  └─ repository/
      └─ OrderActionLogRepositoryImpl.java                [新增] 幂等日志仓储实现
```

#### 修改文件（2个）

```
app-order/src/main/java/com/bluecone/app/order/infra/persistence/
  ├─ po/
  │   └─ OrderPO.java                                     [修改] 新增拒单相关字段
  └─ converter/
      └─ OrderConverter.java                              [修改] 支持新字段转换
```

**修改内容**：
- `OrderPO.java`：新增 `rejectReasonCode`、`rejectReasonDesc`、`rejectedAt`、`rejectedBy` 字段
- `OrderConverter.java`：支持新增字段的 PO ↔ Domain 转换

### 2.6 文档（3个）

```
项目根目录/
  ├─ ORDER-M2-MERCHANT-ACCEPTANCE-IMPLEMENTATION.md       [新增] 完整实现文档
  ├─ ORDER-M2-TEST-COMMANDS.sh                            [新增] 测试脚本（可执行）
  └─ ORDER-M2-FILES-CHECKLIST.md                          [新增] 本文件（改动清单）
```

## 三、核心改动说明

### 3.1 数据库层

| 表名 | 类型 | 说明 |
|------|------|------|
| `bc_order` | 修改 | 新增 4 个字段：`reject_reason_code`、`reject_reason_desc`、`rejected_at`、`rejected_by` |
| `bc_order_action_log` | 新增 | 幂等动作日志表，唯一键 `action_key` |

### 3.2 领域层（DDD 核心）

| 类名 | 类型 | 职责 |
|------|------|------|
| `Order` | 修改 | 新增 `reject()` 方法，封装拒单业务逻辑 |
| `OrderActionLog` | 新增 | 幂等动作日志聚合根 |
| `OrderRejectedEvent` | 新增 | 商户拒单领域事件 |
| `OrderErrorCode` | 新增 | 订单业务错误码枚举 |

### 3.3 应用层（用例编排）

| 类名 | 类型 | 职责 |
|------|------|------|
| `MerchantAcceptOrderCommand` | 修改 | 新增 `requestId`、`expectedVersion` 字段 |
| `MerchantRejectOrderCommand` | 新增 | 商户拒单命令 |
| `MerchantOrderCommandAppServiceImpl` | 重构 | 集成幂等保护和乐观锁 |

### 3.4 接口层（API 暴露）

| 端点 | 类型 | 说明 |
|------|------|------|
| `POST /api/order/merchant/orders/{orderId}/accept` | 修改 | 支持 `requestId`、`expectedVersion` |
| `POST /api/order/merchant/orders/{orderId}/reject` | 新增 | 商户拒单接口 |

## 四、测试文件建议（可选，未在本次实现）

```
app-order/src/test/java/com/bluecone/app/order/
  ├─ domain/model/
  │   └─ OrderTest.java                                   [建议新增] 单元测试：Order.accept()/reject()
  ├─ application/impl/
  │   └─ MerchantOrderCommandAppServiceImplTest.java      [建议新增] 集成测试：幂等、并发、状态约束
  └─ controller/
      └─ OrderControllerTest.java                         [建议新增] API 测试：接口验证
```

**测试覆盖要点**：
- 幂等测试：同一 requestId 重复调用
- 并发测试：两个线程同时接单/拒单
- 状态约束：非 WAIT_ACCEPT 状态不允许接单/拒单
- 版本冲突：expectedVersion 不匹配时抛出异常

## 五、验证步骤

### 5.1 启动前检查

```bash
# 1. 确认 Flyway 迁移已就绪
ls -lh app-infra/src/main/resources/db/migration/V20251218002__*.sql

# 2. 确认所有文件已创建/修改
git status

# 3. 编译项目
mvn clean compile
```

### 5.2 启动应用

```bash
# 启动 Spring Boot 应用
mvn spring-boot:run
```

### 5.3 执行测试脚本

```bash
# 修改测试脚本中的订单 ID 和版本号，然后执行
bash ORDER-M2-TEST-COMMANDS.sh
```

### 5.4 数据库验证

```sql
-- 1. 查询订单主表（确认拒单字段已生效）
DESCRIBE bc_order;

-- 2. 查询幂等动作表（确认表已创建）
DESCRIBE bc_order_action_log;

-- 3. 查询测试订单
SELECT id, order_no, status, version, 
       accept_operator_id, accepted_at,
       reject_reason_code, reject_reason_desc, rejected_at, rejected_by
FROM bc_order
WHERE id IN (123456, 123457, 123458);

-- 4. 查询幂等日志
SELECT id, order_id, action_type, action_key, 
       operator_id, status, created_at
FROM bc_order_action_log
WHERE order_id IN (123456, 123457, 123458)
ORDER BY created_at DESC;
```

## 六、回滚指南（如需）

如果需要回滚本次改动，请按以下顺序执行：

### 6.1 数据库回滚

```sql
-- 1. 删除幂等动作表
DROP TABLE IF EXISTS bc_order_action_log;

-- 2. 删除订单表新增字段
ALTER TABLE bc_order 
  DROP COLUMN reject_reason_code,
  DROP COLUMN reject_reason_desc,
  DROP COLUMN rejected_at,
  DROP COLUMN rejected_by;
```

### 6.2 代码回滚

```bash
# 回滚所有改动（慎用！）
git checkout HEAD -- .

# 或者选择性回滚
git checkout HEAD -- app-order/
git checkout HEAD -- app-infra/src/main/resources/db/migration/V20251218002__*.sql
```

## 七、后续优化建议

- [ ] 补充单元测试和集成测试
- [ ] 接入分布式追踪（Trace ID）
- [ ] 增加拒单原因码字典管理
- [ ] 增加接单/拒单操作日志查询接口
- [ ] 监控指标：接单率、拒单率、版本冲突次数

## 八、相关文档

- [ORDER-M2-MERCHANT-ACCEPTANCE-IMPLEMENTATION.md](./ORDER-M2-MERCHANT-ACCEPTANCE-IMPLEMENTATION.md) - 完整实现文档
- [ORDER-M2-TEST-COMMANDS.sh](./ORDER-M2-TEST-COMMANDS.sh) - 测试脚本
- [PAYMENT-M1-IMPLEMENTATION-SUMMARY.md](./PAYMENT-M1-IMPLEMENTATION-SUMMARY.md) - 支付 M1 实现文档（参考）
