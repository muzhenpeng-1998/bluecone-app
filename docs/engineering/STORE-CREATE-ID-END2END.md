# 门店创建 ID 体系（internal_id / public_id / store_no）全链路说明

本说明以 app-store 的“创建门店”用例为样板，串起：

- internal_id：Ulid128 → MySQL `BINARY(16)`；
- public_id：`sto_{ulid}` → `VARCHAR(40)`；
- store_no：Snowflake long → `BIGINT UNSIGNED`；
- 幂等创建：`IdempotentCreateTemplate`；
- 事件发布：Transactional Outbox（`DomainEventPublisher`）；
- 消费幂等：`EventHandlerTemplate` + `EventDedupRepository`。

---

## 1. 表结构与 DO 映射

### 1.1 主表：bc_store

迁移文件：`docs/sql/migration/V20251213__add_internal_public_id__bc_store.sql`  
补充 store_no：`docs/sql/migration/V20251213__add_store_no__bc_store.sql`

核心字段：

- `internal_id BINARY(16) NULL COMMENT '内部主键 ULID128'`
- `public_id  VARCHAR(40) NULL COMMENT '对外ID prefix_ulid'`
- `store_no   BIGINT UNSIGNED NULL COMMENT '门店数字编号（snowflake long）'`
- 唯一约束：
  - `uk_bc_store_internal_id (internal_id)`
  - `uk_bc_store_tenant_public_id (tenant_id, public_id)`
  - `uk_bc_store_store_no (store_no)`

迁移步骤建议（生产环境）：

1. 加列：新增 `internal_id/public_id/store_no` 及唯一索引；
2. 回填：为历史门店批量生成：
   - `internal_id`：遍历门店表，使用脚本生成 Ulid128 写入；
   - `public_id`：基于 `ResourceType.STORE` + `internal_id` 生成 `sto_{ulid}`；
   - `store_no`：调用 Snowflake long 生成，确保全局唯一；
3. 双写：应用层写入/更新门店时，同时维护三类 ID 字段；
4. 切换读路径：读侧逐步从 `id` 切到 `public_id` / `store_no` 查询。

### 1.2 DO 映射

`app-store/src/main/java/com/bluecone/app/store/dao/entity/BcStore.java`

- `private Ulid128 internalId;   // internal_id BINARY(16)`
- `private String publicId;      // public_id VARCHAR(40)`
- `private Long storeNo;         // store_no BIGINT UNSIGNED`

Ulid128 ↔ BINARY(16) 通过全局 TypeHandler 自动完成：

- 自动配置：`app-id/src/main/java/com/bluecone/app/id/autoconfigure/IdMybatisAutoConfiguration.java`
- TypeHandler：`Ulid128BinaryTypeHandler`

无需在 `BcStore` 上显式声明 `@TableField(typeHandler=...)`。

### 1.3 Read Model：bc_store_read_model

DDL：`docs/sql/bc_store_read_model.sql`

用于演示消费端幂等的只读快照表：

- `store_internal_id BINARY(16) PK`
- `public_id` / `store_no` / `tenant_id` / `store_name` / `updated_at`

DO：`app-store/src/main/java/com/bluecone/app/store/dao/entity/BcStoreReadModel.java`  
Mapper：`app-store/src/main/java/com/bluecone/app/store/dao/mapper/BcStoreReadModelMapper.java`

---

## 2. 创建门店写链路（Controller → Template → Service）

### 2.1 Controller：AdminStoreController#create

文件：`app-application/src/main/java/com/bluecone/app/controller/store/AdminStoreController.java`

入口方法：

```java
@PostMapping
public ApiResponse<CreateStoreResponse> create(
        @RequestHeader(value = "Idempotency-Key", required = false) String idemKey,
        @RequestHeader(value = "X-Idempotency-Key", required = false) String idemKeyAlt,
        @RequestBody CreateStoreCommand command)
```

步骤：

1. 租户解析：
   - 通过 `TenantContext.getTenantId()` 获取当前租户；
   - 使用 `requireTenantId()` 统一做非空与数字校验；
   - 将 `tenantId` 写入 `CreateStoreCommand`。
2. 幂等键解析：
   - 优先使用 `Idempotency-Key`，其次 `X-Idempotency-Key`；
   - 若均缺失，退化为调用 `IdService.nextUlidString()` 生成一个（仅作为兜底，不推荐生产依赖）。
3. 请求摘要 `requestHash`：
   - 使用 `tenantId + name + shortName + cityCode + storeCode` 拼接字符串；
   - 计算 SHA-256 → 64 位 HEX 字符串。
4. 构造 `CreateRequest`：

```java
CreateRequest request = new CreateRequest(
        tenantId,
        "STORE_CREATE",
        ResourceType.STORE.prefix(), // "sto"
        resolvedIdemKey,
        requestHash,
        Duration.ofHours(24),
        Duration.ofSeconds(30),
        TxMode.REQUIRES_NEW,
        false,
        null
);
```

5. 调用 `IdempotentCreateTemplate`：

```java
IdempotentCreateResult<String> result = idempotentCreateTemplate.create(
        request,
        (internalId, publicId) -> {
            Long storeNo = idService.nextLongId(); // Snowflake long
            storeCommandService.createStoreWithPreallocatedIds(command, internalId, publicId, storeNo);

            StoreCreatedEvent event = new StoreCreatedEvent(
                    internalId,
                    publicId,
                    storeNo,
                    command.getName(),
                    buildEventMetadata(tenantId)
            );
            return new CreateWorkWithEventsResult<>(publicId, java.util.List.of(event));
        }
);
```

6. 结果处理：
   - `result.inProgress()==true`：抛 `BizException(CommonErrorCode.CONFLICT, "创建门店请求正在处理，请稍后重试")`；
   - 否则返回 `result.publicId()` 作为响应体的 `storePublicId`。

### 2.2 应用服务：StoreCommandService

文件：`app-store/src/main/java/com/bluecone/app/store/application/service/StoreCommandService.java`

幂等模板使用的方法：

```java
@Transactional(rollbackFor = Exception.class)
public void createStoreWithPreallocatedIds(CreateStoreCommand command,
                                           Ulid128 internalId,
                                           String publicId,
                                           Long storeNo)
```

核心逻辑：

- 校验 `tenantId/name/industryType` 非空；
- 门店编码冲突校验：
  - 若 `storeCode` 为空：使用 `publicId` 作为 `storeCode`；
  - 若非空：在 `(tenantId, storeCode, is_deleted=false)` 维度做唯一性校验；
- 构造 `BcStore`：

```java
BcStore entity = new BcStore();
entity.setTenantId(tenantId);
entity.setInternalId(internalId);
entity.setPublicId(publicId);
entity.setStoreNo(storeNo);
entity.setStoreCode(storeCode);
entity.setName(command.getName());
entity.setShortName(command.getShortName());
entity.setIndustryType(command.getIndustryType());
entity.setCityCode(command.getCityCode());
entity.setStatus("OPEN");
entity.setOpenForOrders(Boolean.TRUE.equals(command.getOpenForOrders()));
entity.setConfigVersion(1L);
entity.setCreatedAt(LocalDateTime.now());
entity.setIsDeleted(false);
bcStoreService.save(entity);
```

- 触发配置变更通知：

```java
Long storeId = entity.getId();
storeConfigChangeService.onStoreConfigChanged(tenantId, storeId, entity.getConfigVersion());
```

> 非幂等场景仍可通过 `createStore(CreateStoreCommand)` 调用，
> 该方法内部会自行生成 internalId/publicId/storeNo 后委托给 `createStoreWithPreallocatedIds`。

---

## 3. 领域事件与 Outbox 发布

### 3.1 StoreCreatedEvent

文件：`app-store/src/main/java/com/bluecone/app/store/event/StoreCreatedEvent.java`

事件名常量：`StoreEventNames.STORE_CREATED = "store.created"`。

载荷字段：

- `Ulid128 storeInternalId`：门店内部主键；
- `String storePublicId`：对外 ID，形如 `sto_{ulid}`；
- `Long storeNo`：Snowflake long 编号；
- `String storeName`：门店名称。

构造方式（创建新事件）：

```java
new StoreCreatedEvent(
        internalId,
        publicId,
        storeNo,
        command.getName(),
        EventMetadata.of(Map.of("tenantId", tenantId.toString(), "traceId", traceId))
)
```

### 3.2 Outbox 发布

`IdempotentCreateTemplate` 在事务内执行：

1. 调用 `CreateWorkWithEvents`，获取 `CreateWorkWithEventsResult`；
2. 将 `publicId` 写入幂等记录（`bc_idempotency_record.result_ref`）；
3. 通过 `DomainEventPublisher.publish(event)` 将 `StoreCreatedEvent` 写入 Outbox：
   - 生产实现：`TransactionalOutboxEventPublisher`；
   - 表：`bc_outbox_message`。

事务提交后，由调度器 `OutboxDispatchService` 异步分发事件：

- 路由：`OutboxEventRouter` 基于 `EventHandler` 泛型类型发现 handler；
- handler：见下文 `StoreCreatedReadModelHandler`。

---

## 4. 消费端幂等：StoreCreatedReadModelHandler

### 4.1 事件处理器

文件：`app-store/src/main/java/com/bluecone/app/store/handler/StoreCreatedReadModelHandler.java`

标记与接口：

- `@EventHandlerComponent`
- `implements EventHandler<StoreCreatedEvent>`（DomainEvent handler）

内部通过 `EventHandlerTemplate` 实现消费幂等：

```java
Ulid128 eventUlid = toUlid128(event.getEventId());
long tenantId = event.getTenantId() != null ? event.getTenantId() : 0L;

EventEnvelope envelope = new EventEnvelope(
        tenantId,
        eventUlid,
        event.getEventType(),
        serializePayload(event),
        null,
        event.getOccurredAt()
);

ConsumeOptions options = new ConsumeOptions(
        Duration.ofSeconds(30),
        false,
        Duration.ZERO,
        20,
        Duration.ofSeconds(1),
        Duration.ofMinutes(5)
);

handlerTemplate.consume("STORE_READMODEL", envelope, env -> {
    BcStoreReadModel model = new BcStoreReadModel();
    model.setStoreInternalId(event.getStoreInternalId());
    model.setPublicId(event.getStorePublicId());
    model.setStoreNo(event.getStoreNo());
    model.setTenantId(event.getTenantId());
    model.setStoreName(event.getStoreName());
    model.setUpdatedAt(LocalDateTime.ofInstant(event.getOccurredAt(), ZoneOffset.UTC));
    readModelMapper.insert(model);
}, options);
```

要点：

- `consumerGroup = "STORE_READMODEL"`；
- `EventDedupRepository` 使用 `uk(consumer_group, event_id)` 保证同一事件只处理一次；
- `eventId` 从 `DomainEvent.eventId`（UUID 字符串）解析为 `Ulid128(msb, lsb)` 存入 `bc_event_consume_record.event_id`。

### 4.2 Read Model 表

`bc_store_read_model` 中记录了一份冗余快照，可用于：

- 快速按 `store_internal_id` 或 `public_id/store_no` 查询门店信息；
- 演示 Outbox + 幂等消费的完整链路；
- 后续扩展为搜索索引同步等场景。

---

## 5. 测试与验证

### 5.1 创建写入字段测试

`app-store/src/test/java/com/bluecone/app/store/application/service/StoreCommandServiceCreateTest.java`

- 使用 Mockito mock `IBcStoreService`，拦截 `save(BcStore)`；
- 调用 `createStoreWithPreallocatedIds(...)`；
- 断言：
  - `entity.getInternalId()` 与传入 `Ulid128` 一致；
  - `entity.getPublicId()` 为传入的 `publicId`；
  - `entity.getStoreNo()` 为传入的 `storeNo`。

### 5.2 幂等与重放

通用行为由已有单元测试覆盖：

- `DefaultIdempotentCreateTemplateTest`：
  - `firstCreateShouldSucceedAndMarkSuccess`；
  - `sameKeyDifferentHashShouldConflict` 等；
- `DefaultEventHandlerTemplateTest`：
  - 覆盖 `REPLAY_SUCCEEDED`、`IN_PROGRESS` 等场景。

---

## 6. 生产配置建议：store_no 的 nodeId

`store_no` 基于 Snowflake long 生成，正确配置 nodeId 是避免撞号的关键：

- 启用 long ID：

```yaml
bluecone:
  id:
    long:
      enabled: true
      node-id: 1   # 0..1023，每个实例唯一
```

- 详见：`docs/engineering/LONG-ID-NODEID.md`：
  - 环境变量 `BLUECONE_NODE_ID` / `BLUECONE_ID_NODE_ID`；
  - 容器/StatefulSet ordinal 映射；
  - 微信云托管环境下通过实例标识映射 nodeId 的建议。

**总结**：只要 `bluecone.id.long.enabled=true` 且为每个实例配置唯一 nodeId，
`createStore` 就能稳定生成：

- 内部主键：`internal_id`（Ulid128/BINARY(16)）；
- 对外 ID：`public_id`（`sto_{ulid}`）；
- 数字门店编号：`store_no`（Snowflake long），并通过 Outbox + EventHandlerTemplate 实现端到端的事件驱动与消费幂等。 

