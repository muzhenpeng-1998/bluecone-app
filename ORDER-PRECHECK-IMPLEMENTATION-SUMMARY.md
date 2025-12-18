# 订单前置校验功能实现总结

## 一、改动文件列表（按模块分组）

### app-store 模块

#### API DTO 层
1. **app-store/src/main/java/com/bluecone/app/store/api/dto/StoreOrderSnapshot.java**
   - 新增字段：`todayOpeningHoursRange`（当天营业时间区间）

2. **app-store/src/main/java/com/bluecone/app/store/api/dto/StoreOrderAcceptResult.java**
   - 新增字段：`detail`（详细错误信息）

#### 领域模型层
3. **app-store/src/main/java/com/bluecone/app/store/domain/model/StoreOpeningSchedule.java**
   - 新增方法：`getOpeningHoursRange(LocalDate date)`（获取指定日期的营业时间区间字符串）

#### 领域服务层
4. **app-store/src/main/java/com/bluecone/app/store/domain/service/impl/StoreOpenStateServiceImpl.java**
   - 调整 `check` 方法的判断顺序，按以下顺序执行：
     1. 门店是否存在且归属 tenant
     2. 门店状态是否允许接单
     3. openForOrders 开关
     4. 渠道能力（若参数带 channelType）
     5. 营业时间/特殊日
     6. 能力校验（capability）
   - 为所有判断分支添加详细的中文注释，说明判断顺序和原因
   - 为所有返回结果添加 `detail` 字段填充

#### 应用服务层
5. **app-store/src/main/java/com/bluecone/app/store/application/StoreContextProviderImpl.java**
   - 修改 `getOrderSnapshot` 方法：使用 `StoreConfig` 替代 `StoreRuntime`，获取完整的门店配置信息
   - 新增方法：`mapToStoreOrderSnapshot(StoreConfig config, LocalDateTime now, String channelType)`
   - 在快照中填充 `configVersion` 和 `todayOpeningHoursRange` 字段

### app-order 模块

#### 应用服务层
6. **app-order/src/main/java/com/bluecone/app/order/application/OrderPreCheckService.java**（新增）
   - 订单前置校验服务接口
   - 提供 `preCheck` 方法，接收 tenantId、storeId、channelType、now、cartSummary 参数

7. **app-order/src/main/java/com/bluecone/app/order/application/impl/OrderPreCheckServiceImpl.java**（新增）
   - 订单前置校验服务实现
   - 调用 `StoreFacade.checkOrderAcceptable` 进行校验
   - 若不可接单，抛出 `BizException`，异常信息中携带 reasonCode

#### 领域错误码
8. **app-order/src/main/java/com/bluecone/app/order/domain/error/OrderErrorCode.java**
   - 新增错误码：`STORE_NOT_ACCEPTABLE("OR-400-003", "门店当前不可接单")`

#### 测试
9. **app-order/src/test/java/com/bluecone/app/order/application/OrderPreCheckServiceTest.java**（新增）
   - 订单前置校验服务集成测试

### app-application 模块

#### Controller 层
10. **app-application/src/main/java/com/bluecone/app/controller/order/OrderPreCheckController.java**（新增）
    - REST Controller：提供 `/api/orders/precheck` 接口
    - 用于调试和管理端接口

---

## 二、验证方式

### A) 集成测试

运行集成测试：
```bash
cd /Users/zhenpengmu/Desktop/code/project/bluecone-app
mvn -pl app-order -am test -Dtest=OrderPreCheckServiceTest
```

### B) REST Controller（推荐用于快速验证）

#### 1. 启动应用
```bash
cd /Users/zhenpengmu/Desktop/code/project/bluecone-app
mvn -pl app-application -am spring-boot:run
```

#### 2. 调用前置校验接口

**场景 1：门店可接单（正常情况）**
```bash
curl -X POST http://localhost:8080/api/orders/precheck \
  -H "Content-Type: application/json" \
  -H "X-Tenant-Id: 1001" \
  -d '{
    "storeId": 1,
    "channelType": null
  }'
```

**场景 2：门店接单开关关闭（校验失败）**
```bash
# 先通过管理端关闭门店接单开关，然后再调用
curl -X POST http://localhost:8080/api/orders/precheck \
  -H "Content-Type: application/json" \
  -H "X-Tenant-Id: 1001" \
  -d '{
    "storeId": 1,
    "channelType": null
  }'
```

**预期响应（失败情况）**：
```json
{
  "acceptable": false,
  "reasonCode": "ST-400-002",
  "reasonMessage": "门店当前不可接单：门店暂不接单（原因码：ST-400-002）"
}
```

---

## 三、reasonCode 枚举表

### StoreErrorCode（门店侧错误码）

| reasonCode | 说明 | 使用场景 |
|-----------|------|---------|
| ST-404-001 | 门店不存在 | 门店不存在或已删除 |
| ST-400-001 | 门店未处于营业状态 | 门店状态不是 OPEN |
| ST-400-002 | 门店暂不接单 | openForOrders 开关为 false |
| ST-400-003 | 当前不在营业时间内 | 不在配置的营业时间内 |
| ST-400-004 | 门店未配置营业时间 | openingSchedule 为 null |
| ST-400-005 | 当前服务类型暂不支持 | capability 未启用 |
| ST-400-010 | 门店未绑定该渠道或渠道未启用 | 渠道未绑定或状态非 ACTIVE |
| OK | 允许接单 | 所有校验通过 |

### OrderErrorCode（订单侧错误码）

| reasonCode | 说明 | 使用场景 |
|-----------|------|---------|
| OR-400-003 | 门店当前不可接单 | 订单前置校验失败（包装门店侧错误） |

**注意**：订单侧抛出异常时，异常消息中包含门店侧的 reasonCode（格式：`门店当前不可接单：xxx（原因码：ST-xxx-xxx）`），便于前端和埋点使用。

---

## 四、最终验收命令

### 运行所有测试（推荐）
```bash
cd /Users/zhenpengmu/Desktop/code/project/bluecone-app
mvn -pl app-application -am clean test
```

### 仅运行订单模块测试
```bash
mvn -pl app-order -am test
```

### 仅运行门店模块测试
```bash
mvn -pl app-store -am test
```

### Testcontainers 运行条件说明

如果本地没有 Docker 环境，Testcontainers 相关测试会自动跳过（通过 `@Testcontainers(disabledWithoutDocker = true)` 注解控制）。

如果需要运行完整的集成测试，请确保：
1. Docker 已安装并运行
2. 测试会使用 Testcontainers 启动 MySQL 和 Redis 容器

---

## 五、功能说明

### StoreFacade.getOrderSnapshot(...)
- **返回字段**：
  - `storeId`、`tenantId`、`storeName`
  - `storeStatus`（status 字段，OPEN/CLOSED 等）
  - `openForOrders`（接单开关）
  - `todayOpeningHoursRange`（当天营业区间，格式：HH:mm-HH:mm）
  - `configVersion`（用于订单侧缓存/一致性判断）

### StoreFacade.checkOrderAcceptable(...)
- **返回结构**：
  - `acceptable`：boolean（是否可接单）
  - `reasonCode`：String（失败原因编码）
  - `reasonMessage`：String（失败原因中文提示）
  - `detail`：String（详细错误信息，包含额外上下文）
- **判断顺序**：
  1. 门店是否存在且归属 tenant
  2. 门店状态是否允许接单（OPEN）
  3. openForOrders 开关
  4. 渠道能力（若参数带 channelType）
  5. 营业时间/特殊日
  6. 能力校验（若参数带 capability）
- **不允许返回 null**：所有情况都有明确的返回值

### OrderPreCheckService.preCheck(...)
- **参数**：
  - `tenantId`：租户 ID（必填）
  - `storeId`：门店 ID（必填）
  - `channelType`：渠道类型（可选）
  - `now`：当前时间（可选，默认使用系统当前时间）
  - `cartSummary`：购物车摘要（预留扩展）
- **行为**：
  - 内部调用 `StoreFacade.checkOrderAcceptable`
  - 若不可接单：抛出 `BizException`，异常信息中携带门店返回的 reasonCode
  - 异常可被前端捕获，用于提示和埋点统计

---

## 六、模块边界说明

- **订单侧只依赖 app-store-api / StoreFacade**，不直接查询门店表
- **所有业务代码保持模块边界清晰**，通过 Facade 接口进行跨模块调用
- **中文注释要求**：所有 public 方法都有中文 JavaDoc；判断分支都有行内中文注释

---

## 七、后续扩展建议

1. **能力校验**：当前 `capability` 参数暂未在 `OrderPreCheckService` 中传递，后续可根据业务需要扩展
2. **购物车摘要**：`CartSummary` 接口已预留，可用于库存校验、限购校验等
3. **缓存优化**：门店快照可通过 `configVersion` 做版本化缓存，提高性能
4. **埋点统计**：reasonCode 可用于前端和埋点系统进行错误统计分析
