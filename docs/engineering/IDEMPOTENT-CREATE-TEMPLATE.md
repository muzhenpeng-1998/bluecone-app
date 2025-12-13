# 幂等创建模板（IdempotentCreateTemplate）接入指南

本说明文档基于幂等基础设施（IdempotencyTemplate/IdempotencyRepository），
进一步抽象出统一的“幂等创建模板”——IdempotentCreateTemplate，用于为所有 create 接口提供统一范式：

- 统一从 app-id 生成 internal_id（Ulid128）与 public_id；
- 统一幂等（占位/冲突检测/重放/并发控制）；
- 统一事务策略，避免“返回成功但事务回滚”的幽灵 public_id；
- 统一对外行为：public_id 为唯一对外标识。

---

## 1. 核心接口与模型

### 1.1 IdempotentCreateTemplate

位于 `com.bluecone.app.core.create.api`：

```java
public interface IdempotentCreateTemplate {

    <T> IdempotentCreateResult<T> create(CreateRequest request, CreateWork<T> work);
}
```

### 1.2 CreateRequest

```java
public record CreateRequest(
        long tenantId,
        String bizType,        // ORDER_CREATE / STORE_CREATE ...
        String resourceType,   // ord / sto / ten / usr / pay ...
        String idemKey,
        String requestHash,    // SHA-256 HEX(64)
        Duration ttl,
        Duration lockTtl,
        TxMode txMode,
        boolean waitForCompletion,
        Duration waitMax
) {}
```

约束与默认值：

- `tenantId > 0`；
- `bizType` 非空，建议大写下划线（如 `ORDER_CREATE`）；
- `resourceType` 非空，且满足 `[a-z0-9]{2,10}`，用于 public_id 前缀（如 `ord`、`sto`、`ten`、`usr`）；
- `idemKey` 非空（通常来自 HTTP Header: `Idempotency-Key`）；
- `requestHash` 必须为 64 位十六进制字符串（对关键字段 canonical JSON -> SHA-256）；
- `ttl` 默认 24h，`lockTtl` 默认 30s；
- `txMode` 默认 `REQUIRES_NEW`；
- `waitForCompletion` 默认 `false`。

### 1.3 TxMode（事务模式）

```java
public enum TxMode {
    REQUIRES_NEW,
    REQUIRED,
    AWARE_ONLY
}
```

- `REQUIRES_NEW`（默认）：模板内部使用新事务提交创建操作与幂等成功标记，返回前已落库，避免幽灵 public_id。
- `REQUIRED`：加入当前事务，与外层共享事务边界（不推荐，除非你非常清楚外层事务控制）。
- `AWARE_ONLY`：不强制开启事务，只做逻辑执行与幂等标记（风险最大，仅用于明确无事务需求的场景）。

### 1.4 结果与回调

```java
public record IdempotentCreateResult<T>(
        boolean replayed,
        boolean inProgress,
        String publicId,
        Ulid128 internalId,
        T value
) {}

@FunctionalInterface
public interface CreateWork<T> {
    T execute(Ulid128 internalId, String publicId);
}
```

说明：

- `replayed`：true 表示本次是重放历史结果（work 未被再次执行）；
- `inProgress`：true 表示存在并发请求正在执行（且当前策略未等待完成）；
- `publicId`：对外唯一标识；模板统一生成；
- `internalId`：内部 ULID128，用于业务表主键（BINARY(16)）；
- `value`：业务返回值（通常是 DTO，重放时可根据 `result_json` 重建或置为 null）。

---

## 2. 标准用法（创建接口伪代码）

### 2.1 构造 CreateRequest

```java
CreateRequest req = new CreateRequest(
        tenantId,
        "ORDER_CREATE",
        "ord",
        idemKey,            // 来自 HTTP Header: Idempotency-Key
        requestHash,        // canonical JSON -> SHA-256 HEX(64)
        Duration.ofHours(24),
        Duration.ofSeconds(30),
        TxMode.REQUIRES_NEW,
        false,              // waitForCompletion
        null                // waitMax
);
```

### 2.2 使用模板执行业务

```java
IdempotentCreateResult<OrderCreatedDTO> result =
        idempotentCreateTemplate.create(req, (internalId, publicId) -> {
            // 1) 使用 internalId 作为业务主键（BINARY(16)）
            // 2) 保存 publicId 到业务表（VARCHAR(40) UNIQUE）
            orderRepository.insert(internalId, publicId, tenantId, ...);

            // 3) 发布领域事件 / Outbox 消息（建议 after-commit）
            // eventPublisher.publish(new OrderCreatedEvent(publicId, ...));

            return new OrderCreatedDTO(publicId);
        });

if (result.inProgress()) {
    // 可返回 “处理中” 或在调用侧进行重试
}

return result.publicId();
```

在 `TxMode.REQUIRES_NEW` 下：

- 模板会：
  1. 通过 `IdempotencyRepository.tryAcquire` 占位/冲突检测；
  2. 生成 `internalId = idService.nextUlid()` 与 `publicId = publicIdCodec.encode(resourceType, internalId)`；
  3. 在新事务中执行 `work` 并调用 `markSuccess`（`result_ref=publicId`）；
  4. 事务提交成功后才返回给调用方。

这样可确保：

> “只要返回 public_id 成功，就意味着 internal_id 主键与幂等记录一定已经提交到数据库。”

---

## 3. 与幂等表字段映射约定

在 Step 9 中，我们通过表 `bc_idempotency_record` 记录幂等信息。本模板对结果字段约定如下：

- `result_ref`：必须写入 public_id（如 `ord_01J...`），便于重放与排查；
- `result_json`：
  - 若业务返回值可安全 JSON 序列化且长度 <= 4096，则写入完整 JSON；
  - 若超过 4096，则写入截断版或为空（当前实现为截断），以免撑爆行大小；
  - 重放时优先使用 public_id+业务表查询，result_json 主要用于调试/审计。

对于重放场景（`REPLAY_SUCCEEDED`）：

- 模板会从 `result_ref` 中解码出内部 ULID：
  - `DecodedPublicId decoded = publicIdCodec.decode(result_ref)`；
  - 校验 `decoded.type()` == `resourceType`；
  - 得到 `internalId = decoded.id()`；
- `value` 若 `result_json` 存在则可反序列化；否则通常为 null，由上层自行决定是否补查。

---

## 4. 并发与等待策略

### 4.1 不等待（默认）

- `waitForCompletion = false` 时：
  - 若 `tryAcquire` 返回 `IN_PROGRESS` / `RETRYABLE`，模板立即返回：

```java
new IdempotentCreateResult<>(false, true, null, null, null);
```

- 上层可以返回“处理中”响应，或在客户端做退避重试。

### 4.2 等待完成

- `waitForCompletion = true` 且 `waitMax > 0` 时：
  - 模板会在 `waitMax` 窗口内轮询 `IdempotencyRepository.find(...)`：
    - 若状态变为 `SUCCEEDED` => 返回 `replayed=true` 的 `IdempotentCreateResult`；
    - 若状态为 `FAILED` => 抛 `IdempotencyInProgressException`（提示上游走错误/重试策略）；
    - 若始终未完成 => 返回 `inProgress=true`。

---

## 5. TxMode 选择建议

### 5.1 REQUIRES_NEW（强烈推荐）

- 特点：
  - 模板在新事务中执行业务写入与幂等成功标记；
  - 返回给调用方时，DB 中已经存在 internal_id/public_id 与幂等记录；
  - 即使外层还有其他事务，也不会影响幂等记录（因为是新事务）。
- 适用场景：
  - 所有对外 `create` 接口（订单创建、门店创建、租户创建等）。

### 5.2 REQUIRED

- 加入当前事务，幂等记录与业务写入同一事务；
- 外层回滚时，幂等记录也会回滚，不会出现“幂等标记成功但业务回滚”的问题；
- 但可能导致外层事务未提交前，重复请求仍然看到 `PROCESSING` 状态，需要依赖 `lock_until` 过期机制来重试。

### 5.3 AWARE_ONLY（慎用）

- 不显式开启事务，仅调用 `work.execute` 与 `markSuccess`；
- 适用于你明确不需要事务的简单场景；
- 必须由业务保证自身幂等性与错误处理，不推荐在重要写路径使用。

---

## 6. 失败与重试策略

### 6.1 supplier 内部抛异常

- 模板会：
  - 在独立小事务中调用 `markFailed(...)` 记录状态与错误信息；
  - 然后将原始异常抛出；
- 后续同 key 请求：
  - 由于 `status=FAILED`，当前实现会在 `tryAcquire` 中返回 `IN_PROGRESS` 或通过租约与过期逻辑允许重新执行；
  - 建议业务在外层根据异常类型与业务语义决定是否重试。

### 6.2 PROCESSING 锁超时

- 当执行者在锁超时前挂掉：
  - 其他实例在 `lock_until` 之后可通过 `tryAcquire` 抢占执行权；
  - 模板会重新生成 internal_id/public_id 并执行创建逻辑；
  - 若希望公用同一个 public_id，可考虑将 public_id 生成与幂等记录创建更紧密绑定（高级用法，可按需扩展）。

---

## 7. 对外 API 规范

统一对外约定：

- `create` 类接口：
  - 请求中携带 `Idempotency-Key` 头与业务请求体；
  - 响应仅返回 `public_id`（以及其他非敏感字段），不返回 internal_id；
  - 使用 `IdempotentCreateTemplate` 保证幂等与 ID 生成。

示例：

```java
public record CreateOrderResponse(String publicId) {}

@PostMapping("/orders")
public CreateOrderResponse createOrder(@RequestHeader("Idempotency-Key") String idemKey,
                                       @RequestBody CreateOrderCommand cmd) {
    String requestHash = hashOfCommand(cmd);
    CreateRequest req = new CreateRequest(
            cmd.tenantId(),
            "ORDER_CREATE",
            "ord",
            idemKey,
            requestHash,
            Duration.ofHours(24),
            Duration.ofSeconds(30),
            TxMode.REQUIRES_NEW,
            false,
            null
    );
    IdempotentCreateResult<OrderCreatedDTO> result =
            idempotentCreateTemplate.create(req, (internalId, publicId) -> {
                // 真实业务：insert + event
                return new OrderCreatedDTO(publicId);
            });

    return new CreateOrderResponse(result.publicId());
}
```

---

## 8. 配置示例

自动装配条件（在 app-application 中）：

- 存在：
  - `IdService`（app-id）
  - `PublicIdCodec`（app-id）
  - `IdempotencyRepository`（app-infra）
  - `PlatformTransactionManager`
- 且：

```yaml
bluecone:
  create:
    enabled: true
```

创建模板 Bean：

```java
@Bean
@ConditionalOnBean({IdService.class, PublicIdCodec.class, IdempotencyRepository.class, PlatformTransactionManager.class})
@ConditionalOnProperty(prefix = "bluecone.create", name = "enabled", havingValue = "true", matchIfMissing = true)
public IdempotentCreateTemplate idempotentCreateTemplate(...) {
    return new DefaultIdempotentCreateTemplate(..., domainEventPublisher);
}
```

以上即为幂等创建模板的标准接入方式，后续所有 create 接口（order/store/tenant/user 等）都可以一键套用。 
