# 订单主链路 M0 路由冲突修复

## 问题描述

在启动 Spring Boot 应用时，遇到了路由映射冲突错误：

```
Ambiguous mapping. Cannot map 'orderHelloController' method 
com.bluecone.app.order.controller.OrderController#confirmOrder(ConfirmOrderRequest)
to {POST [/api/order/confirm]}: There is already 'orderMainFlowController' bean method
com.bluecone.app.controller.order.OrderMainFlowController#confirm(OrderConfirmRequest) mapped.
```

## 原因分析

项目中存在两个 Controller 定义了相同的路由：

### 1. 现有 Controller（app-order 模块）
- **位置**：`app-order/src/main/java/com/bluecone/app/order/controller/OrderController.java`
- **Bean名称**：`orderHelloController`
- **路由**：`POST /api/order/confirm`（第71-76行）
- **说明**：这是现有的订单 Controller，包含多个订单相关接口

### 2. 新创建 Controller（app-application 模块）
- **位置**：`app-application/src/main/java/com/bluecone/app/controller/order/OrderMainFlowController.java`
- **Bean名称**：`orderMainFlowController`
- **路由**：`POST /api/order/confirm`
- **说明**：订单主链路 M0 Controller

## 问题根源

虽然两个 Controller 在不同的模块和包下，但它们定义了相同的 HTTP 路由 `POST /api/order/confirm`，Spring MVC 不允许这种情况。

## 修复方案

修改新创建的 `OrderMainFlowController`，使用不同的路由前缀 `/api/order/m0/`，以避免与现有路由冲突。

### 修复前
```java
@PostMapping("/confirm")
public OrderConfirmResponse confirm(@RequestBody OrderConfirmRequest request) {
    // ...
}

@PostMapping("/submit")
public OrderSubmitResponse submit(@RequestBody OrderSubmitRequest request) {
    // ...
}
```

### 修复后
```java
@PostMapping("/m0/confirm")
public OrderConfirmResponse confirm(@RequestBody OrderConfirmRequest request) {
    // ...
}

@PostMapping("/m0/submit")
public OrderSubmitResponse submit(@RequestBody OrderSubmitRequest request) {
    // ...
}
```

## 新的路由

### 订单主链路 M0 接口
- **确认单接口**：`POST /api/order/m0/confirm`
- **提交单接口**：`POST /api/order/m0/submit`

### 现有订单接口（保持不变）
- **确认订单**：`POST /api/order/confirm`（现有功能）
- **用户提交订单**：`POST /api/order/user/orders/submit`
- **用户订单预览**：`POST /api/order/user/orders/preview`
- **用户取消订单**：`POST /api/order/user/orders/{orderId}/cancel`
- **商户接单**：`POST /api/order/merchant/orders/{orderId}/accept`
- 等等...

## 路由命名说明

使用 `/m0/` 前缀的原因：

1. **版本标识**：`m0` 表示这是订单主链路的 M0（Milestone 0）版本
2. **避免冲突**：与现有路由明确区分，避免路由冲突
3. **易于识别**：清晰地表明这是新实现的订单主链路接口
4. **便于演进**：后续可以有 `/m1/`、`/m2/` 等版本，逐步替换现有接口

## curl 示例（更新后）

### 订单确认单
```bash
curl -X POST http://localhost:8080/api/order/m0/confirm \
  -H "Content-Type: application/json" \
  -d '{
    "tenantId": 1,
    "storeId": 1,
    "userId": 1,
    "deliveryType": "DINE_IN",
    "items": [{"skuId": 101, "quantity": 2, "clientUnitPrice": 10.00}]
  }'
```

### 订单提交单
```bash
curl -X POST http://localhost:8080/api/order/m0/submit \
  -H "Content-Type: application/json" \
  -d '{
    "tenantId": 1,
    "storeId": 1,
    "userId": 1,
    "clientRequestId": "550e8400-e29b-41d4-a716-446655440000",
    "deliveryType": "DINE_IN",
    "items": [{"skuId": 101, "quantity": 2, "clientUnitPrice": 10.00}]
  }'
```

## 验证结果

### 启动应用
```bash
cd /Users/zhenpengmu/Desktop/code/project/bluecone-app/app-application
mvn spring-boot:run
```

**期望结果**：✅ 应用正常启动，无路由冲突错误

### 路由列表
```
POST /api/order/confirm              -> orderHelloController (现有)
POST /api/order/m0/confirm           -> orderMainFlowController (新增)
POST /api/order/m0/submit            -> orderMainFlowController (新增)
POST /api/order/user/orders/submit   -> orderHelloController (现有)
...
```

## 技术说明

### Spring MVC 路由冲突检测

Spring MVC 在启动时会检测所有 `@RequestMapping` 注解，如果发现两个方法映射到相同的 HTTP 方法和路径，会抛出 `IllegalStateException`：

```
Ambiguous mapping. Cannot map 'beanName' method ...
```

### 避免路由冲突的方法

1. **使用不同的路径**（推荐）
   ```java
   @PostMapping("/m0/confirm")  // 使用版本前缀
   @PostMapping("/v2/confirm")  // 使用版本号
   ```

2. **使用不同的 HTTP 方法**
   ```java
   @GetMapping("/confirm")   // GET
   @PostMapping("/confirm")  // POST
   ```

3. **使用不同的请求头**
   ```java
   @PostMapping(value = "/confirm", headers = "X-Api-Version=1")
   @PostMapping(value = "/confirm", headers = "X-Api-Version=2")
   ```

4. **使用不同的请求参数**
   ```java
   @PostMapping(value = "/confirm", params = "version=1")
   @PostMapping(value = "/confirm", params = "version=2")
   ```

### 最佳实践

1. **API 版本化**：使用路径前缀（如 `/v1/`、`/v2/`）或请求头（如 `X-Api-Version`）进行版本管理
2. **模块隔离**：不同模块的 Controller 使用不同的路径前缀
3. **命名规范**：使用清晰的命名，避免混淆

## 总结

通过将订单主链路 M0 的路由修改为 `/api/order/m0/confirm` 和 `/api/order/m0/submit`，成功解决了与现有路由的冲突问题。

**修复后的文件**：
- ✅ 修改：`app-application/.../OrderMainFlowController.java`
- ✅ 更新：`ORDER-MAIN-FLOW-M0-IMPLEMENTATION-SUMMARY.md`
- ✅ 更新：`ORDER-M0-QUICK-START.md`
- ✅ 新增：`ORDER-M0-ROUTE-CONFLICT-FIX.md`（本文件）

**新的 API 路由**：
- ✅ `POST /api/order/m0/confirm` - 订单确认单
- ✅ `POST /api/order/m0/submit` - 订单提交单
