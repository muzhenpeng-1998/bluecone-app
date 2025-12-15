# BlueCone PublicId 规范与接入模板

本说明文档用于约定 BlueCone 多租户 SaaS 内部主键与对外公开 ID（PublicId）的统一规范，
并给出在订单 / 门店 / 租户 / 用户等模块中的标准接入模板，尤其适用于幂等创建与对外 API DTO 设计。

## 1. PublicId 设计目标

- **不可预测**：避免自增 ID / 连续 ULID 直接暴露，降低可枚举风险。
- **可追溯**：可从 PublicId 中还原内部 128 位 ULID，用于排查 / 调试 / 运营。
- **可校验**：可选校验和，用于人工录入防输错、防简单篡改。
- **统一格式**：跨模块统一格式，便于日志检索、API、一致性治理。

内部主键建议使用 `Ulid128` + MySQL `BINARY(16)`，对外只暴露 `PublicId` 字符串。

---

## 2. PublicId 格式规范

PublicId 由三部分组成：

```text
{type}{separator}{payload}[{separator}{checksum2}]
```

- `type`：业务类型前缀，仅允许 `[a-z0-9]{2,10}`（小写字母 + 数字），如：
  - 租户：`ten`
  - 门店：`sto`
  - 用户：`usr`
  - 订单：`ord`
  - 支付：`pay`
  - 退款：`ref`
  - 商品：`sku` / `spu`
- `separator`：分隔符，默认 `"_"`。
- `payload`：内部 ULID 的编码表示，取决于 `format`。
- `checksum2`：可选的 2 位校验码，使用 CRC8 + Crockford Base32。

### 2.1 ULID_BASE32 格式

配置：

```yaml
bluecone:
  id:
    public-id:
      format: ULID_BASE32
```

格式：

```text
{type}_{ulid26}[_{checksum2}]
```

- `ulid26`：标准 ULID 字符串，26 个 Crockford Base32 字符。
- 示例：
  - 无校验和：`ord_01J9ZQF1X8F2ZK2Q9HQWFQ4YQF`
  - 带校验和：`ord_01J9ZQF1X8F2ZK2Q9HQWFQ4YQF_0F`

特点：
- 可读性好，与 ULID 一致，便于人眼识别。

### 2.2 BASE62_128 格式

配置：

```yaml
bluecone:
  id:
    public-id:
      format: BASE62_128
```

格式：

```text
{type}_{base62_22}[_{checksum2}]
```

- `base62_22`：将 128 位 ULID 二进制表示编码为 **固定长度 22** 位 Base62 字符串（`0-9A-Za-z`）。
- 示例：
  - 无校验和：`ord_0k8Zy1bQ2L9xD0aBcDeFgH`
  - 带校验和：`ord_0k8Zy1bQ2L9xD0aBcDeFgH_0F`

特点：
- 比 ULID 更短（22 vs 26），更适合 URL / 移动端 / 日志占用。

### 2.3 校验和（可选）

配置：

```yaml
bluecone:
  id:
    public-id:
      checksumEnabled: true
      checksumBytes: 1   # 当前只支持 1 字节 CRC8
```

- 校验内容：`type + separator + payload`。
- 校验算法：CRC8（poly=0x07） -> 0..255 -> Crockford Base32 两字符编码。
- 作用：防止人工录入错误、简单篡改；不用于安全加密。

---

## 3. DTO 与内部模型规范

### 3.1 DTO 层（对外 API）

- **仅暴露 PublicId**，不暴露内部自增 ID / ULID 二进制。
- 建议 DTO 字段命名为 `public_id` 或 `publicId`，统一风格。

示例：

```java
public record CreateOrderResponse(String publicId) {
}

public record OrderDetailResponse(String publicId, /* 其他字段 */) {
}
```

### 3.2 内部实体 / 表结构建议

- `internal_id`（主键）：
  - 推荐：`BINARY(16)`（存储 `Ulid128` 的 16 字节）
  - 过渡期可用 `VARCHAR(26)` ULID 字符串或 `BIGINT`，但不对外暴露
- `public_id`：
  - 字段类型：`VARCHAR(40)`（足够容纳 prefix + payload + checksum）
  - 建议建唯一索引：`UNIQUE (tenant_id, public_id)` 或全局唯一 `UNIQUE (public_id)`

示例（MySQL）：

```sql
CREATE TABLE bc_order (
  id          BINARY(16)     NOT NULL PRIMARY KEY,
  tenant_id   BIGINT         NOT NULL,
  public_id   VARCHAR(40)    NOT NULL,
  -- ...
  UNIQUE KEY uk_bc_order_public_id (public_id)
);
```

---

## 4. PublicIdCodec 使用示例

AutoConfiguration 已自动装配：

- `IdService`：生成内部 `Ulid128` / String / bytes。
- `PublicIdCodec`：负责 PublicId 编解码与校验。

示例（在订单模块中接入）：

```java
@Service
public class OrderIdFacade {

    private final IdService idService;
    private final PublicIdCodec publicIdCodec;

    public OrderIdFacade(IdService idService, PublicIdCodec publicIdCodec) {
        this.idService = idService;
        this.publicIdCodec = publicIdCodec;
    }

    public String nextOrderPublicId() {
        var ulid = idService.nextUlid();
        return publicIdCodec.encode("ord", ulid).asString();
    }

    public Ulid128 parseOrderPublicId(String publicId) {
        var decoded = publicIdCodec.decode(publicId);
        if (!"ord".equals(decoded.type())) {
            throw new IllegalArgumentException("publicId 类型不匹配，期望 ord，实际 " + decoded.type());
        }
        return decoded.id();
    }
}
```

---

## 5. 幂等创建标准模板（推荐）

场景：幂等创建订单（或门店、租户、用户等），客户端传入 `Idempotency-Key`，服务端返回 PublicId。

### 5.1 伪代码流程

```java
public class CreateOrderService {

    private final IdService idService;
    private final PublicIdCodec publicIdCodec;
    private final IdempotencyStore idempotencyStore; // 自己实现：存幂等映射
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

    public String createOrder(String idempotencyKey, CreateOrderCommand command) {
        // 1. 查询幂等映射
        var existing = idempotencyStore.findByKey(idempotencyKey);
        if (existing != null) {
            return existing.publicId(); // 已经生成过，直接返回
        }

        // 2. 生成内部 ID + PublicId
        var ulid = idService.nextUlid();
        var publicId = publicIdCodec.encode("ord", ulid).asString();

        // 3. 落幂等映射（建议在事务内，与订单创建保持一致）
        idempotencyStore.save(idempotencyKey, ulid, publicId);

        // 4. 落订单主表（internal_id + public_id）
        var entity = new OrderEntity();
        entity.setId(ulid.toBytes());       // BINARY(16)
        entity.setPublicId(publicId);       // VARCHAR(40)
        // ... 填充其他业务字段 ...
        orderRepository.insert(entity);

        // 5. 返回 public_id
        return publicId;
    }
}
```

### 5.2 幂等表结构建议

```sql
CREATE TABLE idempotency_mapping (
  idempotency_key VARCHAR(128) NOT NULL PRIMARY KEY,
  public_id       VARCHAR(40)  NOT NULL,
  internal_id     BINARY(16)   NOT NULL,
  created_at      DATETIME     NOT NULL
);
```

- 查询时优先命中 `idempotency_key`，避免重复写业务表。

---

## 6. Application YAML 示例

示例配置（结合 ULID + PublicId）：

```yaml
bluecone:
  id:
    enabled: true
    legacy-config-enabled: false
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

如果希望对人工录入 PublicId 的场景更安全，可开启：

```yaml
bluecone:
  id:
    public-id:
      checksumEnabled: true
```

---

## 7. 接入建议（order / store / tenant / user 等）

统一建议：

1. **内部实体**统一新增字段：
   - `internal_id`（BINARY(16) / 不对外暴露）
   - `public_id`（VARCHAR(40) / 对外暴露 / 建 UNIQUE）
2. 统一在应用层或领域服务层引入：
   - `IdService`：生成内部 ULID。
   - `PublicIdCodec`：生成 / 解析 PublicId。
3. 对外 API：
   - 请求参数中使用 `public_id` 作为资源标识。
   - 服务端通过 `public_id -> decode -> Ulid128 -> Repository` 查找内部实体。
4. 幂等创建：
   - 将 `public_id` 作为幂等返回值统一管理，避免业务各自编码。

按此模板接入后，BlueCone 全域的对外 ID 将具有统一的格式与语义，便于后续日志追踪、灰度、跨模块排查与治理。 

