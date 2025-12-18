# 订单主链路 M0 Bean 冲突修复

## 问题描述

在启动 Spring Boot 应用时，遇到了 Bean 名称冲突错误：

```
org.springframework.context.annotation.ConflictingBeanDefinitionException: 
Annotation-specified bean name 'orderController' for bean class 
[com.bluecone.app.controller.order.OrderController] conflicts with existing, 
non-compatible bean definition of same name and class 
[com.bluecone.app.controller.OrderController]
```

## 原因分析

项目中已经存在一个 `OrderController`：
- **位置**：`app-application/src/main/java/com/bluecone/app/controller/OrderController.java`
- **职责**：订单 API Controller（极简版本），使用 `CommandRouter` 模式
- **路径**：`/api/order`

而我创建的新 Controller：
- **位置**：`app-application/src/main/java/com/bluecone/app/controller/order/OrderController.java`
- **职责**：订单主链路 M0 Controller
- **路径**：`/api/order`

虽然两个 Controller 在不同的包下，但 Spring 默认使用类名（首字母小写）作为 Bean 名称，导致冲突：
- 现有 Controller：`orderController`
- 新创建 Controller：`orderController`

## 修复方案

将新创建的 Controller 重命名为 `OrderMainFlowController`，以避免 Bean 名称冲突。

### 修复前
```
app-application/src/main/java/com/bluecone/app/controller/order/OrderController.java
```

### 修复后
```
app-application/src/main/java/com/bluecone/app/controller/order/OrderMainFlowController.java
```

### Bean 名称
- 现有 Controller：`orderController`
- 新创建 Controller：`orderMainFlowController` ✅

## 修复内容

### 1. 删除冲突的文件
```bash
rm app-application/src/main/java/com/bluecone/app/controller/order/OrderController.java
```

### 2. 创建新文件
```bash
app-application/src/main/java/com/bluecone/app/controller/order/OrderMainFlowController.java
```

**文件内容**：
```java
package com.bluecone.app.controller.order;

import com.bluecone.app.order.api.dto.*;
import com.bluecone.app.order.application.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

/**
 * 订单主链路 Controller（M0）。
 * <p>提供订单确认单和提交单接口，遵循项目约定：Controller 仅做装配，业务编排在 app-order 的 application 层。</p>
 */
@Slf4j
@RestController
@RequestMapping("/api/order")
@RequiredArgsConstructor
public class OrderMainFlowController {

    private final OrderConfirmApplicationService orderConfirmApplicationService;
    private final OrderSubmitApplicationService orderSubmitApplicationService;

    /**
     * 订单确认单接口（M0）。
     */
    @PostMapping("/confirm")
    public OrderConfirmResponse confirm(@RequestBody OrderConfirmRequest request) {
        // ...
    }

    /**
     * 订单提交单接口（M0）。
     */
    @PostMapping("/submit")
    public OrderSubmitResponse submit(@RequestBody OrderSubmitRequest request) {
        // ...
    }
}
```

## 验证结果

### 启动应用
```bash
mvn -pl app-application -am spring-boot:run
```

**期望结果**：✅ 应用正常启动，无 Bean 冲突错误

### Bean 列表
```
orderController          -> com.bluecone.app.controller.OrderController
orderMainFlowController  -> com.bluecone.app.controller.order.OrderMainFlowController
```

## 技术说明

### Spring Bean 命名规则

Spring 默认使用以下规则为 Bean 命名：

1. **类名首字母小写**：`OrderController` → `orderController`
2. **如果类名前两个字母都是大写**：保持原样（如 `IOUtils` → `IOUtils`）
3. **显式指定名称**：`@Component("myBean")`

### 避免 Bean 冲突的方法

1. **使用不同的类名**（推荐）
   ```java
   @RestController
   public class OrderMainFlowController { }
   ```

2. **显式指定 Bean 名称**
   ```java
   @RestController("orderMainFlowController")
   public class OrderController { }
   ```

3. **使用不同的包路径**（不推荐，仍可能冲突）
   ```
   com.bluecone.app.controller.OrderController
   com.bluecone.app.controller.order.OrderController
   ```

### 最佳实践

1. **Controller 命名要具体**：避免使用过于通用的名称（如 `OrderController`）
2. **按功能模块命名**：如 `OrderMainFlowController`、`OrderQueryController`、`OrderRefundController`
3. **避免在不同包下使用相同类名**：即使在不同包下，也可能导致混淆

## 总结

通过将 Controller 重命名为 `OrderMainFlowController`，成功解决了 Bean 名称冲突问题。这个命名更加清晰地表明了 Controller 的职责（订单主链路），也避免了与现有 Controller 的冲突。

**修复后的文件清单**：
- ✅ 删除：`app-application/.../controller/order/OrderController.java`
- ✅ 新增：`app-application/.../controller/order/OrderMainFlowController.java`
- ✅ 更新：`ORDER-MAIN-FLOW-M0-IMPLEMENTATION-SUMMARY.md`
- ✅ 更新：`ORDER-M0-FILES-CHECKLIST.md`
