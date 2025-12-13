# BlueCone Platform Starter & BOM

## 1. 组件清单

通过引入 BlueCone Platform Starter，你可以在任意 Spring Boot 3 / Java 21 服务中快速获得以下基线能力：

- **ID 能力**
  - `IdService`：ULID 基于时间有序 ID 生成。
  - `PublicIdCodec`：公共 ID 编码/解码（隐藏内部 ID 格式）。
  - MyBatis `Ulid128` TypeHandler（binary/char26）。
  - Jackson 模块注册（TypedId/PublicId 序列化支持）。
- **幂等 & 幂等创建模板**
  - IdempotencyTemplate：通用幂等操作模板。
  - IdempotentCreateTemplate：幂等“创建接口”模板（create + outbox）。
- **事件 / Outbox / Consume（事件驱动）**
  - Outbox 写库 + EventDispatcher（在 app-infra 中实现）。
  - EventHandlerTemplate：消费侧幂等模板（基于 EventDedupRepository）。
- **Observability**
  - Micrometer 指标集成（ID、Outbox/Consume、幂等）。
- **Ops Console（只读运维）**
  - `/ops/console` Minimal Ops Console（Summary + Drill-down）。
  - `/ops/api/**` 只读 JSON API，带强访问控制。
- **治理（ArchUnit）**
  - 可独立引入平台 ArchUnit 规则模块，对新模块做结构治理（推荐另见 `app-platform-archtest`）。

> 注意：starter 本身不包含业务代码，只是聚合已有模块并导出它们的 AutoConfiguration。

## 2. 引入方式

### 2.1 引入 BOM

在你的服务 `pom.xml` 中添加：

```xml
<dependencyManagement>
  <dependencies>
    <dependency>
      <groupId>com.bluecone</groupId>
      <artifactId>app-platform-bom</artifactId>
      <version>1.0.0-SNAPSHOT</version>
      <type>pom</type>
      <scope>import</scope>
    </dependency>
  </dependencies>
</dependencyManagement>
```

### 2.2 引入 Starter

```xml
<dependencies>
  <dependency>
    <groupId>com.bluecone</groupId>
    <artifactId>app-platform-starter</artifactId>
  </dependency>
</dependencies>
```

这样即可一次性引入：

- `app-id`
- `app-core`
- `app-infra`
- `app-security`
- `app-ops`

以及它们的 AutoConfiguration。

## 3. 最小配置示例

仅启用 ID + PublicId + 幂等创建（关闭 outbox/consume/ops）的最小配置：

```yaml
bluecone:
  id:
    enabled: true
    public-id:
      enabled: true
  idempotency:
    enabled: true
  create:
    enabled: true

  eventing:
    outbox:
      enabled: false
    consume:
      enabled: false

  observability:
    enabled: false

  ops:
    console:
      enabled: false
```

在这个配置下：

- `IdService` + `PublicIdCodec` + 幂等模板可用。
- Outbox/Consume/Observability/Ops Console 都不会装配。

## 4. 生产推荐配置清单

建议的生产环境基线（根据实际模块适度调优）：

```yaml
bluecone:
  id:
    enabled: true
    public-id:
      enabled: true

  idempotency:
    enabled: true

  create:
    enabled: true

  eventing:
    outbox:
      enabled: true
    consume:
      enabled: true

  observability:
    enabled: true

  ops:
    console:
      enabled: true
      authMode: ADMIN_AND_TOKEN
      requiredRole: PLATFORM_ADMIN
      token: ${BLUECONE_OPS_TOKEN}
      exportEnabled: true
      exportMaxRows: 2000
      exportExposePayload: false
      rateLimit:
        opsRpm: 120
        exportRpm: 10
```

要点：

- Outbox/Consume：在所有需要可靠事件驱动的服务中开启。
- Observability：建议接入 Prometheus/Grafana；Micrometer 相关 Bean 由 starter 按需装配。
- Ops Console：
  - 仅在受控环境开启。
  - AuthMode 建议 `ADMIN_AND_TOKEN`，token 通���环境变量注入。
  - `exportExposePayload` 默认 `false`，避免导出敏感数据。

## 5. 新模块接入 Checklist

当你新建一个模块/服务时，可以按照以下步骤接入平台基线：

1. **pom 依赖**
   - 引入 `app-platform-bom`（dependencyManagement）。
   - 引入 `app-platform-starter`。
2. **数据库设计**
   - 需要公共 ID 的表：使用内部 ID + public_id 模式。
     - `internal_id`：BINARY(16) / BIGINT 等内部主键。
     - `public_id`：VARCHAR(32) 唯一约束，对外暴露使用。
3. **创建接口**
   - 使用 `IdempotentCreateTemplate` 实现典型 create 接口（例如 `createOrder`、`createStore`）。
   - 保证：
     - 幂等 key 明确（租户 + bizType + idemKey）。
     - Outbox 事件在事务中写入。
4. **事件发布 & 消费**
   - 发布：
     - 使用事务 Outbox 写库（在 app-infra 中实现的 DomainEventPublisher）。
   - 消费：
     - 使用 `EventHandlerTemplate` 实现消费处理，统一处理幂等和重试。
     - 确定好 `consumer_group` 命名（全大写 + 下划线）。
5. **治理（ArchUnit）**
   - 在模块 `pom.xml` 中添加 test 依赖（建议未来使用 `app-platform-archtest`）：
     ```xml
     <dependency>
       <groupId>com.bluecone</groupId>
       <artifactId>app-platform-archtest</artifactId>
       <scope>test</scope>
     </dependency>
     ```
   - 在该模块 test 目录中写一个薄壳 ArchUnit 测试，指向本模块包范围，使平台规则生效。
6. **Ops 与观测**
   - 若需要在该服务暴露 Ops Console：
     - 配置 `bluecone.ops.console.enabled=true`。
     - 按 Step 16 文档设置 RBAC + Token + RateLimit。

## 6. 常见坑与修复

1. **依赖缺失导致 auto-config 未生效**
   - 症状：IdService / EventHandlerTemplate / Outbox 相关 Bean 未装配。
   - 检查点：
     - 是否引入 `app-platform-starter`；
     - 是否有对应的 `@AutoConfiguration` 所依赖的 class 在 classpath（例如 MyBatis、DataSource、MeterRegistry）。
     - 对应的 `bluecone.*.enabled` 是否被错误设置为 `false`。

2. **Ops Console 开启但访问 404**
   - 检查：
     - `bluecone.ops.console.enabled` 是否为 `true`；
     - 是否配置了 token 且请求头中携带 `X-Ops-Token`；
     - 安全模式（AuthMode）是否需要平台管理员角色。

3. **导出过多导致响应变慢**
   - 平台配置中已有 `exportMaxRows` 与 `exportMaxPageSize` 控制导出总行数与单页大小。
   - 若仍有性能压力，可：
     - 下调 `exportMaxRows`；
     - 提升 DB 索引质量（确保按 id/status 条件有覆盖索引）。

4. **ArchUnit 规则未执行**
   - 确认 test 依赖已经引入 `app-platform-archtest`；
   - 确认存在至少一个 @AnalyzeClasses 的测试类来触发规则。

> 未来新增的基础设施能力（例如更多的 metrics/export 类型）应优先接入平台 starter 与 BOM，并更新本文档，以保持所有服务的基线能力一致。

