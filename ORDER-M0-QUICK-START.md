# 订单主链路 M0 快速验证指南

## 一、前置准备

### 1.1 数据库准备
确保 MySQL 数据库已启动，并执行 Flyway 迁移：

```bash
cd /Users/zhenpengmu/Desktop/code/project/bluecone-app
mvn -pl app-infra flyway:migrate
```

### 1.2 检查配置
确保 `app-application/src/main/resources/application-local.yml` 中的数据库配置正确。

## 二、运行测试（推荐）

### 方式1：运行集成测试
```bash
cd /Users/zhenpengmu/Desktop/code/project/bluecone-app
mvn -pl app-application -am clean test -Dtest=OrderMainFlowM0IntegrationTest
```

**期望结果**：
- ✅ 测试场景1：先 confirm 再 submit（通过）
- ✅ 测试场景2：幂等验证 - 同一个 clientRequestId submit 两次（通过）
- ✅ 测试场景3：不同的 clientRequestId 应该创建不同的订单（通过）

### 方式2：启动应用并手动测试

#### 2.1 启动应用
```bash
cd /Users/zhenpengmu/Desktop/code/project/bluecone-app
mvn -pl app-application -am spring-boot:run
```

#### 2.2 执行 curl 命令

##### 步骤1：调用订单确认单接口
```bash
curl -X POST http://localhost:8080/api/order/m0/confirm \
  -H "Content-Type: application/json" \
  -d '{
    "tenantId": 1,
    "storeId": 1,
    "userId": 1,
    "deliveryType": "DINE_IN",
    "channel": "MINI_PROGRAM",
    "orderSource": "MINI_PROGRAM",
    "items": [
      {
        "skuId": 101,
        "productId": 100,
        "quantity": 2,
        "clientUnitPrice": 10.00
      },
      {
        "skuId": 102,
        "productId": 100,
        "quantity": 1,
        "clientUnitPrice": 20.00
      }
    ],
    "remark": "测试订单"
  }'
```

**期望返回**：
```json
{
  "confirmToken": "a1b2c3d4e5f6g7h8i9j0k1l2m3n4o5p6",
  "priceVersion": 1734518400000,
  "totalAmount": 40.00,
  "discountAmount": 0.00,
  "payableAmount": 40.00,
  "currency": "CNY",
  "items": [...],
  "storeAcceptable": true,
  "storeRejectReasonCode": null,
  "storeRejectReasonMessage": null,
  "failureReasons": null
}
```

##### 步骤2：调用订单提交单接口（首次）
```bash
curl -X POST http://localhost:8080/api/order/m0/submit \
  -H "Content-Type: application/json" \
  -d '{
    "tenantId": 1,
    "storeId": 1,
    "userId": 1,
    "clientRequestId": "550e8400-e29b-41d4-a716-446655440000",
    "confirmToken": "a1b2c3d4e5f6g7h8i9j0k1l2m3n4o5p6",
    "priceVersion": 1734518400000,
    "deliveryType": "DINE_IN",
    "channel": "MINI_PROGRAM",
    "orderSource": "MINI_PROGRAM",
    "items": [
      {
        "skuId": 101,
        "productId": 100,
        "quantity": 2,
        "clientUnitPrice": 10.00
      },
      {
        "skuId": 102,
        "productId": 100,
        "quantity": 1,
        "clientUnitPrice": 20.00
      }
    ],
    "remark": "测试订单"
  }'
```

**期望返回**：
```json
{
  "orderId": 123456789012345678,
  "publicOrderNo": "ord_01HN8X5K9G3QRST2VW4XYZ",
  "status": "WAIT_PAY",
  "payableAmount": 40.00,
  "currency": "CNY",
  "idempotent": false
}
```

##### 步骤3：调用订单提交单接口（重复提交，验证幂等）
```bash
# 使用相同的 clientRequestId 再次提交
curl -X POST http://localhost:8080/api/order/m0/submit \
  -H "Content-Type: application/json" \
  -d '{
    "tenantId": 1,
    "storeId": 1,
    "userId": 1,
    "clientRequestId": "550e8400-e29b-41d4-a716-446655440000",
    "confirmToken": "a1b2c3d4e5f6g7h8i9j0k1l2m3n4o5p6",
    "priceVersion": 1734518400000,
    "deliveryType": "DINE_IN",
    "channel": "MINI_PROGRAM",
    "orderSource": "MINI_PROGRAM",
    "items": [
      {
        "skuId": 101,
        "productId": 100,
        "quantity": 2,
        "clientUnitPrice": 10.00
      },
      {
        "skuId": 102,
        "productId": 100,
        "quantity": 1,
        "clientUnitPrice": 20.00
      }
    ],
    "remark": "测试订单"
  }'
```

**期望返回**：
```json
{
  "orderId": 123456789012345678,
  "publicOrderNo": "ord_01HN8X5K9G3QRST2VW4XYZ",
  "status": "WAIT_PAY",
  "payableAmount": 40.00,
  "currency": "CNY",
  "idempotent": true
}
```

**验证点**：
- ✅ `orderId` 和 `publicOrderNo` 与第一次提交完全相同
- ✅ `idempotent` 字段为 `true`

## 三、数据库验证

### 3.1 查询订单主表
```sql
SELECT * FROM bc_order WHERE tenant_id = 1 ORDER BY created_at DESC LIMIT 10;
```

**期望结果**：
- 有订单记录
- `order_no` 格式为 `ord_xxx`
- `status` 为 `WAIT_PAY`
- `payable_amount` 为 `40.00`

### 3.2 查询订单明细表
```sql
SELECT * FROM bc_order_item WHERE tenant_id = 1 ORDER BY created_at DESC LIMIT 10;
```

**期望结果**：
- 有订单明细记录（2条）
- `product_name` 为 `商品名称-101` 和 `商品名称-102`
- `quantity` 为 `2` 和 `1`
- `unit_price` 为 `10.00` 和 `20.00`

### 3.3 查询幂等记录表
```sql
SELECT * FROM bc_idempotency_record WHERE tenant_id = 1 AND biz_type = 'ORDER_SUBMIT' ORDER BY created_at DESC LIMIT 10;
```

**期望结果**：
- 有幂等记录
- `idem_key` 为 `1:1:1:550e8400-e29b-41d4-a716-446655440000`
- `status` 为 `1`（SUCCEEDED）
- `result_ref` 为订单号（如 `ord_01HN8X5K9G3QRST2VW4XYZ`）

## 四、常见问题排查

### 4.1 测试失败：门店不可接单
**原因**：门店不存在或门店状态不可接单。

**解决方案**：
1. 确保门店表（bc_store）中有 `id=1` 的门店记录
2. 确保门店状态为可接单状态
3. 确保门店营业时间配置正确

### 4.2 测试失败：数据库连接失败
**原因**：数据库未启动或配置错误。

**解决方案**：
1. 检查 MySQL 是否启动：`mysql -u root -p`
2. 检查配置文件：`app-application/src/main/resources/application-local.yml`
3. 检查数据库是否存在：`SHOW DATABASES;`

### 4.3 测试失败：表不存在
**原因**：Flyway 迁移未执行。

**解决方案**：
```bash
mvn -pl app-infra flyway:migrate
```

### 4.4 测试失败：幂等记录冲突
**原因**：幂等记录已存在，但请求内容不一致。

**解决方案**：
1. 清空幂等记录表：`DELETE FROM bc_idempotency_record WHERE tenant_id = 1;`
2. 或者使用不同的 `clientRequestId`

## 五、验收检查清单

### 5.1 功能验收
- [ ] 订单确认单接口返回 confirmToken 和 priceVersion
- [ ] 订单确认单接口返回正确的价格计算结果
- [ ] 订单确认单接口返回门店可接单状态
- [ ] 订单提交单接口返回 orderId 和 publicOrderNo
- [ ] 订单提交单接口返回订单状态为 WAIT_PAY
- [ ] 订单提交单接口支持幂等（同一个 clientRequestId 提交两次，返回同一个 orderId）
- [ ] 订单提交单接口对不同的 clientRequestId 创建不同的订单

### 5.2 数据库验收
- [ ] bc_order 表中有订单记录
- [ ] bc_order_item 表中有订单明细记录
- [ ] bc_idempotency_record 表中有幂等记录
- [ ] bc_public_id_map 表中有 PublicId 映射记录（如果启用）

### 5.3 代码质量验收
- [ ] 所有新增类/方法都有中文注释（行内 + JavaDoc）
- [ ] 错误信息结构化（code + message + detail）
- [ ] 业务编排在 app-order 的 application 层，app-application 仅做 Controller/装配
- [ ] 不跨模块直接查表：订单模块通过 StoreFacade 获取门店信息

## 六、下一步

完成验收后，可以继续优化：
1. 接入 ProductFacade，校验商品是否存在、是否上架、库存是否充足等
2. 从商品服务获取实时价格，替换客户端传递的价格
3. 接入 PromotionFacade，支持优惠券、满减、会员价等优惠计算
4. 校验 confirmToken 是否有效、priceVersion 是否过期
5. 在订单提交时扣减库存（预扣或实扣）
6. 接入支付模块，支持微信支付、支付宝支付等

## 七、联系方式

如有问题，请联系开发团队或查看详细文档：
- 详细实现文档：`ORDER-MAIN-FLOW-M0-IMPLEMENTATION-SUMMARY.md`
- 项目 README：`README.md`
