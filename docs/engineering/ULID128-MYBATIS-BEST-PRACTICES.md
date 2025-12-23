# Ulid128 在 MyBatis Plus 中的使用最佳实践

## 概述

本文档说明如何在 MyBatis Plus 实体类中正确使用 `Ulid128` 类型字段。

## 核心规则

**⚠️ 重要：所有使用 `Ulid128` 类型的实体类字段，必须显式添加 `@TableField(typeHandler = Ulid128BinaryTypeHandler.class)` 注解。**

## 标准模板

### 1. 实体类定义

```java
package com.bluecone.app.xxx.dao.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.bluecone.app.id.core.Ulid128;
import com.bluecone.app.id.mybatis.Ulid128BinaryTypeHandler;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("bc_example")
public class BcExample {
    
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    
    private Long tenantId;
    
    /**
     * 内部主键 ULID128，对应列 internal_id (BINARY(16))。
     */
    @TableField(typeHandler = Ulid128BinaryTypeHandler.class)
    private Ulid128 internalId;
    
    /**
     * 对外 ID，对应列 public_id (VARCHAR(40))。
     */
    private String publicId;
    
    private String name;
    
    private LocalDateTime createdAt;
    
    private LocalDateTime updatedAt;
}
```

### 2. 必需的 Import 语句

```java
import com.baomidou.mybatisplus.annotation.TableField;
import com.bluecone.app.id.core.Ulid128;
import com.bluecone.app.id.mybatis.Ulid128BinaryTypeHandler;
```

### 3. 数据库表结构

```sql
CREATE TABLE bc_example (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    tenant_id BIGINT NOT NULL COMMENT '租户ID',
    internal_id BINARY(16) NULL COMMENT '内部主键 ULID128',
    public_id VARCHAR(40) NULL COMMENT '对外ID（格式：prefix_ulid）',
    name VARCHAR(255) NOT NULL COMMENT '名称',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    
    UNIQUE KEY uk_internal_id (internal_id),
    UNIQUE KEY uk_tenant_public_id (tenant_id, public_id),
    KEY idx_tenant_id (tenant_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
```

## 常见场景

### 场景 1：主键使用 Ulid128

```java
@Data
@TableName("bc_example")
public class BcExample {
    
    /**
     * 使用 Ulid128 作为主键
     */
    @TableId(value = "id", type = IdType.INPUT)
    @TableField(typeHandler = Ulid128BinaryTypeHandler.class)
    private Ulid128 id;
    
    private String name;
}
```

**对应表结构**：
```sql
CREATE TABLE bc_example (
    id BINARY(16) PRIMARY KEY COMMENT '主键（ULID128）',
    name VARCHAR(255) NOT NULL
) ENGINE=InnoDB;
```

### 场景 2：普通字段使用 Ulid128

```java
@Data
@TableName("bc_example")
public class BcExample {
    
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    
    /**
     * 关联的其他实体的内部ID
     */
    @TableField(typeHandler = Ulid128BinaryTypeHandler.class)
    private Ulid128 relatedEntityId;
}
```

### 场景 3：可空的 Ulid128 字段

```java
@Data
@TableName("bc_example")
public class BcExample {
    
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    
    /**
     * 可选的关联ID（允许为 NULL）
     */
    @TableField(typeHandler = Ulid128BinaryTypeHandler.class)
    private Ulid128 optionalRelatedId;  // 可以为 null
}
```

**对应表结构**：
```sql
CREATE TABLE bc_example (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    optional_related_id BINARY(16) NULL  -- 允许 NULL
) ENGINE=InnoDB;
```

## 错误示例

### ❌ 错误 1：缺少 TypeHandler 注解

```java
// 错误：缺少 @TableField 注解
private Ulid128 internalId;
```

**后果**：运行时抛出异常：
```
MyBatisSystemException: Type handler was null on parameter mapping for property 'internalId'
```

### ❌ 错误 2：使用错误的 TypeHandler

```java
// 错误：对 BINARY(16) 字段使用了字符串 TypeHandler
@TableField(typeHandler = Ulid128Char26TypeHandler.class)
private Ulid128 internalId;
```

**后果**：数据写入错误或读取失败。

### ❌ 错误 3：Import 错误的 TypeHandler

```java
// 错误：使用了 internal 包的 TypeHandler（不应该在业务代码中使用）
import com.bluecone.app.id.internal.mybatis.Ulid128BinaryTypeHandler;

@TableField(typeHandler = Ulid128BinaryTypeHandler.class)
private Ulid128 internalId;
```

**正确做法**：使用 API 包的 TypeHandler：
```java
import com.bluecone.app.id.mybatis.Ulid128BinaryTypeHandler;
```

## TypeHandler 选择指南

| 数据库字段类型 | Java 类型 | TypeHandler | 使用场景 |
|--------------|----------|-------------|---------|
| `BINARY(16)` | `Ulid128` | `Ulid128BinaryTypeHandler` | **推荐**：高性能，节省空间 |
| `CHAR(26)` 或 `VARCHAR(26)` | `Ulid128` | `Ulid128Char26TypeHandler` | 过渡期或需要可读性的场景 |

**推荐使用 `BINARY(16)` + `Ulid128BinaryTypeHandler`**，因为：
- 存储空间更小（16 字节 vs 26 字节）
- 索引效率更高
- 查询性能更好

## 查询示例

### 使用 MyBatis Plus 查询

```java
@Service
public class ExampleService {
    
    @Autowired
    private BcExampleMapper exampleMapper;
    
    public BcExample findByInternalId(Ulid128 internalId) {
        return exampleMapper.selectOne(
            new LambdaQueryWrapper<BcExample>()
                .eq(BcExample::getInternalId, internalId)
        );
    }
    
    public List<BcExample> findByInternalIds(List<Ulid128> internalIds) {
        return exampleMapper.selectList(
            new LambdaQueryWrapper<BcExample>()
                .in(BcExample::getInternalId, internalIds)
        );
    }
}
```

### 使用 XML Mapper 查询

```xml
<mapper namespace="com.bluecone.app.xxx.dao.mapper.BcExampleMapper">
    
    <!-- 根据 internal_id 查询 -->
    <select id="findByInternalId" resultType="com.bluecone.app.xxx.dao.entity.BcExample">
        SELECT * FROM bc_example
        WHERE internal_id = #{internalId, typeHandler=com.bluecone.app.id.mybatis.Ulid128BinaryTypeHandler}
    </select>
    
    <!-- 批量查询 -->
    <select id="findByInternalIds" resultType="com.bluecone.app.xxx.dao.entity.BcExample">
        SELECT * FROM bc_example
        WHERE internal_id IN
        <foreach collection="internalIds" item="id" open="(" separator="," close=")">
            #{id, typeHandler=com.bluecone.app.id.mybatis.Ulid128BinaryTypeHandler}
        </foreach>
    </select>
    
</mapper>
```

## 测试验证

### 单元测试示例

```java
@SpringBootTest
class BcExampleMapperTest {
    
    @Autowired
    private BcExampleMapper exampleMapper;
    
    @Autowired
    private IdService idService;
    
    @Test
    void testInsertAndQuery() {
        // 1. 准备数据
        BcExample example = new BcExample();
        example.setTenantId(1L);
        example.setInternalId(idService.generateUlid128());
        example.setPublicId("exp_" + example.getInternalId().toString());
        example.setName("测试实体");
        
        // 2. 插入数据
        int rows = exampleMapper.insert(example);
        assertThat(rows).isEqualTo(1);
        assertThat(example.getId()).isNotNull();
        
        // 3. 查询验证
        BcExample found = exampleMapper.selectById(example.getId());
        assertThat(found).isNotNull();
        assertThat(found.getInternalId()).isEqualTo(example.getInternalId());
        assertThat(found.getPublicId()).isEqualTo(example.getPublicId());
        
        // 4. 根据 internal_id 查询
        BcExample foundByInternalId = exampleMapper.selectOne(
            new LambdaQueryWrapper<BcExample>()
                .eq(BcExample::getInternalId, example.getInternalId())
        );
        assertThat(foundByInternalId).isNotNull();
        assertThat(foundByInternalId.getId()).isEqualTo(example.getId());
    }
}
```

## 故障排查

### 问题 1：插入时报错 "Type handler was null"

**原因**：实体类字段缺少 `@TableField(typeHandler = Ulid128BinaryTypeHandler.class)` 注解。

**解决方法**：添加注解并导入正确的类。

### 问题 2：查询返回的 internalId 为 null

**原因**：
1. 数据库字段确实为 NULL
2. TypeHandler 配置错误
3. 字段名映射错误

**排查步骤**：
1. 检查数据库数据：`SELECT HEX(internal_id) FROM bc_example WHERE id = ?`
2. 检查实体类注解是否正确
3. 检查字段名是否与数据库列名匹配（MyBatis Plus 默认使用驼峰转下划线）

### 问题 3：数据库中存储的数据格式错误

**原因**：使用了错误的 TypeHandler（如对 BINARY 字段使用了 Char26TypeHandler）。

**解决方法**：
1. 确认数据库字段类型
2. 使用正确的 TypeHandler
3. 清理错误数据并重新插入

## 迁移指南

### 从 Long ID 迁移到 Ulid128

如果现有表使用 Long 作为主键，需要迁移到 Ulid128：

```sql
-- 1. 添加新字段
ALTER TABLE bc_example
    ADD COLUMN internal_id BINARY(16) NULL COMMENT '内部主键 ULID128',
    ADD COLUMN public_id VARCHAR(40) NULL COMMENT '对外ID';

-- 2. 为历史数据生成 ID（通过应用层脚本）
-- 见：docs/engineering/ID-MIGRATION-GUIDE.md

-- 3. 添加唯一索引
ALTER TABLE bc_example
    ADD UNIQUE KEY uk_internal_id (internal_id),
    ADD UNIQUE KEY uk_tenant_public_id (tenant_id, public_id);

-- 4. 后续新数据自动生成
```

**应用层代码修改**：

```java
// 修改前
@Data
@TableName("bc_example")
public class BcExample {
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
}

// 修改后
@Data
@TableName("bc_example")
public class BcExample {
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    
    @TableField(typeHandler = Ulid128BinaryTypeHandler.class)
    private Ulid128 internalId;
    
    private String publicId;
}
```

## 参考资料

- [ID 体系架构文档](APP-ID-V2-IMPLEMENTATION-SUMMARY.md)
- [Ulid128 MyBatis 集成](../../app-id/README-ulid-mybatis.md)
- [门店创建 ID 全链路](STORE-CREATE-ID-END2END.md)
- [Public ID 治理](PUBLIC-ID-GOVERNANCE.md)

## 更新记录

| 日期 | 版本 | 说明 |
|------|------|------|
| 2025-12-21 | 1.0 | 初始版本，基于实际问题总结最佳实践 |

