# Ulid128 与 MyBatis/MyBatis-Plus 集成指南

本说明文档描述如何在 BlueCone 各业务模块（order / store / tenant / user 等）
中通过 `Ulid128` + MyBatis/MyBatis-Plus 实现内部主键的标准化持久化方案。

## 1. 推荐数据库字段设计

### 1.1 内部主键（internal_id）

推荐使用 128 位 ULID 的二进制表示：

```sql
internal_id BINARY(16) NOT NULL
```

特点：

- 空间紧凑：16 字节，适合作为主键和索引。
- 时间有序：结合 `UlidIdGenerator` 的单调性与 STRIPED 模式，利于索引局部性。

### 1.2 对外公开 ID（public_id）

结合 PublicId 规范：

```sql
public_id VARCHAR(40) NOT NULL
UNIQUE KEY uk_xxx_public_id (public_id)
```

说明：

- `VARCHAR(40)` 足够容纳 `{type}_{payload}_{checksum2}` 等格式。
- 建议对 `public_id` 建唯一索引，便于通过 PublicId 直接查询。

完整示例：

```sql
CREATE TABLE bc_order (
  internal_id BINARY(16)  NOT NULL PRIMARY KEY,
  public_id   VARCHAR(40) NOT NULL,
  tenant_id   BIGINT      NOT NULL,
  -- 其他业务字段 ...
  UNIQUE KEY uk_bc_order_public_id (public_id)
);
```

---

## 2. MyBatis-Plus 实体字段写法示例

在业务模块中，当引入 `app-id` 依赖后，`Ulid128` 的 TypeHandler 会通过 AutoConfiguration 自动注册。

### 2.1 BINARY(16) 映射（推荐）

```java
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.bluecone.app.id.core.Ulid128;
import org.apache.ibatis.type.JdbcType;

public class OrderEntity {

    @TableId(value = "internal_id", jdbcType = JdbcType.BINARY)
    private Ulid128 id;

    @TableField("public_id")
    private String publicId;

    // ... 其他字段与 getter/setter
}
```

说明：

- `Ulid128BinaryTypeHandler` 会把 `Ulid128` 与 `BINARY(16)` 之间自动转换。
- 若不显式指定 `jdbcType`，在大多数情况下也可正常工作，但推荐在关键主键上显式声明。

### 2.2 CHAR(26)/VARCHAR(26) 映射（过渡期）

若历史表使用 ULID 字符串（26 字符）：

```sql
internal_id CHAR(26) NOT NULL
```

实体可写为：

```java
public class LegacyOrderEntity {

    @TableId(value = "internal_id", jdbcType = JdbcType.CHAR)
    private Ulid128 id;

    // ...
}
```

说明：

- `Ulid128Char26TypeHandler` 会在内部使用 sulky-ulid 的解析能力，将 26 字符 ULID 解析为 `Ulid128`。
- 该方案仅用于兼容旧库，新建表建议直接使用 `BINARY(16)`。

---

## 3. Mapper SQL 示例

### 3.1 通过 internal_id 查询

```java
@Mapper
public interface OrderMapper {

    @Select("""
            SELECT *
            FROM bc_order
            WHERE internal_id = #{id,jdbcType=BINARY}
            """)
    OrderEntity findById(@Param("id") Ulid128 id);
}
```

### 3.2 通过 public_id 查询

```java
@Select("""
        SELECT *
        FROM bc_order
        WHERE public_id = #{publicId}
        """)
OrderEntity findByPublicId(@Param("publicId") String publicId);
```

MyBatis-Plus 使用 Wrapper 时，`Ulid128` 也会因 TypeHandler 自动完成转换：

```java
LambdaQueryWrapper<OrderEntity> wrapper = Wrappers.lambdaQuery(OrderEntity.class)
        .eq(OrderEntity::getId, ulid128);
OrderEntity entity = orderMapper.selectOne(wrapper);
```

---

## 4. 渐进迁移策略（重要）

### 4.1 新表（推荐）

- 新增表时，直接采用：
  - `internal_id BINARY(16) NOT NULL PRIMARY KEY`
  - `public_id VARCHAR(40) NOT NULL UNIQUE`
  - 实体字段使用 `Ulid128` + `String`。

### 4.2 老表使用 BIGINT 主键

如果现有表主键为 `BIGINT`：

- 短期内不强制迁移：
  - 对外 API DTO 统一暴露 `public_id`（PublicId），不再直接暴露 BIGINT。
  - 内部可维护 `BIGINT id` + `VARCHAR(40) public_id` 双字段，逐步切流。
- 新增业务能力时尽量依赖 `public_id` 作对外标识。

### 4.3 老表使用 ULID 字符串主键

如果现有表使用 `CHAR(26)`/`VARCHAR(26)` 存储 ULID 字符串：

1. 引入 `app-id` 依赖。
2. 实体主键字段改为 `Ulid128`，并使用 `JdbcType.CHAR`/`VARCHAR` 映射。
3. 新增 `internal_id BINARY(16)` 字段可作为过渡主键，在数据迁移完成后替代字符串主键。

### 4.4 跨表关联建议

- 不建议使用数据库层外键约束做跨模块关联。
- 推荐：
  - 内部使用 `Ulid128` + 业务约束 + 唯一索引；
  - 对外统一暴露 `public_id`，避免跨表互相暴露内部主键。

---

## 5. AutoConfiguration 行为说明

当类路径中存在 MyBatis 时（`org.apache.ibatis.session.Configuration` 可用），
`app-id` 会通过 `IdMybatisAutoConfiguration` 自动注册以下 TypeHandler：

- `Ulid128BinaryTypeHandler`：
  - 默认映射：`Ulid128.class` + `JdbcType.BINARY` / `JdbcType.VARBINARY`
  - 推荐用于 `BINARY(16)` 主键字段。
- `Ulid128Char26TypeHandler`：
  - 映射：`Ulid128.class` + `JdbcType.CHAR` / `JdbcType.VARCHAR`
  - 用于兼容 `CHAR(26)` / `VARCHAR(26)` 字段。

可通过配置禁用：

```yaml
bluecone:
  id:
    mybatis:
      enabled: false
```

此时，不会自动向 MyBatis 注册任何 TypeHandler，必要时可由业务项目自行配置。

---

## 6. Application YAML 示例

综合 ULID + PublicId + MyBatis 的推荐配置：

```yaml
bluecone:
  id:
    enabled: true
    mybatis:
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

按照上述约定接入后，各业务模块可以直接使用 `Ulid128` 作为实体主键类型，
由 `app-id` 统一负责与数据库层的二进制/字符串映射，降低重复工作与错误风险。 

