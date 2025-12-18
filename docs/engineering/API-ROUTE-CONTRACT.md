# API Route Contract（上下文路由契约）

本文档描述 bluecone-app 中的 **API 路由契约机制**，用于统一管理：

- API 侧（USER / MERCHANT / PLATFORM）的划分；
- 每条路由所需的上下文（Store / User / Product / Inventory）；
- 中间件链路的启停与执行顺序；
- 对运维接口（/ops/**、/actuator/**）的硬兜底保护。

目标：**防误伤、防缺上下文、提升配置一致性与可观测性**。

---

## 1. 模型与核心类

模块：`app-core`  
包：`com.bluecone.app.core.apicontract`

- `ApiSide`：API 所属侧：
  - `USER`：C 端用户 / 小程序 / H5；
  - `MERCHANT`：B 端商户控制台 / SaaS 后台；
  - `PLATFORM`：平台管理 / 运维 / admin / actuator。
- `ContextType`：上下文类型：
  - `STORE`、`USER`、`PRODUCT`、`INVENTORY`。
- `RouteContract`：

```java
public record RouteContract(
        ApiSide side,
        List<String> includePatterns,
        List<String> excludePatterns,
        EnumSet<ContextType> requiredContexts,
        EnumSet<ContextType> optionalContexts
) {}
```

- `ContractMatch`：一次匹配结果（side + contract + path）。
- `RouteContractMatcher`：路由匹配接口。

默认实现：`DefaultRouteContractMatcher`（`app-application/src/main/java/com/bluecone/app/apicontract/DefaultRouteContractMatcher.java`）  
基于 `AntPathMatcher`，按声明顺序匹配，**第一条命中即生效**。

---

## 2. 配置：ApiContractProperties

模块：`app-application`  
类：`com.bluecone.app.config.ApiContractProperties`

```yaml
bluecone:
  api:
    contract:
      enabled: true
      routes:
        - side: USER
          includePatterns: ["/api/mini/**"]
          excludePatterns: ["/api/mini/public/**"]
          requiredContexts: ["STORE","USER"]
          optionalContexts: ["PRODUCT","INVENTORY"]
```

- `enabled`：总开关；false 时所有契约/中间件不生效。
- `routes`：按顺序声明的路由契约，字段与 `RouteContract` 对齐。
  - `includePatterns`：Ant 风格路径，命中则进入当前契约候选；
  - `excludePatterns`：命中则强排除当前契约。

> 若用户未配置 `routes`（或为空）：
> - `/ops/**`、`/actuator/**` 仍由过滤器做 **硬排除兜底**；
> - 其他路径不会命中任何契约 → 不注入任何上下文。

示例 YAML：`docs/engineering/API-CONTRACT-EXAMPLE.yml`。

---

## 3. ApiContext 增强

类：`app-application/src/main/java/com/bluecone/app/gateway/ApiContext.java`

增加字段：

- `ApiSide apiSide`：当前请求所属侧；
- `EnumSet<ContextType> requiredContexts`：必须成功解析的上下文；
- `EnumSet<ContextType> optionalContexts`：可选上下文。

写入时机：

- `ApiContractFilter` 在匹配路由后，将 side/required/optional 写入 `HttpServletRequest` 属性；
- `ContextMiddlewareChainFilter` 在执行上下文中间件前，创建/更新 `ApiContext` 并回填上述字段；
- 对于非网关直出 Controller，可通过 `ApiContextHolder.get()` 获取同一份上下文。

未命中任何契约时：`ApiContext` 不会被创建/扩展。

---

## 4. 中间件治理链

### 4.1 统一接口：ContextMiddleware

包：`app-application/src/main/java/com/bluecone/app/apicontract/ContextMiddleware.java`

```java
public interface ContextMiddleware {
    ContextType type();
    int order();                 // 数字越小越先执行
    boolean supports(ApiSide side);
    void apply(HttpServletRequest req,
               HttpServletResponse resp,
               FilterChain chain,
               ApiContext ctx) throws Exception;
}
```

约定：

- `supports(ApiSide)` 用于声明中间件支持的 API 侧（如 Store/User 只支持 USER/MERCHANT）；
- `order()` 控制执行顺序，默认：
  - `STORE=100`，`USER=200`，`PRODUCT=300`，`INVENTORY=400`；
- `apply(...)` 内部负责解析上下文并注入 `ApiContext` / MDC，不直接调用 `chain.doFilter`。

现有中间件已适配该接口：

- `StoreMiddleware` → `ContextType.STORE`；
- `UserMiddleware` → `ContextType.USER`；
- `InventoryMiddleware` → `ContextType.INVENTORY`。

（若未来新增 ProductContext，只需实现新的 `ContextMiddleware` 并声明 `type=PRODUCT` 即可。）

### 4.2 契约过滤器：ApiContractFilter

类：`app-application/src/main/java/com/bluecone/app/apicontract/ApiContractFilter.java`

- 类型：`OncePerRequestFilter`，`@Order(0)`；
- 职责：
  - 读取 `HttpServletRequest.getRequestURI()`；
  - 调用 `RouteContractMatcher.match(path)`；
  - 若命中：
    - 将 `ApiSide`、`requiredContexts`、`optionalContexts` 写入 Request Attribute：
      - `bluecone.api.contract.side`
      - `bluecone.api.contract.requiredContexts`
      - `bluecone.api.contract.optionalContexts`
    - 写入 MDC：
      - `apiSide`：`USER/MERCHANT/PLATFORM`；
      - `apiContract`：`side:firstIncludePattern`（便于在日志中快速定位命中契约）。
  - 若未命中：仅输出 debug 日志，不做任何拦截/注入。
- 硬排除兜底（无视契约配置）：
  - `/ops/**`；
  - `/actuator/**`。

### 4.3 中间件编排器：ContextMiddlewareChainFilter

类：`app-application/src/main/java/com/bluecone/app/apicontract/ContextMiddlewareChainFilter.java`

- 类型：`OncePerRequestFilter`，`@Order(10)`；
- 前置依赖：仅在 `ApiContractFilter` 已写入 side/contexts 且 `enabled=true` 时生效；
- 逻辑：
  1) 从 Request Attribute 读取：
     - `ApiSide`；
     - `required/optional ContextType` 集合；
  2) 若二者均为空 → 直接放行；
  3) 创建/获取 `ApiContext`（通过 `ApiContextHolder`）并填充 side/required/optional；
  4) 收集所有 `ContextMiddleware` Bean，按 `order()` 升序排序；
  5) 对每个中间件：
     - 若 `type` 不在 required/optional 中 → 跳过；
     - 若 `!supports(side)` → 跳过；
     - 若在 `requiredContexts` 中：
       - 执行 `apply(...)`；
       - 若抛异常 → 立即返回统一错误响应（见下一节）；
     - 若在 `optionalContexts` 中：
       - 执行 `apply(...)`；
       - 若抛异常 → 记录 debug 日志，**忽略错误并继续**；
  6) 所有必选上下文成功后，调用 `chain.doFilter` 进入后续 Controller / 网关。

同样对 `/ops/**`、`/actuator/**` 做硬排除，确保不会误拦截运维/健康检查接口。

---

## 5. 错误码与响应规范（上下文解析失败）

仅针对于“上下文解析失败”（Store/User/Inventory 等），`ContextMiddlewareChainFilter` 会在 filter 层直接返回统一错误。

### 5.1 HTTP 状态码映射

- `PublicIdInvalidException` → `400 BAD_REQUEST`；
- `PublicIdNotFoundException` → `404 NOT_FOUND`；
- `BizException`：
  - 错误码包含 `-404-` → `404 NOT_FOUND`；
  - 包含 `-410-` → `410 GONE`（如 `STORE_DISABLED` / `USER_DELETED`）；
  - 包含 `-403-` → `403 FORBIDDEN`（如用户冻结）；
  - 包含 `-409-` → `409 CONFLICT`（如门店不可接单）；
  - `SYS-401-000` → `401 UNAUTHORIZED`；
  - 其他 → 默认为 `400 BAD_REQUEST`。

### 5.2 响应体格式

- 复用现有 `ApiResponse` 包装（`app-application/src/main/java/com/bluecone/app/api/ApiResponse.java`）；
- 结构：

```json
{
  "code": "ST-404-001",
  "message": "门店不存在",
  "data": null,
  "traceId": "...",
  "timestamp": "..."
}
```

> 这样可以与现有 `GlobalExceptionHandler` 保持一致的返回体结构，便于前端统一处理。

---

## 6. 路由扩展与维护

### 6.1 新增路由契约

当新增一类 API 路由（例如新的 Merchant 控制台模块），只需在配置中追加一条 route：

```yaml
bluecone:
  api:
    contract:
      routes:
        - side: MERCHANT
          includePatterns: ["/api/merchant/order/**"]
          excludePatterns: []
          requiredContexts: ["USER"]
          optionalContexts: ["STORE"]
```

注意：

- 契约按声明顺序匹配，**第一条命中即生效**；
- 若多个契约的 `includePatterns` 可能重叠，请保证顺序明确、不要产生歧义。

### 6.2 required vs optional

- `requiredContexts`：
  - 对应的中间件视为 **强依赖**；
  - 解析失败直接返回错误，不再进入 Controller；
  - 典型场景：门店必须存在且可接单、用户必须登录。
- `optionalContexts`：
  - 中间件按“尽力而为”模式执行；
  - 解析失败仅记录日志，不中断请求；
  - 典型场景：一些统计型上下文、可选的库存策略等。

推荐策略：

- USER 侧：
  - 门店 + 用户 通常是 `required`；
  - 商品/库存可视业务需求配置为 `optional`。
- MERCHANT 侧：
  - 用户（操作人）通常必需；
  - 门店根据是否允许跨门店视情况设为 `required` 或 `optional`。
- PLATFORM 侧：
  - 一般不注入业务上下文，`required/optional` 均为空。

---

## 7. 常见问题（FAQ）

### 7.1 为什么 /ops/** 永远不拦截？

- 运维接口（包括 `/ops/**` 与 `/actuator/**`）用于健康检查、调试与平台自运维；
- 无论 `bluecone.api.contract.routes` 如何配置，`ApiContractFilter` 与 `ContextMiddlewareChainFilter` 都会对这两类路径做 **硬排除**；
- 这样可以避免：
  - 误将健康检查视为业务请求并打满依赖缓存/数据库；
  - 由于上下文解析失败导致监控/探活接口不可用。

### 7.2 如何调试命中哪个 contract？

过滤器会在 MDC 中写入：

- `apiSide`：`USER` / `MERCHANT` / `PLATFORM`；
- `apiContract`：`<SIDE>:<firstIncludePattern>`，例如 `USER:/api/mini/**`。

在日志与 APM 中可直接筛选：

- 查看当前请求命中的 API 契约；
- 判断 required/optional 中是否包含预期的 ContextType；
- 结合现有 Store/User/Inventory 中间件日志，排查上下文解析链路。

---

## 8. 回归与测试建议

新增测试：位于 `app-application/src/test/java/com/bluecone/app/apicontract`（见代码）：

- `ApiContractHardExcludeTest`：
  - `/ops/api/summary`、`/actuator/health` 不应触发任何上下文中间件；
- `UserApiRequiresStoreTest`：
  - 构造 `/api/mini/order/confirm` 等用户侧路由，校验缺少/非法/不存在 storeId 时返回约定的错误码与状态码，并验证负缓存命中；
- `AdminApiNoContextInjectedTest`：
  - `/api/admin/**` 路径下 `ApiContext.requiredContexts` 为空；
  - Store/User 中间件不执行（通过 spy Bean 验证）。

在 CI 中建议保留这些测试，以防后续配置或中间件改动误伤运维接口或破坏上下文契约。

