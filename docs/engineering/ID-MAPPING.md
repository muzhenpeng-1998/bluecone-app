# ID 映射与落库规范（Ulid128 / LongId / PublicId）

本说明文档描述在 BlueCone 多模块工程中，`Ulid128`、`PublicId` 与基于 Snowflake 的
long 型 ID 在数据库与 JSON 层的推荐映射方式，以及 nodeId 配置建议。

> 目标：做到“Ulid128 工程化闭环”——任何 DO/DTO 使用 `Ulid128` 都能自然工作：
> - MyBatis/MyBatis-Plus 持久化：`Ulid128 <-> BINARY(16)` 自动映射；
> - Jackson：`Ulid128 <-> ULID 字符串(26)` 自动序列化/反序列化；
> - 对外 API 只暴露 `public_id` 字符串，内部统一用 `Ulid128`/LongId。

---

## 1. Ulid128 落库规范

### 1.1 推荐字段类型：BINARY(16)

内部主键（internal_id）推荐使用 `BINARY(16)`：

```sql
internal_id BINARY(16) NOT NULL
```

特点：

- 空间紧凑：16 字节，比 `CHAR(26)` 更省空间。
- 写入有序：配合单调 ULID 生成器（`UlidIdGenerator` / `MonotonicUlidGenerator`），索引局部性更好。
- 强类型：Java 侧使用 `Ulid128`，避免误用/串用。

典型表结构示例：

```sql
CREATE TABLE bc_order (
  internal_id BINARY(16)  NOT NULL PRIMARY KEY,
  public_id   VARCHAR(40) NOT NULL,
  tenant_id   BIGINT      NOT NULL,
  -- 其他业务字段 ...
  UNIQUE KEY uk_bc_order_public_id (public_id)
);
```

### 1.2 MyBatis/MyBatis-Plus TypeHandler：Ulid128 &lt;-&gt; BINARY(16)

`app-id` 模块内已提供 `Ulid128` 的 MyBatis TypeHandler，并通过自动配置全局注册：

- 实现类：
  - `app-id/src/main/java/com/bluecone/app/id/mybatis/Ulid128BinaryTypeHandler.java`
  - `app-id/src/main/java/com/bluecone/app/id/mybatis/Ulid128Char26TypeHandler.java`
- 自动装配：
  - `app-id/src/main/java/com/bluecone/app/id/autoconfigure/IdMybatisAutoConfiguration.java`
  - 条件：`bluecone.id.mybatis.enabled=true`（默认开启）

注册逻辑（关键代码）：

```java
@Bean
public ConfigurationCustomizer blueconeUlidTypeHandlerCustomizer(
        Ulid128BinaryTypeHandler binary,
        Ulid128Char26TypeHandler char26) {
    return configuration -> {
        TypeHandlerRegistry r = configuration.getTypeHandlerRegistry();
        // 默认将 Ulid128 映射为二进制处理器
        r.register(Ulid128.class, binary);
        r.register(Ulid128.class, JdbcType.BINARY, binary);
        r.register(Ulid128.class, JdbcType.VARBINARY, binary);
        // CHAR/VARCHAR 显式指定时使用字符串形式
        r.register(Ulid128.class, JdbcType.CHAR, char26);
        r.register(Ulid128.class, JdbcType.VARCHAR, char26);
    };
}
```

`Ulid128BinaryTypeHandler` 实现要点（BINARY(16)）：

```java
public class Ulid128BinaryTypeHandler extends BaseTypeHandler<Ulid128> {

    @Override
    public void setNonNullParameter(PreparedStatement ps, int i, Ulid128 parameter, JdbcType jdbcType)
            throws SQLException {
        byte[] bytes = parameter.toBytes(); // 长度必须为 16
        ps.setBytes(i, bytes);
    }

    @Override
    public Ulid128 getNullableResult(ResultSet rs, String columnName) throws SQLException {
        byte[] bytes = rs.getBytes(columnName);
        return convertBytes(bytes);
    }

    // 其他 getNullableResult 省略

    private Ulid128 convertBytes(byte[] bytes) {
        if (bytes == null) {
            return null;
        }
        if (bytes.length != 16) {
            throw new IllegalArgumentException("BINARY(16) 字段读取到长度=" + bytes.length + "，期望=16");
        }
        return Ulid128.fromBytes(bytes);
    }
}
```

因此，在实体中直接使用 `Ulid128` 即可：

```java
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.bluecone.app.id.core.Ulid128;
import org.apache.ibatis.type.JdbcType;

@TableName("bc_order")
public class OrderEntity {

    @TableId(value = "internal_id", jdbcType = JdbcType.BINARY)
    private Ulid128 id;

    private String publicId;
    // ...
}
```

无需在每个字段上显式声明 `typeHandler`，全局注册已经处理。

### 1.3 过渡期：CHAR(26)/VARCHAR(26) 兼容

对于历史使用 ULID 字符串的表：

```sql
internal_id CHAR(26) NOT NULL
```

可使用 `Ulid128Char26TypeHandler` 做兼容：

```java
@TableId(value = "internal_id", jdbcType = JdbcType.CHAR)
private Ulid128 id;
```

但新表仍推荐直接使用 `BINARY(16)`，避免字符串主键带来的空间与性能问题。

---

## 2. Jackson：Ulid128 &lt;-&gt; ULID 字符串

### 2.1 Ulid128JacksonModule

`app-id` 提供了 `Ulid128JacksonModule`，负责 `Ulid128` 与 ULID 字符串之间的转换：

- 实现类：
  - `app-id/src/main/java/com/bluecone/app/id/jackson/Ulid128JacksonModule.java`

关键实现：

```java
public class Ulid128JacksonModule extends SimpleModule {

    public Ulid128JacksonModule() {
        super("Ulid128JacksonModule");
        addSerializer(Ulid128.class, new Ulid128Serializer());
        addDeserializer(Ulid128.class, new Ulid128Deserializer());
    }

    // 序列化：Ulid128 -> ULID 字符串
    public static class Ulid128Serializer extends JsonSerializer<Ulid128> {
        @Override
        public void serialize(Ulid128 value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
            if (value == null) {
                gen.writeNull();
            } else {
                gen.writeString(value.toString());
            }
        }
    }

    // 反序列化：ULID 字符串 -> Ulid128
    public static class Ulid128Deserializer extends JsonDeserializer<Ulid128> {
        @Override
        public Ulid128 deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
            if (p.currentToken() == JsonToken.VALUE_NULL) {
                return null;
            }
            if (p.currentToken() != JsonToken.VALUE_STRING) {
                throw JsonMappingException.from(p, "Ulid128 必须从字符串反序列化");
            }
            String text = p.getText();
            if (text == null || text.isBlank()) {
                return null;
            }
            try {
                ULID.Value value = ULID.parseULID(text);
                return new Ulid128(value.getMostSignificantBits(), value.getLeastSignificantBits());
            } catch (IllegalArgumentException ex) {
                throw JsonMappingException.from(p, "无效的 ULID 字符串: " + text, ex);
            }
        }
    }
}
```

使用示例（显式注册）：

```java
ObjectMapper mapper = new ObjectMapper();
mapper.registerModule(new Ulid128JacksonModule());

Ulid128 id = new Ulid128(...);
String json = mapper.writeValueAsString(id);      // 输出 ULID 字符串
Ulid128 parsed = mapper.readValue(json, Ulid128.class); // 反序列化回 Ulid128
```

### 2.2 Spring Boot 自动装配

在实际应用中，不需要显式手动注册：

- `IdJacksonAutoConfiguration` 会自动装配 `BlueconeIdJacksonModule`：
  - `app-id/src/main/java/com/bluecone/app/id/autoconfigure/IdJacksonAutoConfiguration.java`
- `BlueconeIdJacksonModule` 内部已经集成：
  - `TypedId` -> `public_id` 字符串序列化；
  - `Ulid128` 的序列化 + 反序列化（使用 `Ulid128JacksonModule.Ulid128Serializer/Deserializer`）。

关键片段：

```java
@AutoConfiguration(after = IdAutoConfiguration.class)
@ConditionalOnClass(ObjectMapper.class)
@ConditionalOnProperty(prefix = "bluecone.id.jackson", name = "enabled", havingValue = "true", matchIfMissing = true)
public class IdJacksonAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(name = "blueconeIdJacksonModule")
    public Module blueconeIdJacksonModule(PublicIdCodec codec) {
        return new BlueconeIdJacksonModule(codec);
    }
}
```

因此，只要应用引入 `app-id`，且开启 `bluecone.id.jackson.enabled`（默认 true），
任何 DO/DTO 中的 `Ulid128` 字段都可以自动：

- 序列化为 26 字符 ULID 字符串（便于阅读/复制）；
- 从 26 字符 ULID 字符串反序列化回 `Ulid128`，非法字符串会抛 `JsonMappingException`。

---

## 3. PublicId 落库与使用规范

### 3.1 推荐字段设计

对外公开 ID（public_id）推荐：

```sql
public_id VARCHAR(40) NOT NULL
UNIQUE KEY uk_xxx_public_id (public_id)
```

特点：

- 存储 `prefix_ulid[_checksum]` 等格式，长度预留到 40 基本足够；
- 建唯一索引便于通过 `public_id` 直接查询主表。

### 3.2 生成与解析

- 生成：统一通过 `PublicIdFactory` 或 `PublicIdCodec`：

```java
import com.bluecone.app.id.api.IdService;
import com.bluecone.app.id.api.ResourceType;

Ulid128 internalId = idService.nextUlid();
String publicId = idService.nextPublicId(ResourceType.ORDER);
// 或：publicIdCodec.encode("ord", internalId).asString();
```

- 解析：统一通过 `PublicIdCodec.decode`，并校验类型：

```java
DecodedPublicId decoded = publicIdCodec.decode(publicId);
if (!"ord".equals(decoded.type())) {
    throw new IllegalArgumentException("public_id 类型不匹配");
}
Ulid128 internalId = decoded.id();
```

---

## 4. LongId（Snowflake）落库与 nodeId 建议

### 4.1 字段类型：BIGINT UNSIGNED

Snowflake long 型 ID 推荐：

```sql
id BIGINT UNSIGNED NOT NULL PRIMARY KEY
```

理由：

- 63 位正整数，使用 `BIGINT UNSIGNED` 可覆盖全量空间；
- 相比 `BINARY(16)` 更节省空间（8 字节），适合极端追求索引空间的场景；
- 但缺乏可读性，不适合作为对外公开 ID。

推荐用法：

- 内部主键可以使用 long ID（例如与第三方/历史系统兼容）；
- 对外仍暴露 `public_id`（基于 `Ulid128`），避免可预测/递增 ID 暴露。

### 4.2 生成器：SnowflakeLongIdGenerator

`app-id` 提供了基于 Snowflake 算法的 long ID 生成器：

- 实现类：
  - `app-id/src/main/java/com/bluecone/app/id/core/SnowflakeLongIdGenerator.java`
- 位布局：`time(41) | node(10) | seq(12)`，最高位固定为 0。
- 配置属性：
  - `bluecone.id.long.enabled`：是否启用 long ID 生成；
  - `bluecone.id.long.nodeId`：节点 ID，范围 [0, 1023]；
  - `bluecone.id.long.epochMillis`：自定义纪元时间（毫秒）。

自动装配摘录：

```java
@Bean
@ConditionalOnMissingBean
@ConditionalOnProperty(prefix = "bluecone.id.long", name = "enabled", havingValue = "true")
public SnowflakeLongIdGenerator snowflakeLongIdGenerator(BlueconeIdProperties props) {
    BlueconeIdProperties.LongId longProps = props.getLong();
    return new SnowflakeLongIdGenerator(longProps.getEpochMillis(), longProps.getNodeId());
}
```

`IdService` 提供统一入口：

```java
long nextId = idService.nextLongId();
```

### 4.3 nodeId 配置建议

**关键要求：多实例部署时，每个实例的 nodeId 必须唯一。** 否则会产生 ID 冲突。

推荐方案（任选或组合）：

1. **环境变量配置（简单直接）**
   - 在部署配置中为每个实例注入唯一的 `BLUECONE_ID_NODE_ID`；
   - 通过 Spring 配置映射到 `bluecone.id.long.node-id`。

2. **Kubernetes StatefulSet ordinal 映射**
   - StatefulSet 中 Pod 名如：`app-0`、`app-1`、`app-2`；
   - 启动脚本解析 ordinal（0/1/2），映射到 `nodeId`（可直接使用或加偏移量）；
   - 示例：`nodeId = ordinal` 或 `nodeId = base + ordinal`。

3. **云托管实例标识映射**
   - 若云平台提供实例 ID（如腾讯云微信云托管的实例标识），可在启动脚本中：
     - 将实例 ID 映射到 [0, 1023] 的整数（例如取 hash % 1024）；
     - 通过环境变量或配置中心注入到应用。

4. **集中分配（配置中心/注册中心）**
   - 在配置中心为每个逻辑节点预分配 nodeId；
   - 应用启动时根据节点名拉取 nodeId 配置；
   - 适合大规模集群和统一治理场景。

**注意事项：**

- 不建议简单使用 IP/端口转换为 nodeId，因为容器环境下 IP 可能频繁变化；
- 如节点数量可能超过 1024，需要重新评估位宽分配或采用 Ulid128/BINARY(16) 方案。

### 4.4 什么时候用 long 而不是 Ulid128

适合使用 long ID 的场景：

- 需要与已有只支持 BIGINT 主键的系统/库兼容；
- 需要进一步压缩索引与存储空间（8 字节 vs 16 字节），且对可读性要求不高；
- 对 ID 的时间有序性要求较高，但可以接受不可读。

仍然推荐使用 `Ulid128` 的场景：

- 大部分新业务表的内部主键（配合 BINARY(16)）；
- 配合 `PublicId`/`TypedId` 做对外标识，增强可读性与防串用能力；
- 需要跨模块统一 ID 格式、利于治理和审计。

**强烈不推荐：**

- 直接对外暴露 long ID 作为 public_id：
  - 容易被枚举/猜测；
  - 可预测性强，存在安全与业务风控风险。

---

## 5. 总结：Ulid128 工程化闭环现状

当前仓库中，围绕 `Ulid128` 已具备：

- **生成与门面：**
  - `IdService.nextUlid()` / `nextUlidString()` / `nextUlidBytes()`；
  - 单调 ULID 生成器 + 时钟回拨处理（STRICT/STRIPED 模式）。
- **MyBatis 映射：**
  - `Ulid128BinaryTypeHandler` + `Ulid128Char26TypeHandler`；
  - 通过 `IdMybatisAutoConfiguration` 全局注册，DO 可直接使用 `Ulid128`。
- **Jackson 映射：**
  - `Ulid128JacksonModule` + `BlueconeIdJacksonModule`；
  - 通过 `IdJacksonAutoConfiguration` 自动注册，DTO 中 `Ulid128` 字段可自动 JSON 化。
- **PublicId 与 TypedId：**
  - `PublicIdCodec` + `DefaultPublicIdCodec`；
  - `TypedId` + `TypedIds` 工具，支持强类型 ID 与 `public_id` 往返。
- **LongId 支持：**
  - `SnowflakeLongIdGenerator` + `IdService.nextLongId()`；
  - 通过 `bluecone.id.long.*` 属性配置。

业务模块只需：

- DO：内部主键使用 `Ulid128` + `BINARY(16)`；
- DTO：对外只暴露 `public_id` 字符串，避免直接暴露 internal_id/long ID；
- 生成 ID 时仅依赖 `IdService` / `PublicIdCodec` / `TypedId` 门面，
  即可享受上述工程能力，无需关心底层实现细节。 

