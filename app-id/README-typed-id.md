# BlueCone 强类型 ID（TypedId）接入指南

本说明文档用于约定 BlueCone 多模块（order / store / tenant / user 等）中的强类型 ID 体系，
通过 `TypedId` + `Ulid128` + `PublicId` 的组合，减少把门店 ID 传成订单 ID 这类低级事故。

## 1. 设计目标

- **强类型约束**：在编译期区分 TenantId / StoreId / OrderId / UserId / PaymentId。
- **对外只暴露 public_id**：外部 API 不暴露内部自增或 ULID 主键，避免可枚举。
- **内部统一 Ulid128**：内部领域与持久化使用 `Ulid128`（配合 BINARY(16) 与 TypeHandler）。
- **统一编码/解码工具**：PublicIdCodec + TypedIds 提供统一的 PublicId roundtrip 范式。

---

## 2. TypedId 基本模型

核心抽象位于 `com.bluecone.app.id.typed.api`：

```java
public sealed interface TypedId
        permits TenantId, StoreId, OrderId, UserId, PaymentId {

    Ulid128 internal();   // 内部 ULID，用于 DB/领域

    String type();        // 类型前缀，例如 ten/sto/ord/usr/pay

    default String asPublic(PublicIdCodec codec) {
        return codec.encode(type(), internal()).asString();
    }
}
```

示例强类型 ID：

```java
public record TenantId(Ulid128 internal) implements TypedId {
    @Override public String type() { return "ten"; }
}

public record StoreId(Ulid128 internal) implements TypedId {
    @Override public String type() { return "sto"; }
}

public record OrderId(Ulid128 internal) implements TypedId {
    @Override public String type() { return "ord"; }
}

public record UserId(Ulid128 internal) implements TypedId {
    @Override public String type() { return "usr"; }
}

public record PaymentId(Ulid128 internal) implements TypedId {
    @Override public String type() { return "pay"; }
}
```

---

## 3. 创建与解析工具：TypedIds

统一创建与从 public_id 解析的工具类：

```java
public final class TypedIds {

    public static TenantId newTenantId(IdService ids) {
        return new TenantId(ids.nextUlid());
    }

    public static StoreId newStoreId(IdService ids) {
        return new StoreId(ids.nextUlid());
    }

    public static OrderId newOrderId(IdService ids) {
        return new OrderId(ids.nextUlid());
    }

    // ...

    public static <T extends TypedId> T fromPublic(String publicId,
                                                   PublicIdCodec codec,
                                                   Function<Ulid128, T> ctor,
                                                   String expectedType) {
        DecodedPublicId decoded = codec.decode(publicId);
        if (!expectedType.equals(decoded.type())) {
            throw new IllegalArgumentException(
                    "public_id 类型不匹配，期望=" + expectedType + "，实际=" + decoded.type());
        }
        return ctor.apply(decoded.id());
    }

    public static Ulid128 toInternal(TypedId id) {
        return id == null ? null : id.internal();
    }

    public static <T extends TypedId> T fromInternal(Ulid128 ulid, Function<Ulid128, T> ctor) {
        return ulid == null ? null : ctor.apply(ulid);
    }
}
```

推荐解析范式：

```java
OrderId orderId = TypedIds.fromPublic(req.publicId(), publicIdCodec, OrderId::new, "ord");
```

如传入用户的 public_id 却按订单解析，`expectedType="ord"` 会触发异常，避免“类型串用”事故。

---

## 4. 分层建议：DTO / Domain / Persistence

### 4.1 Controller / DTO 层

- 对外请求/响应只使用 `public_id` 字符串。
- 不直接在 DTO 中暴露 TypedId 或 internal_id。

示例：

```java
public record CreateOrderRequest(String idempotencyKey, /* 其他字段 */) {}

public record CreateOrderResponse(String publicId) {}
```

### 4.2 Application / Domain 层

- 使用强类型 ID：`OrderId` / `StoreId` / `TenantId` 等。
- 从 DTO 的 `public_id` 解析为 TypedId。

示例：

```java
@Service
public class OrderApplicationService {

    private final IdService idService;
    private final PublicIdCodec publicIdCodec;

    public OrderApplicationService(IdService idService, PublicIdCodec publicIdCodec) {
        this.idService = idService;
        this.publicIdCodec = publicIdCodec;
    }

    public CreateOrderResponse create(CreateOrderRequest req) {
        OrderId orderId = TypedIds.newOrderId(idService);
        String publicId = orderId.asPublic(publicIdCodec);
        // 将 internal 与 publicId 交给下游持久化层
        // ...
        return new CreateOrderResponse(publicId);
    }

    public OrderId parseOrderId(String publicId) {
        return TypedIds.fromPublic(publicId, publicIdCodec, OrderId::new, "ord");
    }
}
```

### 4.3 Persistence / MyBatis 层

- 推荐实体层使用 `Ulid128` 对应 `BINARY(16)` 字段。
- TypedId 与 `Ulid128` 转换通过 `TypedIds.toInternal/fromInternal` 完成。

示例实体与 Mapper：

```java
public class OrderEntity {

    @TableId(value = "internal_id", jdbcType = JdbcType.BINARY)
    private Ulid128 id;           // internal_id

    private String publicId;      // public_id
    // ...
}

@Mapper
public interface OrderMapper {

    int insert(OrderEntity entity);

    OrderEntity findById(@Param("id") Ulid128 id);
}
```

在领域层可封装转换：

```java
OrderEntity toEntity(OrderId orderId, String publicId, /* other fields */) {
    OrderEntity e = new OrderEntity();
    e.setId(TypedIds.toInternal(orderId));
    e.setPublicId(publicId);
    // ...
    return e;
}
```

---

## 5. Jackson 序列化行为

`app-id` 提供 `BlueconeIdJacksonModule`，自动将 `TypedId` 序列化为 `public_id` 字符串：

```java
ObjectMapper mapper = new ObjectMapper();
mapper.registerModule(new BlueconeIdJacksonModule(publicIdCodec));

record Resp(OrderId orderId) {}

String json = mapper.writeValueAsString(new Resp(orderId));
// => {"orderId":"ord_01J9ZQF1X8F2ZK2Q9HQWFQ4YQF"}
```

通过 `IdJacksonAutoConfiguration`，在类路径存在 Jackson 且配置未禁用时，会自动向 Spring 上下文注册该 Module：

```yaml
bluecone:
  id:
    jackson:
      enabled: true
```

说明：

- 当前仅提供序列化（输出），反序列化推荐仍通过 DTO 中的 `public_id` + `TypedIds.fromPublic` 完成，避免在 ObjectMapper 内部耦合 Spring Bean 查找。

---

## 6. 幂等创建与 TypedId 标准模板

结合 PublicId 与 TypedId，推荐的幂等创建流程如下：

### 6.1 创建订单（伪代码）

```java
public class CreateOrderService {

    private final IdService idService;
    private final PublicIdCodec publicIdCodec;
    private final IdempotencyStore idempotencyStore;
    private final OrderRepository orderRepository;

    public CreateOrderService(IdService idService,
                              PublicIdCodec publicIdCodec,
                              IdempotencyStore idempotencyStore,
                              OrderRepository orderRepository) {
        this.idService = idService;
        this.publicIdCodec = publicIdCodec;
        this.idempotencyStore = idempotencyStore;
        this.orderRepository = orderRepository;
    }

    public String create(String idempotencyKey, CreateOrderCommand cmd) {
        // 1. 幂等命中直接返回 public_id
        var existing = idempotencyStore.findByKey(idempotencyKey);
        if (existing != null) {
            return existing.publicId();
        }

        // 2. 生成强类型 ID + public_id
        OrderId orderId = TypedIds.newOrderId(idService);
        String publicId = orderId.asPublic(publicIdCodec);

        // 3. 落幂等映射（idempotency_key -> internal_id + public_id）
        idempotencyStore.save(idempotencyKey, orderId.internal(), publicId);

        // 4. 落订单主表（internal_id BINARY(16) + public_id VARCHAR UNIQUE）
        OrderEntity entity = new OrderEntity();
        entity.setId(orderId.internal());
        entity.setPublicId(publicId);
        // ... 填充其他业务字段 ...
        orderRepository.insert(entity);

        return publicId;
    }
}
```

### 6.2 查询订单

```java
public OrderDetailResponse queryByPublicId(String publicId) {
    OrderId orderId = TypedIds.fromPublic(publicId, publicIdCodec, OrderId::new, "ord");
    Ulid128 internalId = orderId.internal();
    OrderEntity entity = orderRepository.findById(internalId);
    // ...
}
```

---

## 7. Application YAML 示例

结合 TypedId / PublicId / MyBatis / Jackson 的推荐配置：

```yaml
bluecone:
  id:
    enabled: true
    mybatis:
      enabled: true
    jackson:
      enabled: true
    ulid:
      mode: STRIPED
      stripes: 32
      metricsEnabled: true
      rollback:
        policy: USE_LAST
        failFastThresholdMs: 5000
        waitMaxMs: 50
    public-id:
      enabled: true
      separator: "_"
      format: ULID_BASE32   # 或 BASE62_128
      checksumEnabled: false
      checksumBytes: 1
```

按照上述约定接入后：

- 对外统一使用 `public_id` 字符串。
- 内部使用 `TypedId` + `Ulid128` + BINARY(16) 主键。
- Jackson、MyBatis 等基础设施由 `app-id` 统一提供，业务模块只需专注于领域逻辑。 

