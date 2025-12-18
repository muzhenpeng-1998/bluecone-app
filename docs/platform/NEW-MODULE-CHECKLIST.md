# 新模块接入 Checklist

## 概述

新业务模块接入 BlueCone 平台时，必须完成以下检查项，确保符合平台治理规范。

---

## 1. 依赖管理

### 1.1 引入 Platform BOM

在模块 `pom.xml` 中，确保父 POM 已引入 `app-platform-bom`：

```xml
<parent>
    <groupId>com.bluecone</groupId>
    <artifactId>bluecone-app</artifactId>
    <version>1.0.0-SNAPSHOT</version>
</parent>
```

### 1.2 引入核心 Starter

```xml
<dependencies>
    <dependency>
        <groupId>com.bluecone</groupId>
        <artifactId>app-platform-starter</artifactId>
    </dependency>
</dependencies>
```

### 1.3 引入 ArchKit（测试）

```xml
<dependency>
    <groupId>com.bluecone</groupId>
    <artifactId>app-platform-archkit</artifactId>
    <scope>test</scope>
</dependency>
```

---

## 2. ID 管理规范

### 2.1 使用 IdService 生成 ID

**禁止**：直接 `new UlidFactory()` 或 `new SnowflakeLongIdGenerator()`

**正确**：
```java
@Service
public class MyService {
    private final IdService idService;

    public void createEntity() {
        String id = idService.nextUlid();  // 或 nextLongId()
        // ...
    }
}
```

### 2.2 Public ID 与 Internal ID 分离

- **Internal ID**：数据库主键，ULID/Long ID，不对外暴露
- **Public ID**：API 返回的 ID，经过编码（Base62/Base58），可解码回 Internal ID

**字段命名规范**：
- Internal ID：`id`、`internalId`、`*Id`（仅内部使用）
- Public ID：`publicId`、`*PublicId`（API DTO 使用）

### 2.3 注册 Public ID 映射

在创建实体时，注册 Public ID 映射：

```java
@Service
public class MyService {
    private final IdService idService;
    private final PublicIdRegistrar publicIdRegistrar;

    public void createEntity(String entityType) {
        String internalId = idService.nextUlid();
        String publicId = idService.nextPublicId(entityType);
        
        // 注册映射
        publicIdRegistrar.register(publicId, internalId, entityType);
        
        // 保存实体...
    }
}
```

---

## 3. Public ID Resolution

### 3.1 使用 PublicIdResolver 解析

在 Controller/Gateway 层，使用 `PublicIdResolver` 解析 Public ID：

```java
@RestController
public class MyController {
    private final PublicIdResolver publicIdResolver;

    @GetMapping("/api/entities/{publicId}")
    public EntityResponse getEntity(@PathVariable String publicId) {
        String internalId = publicIdResolver.resolve(publicId, "entity-type");
        // 使用 internalId 查询...
    }
}
```

### 3.2 配置 Resolver 缓存

```yaml
bluecone:
  idresolve:
    enabled: true
    cache-ttl: 3600s
    cache-max-size: 100000
```

---

## 4. ContextKit 使用

### 4.1 使用二级缓存

```java
@Service
public class MyService {
    private final ContextCache contextKitCache;

    public MyEntity getEntity(String id) {
        return contextKitCache.get(
            "entity:" + id,
            MyEntity.class,
            () -> loadFromDb(id)
        );
    }
}
```

### 4.2 使用 SnapshotProvider

```java
@Service
public class MyService {
    private final SnapshotProvider snapshotProvider;

    public StoreSnapshot getSnapshot(String storeId) {
        return snapshotProvider.getStoreSnapshot(storeId);
    }
}
```

---

## 5. Cache Invalidation

### 5.1 启用缓存失效

```yaml
bluecone:
  cache:
    invalidation:
      enabled: true
      transport: OUTBOX  # 或 REDIS_PUBSUB
```

### 5.2 发布失效事件

```java
@Service
public class MyService {
    private final CacheInvalidationPublisher invalidationPublisher;

    public void updateEntity(String id) {
        // 更新实体...
        
        // 发布失效事件
        invalidationPublisher.invalidate("entity:" + id);
    }
}
```

### 5.3 启用风暴保护（可选）

```yaml
bluecone:
  cache:
    invalidation:
      protection:
        enabled: true
        coalesce-threshold-per-minute: 100
        storm-threshold-per-minute: 1000
```

---

## 6. Event Publishing

### 6.1 使用 DomainEventPublisher

**禁止**：直接操作 `OutboxMapper` 或 `OutboxRepository`

**正确**：
```java
@Service
public class MyService {
    private final DomainEventPublisher eventPublisher;

    public void createOrder(Order order) {
        // 保存订单...
        
        // 发布领域事件
        OrderCreatedEvent event = new OrderCreatedEvent(order.getId());
        eventPublisher.publish(event);
    }
}
```

### 6.2 事件消费幂等性

使用 `EventConsumeIdempotencyService`：

```java
@Component
public class OrderEventConsumer {
    private final EventConsumeIdempotencyService idempotencyService;

    @EventListener
    public void onOrderCreated(OrderCreatedEvent event) {
        idempotencyService.executeIdempotent(
            event.getEventId(),
            "order-created",
            () -> {
                // 处理事件...
            }
        );
    }
}
```

---

## 7. 分层架构规范

### 7.1 包结构

```
com.bluecone.app.{module}
├── domain/          # 领域层（纯业务逻辑）
│   ├── entity/
│   ├── repository/  # 仓储接口
│   └── service/
├── application/     # 应用层（编排）
│   └── service/
├── infra/           # 基础设施层（实现）
│   └── repository/
└── gateway/         # 网关层（API）
    └── controller/
```

### 7.2 依赖规则

- **Domain** 不依赖 Application/Infra/Gateway
- **Application** 不依赖 Infra 实现（只依赖接口）
- **Gateway** 不直接访问 Mapper/Repository（通过 Application Service）

---

## 8. ArchUnit 测试

### 8.1 创建 ArchTest 类

在 `src/test/java/{module}` 下创建：

```java
package com.bluecone.app.mymodule;

import com.bluecone.app.platform.archkit.*;
import com.tngtech.archunit.core.domain.JavaClasses;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class MyModuleArchTest extends AbstractArchTestTemplate {

    private static JavaClasses classes;

    @BeforeAll
    static void setUp() {
        classes = new MyModuleArchTest().importClasses("com.bluecone.app.mymodule");
    }

    @Test
    void shouldFollowLayerRules() {
        LayerRules.checkAll(classes);
    }

    @Test
    void shouldFollowIdRules() {
        IdRules.checkAll(classes);
    }

    @Test
    void shouldFollowContextRules() {
        ContextRules.checkAll(classes);
    }

    @Test
    void shouldFollowEventRules() {
        EventRules.checkAll(classes);
    }

    @Test
    void shouldFollowNamingRules() {
        NamingRules.checkAll(classes);
    }
}
```

### 8.2 运行测试

```bash
mvn test -Dtest=MyModuleArchTest
```

---

## 9. 配置管理

### 9.1 模块配置文件

在 `src/main/resources/application-{module}.yml` 中定义模块特定配置：

```yaml
bluecone:
  mymodule:
    enabled: true
    feature-x: true
```

### 9.2 配置属性类

```java
@ConfigurationProperties(prefix = "bluecone.mymodule")
public class MyModuleProperties {
    private boolean enabled = true;
    private boolean featureX = false;
    // getters/setters
}
```

---

## 10. 日志与监控

### 10.1 使用结构化日志

```java
@Slf4j
@Service
public class MyService {
    public void doSomething(String id) {
        log.info("Processing entity: id={}", id);
        // ...
        log.error("Failed to process entity: id={}, error={}", id, error.getMessage());
    }
}
```

### 10.2 添加 Metrics（可选）

```java
@Service
public class MyService {
    private final MeterRegistry meterRegistry;

    public void doSomething() {
        Counter counter = meterRegistry.counter("mymodule.operation.count");
        counter.increment();
    }
}
```

---

## 11. 数据库规范

### 11.1 表命名

- 使用 `bc_` 前缀（BlueCone）
- 小写 + 下划线分隔：`bc_my_entity`

### 11.2 字段规范

- **主键**：`id` (BIGINT/VARCHAR)，使用 ULID
- **创建时间**：`created_at` (DATETIME)
- **更新时间**：`updated_at` (DATETIME)
- **版本号**：`version` (INT)，乐观锁
- **删除标记**：`deleted` (TINYINT)，逻辑删除

### 11.3 索引规范

- 主键索引：`PRIMARY KEY (id)`
- 唯一索引：`UNIQUE KEY uk_{field}({field})`
- 普通索引：`KEY idx_{field}({field})`

---

## 12. API 规范

### 12.1 RESTful 路由

```
GET    /api/{module}/{resource}           # 列表
GET    /api/{module}/{resource}/{id}      # 详情
POST   /api/{module}/{resource}           # 创建
PUT    /api/{module}/{resource}/{id}      # 更新
DELETE /api/{module}/{resource}/{id}      # 删除
```

### 12.2 响应格式

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "publicId": "abc123",
    "name": "Example"
  }
}
```

### 12.3 错误码

- `0`：成功
- `400`：请求参数错误
- `404`：资源不存在
- `500`：服务器内部错误

---

## 13. 测试规范

### 13.1 单元测试

- 覆盖率目标：≥ 70%
- 使用 JUnit 5 + Mockito

### 13.2 集成测试

- 使用 Testcontainers（MySQL/Redis）
- 测试类命名：`*IT.java`

### 13.3 E2E 测试

- 测试关键业务流程
- 测试类命名：`*E2ETest.java`

---

## 14. 文档规范

### 14.1 README.md

每个模块必须包含 `README.md`，说明：
- 模块职责
- 核心功能
- 依赖关系
- 配置说明

### 14.2 代码注释

- 类级别：说明职责与使用场景
- 方法级别：说明参数、返回值、异常
- 复杂逻辑：添加行内注释

---

## 15. 验收标准

### 15.1 编译通过

```bash
mvn clean compile
```

### 15.2 测试通过

```bash
mvn test
```

### 15.3 ArchUnit 通过

```bash
mvn test -Dtest=*ArchTest
```

### 15.4 代码审查

- 通过 PR Review
- 符合团队编码规范

---

## 16. 常见错误

### 16.1 直接使用 ULID/Snowflake

❌ **错误**：
```java
String id = UlidCreator.getUlid().toString();
```

✅ **正确**：
```java
String id = idService.nextUlid();
```

### 16.2 Controller 直接访问 Mapper

❌ **错误**：
```java
@RestController
public class MyController {
    @Autowired
    private MyMapper myMapper;
}
```

✅ **正确**：
```java
@RestController
public class MyController {
    @Autowired
    private MyApplicationService myApplicationService;
}
```

### 16.3 直接操作 Outbox 表

❌ **错误**：
```java
outboxMapper.insert(outboxEvent);
```

✅ **正确**：
```java
eventPublisher.publish(domainEvent);
```

---

## 17. 参考资料

- [Platform Starters](./PLATFORM-STARTERS.md)
- [ID Governance](../arch/ID-GOVERNANCE.md)
- [Cache Invalidation Events](../engineering/CACHE-INVALIDATION-EVENTS.md)
- [Outbox Eventing](../engineering/OUTBOX-EVENTING.md)

---

## 18. 联系方式

如有疑问，请联系平台团队或查阅相关文档。

