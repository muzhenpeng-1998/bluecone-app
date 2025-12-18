# BlueCone Platform Starters

## 概述

BlueCone Platform Starters 提供了统一的平台能力接入方式，通过 Spring Boot AutoConfiguration 机制，让业务模块"开箱即用"核心能力。

## 组件清单与职责边界

### 1. app-platform-bom

**职责**：统一版本管理（BOM - Bill of Materials）

- 管理平台强控的依赖版本（ArchUnit、Caffeine、ULID 等）
- 不覆盖 Spring Boot BOM 已管理的版本
- 确保全仓库依赖版本一致性

**使用场景**：所有业务模块通过父 POM 或 dependencyManagement 引入

---

### 2. app-platform-starter

**职责**：核心能力聚合（ID、Resolver、ContextKit、Cache Invalidation）

**包含能力**：
- **ID Generation**：ULID/Snowflake ID 生成服务
- **Public ID Resolution**：公开 ID 与内部 ID 映射解析
- **ContextKit**：二级缓存（Caffeine + Redis）、版本校验、快照提供
- **Cache Invalidation**：缓存失效事件发布与订阅
- **Cache Epoch**：基于 Epoch 的缓存键管理
- **Idempotency**：幂等性记录与校验

**默认行为**：
- ID、Resolver、ContextKit 默认启用（matchIfMissing=true）
- Cache Invalidation 默认关闭（需显式启用）

**不包含**：
- Ops Console（独立在 app-platform-starter-ops）
- 业务特定配置（Store/User/Inventory Context）

---

### 3. app-platform-starter-ops

**职责**：运维控制台能力（独立 starter，默认关闭）

**包含能力**：
- Ops Console 路由（/ops/**）
- 缓存失效观测
- 系统诊断工具

**默认行为**：
- 默认关闭（`bluecone.ops.enabled=false`）
- 避免污染业务服务

**路由隔离**：
- Ops 路由（/ops/**, /actuator/**）硬排除，不受业务 context middleware 影响

---

### 4. app-platform-archkit

**职责**：架构治理规则库（ArchUnit）

**包含规则**：
- **LayerRules**：分层架构规则（domain/application/infra 依赖约束）
- **IdRules**：ID 生成规则（禁止直接 new ULID/Snowflake）
- **ContextRules**：数据访问规则（Controller 不直接访问 Mapper）
- **EventRules**：事件发布规则（必须通过 DomainEventPublisher）
- **NamingRules**：命名规范（public_id/internal_id 字段约束）

**使用方式**：
1. 业务模块添加 test scope 依赖
2. 创建薄测试类继承 `AbstractArchTestTemplate`
3. 调用规则检查方法

---

## Maven 引入方式

### 1. 在根 pom.xml 引入 BOM

```xml
<dependencyManagement>
    <dependencies>
        <!-- Spring Boot BOM -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-dependencies</artifactId>
            <version>${spring.boot.version}</version>
            <type>pom</type>
            <scope>import</scope>
        </dependency>

        <!-- BlueCone Platform BOM -->
        <dependency>
            <groupId>com.bluecone</groupId>
            <artifactId>app-platform-bom</artifactId>
            <version>${project.version}</version>
            <type>pom</type>
            <scope>import</scope>
        </dependency>
    </dependencies>
</dependencyManagement>
```

### 2. 业务模块引入核心 Starter

```xml
<dependencies>
    <!-- 核心能力 Starter -->
    <dependency>
        <groupId>com.bluecone</groupId>
        <artifactId>app-platform-starter</artifactId>
    </dependency>

    <!-- 可选：Ops Console Starter（默认关闭） -->
    <dependency>
        <groupId>com.bluecone</groupId>
        <artifactId>app-platform-starter-ops</artifactId>
    </dependency>

    <!-- 可选：ArchUnit 治理规则（test scope） -->
    <dependency>
        <groupId>com.bluecone</groupId>
        <artifactId>app-platform-archkit</artifactId>
        <scope>test</scope>
    </dependency>
</dependencies>
```

---

## 默认配置模板

### application.yml

```yaml
bluecone:
  # ========== ID Generation ==========
  id:
    enabled: true  # 默认启用
    ulid:
      mode: STRIPED  # STRICT | STRIPED
      stripes: 16
      metrics-enabled: true
    long:
      enabled: false  # Snowflake long ID（按需启用）
      node-id: 1
      epoch-millis: 1609459200000
    public-id:
      enabled: true  # 公开 ID 编码

  # ========== Public ID Resolution ==========
  idresolve:
    enabled: true  # 默认启用
    cache-ttl: 3600s
    cache-max-size: 100000

  # ========== ContextKit ==========
  contextkit:
    enabled: true  # 默认启用
    version-check-window: 300s
    version-check-sample-rate: 0.01

  # ========== Cache Invalidation ==========
  cache:
    invalidation:
      enabled: false  # 默认关闭，需显式启用
      transport: OUTBOX  # OUTBOX | REDIS_PUBSUB | IN_PROCESS
      redis-topic: bluecone:cache:invalidation
      max-keys-per-event: 1000
    invalidation:
      protection:
        enabled: false  # 风暴保护（按需启用）
        coalesce-threshold-per-minute: 100
        storm-threshold-per-minute: 1000
        storm-cooldown: 60s
        max-keys-per-event: 1000
        redis-storm-enabled: true
        redis-epoch-enabled: true

  # ========== Ops Console ==========
  ops:
    enabled: false  # 默认关闭
    console:
      title: "BlueCone Ops Console"
      base-path: /ops

  # ========== Instance ID ==========
  instance:
    id: ${HOSTNAME:localhost}  # 实例标识（用于日志追踪）
```

---

## 能力开关表

| 配置项 | 默认值 | 说明 |
|--------|--------|------|
| `bluecone.id.enabled` | `true` | ID 生成服务 |
| `bluecone.idresolve.enabled` | `true` | Public ID 解析 |
| `bluecone.contextkit.enabled` | `true` | ContextKit 缓存与版本校验 |
| `bluecone.cache.invalidation.enabled` | `false` | 缓存失效事件 |
| `bluecone.cache.invalidation.protection.enabled` | `false` | 缓存失效风暴保护 |
| `bluecone.ops.enabled` | `false` | Ops Console |
| `bluecone.id.long.enabled` | `false` | Snowflake long ID |
| `bluecone.id.public-id.enabled` | `true` | 公开 ID 编码 |
| `bluecone.id.jackson.enabled` | `true` | Jackson 序列化支持 |

---

## 常见问题

### 1. 为什么 Bean 没有装配？

**排查步骤**：

1. **检查依赖是否引入**
   - 某些能力依赖 Redis/MyBatis/Web，需确保相关依赖存在
   - 使用 `@ConditionalOnClass` 检查 classpath

2. **检查配置开关**
   - 查看 `application.yml` 中对应的 `enabled` 配置
   - 某些能力默认关闭（如 Cache Invalidation、Ops）

3. **查看启动日志**
   - 开启 debug 日志：`logging.level.org.springframework.boot.autoconfigure=DEBUG`
   - 查找 `ConditionalOnProperty` 匹配失败原因

4. **检查 AutoConfiguration 顺序**
   - 某些配置依赖其他 Bean 先装配（如 `@ConditionalOnBean`）

---

### 2. Cache Invalidation 不生效？

**常见原因**：

1. **未显式启用**
   ```yaml
   bluecone.cache.invalidation.enabled: true
   ```

2. **缺少依赖 Bean**
   - 需要 `ContextCache`（contextKitCache）
   - 需要 `CacheEpochProvider`
   - 需要 `DomainEventPublisher`（OUTBOX 模式）
   - 需要 `StringRedisTemplate`（REDIS_PUBSUB 模式）

3. **Transport 配置错误**
   - OUTBOX：需要 outbox 表与 scheduler
   - REDIS_PUBSUB：需要 Redis 连接

---

### 3. Ops Console 无法访问？

**检查清单**：

1. **是否启用**
   ```yaml
   bluecone.ops.enabled: true
   ```

2. **是否引入 starter-ops**
   ```xml
   <dependency>
       <groupId>com.bluecone</groupId>
       <artifactId>app-platform-starter-ops</artifactId>
   </dependency>
   ```

3. **Web 环境是否存在**
   - Ops 依赖 Spring Web，需引入 `spring-boot-starter-web`

4. **路由是否冲突**
   - Ops 路由默认 `/ops/**`，确保无业务路由冲突

---

### 4. ArchUnit 测试失败？

**常见原因**：

1. **规则过于严格**
   - 某些规则可能不适用于特定模块
   - 可选择性应用规则，不必全部执行

2. **历史代码不符合规范**
   - 新模块严格执行
   - 老模块逐步重构

3. **包结构不匹配**
   - 规则基于包命名约定（domain/application/infra）
   - 调整规则或重构包结构

---

### 5. 如何调试 AutoConfiguration？

**Debug 技巧**：

1. **查看条件匹配报告**
   ```yaml
   logging:
     level:
       org.springframework.boot.autoconfigure: DEBUG
   ```

2. **使用 Spring Boot Actuator**
   ```yaml
   management:
     endpoints:
       web:
         exposure:
           include: conditions
   ```
   访问：`/actuator/conditions`

3. **查看 AutoConfiguration 导入**
   - 检查 `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`
   - 确认类路径正确

---

## 新模块接入 Checklist

参见：[NEW-MODULE-CHECKLIST.md](./NEW-MODULE-CHECKLIST.md)

---

## 相关文档

- [ID Governance](../arch/ID-GOVERNANCE.md)
- [Cache Invalidation Events](../engineering/CACHE-INVALIDATION-EVENTS.md)
- [Cache Invalidation Auto Protection](../engineering/CACHE-INVALIDATION-AUTO-PROTECTION.md)
- [Outbox Eventing](../engineering/OUTBOX-EVENTING.md)
- [Event Consume Idempotency](../engineering/EVENT-CONSUME-IDEMPOTENCY.md)

---

## 版本历史

- **v1.0.0** (2025-12-14): 初始版本，包含核心 Starter、Ops Starter、ArchKit

