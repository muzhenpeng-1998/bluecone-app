# 可观测性落地实施总结

本文档总结 bluecone-app 可观测性最小集合的实施情况。

## 实施概览

### 目标

在 bluecone-app 单体多模块中落地可观测性最小集合：

1. ✅ JSON 结构化日志（stdout），包含 traceId/tenantId/orderId/outboxEventId/idempotencyKey 等关键字段
2. ✅ Spring Boot Actuator + Prometheus 指标端点 `/actuator/prometheus`
3. ✅ 为 outbox/consumer/补偿任务增加 Micrometer 自定义指标
4. ✅ 接入 OpenTelemetry（支持 JVM Javaagent 方式和 SDK 方案）
5. ✅ 安全：Actuator 端点只允许内网/管理员访问

### 实施时间

- 开始时间：2024-12-19
- 完成时间：2024-12-19
- 总耗时：约 2 小时

---

## 实施清单

### 1. 依赖管理

#### 根 pom.xml

添加了以下依赖版本管理：

```xml
<properties>
    <micrometer.version>1.12.5</micrometer.version>
    <opentelemetry.version>1.36.0</opentelemetry.version>
</properties>

<dependencyManagement>
    <dependencies>
        <!-- Micrometer and Prometheus -->
        <dependency>
            <groupId>io.micrometer</groupId>
            <artifactId>micrometer-registry-prometheus</artifactId>
            <version>${micrometer.version}</version>
        </dependency>
        
        <!-- OpenTelemetry (optional SDK approach) -->
        <dependency>
            <groupId>io.opentelemetry</groupId>
            <artifactId>opentelemetry-api</artifactId>
            <version>${opentelemetry.version}</version>
        </dependency>
        <!-- ... 其他 OpenTelemetry 依赖 ... -->
    </dependencies>
</dependencyManagement>
```

#### app-application/pom.xml

添加了 Prometheus 依赖：

```xml
<dependency>
    <groupId>io.micrometer</groupId>
    <artifactId>micrometer-registry-prometheus</artifactId>
</dependency>
```

### 2. 日志配置

#### logback-spring.xml

更新了 JSON 日志配置，包含以下 MDC 字段：

- `traceId`：分布式追踪 ID
- `spanId`：Span ID
- `requestId`：HTTP 请求 ID
- `tenantId`：租户 ID
- `userId`：用户 ID
- `orderId`：订单 ID
- `outboxEventId`：Outbox 事件 ID
- `idempotencyKey`：幂等键

**示例日志输出：**

```json
{
  "@timestamp": "2024-12-19T10:30:45.123Z",
  "level": "INFO",
  "message": "Order created successfully",
  "traceId": "0af7651916cd43dd8448eb211c80319c",
  "spanId": "b7ad6b7169203331",
  "requestId": "550e8400-e29b-41d4-a716-446655440000",
  "tenantId": "1001",
  "orderId": "789012"
}
```

### 3. MDC 过滤器

#### ObservabilityMdcFilter

创建了 HTTP 过滤器，自动填充 MDC 字段：

- **位置：** `app-infra/src/main/java/com/bluecone/app/infra/observability/ObservabilityMdcFilter.java`
- **功能：**
  - 从 HTTP 请求头提取 traceId、spanId、requestId、tenantId、userId、orderId
  - 如果请求头不存在，自动生成 traceId、spanId、requestId
  - 将 traceId 和 requestId 写入响应头，便于客户端关联
  - 请求结束后自动清理 MDC，防止内存泄漏

**支持的请求头：**

| 请求头 | MDC 字段 | 说明 |
|--------|----------|------|
| `X-Trace-Id` | `traceId` | 分布式追踪 ID |
| `X-Span-Id` | `spanId` | Span ID |
| `X-Request-Id` | `requestId` | 请求 ID |
| `X-Tenant-Id` | `tenantId` | 租户 ID |
| `X-User-Id` | `userId` | 用户 ID |
| `X-Order-Id` | `orderId` | 订单 ID |

### 4. Actuator 配置

#### application.yml

添加了 Actuator 和 Micrometer 配置：

```yaml
management:
  endpoints:
    web:
      base-path: /internal/actuator
      exposure:
        include: health,info,prometheus,metrics
  endpoint:
    health:
      show-details: when-authorized
      probes:
        enabled: true
  metrics:
    export:
      prometheus:
        enabled: true
    tags:
      application: ${spring.application.name}
      environment: ${spring.profiles.active:default}
```

**暴露的端点：**

| 端点 | 说明 | 访问地址 |
|------|------|----------|
| `/internal/actuator/health` | 健康检查 | http://localhost/internal/actuator/health |
| `/internal/actuator/info` | 应用信息 | http://localhost/internal/actuator/info |
| `/internal/actuator/prometheus` | Prometheus 指标 | http://localhost/internal/actuator/prometheus |
| `/internal/actuator/metrics` | 指标列表 | http://localhost/internal/actuator/metrics |

### 5. 安全配置

#### ActuatorSecurityConfig

创建了 Actuator 安全配置，限制访问：

- **位置：** `app-infra/src/main/java/com/bluecone/app/infra/observability/ActuatorSecurityConfig.java`
- **功能：**
  - Health 和 Info 端点：公开访问（用于负载均衡器健康检查）
  - Prometheus 和其他敏感端点：仅限内网访问
  - 默认允许的内网网段：`10.0.0.0/8`, `172.16.0.0/12`, `192.168.0.0/16`, `127.0.0.0/8`
  - 支持自定义允许的网段：`bluecone.observability.allowed-networks`

**安全策略：**

```
┌─────────────────────────────────────────────────────────────┐
│                    Actuator 端点访问控制                       │
├─────────────────────────────────────────────────────────────┤
│ /internal/actuator/health    → 公开访问                      │
│ /internal/actuator/info      → 公开访问                      │
│ /internal/actuator/prometheus → 仅限内网（10.x, 172.x, 192.x）│
│ /internal/actuator/metrics   → 仅限内网                      │
└─────────────────────────────────────────────────────────────┘
```

### 6. 自定义指标

#### OutboxMetrics

- **位置：** `app-infra/src/main/java/com/bluecone/app/infra/observability/metrics/OutboxMetrics.java`
- **指标：**
  - `outbox_events_published_total`（Counter）：已发布事件总数
  - `outbox_events_dispatched_total`（Counter）：已分发事件总数（按事件类型）
  - `outbox_events_retry_total`（Counter）：事件重试总数
  - `outbox_events_pending_count`（Gauge）：待处理事件数量
  - `outbox_dispatch_duration_seconds`（Timer）：事件分发耗时

#### ConsumerMetrics

- **位置：** `app-infra/src/main/java/com/bluecone/app/infra/observability/metrics/ConsumerMetrics.java`
- **指标：**
  - `consumer_events_processed_total`（Counter）：已处理事件总数
  - `consumer_events_deduped_total`（Counter）：去重事件总数
  - `consumer_processing_duration_seconds`（Timer）：事件处理耗时

#### JobMetrics

- **位置：** `app-infra/src/main/java/com/bluecone/app/infra/observability/metrics/JobMetrics.java`
- **指标：**
  - `job_executions_total`（Counter）：任务执行总数
  - `job_failures_total`（Counter）：任务失败总数
  - `job_execution_duration_seconds`（Timer）：任务执行耗时
  - `job_consistency_checked_total`（Counter）：一致性检查记录数
  - `job_consistency_missing_total`（Counter）：缺失记录数
  - `job_consistency_repaired_total`（Counter）：修复记录数

### 7. 指标集成

#### LoggingOutboxMetricsRecorder

增强了现有的 `LoggingOutboxMetricsRecorder`，集成 `OutboxMetrics`：

- 记录 Outbox 事件发布成功/失败
- 记录事件重试次数
- 在日志中添加 `outboxEventId` MDC 字段

#### OutboxPublisherJob

增强了 Outbox 发布任务，集成 `JobMetrics`：

- 记录任务执行成功/失败
- 记录任务执行耗时

#### OrderAssetConsistencyJob

增强了订单资产一致性任务，集成 `JobMetrics`：

- 记录任务执行成功/失败
- 记录任务执行耗时
- 记录一致性检查和修复数量

### 8. OpenTelemetry 集成

#### 文档

创建了 OpenTelemetry 集成文档：

- **位置：** `docs/OPENTELEMETRY-INTEGRATION.md`
- **内容：**
  - 方案一：Javaagent 方式（推荐，无侵入）
  - 方案二：SDK 方式（需要代码集成）
  - 配置腾讯云可观测平台
  - 验证和测试
  - 故障排查

**Javaagent 启动示例：**

```bash
java -javaagent:opentelemetry-javaagent.jar \
  -Dotel.service.name=bluecone-app \
  -Dotel.traces.exporter=otlp \
  -Dotel.exporter.otlp.endpoint=http://otel-collector:4318 \
  -jar target/bluecone-app.jar
```

### 9. 告警规则

#### 文档

创建了可观测性与告警清单文档：

- **位置：** `docs/OBSERVABILITY-AND-ALERTING.md`
- **内容：**
  - 6 条最小告警规则
  - 监控仪表板配置
  - 运维手册

**6 条最小告警规则：**

1. **ApplicationHealthCheckFailed**：应用健康检查失败
2. **OutboxPublishFailureRateHigh**：Outbox 事件发布失败率过高（>10%）
3. **OutboxPendingEventsHigh**：Outbox 待处理事件堆积（>1000）
4. **JobExecutionFailureRateHigh**：任务执行失败率过高（>20%）
5. **ConsistencyRepairCountHigh**：一致性修复数量异常（>10/小时）
6. **JvmMemoryUsageHigh**：JVM 内存使用率过高（>85%）

### 10. 快速开始文档

创建了快速开始文档：

- **位置：** `docs/OBSERVABILITY-QUICK-START.md`
- **内容：**
  - 本地启动和测试
  - 访问 Actuator 端点
  - 查看结构化日志
  - 集成 OpenTelemetry
  - 故障排查

---

## 技术架构

### 可观测性架构图

```
┌─────────────────────────────────────────────────────────────────┐
│                         bluecone-app                             │
├─────────────────────────────────────────────────────────────────┤
│                                                                   │
│  ┌─────────────────┐  ┌─────────────────┐  ┌─────────────────┐ │
│  │  HTTP Request   │  │  Scheduled Job  │  │  Event Consumer │ │
│  └────────┬────────┘  └────────┬────────┘  └────────┬────────┘ │
│           │                    │                     │          │
│           ▼                    ▼                     ▼          │
│  ┌─────────────────────────────────────────────────────────┐   │
│  │           ObservabilityMdcFilter (HTTP Filter)          │   │
│  │  - Populate MDC: traceId, spanId, requestId, tenantId  │   │
│  └─────────────────────────────────────────────────────────┘   │
│           │                    │                     │          │
│           ▼                    ▼                     ▼          │
│  ┌─────────────────────────────────────────────────────────┐   │
│  │                   Business Logic                         │   │
│  │  - OutboxMetrics: record publish/dispatch/retry         │   │
│  │  - ConsumerMetrics: record process/dedup                │   │
│  │  - JobMetrics: record execution/consistency             │   │
│  └─────────────────────────────────────────────────────────┘   │
│           │                    │                     │          │
│           ▼                    ▼                     ▼          │
│  ┌─────────────────┐  ┌─────────────────┐  ┌─────────────────┐ │
│  │  Logback JSON   │  │  Micrometer     │  │  OpenTelemetry  │ │
│  │  (stdout)       │  │  (Prometheus)   │  │  (Traces)       │ │
│  └────────┬────────┘  └────────┬────────┘  └────────┬────────┘ │
└───────────┼────────────────────┼────────────────────┼──────────┘
            │                    │                     │
            ▼                    ▼                     ▼
   ┌────────────────┐   ┌────────────────┐   ┌────────────────┐
   │  Log Collector │   │  Prometheus    │   │  Jaeger/APM    │
   │  (ELK/Loki)    │   │  (Metrics)     │   │  (Traces)      │
   └────────────────┘   └────────────────┘   └────────────────┘
            │                    │                     │
            └────────────────────┴─────────────────────┘
                                 │
                                 ▼
                        ┌────────────────┐
                        │    Grafana     │
                        │  (Dashboard)   │
                        └────────────────┘
```

### 数据流

1. **HTTP 请求** → `ObservabilityMdcFilter` → 填充 MDC → 业务逻辑 → 记录指标 → 输出日志
2. **Scheduled Job** → 业务逻辑 → 记录指标 → 输出日志
3. **Event Consumer** → 业务逻辑 → 记录指标 → 输出日志

### 关键组件

| 组件 | 职责 | 输出 |
|------|------|------|
| `ObservabilityMdcFilter` | 填充 MDC 上下文 | MDC 字段 |
| `OutboxMetrics` | Outbox 指标记录 | Prometheus 指标 |
| `ConsumerMetrics` | Consumer 指标记录 | Prometheus 指标 |
| `JobMetrics` | Job 指标记录 | Prometheus 指标 |
| `LoggingOutboxMetricsRecorder` | Outbox 日志和指标 | JSON 日志 + Prometheus 指标 |
| `ActuatorSecurityConfig` | Actuator 安全控制 | 访问控制 |

---

## 测试验证

### 本地测试步骤

1. **启动应用**

```bash
mvn clean package -DskipTests
java -jar app-application/target/bluecone-app.jar
```

2. **访问健康检查**

```bash
curl http://localhost/internal/actuator/health
```

3. **访问 Prometheus 指标**

```bash
curl http://localhost/internal/actuator/prometheus | grep outbox
```

4. **触发订单创建（触发 Outbox 事件）**

```bash
curl -X POST http://localhost/api/orders \
  -H "Content-Type: application/json" \
  -H "X-Tenant-Id: 1001" \
  -d '{"storeId": 1, "userId": 123, "items": [{"skuId": 1001, "quantity": 2}]}'
```

5. **查看指标变化**

```bash
curl http://localhost/internal/actuator/prometheus | grep outbox_events_published_total
```

6. **查看日志**

```bash
tail -f logs/bluecone-app.log | jq 'select(.logger_name | contains("Outbox"))'
```

### 预期结果

- ✅ 健康检查返回 `{"status": "UP"}`
- ✅ Prometheus 指标包含自定义指标（`outbox_events_*`, `consumer_events_*`, `job_*`）
- ✅ 日志为 JSON 格式，包含 `traceId`, `tenantId`, `orderId` 等字段
- ✅ 创建订单后，`outbox_events_published_total` 指标增加

---

## 文档清单

| 文档 | 说明 |
|------|------|
| `OBSERVABILITY-QUICK-START.md` | 快速开始指南 |
| `OBSERVABILITY-AND-ALERTING.md` | 可观测性与告警清单（含 6 条告警规则） |
| `OPENTELEMETRY-INTEGRATION.md` | OpenTelemetry 集成指南 |
| `OBSERVABILITY-IMPLEMENTATION-SUMMARY.md` | 实施总结（本文档） |

---

## 后续工作

### 短期（1-2 周）

- [ ] 配置 Prometheus 抓取 bluecone-app 指标
- [ ] 配置 Grafana 仪表板
- [ ] 配置 6 条最小告警规则
- [ ] 配置日志采集（ELK/Loki）

### 中期（1-2 月）

- [ ] 对接腾讯云可观测平台（OpenTelemetry）
- [ ] 优化告警规则（根据实际运行情况调整阈值）
- [ ] 添加更多自定义指标（业务指标）
- [ ] 性能测试和优化

### 长期（3-6 月）

- [ ] 建立可观测性最佳实践
- [ ] 培训团队使用可观测性工具
- [ ] 建立故障排查 Runbook
- [ ] 持续优化和改进

---

## 参考资料

- [Spring Boot Actuator 文档](https://docs.spring.io/spring-boot/docs/current/reference/html/actuator.html)
- [Micrometer 文档](https://micrometer.io/docs)
- [Prometheus 文档](https://prometheus.io/docs/)
- [OpenTelemetry 文档](https://opentelemetry.io/docs/)
- [Grafana 文档](https://grafana.com/docs/)

---

## 联系方式

如有问题或建议，请联系：

- 技术负责人：[Your Name]
- 邮箱：[your.email@example.com]
- 文档维护：[Your Team]

---

**最后更新时间：** 2024-12-19
