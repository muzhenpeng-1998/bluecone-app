# Order + Payment + Store Context + Outbox + Webhook Design

## 1. 背景与目标（Background & Goals）

BlueCone 是一个多租户的餐饮 / 门店 SaaS。用户下单、支付、商户接单、对外回调必须在租户和门店之间高隔离地运行。本设计解决以下问题：

- **统一门店上下文**：由 `com.bluecone.app.application.gateway.ApiContext` 承载 tenantId / storeId / `StoreOrderSnapshot`，中间件负责注入并在每个请求内透传，避免散落的租户或门店解析逻辑。
- **可靠状态流转**：`DomainEvent` 与订单 / 支付更新在一个事务内写入 `bc_outbox_event`，借助 `com.bluecone.app.infra.event.outbox.TransactionalOutboxEventPublisher` 保证“关键状态变更”不会因异常丢失。
- **异步对外推送**：`OutboxEventConsumer` 将事件转交给 `WebhookOutboxEventHandler`，按照租户 Webhook 配置做 HTTP 推送，并使用 HMAC-SHA256 具备基础签名能力。
- **运营自服务**：`/api/admin/webhook-configs` 下的配置 API 让租户运维可以新增 / 编辑 / 启停 / 测试回调 URL，不再依赖 DB 手工修改。

核心目标：

1. **高隔离**：所有请求和事件都带 tenantId/storeId，`StoreContextProvider` 保障跨租户数据不可见。
2. **高可靠**：订单 / 支付状态与 Outbox 写入使用同一事务提交，失败立即回滚，避免“状态成功但事件没记录”。
3. **易扩展**：`OutboxEventHandler` 是一个列表，Webhook 只是第一个消费者，未来可以按需新增清分、通知、MQ 推送等 handler。
4. **易运维**：Webhook 配置支持手动启停、密钥生成、手动测试；Outbox 表可根据 status/retryCount 做可观测性分析。

## 2. 业务场景与关键事件（Use Cases & Key Events）

### 2.1 用户提交订单

- Controller 将 ApiContext 中的 tenantId/storeId/userId 组装进 `ConfirmOrderRequest`，调用 `com.bluecone.app.order.application.impl.OrderConfirmAppServiceImpl.confirmOrder`。
- 服务会校验 clientOrderNo、加载 `StoreOrderSnapshot`、生成订单与支付单。
- 成功后发布 `OrderSubmittedEvent`（`ORDER_SUBMITTED`），字段包含：
  - `tenantId`、`storeId`、`userId`
  - `orderId`、`payOrderId`
  - `totalAmount`（以分为单位）、`channel`
  - metadata 中附带 `aggregateType=ORDER`、`aggregateId=orderId`

### 2.2 支付成功

- 支付调试入口 `com.bluecone.app.application.payment.debug.PaymentDebugController` 触发 `com.bluecone.app.payment.simple.application.PaymentCommandAppService.markPaid`。
- 仓储 `SimplePaymentOrderRepository` 更新支付单状态并幂等返回。
- 发布 `PaymentSuccessEvent`（`PAYMENT_SUCCESS`），包含 tenant/store/user/order/payOrder/amount/channel/outTransactionNo，`aggregateType=PAYMENT`，`aggregateId=payOrderId`。

### 2.3 商户接单

- 商户后台调用 `com.bluecone.app.order.application.impl.MerchantOrderCommandAppServiceImpl.acceptOrder`。
- 校验订单归属与状态后执行 `Order.accept()`，只有从 `WAIT_ACCEPT` -> `ACCEPTED` 时才算一次有效接单。
- 发布 `OrderAcceptedEvent`（`ORDER_ACCEPTED`），包含 tenant/store/order/operator/payOrderId/totalAmount。

### 2.4 事件驱动的下游

当前事件的直接下游：

- `WebhookOutboxEventHandler`：将 ORDER/PAYMENT 聚合的事件推送到租户自建系统（如 OMS、ERP、日志服务）。
- `LoggingOutboxEventHandler`：输出结构化日志，方便 Cloud Log / APM 收集。

预留的下游用途：

- 内部清分 / 对账服务。
- 通知 / 营销模块（短信、订阅消息、站内信）。
- 如果未来引入 MQ，可在 OutboxHandler 中追加 MQ Producer。

## 3. 模块与责任划分（Module Responsibilities）

| 模块 | 责任 |
| --- | --- |
| `app-store` | 维护 `StoreRuntime` 领域模型、`StoreRepository.loadStoreRuntime`、`StoreOpenStateService` 营业判断、`StoreContextProvider` 对外提供 `StoreBaseView` 与 `StoreOrderSnapshot`。 |
| `app-application` | 处理 HTTP 请求，网关层通过 `TenantMiddleware` / `StoreMiddleware` 解析上下文并注入 `ApiContext`；暴露用户菜单 / 订单 / 支付调试 / 商户接单 API；承载 `OutboxEventConsumer`、`LoggingOutboxEventHandler`、`WebhookOutboxEventHandler` 以及 `/api/admin/webhook-configs` 管理端接口。 |
| `app-order` | 定义订单聚合、状态机 (`OrderStatus`) 以及 `OrderConfirmAppService`、`MerchantOrderCommandAppService`；负责 `OrderSubmittedEvent`、`OrderAcceptedEvent` 的生产。 |
| `app-payment` | 定义支付单模型与 `SimplePaymentOrderRepository`，`PaymentCommandAppService` 标记支付成功并产出 `PaymentSuccessEvent`。 |
| `app-infra` | 提供 Outbox 基础设施（`OutboxEventDO`、Mapper、Repository、TransactionalOutboxEventPublisher）、WebhookConfig DO/Mapper/Repository、HTTP Client 以及 `TenantContext` 等跨模块工具。 |

## 4. 门店上下文与营业态守卫设计（Store Context & OpenState）

### 4.1 ApiContext 统一上下文

`ApiContext`（`com.bluecone.app.application.gateway.ApiContext`）在请求进入时被创建并放入 `ApiContextHolder`。它包含：

- Trace 信息：traceId、requestTime。
- 身份信息：tenantId、userId、storeId、clientType。
- 门店快照：`StoreOrderSnapshot`（由 `StoreMiddleware` 设置）。
- 任意属性：`attributes` Map，供中间件与 Handler 传值。

业务代码通过 `RuntimeContextUtil.currentTenantId()` / `currentStoreId()` 获取上下文，避免散落在 controller 内的 header/param 解析。

### 4.2 请求链路与中间件

1. **TenantMiddleware**：读取 `ctx.getTenantId()`，如果 `ApiContract` 标记 `tenantRequired=true` 却缺失，则抛 `BusinessException`（`ErrorCode.PERMISSION_DENIED`），并在 finally 中清理 `TenantContext`。
2. **StoreMiddleware**：判断 URL 是否需要门店上下文（`/api/order/user/**` 或 `/api/product/user/**`）；解析 storeId（优先 header `X-Store-Id`，其次 query、path），若缺失抛 `BusinessException.of(ErrorCode.PARAM_MISSING, "storeId missing")`；调用 `StoreContextProvider.getOrderSnapshot`，将 snapshot 和 IDs 写入 `ApiContext` 以及 `HttpServletRequest` 属性，供后台和 `RequestContextHelper` 复用。

### 4.3 StoreRuntime 与营业态判断

- `StoreRepository.loadStoreRuntime(tenantId, storeId)` 返回 `StoreRuntime`，包含 `bizStatus`、`forceClosed`、takeout/pickup/dineIn 能力等。
- `StoreContextProviderImpl` 负责将 `StoreRuntime` 转换为 `StoreOrderSnapshot`，并调用 `StoreOpenStateService.isStoreOpenForOrder` 给出 `canAcceptOrder`。
- `StoreOpenStateServiceImpl` 的最小实现仅检查 `forceClosed` 与 `bizStatus == 1`。未来会接入营业时间 / 特殊日 / 渠道配置。

### 4.4 下单前的营业态守卫

Controller（例如用户下单 API）在调用订单应用服务前，需要读取 `ApiContext` 中的 snapshot。如果 `snapshot.getCanAcceptOrder()` 为 false，则抛 `BizException(StoreErrorCode.STORE_NOT_ACCEPTING_ORDERS)`，统一返回“STORE_CLOSED”语义。这样可以保证门店关闭时所有入口一致拒绝。

## 5. Outbox 事件模型与写入时机（Outbox Model & Write Points）

### 5.1 表结构

`bc_outbox_event`（老版本代码使用 `bc_outbox_message`，字段一致）由 `OutboxEventDO` 描述，主要字段：

- `id`：自增主键。
- `tenant_id`：关联租户，便于清理。
- `aggregate_type` / `aggregate_id`：标识事件来源的聚合。
- `event_type` + `event_body`：事件语义与 JSON 载荷。
- `status`：0=NEW、1=DONE、2=FAILED、9=IGNORED。
- `retry_count`：失败后自增，驱动退避。
- `available_at`：下一次可消费时间，失败后延迟写回。
- `created_at` / `updated_at`。

Repository（`com.bluecone.app.infra.event.outbox.OutboxEventRepositoryImpl`）负责 `save` / `findReadyEvents` / `markSent` / `markFailed`。

### 5.2 写入时机

1. **ORDER_SUBMITTED**：`OrderConfirmAppServiceImpl.confirmOrder` 成功创建订单和支付单后，实例化 `OrderSubmittedEvent` 并通过 `DomainEventPublisher` 写入 Outbox。
2. **PAYMENT_SUCCESS**：`PaymentCommandAppService.markPaid` 将支付单从 `WAIT_PAY` 更新为 `SUCCESS` 时，发布 `PaymentSuccessEvent`。
3. **ORDER_ACCEPTED**：`MerchantOrderCommandAppServiceImpl.acceptOrder` 首次将订单流转到 `ACCEPTED` 后发布 `OrderAcceptedEvent`。重复接单直接返回不再写事件。

### 5.3 幂等策略

- 支付链路：如果支付单已是 `SUCCESS`，`markPaid` 直接返回 DTO 不产生新事件。
- 接单链路：`Order.accept()` 如果已经是 `ACCEPTED`，不抛错但也不会再生成事件。
- 订单提交流程：`clientOrderNo` 保证幂等，重复请求直接返回已有订单，从而不会重复写 `ORDER_SUBMITTED`。

若未来需要“强制重复投递 + 幂等消费”，可以在 Outbox 表增加 `event_key` 唯一约束并在 Handler 侧实现去重。

## 6. Outbox 消费与 Handler 机制（Outbox Consumer & Handlers）

- `OutboxEventConsumer`（`app-application`，dev/test profile）每 3 秒运行一次，批量（100 条）查询 `status=0` 且 `available_at <= now` 的事件，按 `created_at` 排序处理。
- 对每条事件调用所有 `OutboxEventHandler`。Handler 通过 `supports` 判断自己是否关心该事件：
  - 所有 Handler 都成功执行则调用 `markSent` 将 status=1。
  - 任意 Handler 抛异常则捕获，打印日志，并计算 `nextRetryCount = retryCount+1`、`nextAvailableAt = now + nextRetryCount 分钟`，写回 `status=2`。
- 当前内置 Handler：
  1. `LoggingOutboxEventHandler`：简单输出结构化日志，覆盖所有事件。
  2. `WebhookOutboxEventHandler`：仅处理 `aggregateType` 为 `ORDER` 或 `PAYMENT` 的事件，拉取租户配置推送 HTTP。
- 扩展点：在 Spring 容器中新增实现 `OutboxEventHandler` 的 Bean 即可，典型用途包括清分、通知、MQ、数据湖同步等。

## 7. Webhook 配置与推送流程（Webhook Config & Dispatch Flow）

### 7.1 配置表结构

`bc_webhook_config`（`com.bluecone.app.infra.webhook.entity.WebhookConfigDO`）字段：

- `id`（自增）、`tenant_id`、`event_type`。
- `target_url`：HTTP POST 地址，仅允许 http/https。
- `secret`：HMAC-SHA256 密钥，可为空。
- `enabled`：1=启用，0=禁用。
- `description`、`created_at`、`updated_at`。

约定：每个租户 + 事件类型最多一条配置，逻辑上通过 create 业务保证。Repository (`WebhookConfigRepository`) 支持 `findEnabledWebhook`、`listByTenant`、`findById`、`save`、`update`、`deleteById`。

### 7.2 Outbox Handler 推送流程

`WebhookOutboxEventHandler` 执行流程：

1. 读取 `OutboxEventDO` 中的 `tenantId`、`eventType`。
2. 调用 `WebhookConfigRepository.findEnabledWebhook(tenantId, eventType)`。
   - 若无配置或 enabled=0，直接返回，Outbox 认为该事件已成功处理，不重试。
3. 构造 `WebhookPayload`：
   - 基本字段：tenantId、aggregateType、aggregateId、eventType、eventBodyRaw（原始 JSON）、occurredAt。
4. 将 payload 序列化为 JSON，若配置存在 secret，则使用 `HmacSHA256` 计算签名，并在请求头 `X-Webhook-Signature` 中携带。
5. 通过 `RestTemplate`（dev/test 环境）或 `WebClient`（管理端测试接口）发送 POST。
   - HTTP 非 2xx 会抛异常，交给 Outbox 重试。

### 7.3 管理端配置 API

`WebhookConfigAppService` + `WebhookConfigAdminController` 暴露 `/api/admin/webhook-configs`：

- `GET /api/admin/webhook-configs`：`listByCurrentTenant()`，使用 `RuntimeContextUtil.currentTenantId()` 保证隔离。
- `POST`：`create(WebhookConfigCreateRequest)`。如果未传 secret，`generateSecret()` 自动生成。
- `PUT /{id}`：`update(WebhookConfigUpdateRequest)`。允许更新 targetUrl/secret/enabled/description，空字符串 secret 代表清空。
- `DELETE /{id}`：物理删除配置。
- `POST /{id}/test`：`WebhookConfigTestRequest` 可附带 `testData`。服务端直接构造一条 `WEBHOOK_TEST` payload，使用 `WebClient` 发出请求，不写 Outbox，不影响正式流程。
- 所有接口都依赖 ApiContext 注入的 tenantId；如果 context 缺失，抛 `BizErrorCode.CONTEXT_MISSING`。

## 8. 错误码与监控（Error Codes & Observability）

### 8.1 关键错误码 / 提示

| 语义 | 抛出位置 | 代码 / 文本 |
| --- | --- | --- |
| `CONTEXT_MISSING` | `WebhookConfigAppService` | `BizErrorCode.CONTEXT_MISSING` |
| `STORE_ID_MISSING` | `StoreMiddleware` | `BusinessException.of(ErrorCode.PARAM_MISSING, "storeId missing")` |
| `STORE_CLOSED` | `StoreOpenStateServiceImpl` | `StoreErrorCode.STORE_NOT_ACCEPTING_ORDERS` / `STORE_STATUS_NOT_OPEN` 等 |
| `ORDER_STATUS_NOT_ALLOW_ACCEPT` | `Order.accept()` | `BizException(CommonErrorCode.BAD_REQUEST, "ORDER_STATUS_NOT_ALLOW_ACCEPT")` |
| `ORDER_STORE_MISMATCH` | `MerchantOrderCommandAppServiceImpl.acceptOrder` | `BizException(CommonErrorCode.BAD_REQUEST, "订单不属于当前门店")` |
| `PAY_ORDER_NOT_FOUND` | `PaymentCommandAppService.markPaid` | `IllegalStateException("支付单不存在")` |
| `PAY_ORDER_STATUS_INVALID` | `PaymentCommandAppService.markPaid` | `IllegalStateException("支付单状态不允许标记成功")` |

错误统一经过 `com.bluecone.app.exception.GlobalExceptionHandler` 转换成 `ApiResponse.fail(code, message)`，便于前端识别。

### 8.2 日志与监控建议

- 在 `StoreMiddleware`、`OrderConfirmAppService`, `PaymentCommandAppService`, `MerchantOrderCommandAppService`, `OutboxEventConsumer`, `WebhookOutboxEventHandler` 等 class 已写入 `log.info`/`log.error`，字段包含 tenantId/storeId/orderId/payOrderId/eventType。建议在日志收集系统配置这些字段为结构化键，方便查询。
- 监控 Outbox backlog：定期查询 `bc_outbox_event` 中 `status=0` 或 `status=2` 的数量，异常飙升时报警。
- 监控 Webhook 成功率：统计 `WebhookOutboxEventHandler` 中 HTTP 状态或管理端测试接口的结果，必要时把响应时间、失败堆栈写入监控。
- 结合 `retry_count`、`available_at` 可以构建一个“死信”仪表盘，超出阈值的事件需要人工介入。

## 9. 未来演进方向与已知限制（Future Work & Limitations）

### 9.1 已知限制

1. **Outbox 消费单实例**：`OutboxEventConsumer` 当前仅在 dev/test profile 启动，且没有针对多实例竞争的锁或版本控制，部署多副本时可能重复消费。
2. **Webhook 配置粒度有限**：每租户 + eventType 只允许一个 URL，无法针对门店或渠道做更细分的推送。
3. **营业态逻辑简单**：`StoreOpenStateServiceImpl.isStoreOpenForOrder` 只检查 `forceClosed` 与 `bizStatus`，没有考虑营业时间、特殊日、渠道状态。
4. **事件消费幂等**：Outbox Handler 只保证“至少一次”，没有提供 handler 级别的幂等框架，下游需要自己确保幂等。
5. **签名策略简单**：HMAC-SHA256 直接对 JSON 字符串签名，未支持时间戳 / nonce，碰到中间人攻击需做加强。

### 9.2 演进方向

1. **营业时间治理**：引入 `bc_store_opening_hours`、`bc_store_special_day` 等配置，完善 `StoreOpenStateService` 判定。
2. **多实例安全消费**：在 `OutboxEventRepository.findReadyEvents` 中增加例如 `status=0` 且 `version` 比较的乐观锁，或者利用数据库 `SELECT ... FOR UPDATE SKIP LOCKED`。
3. **Webhook 重试策略**：支持最大重试次数、指数退避、死信队列，针对 4xx/5xx 区分处理。
4. **事件 Schema 版本化**：在 payload header 中记录 `schemaVersion`，方便后续扩展字段并兼容老消费者。
5. **配置粒度扩展**：允许同一租户针对 `eventType + storeId` 或渠道配置不同 URL，并提供批量导出 / 导入工具。

这份文档结合现有实现给出了端到端的链路说明。接手项目的同学只需对照此文档，即可理解：小程序下单如何经过门店上下文守卫，事件如何写入 Outbox，再如何被 Webhook 推送以及如何在管理端配置回调。***
