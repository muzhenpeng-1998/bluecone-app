# 幂等模板（IdempotencyTemplate）接入指南

本说明文档用于统一 BlueCone 在多模块（order / store / tenant / user 等）中的幂等实现方式，
将幂等逻辑沉淀为 app-core 的 `IdempotencyTemplate` + app-infra 的存储/锁实现，避免在业务层重复“手撸幂等”。

## 1. 幂等键规范

### 1.1 Idempotency-Key

- 推荐在 HTTP Header 中使用 `Idempotency-Key` 承载幂等键；
- 服务端幂等唯一键由以下维度组合：
  - `tenant_id`：多租户隔离维度；
  - `biz_type`：业务类型（如 `ORDER_CREATE` / `STORE_CREATE`）；
  - `idem_key`：Idempotency-Key（调用方生成，要求在 TTL 内唯一）。

数据库约束：

- 表 `bc_idempotency_record` 中联合唯一键：

```sql
UNIQUE KEY uk_tenant_biz_key (tenant_id, biz_type, idem_key)
```

多租户隔离依靠 tenant_id + biz_type 组合，避免不同租户之间的 Idempotency-Key 冲突。

### 1.2 request_hash 计算

为防止调用方误复用同一个 Idempotency-Key 但携带不同请求体，本模板要求传入 `request_hash` 字段：

- 计算方式（推荐）：
  1. 将请求中对结果有影响的核心字段（如商品列表、金额、外部业务ID等）序列化为“规范化 JSON”；
  2. 对该 JSON 字符串计算 `SHA-256` 摘要；
  3. 将结果编码为 64 位十六进制字符串（全小写/大写均可，保持一致即可）。

伪代码：

```java
String canonicalJson = toCanonicalJson(importantFields);
String requestHash = sha256Hex(canonicalJson); // 长度 64
```

当同一 `(tenantId, bizType, idemKey)` 下，history.request_hash 与当前不同时，
模板会抛出 `IdempotencyConflictException`，提示调用方修复 Idempotency-Key 复用问题。

---

## 2. 标准用法（模板调用范式）

核心接口（app-core）：

```java
public interface IdempotencyTemplate {
    <T> IdempotentResult<T> execute(IdempotencyRequest request,
                                    Class<T> resultType,
                                    Supplier<T> supplier);
}
```

### 2.1 创建幂等请求对象

```java
IdempotencyRequest req = new IdempotencyRequest(
        tenantId,
        "ORDER_CREATE",
        idemKey,        // 来自 HTTP Header: Idempotency-Key
        requestHash,    // 对关键字段 canonical JSON -> SHA-256
        Duration.ofHours(24),     // ttl
        Duration.ofSeconds(30),   // lockTtl
        false,                    // waitForCompletion
        null                      // waitMax
);
```

### 2.2 使用模板执行真实业务

```java
IdempotentResult<CreateOrderResultDTO> result = idempotencyTemplate.execute(
        req,
        CreateOrderResultDTO.class,
        () -> {
            // 真实业务逻辑（仅在获得执行权时执行一次）
            // 1. 生成 internal_id / public_id
            // 2. 落库（订单主表）
            // 3. 发布领域事件 / Outbox
            return new CreateOrderResultDTO(publicId);
        }
);

if (result.inProgress()) {
    // 可选择返回“处理中”响应给调用方，或在上层重试
}

CreateOrderResultDTO dto = result.value();  // 若 replayed 或第一次执行成功时非 null
```

说明：

- 当首次执行时，模板会创建幂等记录，状态置为 PROCESSING，然后执行 supplier 并在成功后写入 result_ref / result_json。
- 相同幂等键的后续请求：
  - 若 `request_hash` 相同：
    - 若已有成功记录未过期 => 直接重放结果（`replayed=true`）；
    - 若记录仍在 PROCESSING 且锁未过期 => 根据 `waitForCompletion` 决定返回 inProgress 或等待；
  - 若 `request_hash` 不同 => 抛 `IdempotencyConflictException`。

---

## 3. 与 PublicId 的绑定建议

幂等模板本身对业务 DTO 和 PublicId 无直接依赖，但推荐以下模式：

1. 创建接口（如创建订单）：
   - 在 supplier 中生成内部 `Ulid128` + `public_id`；
   - 幂等表中的 `result_ref` 字段优先存 public_id（稳定且很小）；
   - 对外 API 仅返回 public_id。

2. 查询接口：
   - 请求中传入 `public_id`；
   - 服务端使用 `PublicIdCodec` 解码为内部 ULID，再查询业务表。

结合 IdempotencyTemplate 的完整模板：

```java
IdempotentResult<CreateOrderResultDTO> result = idempotencyTemplate.execute(
        req,
        CreateOrderResultDTO.class,
        () -> {
            OrderId orderId = TypedIds.newOrderId(idService);
            String publicId = orderId.asPublic(publicIdCodec);

            // 幂等表可存 public_id 作为 result_ref
            // 订单表：internal_id (BINARY(16)) + public_id (UNIQUE)
            orderRepository.insert(orderId.internal(), publicId, ...);

            return new CreateOrderResultDTO(publicId);
        }
);
```

幂等表中的 `result_ref` 列推荐用于：

- 存储 stable public_id；
- 日志/排查时快速关联到业务主表。

---

## 4. 并发与等待策略

### 4.1 不等待模式（默认）

配置：

```java
waitForCompletion = false
waitMax = null
```

行为：

- 当同一幂等键已有请求正在处理（PROCESSING 且 lock_until 未过期）时，
  模板不会阻塞当前线程，而是返回：

```java
new IdempotentResult<>(false, true, null);
```

- 上层可选择：
  - 返回“处理中”提示；
  - 或者在前端/调用方侧做退避重试。

### 4.2 等待完成模式

配置：

```java
waitForCompletion = true
waitMax = Duration.ofSeconds(2); // 例如最多等待 2 秒
```

行为：

- 当检测到已有 PROCESSING 记录时，模板会在 `waitMax` 窗口内轮询幂等表：
  - 若在窗口内变为 SUCCEEDED => 自动重放结果（`replayed=true`）；
  - 若一直未完成 => 返回 `inProgress=true`；
  - 若状态为 FAILED => 抛 `IdempotencyInProgressException`，提示调用方稍后重试或走失败流程。

---

## 5. 常见错误与修复

### 5.1 Idempotency-Key 复用导致冲突

症状：

- 模板抛出 `IdempotencyConflictException`，提示同一 `(tenantId, bizType, idemKey)` 下请求摘要不一致。

原因：

- 调用方对不同业务请求复用了相同 `Idempotency-Key`。

修复建议：

- 确保 Idempotency-Key 与业务请求一一对应：
  - 对每次“创建类”请求生成新的全局唯一键；
  - 不要跨不同业务类型/不同请求体复用。

### 5.2 supplier 内部抛异常

症状：

- supplier 抛出 RuntimeException 或 checked Exception；
- 幂等表记录状态被标记为 FAILED 并记录 error_msg。

行为：

- 模板会调用 `markFailed` 更新幂等记录，然后将异常抛回调用方；
- 后续在 TTL 内重复请求时，当前实现会返回 IN_PROGRESS/异常，由上层决定是否重试或返回错误给客户端。

修复建议：

- 在业务层明确失败语义与重试策略；
- 若希望 FAILED 状态下能自动重试，可扩展 IdempotencyRepository 的 `RETRYABLE` 分支逻辑。

### 5.3 lockTtl 过短导致重复执行

症状：

- 部分长耗时请求的 `lock_until` 提前过期，导致其他实例从 DB 抢到执行权，造成多次执行。

修复建议：

- 调整 `lockTtl` 至略大于业务平均耗时；
- 对超长执行链路，可在 supplier 内部分阶段提交/幂等，自身也具备幂等性。

---

## 6. 总结

通过 `IdempotencyTemplate` + `IdempotencyRepository` + `IdempotencyLock` 的组合，
BlueCone 可以在各业务模块统一实现：

- 多租户隔离的幂等控制；
- 请求内容冲突检测（request_hash）；
- 高并发下的“一次执行 + 多次重放”；
- 与 PublicId / TypedId 体系的自然衔接。

业务模块无需自行维护幂等表和并发逻辑，只需按照本规范构造 `IdempotencyRequest` 并提供真实业务 supplier，
即可获得稳定且可演进的幂等能力。 

