# 可观测性快速开始

本文档提供 bluecone-app 可观测性功能的快速上手指南。

## 快速概览

### 已实现的功能

✅ **结构化 JSON 日志**
- 所有日志输出为 JSON 格式
- 包含 traceId、spanId、tenantId、orderId、outboxEventId 等关键字段
- 支持通过 MDC 自动注入上下文信息

✅ **Prometheus 指标**
- Spring Boot Actuator + Micrometer
- 自定义指标：Outbox、Consumer、Job
- JVM 和系统指标

✅ **OpenTelemetry 分布式追踪**
- 支持 Javaagent（无侵入）和 SDK 两种方式
- 自动追踪 HTTP、数据库、Redis 等操作
- 支持对接腾讯云可观测平台

✅ **安全配置**
- Actuator 端点仅限内网访问
- 支持自定义允许的网络段

✅ **告警规则**
- 6 条最小告警规则
- 覆盖应用健康、Outbox、任务、JVM 等关键指标

---

## 快速开始

### 1. 本地启动应用

```bash
# 构建应用
mvn clean package -DskipTests

# 启动应用
java -jar app-application/target/bluecone-app.jar
```

### 2. 访问 Actuator 端点

**健康检查：**

```bash
curl http://localhost/internal/actuator/health
```

**响应示例：**

```json
{
  "status": "UP",
  "components": {
    "db": {
      "status": "UP",
      "details": {
        "database": "MySQL",
        "validationQuery": "isValid()"
      }
    },
    "diskSpace": {
      "status": "UP"
    },
    "ping": {
      "status": "UP"
    }
  }
}
```

**Prometheus 指标：**

```bash
curl http://localhost/internal/actuator/prometheus
```

**响应示例：**

```
# HELP outbox_events_published_total Total number of outbox events published successfully
# TYPE outbox_events_published_total counter
outbox_events_published_total{status="success"} 42.0
outbox_events_published_total{status="failure"} 3.0

# HELP outbox_events_pending_count Current number of pending outbox events
# TYPE outbox_events_pending_count gauge
outbox_events_pending_count 5.0

# HELP job_executions_total Total number of job executions
# TYPE job_executions_total counter
job_executions_total{job_name="outbox_publisher",status="success"} 120.0
job_executions_total{job_name="order_asset_consistency",status="success"} 12.0
```

### 3. 查看结构化日志

**查看所有日志：**

```bash
tail -f logs/bluecone-app.log | jq
```

**按订单 ID 过滤：**

```bash
cat logs/bluecone-app.log | jq 'select(.orderId == "789012")'
```

**按 traceId 查询完整链路：**

```bash
cat logs/bluecone-app.log | jq 'select(.traceId == "0af7651916cd43dd8448eb211c80319c")'
```

**查看错误日志：**

```bash
tail -f logs/bluecone-app-error.log | jq
```

### 4. 集成 OpenTelemetry（可选）

**下载 Javaagent：**

```bash
wget https://github.com/open-telemetry/opentelemetry-java-instrumentation/releases/download/v1.36.0/opentelemetry-javaagent.jar
```

**启动应用（使用 Jaeger）：**

```bash
# 启动 Jaeger
docker run -d --name jaeger \
  -p 16686:16686 \
  -p 4318:4318 \
  jaegertracing/all-in-one:latest

# 启动应用
java -javaagent:opentelemetry-javaagent.jar \
  -Dotel.service.name=bluecone-app \
  -Dotel.traces.exporter=otlp \
  -Dotel.exporter.otlp.endpoint=http://localhost:4318 \
  -Dotel.exporter.otlp.protocol=http/protobuf \
  -jar app-application/target/bluecone-app.jar
```

**访问 Jaeger UI：**

```
http://localhost:16686
```

---

## 配置说明

### Actuator 配置

在 `application.yml` 中已配置：

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
```

### 安全配置

默认允许的内网网段：

- `10.0.0.0/8`
- `172.16.0.0/12`
- `192.168.0.0/16`
- `127.0.0.0/8`

**自定义允许的网段：**

在 `application.yml` 中添加：

```yaml
bluecone:
  observability:
    allowed-networks: 10.0.0.0/8,172.16.0.0/12,192.168.0.0/16,127.0.0.0/8,100.64.0.0/10
```

### 日志配置

日志配置在 `logback-spring.xml` 中，已包含以下 MDC 字段：

- `traceId`
- `spanId`
- `requestId`
- `tenantId`
- `userId`
- `orderId`
- `outboxEventId`
- `idempotencyKey`

**无需额外配置**，MDC 字段会自动注入到日志中。

---

## 自定义指标

### 1. Outbox 指标

在 `OutboxMetrics` 中提供：

```java
@Autowired
private OutboxMetrics outboxMetrics;

// 记录成功发布
outboxMetrics.recordPublishSuccess("order.paid");

// 记录失败发布
outboxMetrics.recordPublishFailure("order.paid");

// 记录重试
outboxMetrics.recordRetry(3);

// 更新待处理事件数量
outboxMetrics.setPendingEventsCount(100);

// 记录分发耗时
Timer.Sample sample = outboxMetrics.startDispatchTimer();
// ... 执行分发逻辑 ...
outboxMetrics.stopDispatchTimer(sample);
```

### 2. Consumer 指标

在 `ConsumerMetrics` 中提供：

```java
@Autowired
private ConsumerMetrics consumerMetrics;

// 记录成功处理
consumerMetrics.recordProcessSuccess("order.paid");

// 记录失败处理
consumerMetrics.recordProcessFailure("order.paid");

// 记录去重
consumerMetrics.recordDeduped("order.paid");

// 记录处理耗时
Timer.Sample sample = consumerMetrics.startProcessingTimer();
// ... 执行处理逻辑 ...
consumerMetrics.stopProcessingTimer(sample, "order.paid");
```

### 3. Job 指标

在 `JobMetrics` 中提供：

```java
@Autowired
private JobMetrics jobMetrics;

// 记录成功执行
jobMetrics.recordExecutionSuccess("outbox_publisher");

// 记录失败执行
jobMetrics.recordExecutionFailure("outbox_publisher");

// 记录执行耗时
Timer.Sample sample = jobMetrics.startExecutionTimer();
// ... 执行任务逻辑 ...
jobMetrics.stopExecutionTimer(sample, "outbox_publisher");

// 记录一致性检查结果
jobMetrics.recordConsistencyCheck("order_asset_consistency", 100, 5, 3);
```

---

## 监控集成

### Prometheus 抓取配置

在 Prometheus 配置文件中添加：

```yaml
scrape_configs:
  - job_name: 'bluecone-app'
    metrics_path: '/internal/actuator/prometheus'
    scrape_interval: 15s
    static_configs:
      - targets: ['localhost:80']
```

### Grafana 仪表板

推荐导入以下仪表板：

1. **Spring Boot 2.1 Statistics**（ID: 6756）
2. **JVM (Micrometer)**（ID: 4701）
3. **自定义仪表板**（参考 [OBSERVABILITY-AND-ALERTING.md](./OBSERVABILITY-AND-ALERTING.md)）

---

## 测试验证

### 1. 触发 Outbox 事件

```bash
# 创建订单（触发 Outbox 事件）
curl -X POST http://localhost/api/orders \
  -H "Content-Type: application/json" \
  -H "X-Tenant-Id: 1001" \
  -d '{
    "storeId": 1,
    "userId": 123,
    "items": [
      {"skuId": 1001, "quantity": 2}
    ]
  }'
```

### 2. 查看指标变化

```bash
# 查看 Outbox 指标
curl http://localhost/internal/actuator/prometheus | grep outbox

# 预期输出：
# outbox_events_published_total{status="success"} 1.0
# outbox_events_dispatched_total{event_type="order.created"} 1.0
```

### 3. 查看日志

```bash
# 查看 Outbox 日志
cat logs/bluecone-app.log | jq 'select(.logger_name | contains("Outbox"))'
```

### 4. 查看追踪（如果启用 OpenTelemetry）

访问 Jaeger UI：http://localhost:16686

搜索服务：`bluecone-app`

---

## 故障排查

### 问题 1：无法访问 Actuator 端点

**症状：** 访问 `/internal/actuator/prometheus` 返回 403 Forbidden

**原因：** IP 地址不在允许的内网网段

**解决方法：**

1. 检查请求来源 IP：`curl -H "X-Forwarded-For: 127.0.0.1" http://localhost/internal/actuator/prometheus`
2. 添加 IP 到允许列表：在 `application.yml` 中配置 `bluecone.observability.allowed-networks`

### 问题 2：日志中缺少 MDC 字段

**症状：** 日志中没有 `traceId`、`orderId` 等字段

**原因：** MDC 未正确设置

**解决方法：**

1. 确认 `ObservabilityMdcFilter` 已启用
2. 检查请求头是否包含 `X-Trace-Id`、`X-Order-Id` 等
3. 查看应用日志：`cat logs/bluecone-app.log | jq 'select(.logger_name == "com.bluecone.app.infra.observability.ObservabilityMdcFilter")'`

### 问题 3：自定义指标未显示

**症状：** `/internal/actuator/prometheus` 中没有自定义指标

**原因：** 指标未被记录

**解决方法：**

1. 触发相关操作（例如创建订单）
2. 检查指标类是否被正确注入：`@Autowired private OutboxMetrics outboxMetrics;`
3. 查看应用日志是否有错误

---

## 下一步

- 阅读 [OpenTelemetry 集成指南](./OPENTELEMETRY-INTEGRATION.md)
- 阅读 [可观测性与告警清单](./OBSERVABILITY-AND-ALERTING.md)
- 配置 Prometheus 和 Grafana
- 配置告警规则
- 对接腾讯云可观测平台

---

## 参考资料

- [Spring Boot Actuator 文档](https://docs.spring.io/spring-boot/docs/current/reference/html/actuator.html)
- [Micrometer 文档](https://micrometer.io/docs)
- [Prometheus 文档](https://prometheus.io/docs/)
- [OpenTelemetry 文档](https://opentelemetry.io/docs/)
