# 可观测性与告警清单

本文档提供 bluecone-app 的可观测性最小集合实施清单，包括日志、指标、追踪和告警规则。

## 目录

- [概述](#概述)
- [日志（Logs）](#日志logs)
- [指标（Metrics）](#指标metrics)
- [追踪（Traces）](#追踪traces)
- [告警规则（Alerts）](#告警规则alerts)
- [监控仪表板](#监控仪表板)
- [运维手册](#运维手册)

---

## 概述

### 可观测性三大支柱

1. **日志（Logs）**：结构化 JSON 日志，包含关键上下文字段
2. **指标（Metrics）**：Prometheus 格式指标，用于监控和告警
3. **追踪（Traces）**：OpenTelemetry 分布式追踪，用于性能分析和故障排查

### 关键字段

所有日志和追踪数据包含以下关键字段：

| 字段 | 说明 | 示例 |
|------|------|------|
| `traceId` | 分布式追踪 ID | `0af7651916cd43dd8448eb211c80319c` |
| `spanId` | Span ID | `b7ad6b7169203331` |
| `requestId` | HTTP 请求 ID | `550e8400-e29b-41d4-a716-446655440000` |
| `tenantId` | 租户 ID | `1001` |
| `userId` | 用户 ID | `123456` |
| `orderId` | 订单 ID | `789012` |
| `outboxEventId` | Outbox 事件 ID | `1234` |
| `idempotencyKey` | 幂等键 | `order-create-123456` |

---

## 日志（Logs）

### 日志格式

所有日志以 **JSON 格式** 输出到 `stdout`，便于日志采集和分析。

**示例日志：**

```json
{
  "@timestamp": "2024-12-19T10:30:45.123Z",
  "level": "INFO",
  "logger_name": "com.bluecone.app.order.OrderService",
  "message": "Order created successfully",
  "thread_name": "http-nio-80-exec-1",
  "app": "bluecone-app",
  "traceId": "0af7651916cd43dd8448eb211c80319c",
  "spanId": "b7ad6b7169203331",
  "requestId": "550e8400-e29b-41d4-a716-446655440000",
  "tenantId": "1001",
  "userId": "123456",
  "orderId": "789012"
}
```

### 日志级别

| 级别 | 用途 | 示例场景 |
|------|------|----------|
| `ERROR` | 错误和异常 | 订单创建失败、数据库连接失败 |
| `WARN` | 警告和异常情况 | 重试次数过多、缓存未命中 |
| `INFO` | 关键业务事件 | 订单创建、支付成功、资产提交 |
| `DEBUG` | 调试信息 | SQL 语句、缓存命中、事件分发 |

### 日志查询示例

**查询订单相关日志：**

```bash
# 使用 jq 过滤 JSON 日志
cat logs/bluecone-app.log | jq 'select(.orderId == "789012")'
```

**查询错误日志：**

```bash
cat logs/bluecone-app.log | jq 'select(.level == "ERROR")'
```

**按 traceId 查询完整请求链路：**

```bash
cat logs/bluecone-app.log | jq 'select(.traceId == "0af7651916cd43dd8448eb211c80319c")'
```

---

## 指标（Metrics）

### Actuator 端点

| 端点 | 说明 | 访问地址 |
|------|------|----------|
| `/internal/actuator/health` | 健康检查 | http://localhost/internal/actuator/health |
| `/internal/actuator/info` | 应用信息 | http://localhost/internal/actuator/info |
| `/internal/actuator/prometheus` | Prometheus 指标 | http://localhost/internal/actuator/prometheus |
| `/internal/actuator/metrics` | 指标列表 | http://localhost/internal/actuator/metrics |

### 自定义指标

#### 1. Outbox 指标

| 指标名称 | 类型 | 说明 | 标签 |
|---------|------|------|------|
| `outbox_events_published_total` | Counter | 已发布事件总数 | `status`（success/failure） |
| `outbox_events_dispatched_total` | Counter | 已分发事件总数 | `event_type` |
| `outbox_events_retry_total` | Counter | 事件重试总数 | `retry_count` |
| `outbox_events_pending_count` | Gauge | 待处理事件数量 | - |
| `outbox_dispatch_duration_seconds` | Timer | 事件分发耗时 | - |

**示例查询：**

```promql
# 每秒成功发布的事件数
rate(outbox_events_published_total{status="success"}[1m])

# 失败率
rate(outbox_events_published_total{status="failure"}[1m]) / rate(outbox_events_published_total[1m])

# 平均分发耗时
rate(outbox_dispatch_duration_seconds_sum[1m]) / rate(outbox_dispatch_duration_seconds_count[1m])
```

#### 2. Consumer 指标

| 指标名称 | 类型 | 说明 | 标签 |
|---------|------|------|------|
| `consumer_events_processed_total` | Counter | 已处理事件总数 | `event_type`, `status` |
| `consumer_events_deduped_total` | Counter | 去重事件总数 | `event_type` |
| `consumer_processing_duration_seconds` | Timer | 事件处理耗时 | `event_type` |

**示例查询：**

```promql
# 每秒处理的事件数（按类型）
rate(consumer_events_processed_total{status="success"}[1m])

# 去重率
rate(consumer_events_deduped_total[1m]) / rate(consumer_events_processed_total[1m])

# P95 处理耗时
histogram_quantile(0.95, rate(consumer_processing_duration_seconds_bucket[5m]))
```

#### 3. Job 指标

| 指标名称 | 类型 | 说明 | 标签 |
|---------|------|------|------|
| `job_executions_total` | Counter | 任务执行总数 | `job_name`, `status` |
| `job_failures_total` | Counter | 任务失败总数 | `job_name` |
| `job_execution_duration_seconds` | Timer | 任务执行耗时 | `job_name` |
| `job_consistency_checked_total` | Counter | 一致性检查记录数 | `job_name` |
| `job_consistency_missing_total` | Counter | 缺失记录数 | `job_name` |
| `job_consistency_repaired_total` | Counter | 修复记录数 | `job_name` |

**示例查询：**

```promql
# 任务失败率
rate(job_executions_total{status="failure"}[1m]) / rate(job_executions_total[1m])

# 一致性修复数量
increase(job_consistency_repaired_total[1h])

# 任务执行耗时
rate(job_execution_duration_seconds_sum[1m]) / rate(job_execution_duration_seconds_count[1m])
```

#### 4. JVM 指标

| 指标名称 | 说明 |
|---------|------|
| `jvm_memory_used_bytes` | JVM 内存使用量 |
| `jvm_gc_pause_seconds` | GC 暂停时间 |
| `jvm_threads_live_threads` | 活跃线程数 |
| `process_cpu_usage` | CPU 使用率 |
| `system_cpu_usage` | 系统 CPU 使用率 |

---

## 追踪（Traces）

### 追踪集成

使用 **OpenTelemetry Javaagent** 自动注入追踪代码，支持：

- HTTP 请求追踪
- 数据库查询追踪
- Redis 操作追踪
- 异步任务追踪

详见：[OpenTelemetry 集成指南](./OPENTELEMETRY-INTEGRATION.md)

### Trace Context 传播

所有 HTTP 请求自动传播 Trace Context（W3C Trace Context 标准）：

- **请求头**：`traceparent`、`tracestate`
- **响应头**：`X-Trace-Id`、`X-Request-Id`

### 关键 Span 属性

| 属性 | 说明 | 示例 |
|------|------|------|
| `http.method` | HTTP 方法 | `POST` |
| `http.url` | 请求 URL | `/api/orders` |
| `http.status_code` | HTTP 状态码 | `200` |
| `db.system` | 数据库类型 | `mysql` |
| `db.statement` | SQL 语句 | `SELECT * FROM bc_order WHERE id = ?` |
| `tenant.id` | 租户 ID | `1001` |
| `order.id` | 订单 ID | `789012` |

---

## 告警规则（Alerts）

### 最小告警规则集（6 条）

#### 1. 应用健康检查失败

**告警名称：** `ApplicationHealthCheckFailed`

**规则：**

```yaml
- alert: ApplicationHealthCheckFailed
  expr: up{job="bluecone-app"} == 0
  for: 1m
  labels:
    severity: critical
  annotations:
    summary: "应用健康检查失败"
    description: "应用 {{ $labels.instance }} 健康检查失败，持续时间超过 1 分钟"
```

**处理方法：**

1. 检查应用日志：`kubectl logs -f <pod-name>`
2. 检查数据库连接：`curl http://localhost/internal/actuator/health`
3. 重启应用：`kubectl rollout restart deployment/bluecone-app`

---

#### 2. Outbox 事件发布失败率过高

**告警名称：** `OutboxPublishFailureRateHigh`

**规则：**

```yaml
- alert: OutboxPublishFailureRateHigh
  expr: |
    (
      rate(outbox_events_published_total{status="failure"}[5m])
      /
      rate(outbox_events_published_total[5m])
    ) > 0.1
  for: 5m
  labels:
    severity: warning
  annotations:
    summary: "Outbox 事件发布失败率过高"
    description: "Outbox 事件发布失败率超过 10%，当前值：{{ $value | humanizePercentage }}"
```

**处理方法：**

1. 查看错误日志：`cat logs/bluecone-app-error.log | jq 'select(.logger_name | contains("Outbox"))'`
2. 检查 Outbox 表状态：`SELECT status, COUNT(*) FROM bc_outbox_message GROUP BY status`
3. 检查事件消费者是否正常运行

---

#### 3. Outbox 待处理事件堆积

**告警名称：** `OutboxPendingEventsHigh`

**规则：**

```yaml
- alert: OutboxPendingEventsHigh
  expr: outbox_events_pending_count > 1000
  for: 10m
  labels:
    severity: warning
  annotations:
    summary: "Outbox 待处理事件堆积"
    description: "Outbox 待处理事件数量超过 1000，当前值：{{ $value }}"
```

**处理方法：**

1. 检查 Outbox Publisher Job 是否正常运行
2. 检查数据库性能：`SHOW PROCESSLIST`
3. 临时增加 `dispatch-batch-size` 配置
4. 检查是否有大量 DEAD 状态事件：`SELECT COUNT(*) FROM bc_outbox_message WHERE status = 'DEAD'`

---

#### 4. 任务执行失败率过高

**告警名称：** `JobExecutionFailureRateHigh`

**规则：**

```yaml
- alert: JobExecutionFailureRateHigh
  expr: |
    (
      rate(job_executions_total{status="failure"}[10m])
      /
      rate(job_executions_total[10m])
    ) > 0.2
  for: 10m
  labels:
    severity: warning
  annotations:
    summary: "任务执行失败率过高"
    description: "任务 {{ $labels.job_name }} 执行失败率超过 20%，当前值：{{ $value | humanizePercentage }}"
```

**处理方法：**

1. 查看任务日志：`cat logs/bluecone-app.log | jq 'select(.logger_name | contains("Job"))'`
2. 检查任务执行历史：`SELECT * FROM bc_job_execution WHERE status = 'FAILED' ORDER BY created_at DESC LIMIT 10`
3. 检查数据库连接和性能

---

#### 5. 一致性修复数量异常

**告警名称：** `ConsistencyRepairCountHigh`

**规则：**

```yaml
- alert: ConsistencyRepairCountHigh
  expr: increase(job_consistency_repaired_total[1h]) > 10
  for: 5m
  labels:
    severity: warning
  annotations:
    summary: "一致性修复数量异常"
    description: "过去 1 小时内一致性修复数量超过 10，当前值：{{ $value }}"
```

**处理方法：**

1. 检查是否有大量订单支付成功但未提交资产
2. 查看 Outbox 事件发布日志
3. 检查事件消费者是否正常处理 `order.paid` 事件
4. 分析根因：是否是代码 bug、数据库事务问题、网络问题等

---

#### 6. JVM 内存使用率过高

**告警名称：** `JvmMemoryUsageHigh`

**规则：**

```yaml
- alert: JvmMemoryUsageHigh
  expr: |
    (
      jvm_memory_used_bytes{area="heap"}
      /
      jvm_memory_max_bytes{area="heap"}
    ) > 0.85
  for: 5m
  labels:
    severity: warning
  annotations:
    summary: "JVM 堆内存使用率过高"
    description: "JVM 堆内存使用率超过 85%，当前值：{{ $value | humanizePercentage }}"
```

**处理方法：**

1. 检查是否有内存泄漏：`jmap -histo:live <pid>`
2. 生成堆转储文件：`jmap -dump:format=b,file=heap.bin <pid>`
3. 分析堆转储文件：使用 MAT（Memory Analyzer Tool）
4. 临时增加 JVM 堆内存：`-Xmx4g`
5. 优化代码：减少对象创建、使用对象池、及时释放资源

---

## 监控仪表板

### Grafana 仪表板配置

**推荐仪表板：**

1. **应用概览**
   - 请求 QPS、错误率、P95 延迟
   - JVM 内存、CPU 使用率
   - 活跃线程数、GC 暂停时间

2. **Outbox 监控**
   - 事件发布成功率、失败率
   - 待处理事件数量
   - 事件分发耗时（P50、P95、P99）
   - 按事件类型分组的发布量

3. **任务监控**
   - 任务执行成功率、失败率
   - 任务执行耗时
   - 一致性检查和修复数量

4. **数据库监控**
   - 数据库连接池使用率
   - SQL 执行耗时
   - 慢查询数量

### Prometheus 抓取配置

```yaml
scrape_configs:
  - job_name: 'bluecone-app'
    metrics_path: '/internal/actuator/prometheus'
    scrape_interval: 15s
    static_configs:
      - targets: ['bluecone-app:80']
        labels:
          environment: 'production'
          application: 'bluecone-app'
```

---

## 运维手册

### 日常巡检清单

**每日检查：**

- [ ] 应用健康状态：`curl http://localhost/internal/actuator/health`
- [ ] 错误日志数量：`cat logs/bluecone-app-error.log | wc -l`
- [ ] Outbox 待处理事件数量：`SELECT COUNT(*) FROM bc_outbox_message WHERE status IN ('NEW', 'FAILED')`
- [ ] 任务执行失败数量：`SELECT COUNT(*) FROM bc_job_execution WHERE status = 'FAILED' AND created_at > NOW() - INTERVAL 1 DAY`

**每周检查：**

- [ ] JVM 内存使用趋势
- [ ] GC 暂停时间趋势
- [ ] 数据库连接池使用率
- [ ] 慢查询分析

### 故障排查流程

**步骤 1：确认告警**

1. 查看告警详情（Prometheus AlertManager）
2. 确认告警级别（critical/warning）
3. 查看告警持续时间

**步骤 2：查看日志**

1. 查看错误日志：`cat logs/bluecone-app-error.log | tail -100`
2. 按 traceId 查询完整链路：`cat logs/bluecone-app.log | jq 'select(.traceId == "xxx")'`
3. 查看应用日志：`kubectl logs -f <pod-name>`

**步骤 3：查看指标**

1. 访问 Prometheus：http://prometheus:9090
2. 查询相关指标（参考上文指标列表）
3. 查看 Grafana 仪表板

**步骤 4：查看追踪**

1. 访问 Jaeger/腾讯云 APM
2. 搜索相关 Trace ID
3. 分析 Span 详情和耗时

**步骤 5：执行修复**

1. 根据告警类型执行对应的处理方法
2. 验证修复效果
3. 记录故障原因和处理过程

### 性能优化建议

**Outbox 优化：**

- 调整 `dispatch-batch-size`（默认 100）
- 调整 `publish-cron`（默认 5 秒）
- 定期清理 DONE/DEAD 状态事件

**数据库优化：**

- 添加索引：`CREATE INDEX idx_outbox_status_next_retry ON bc_outbox_message(status, next_retry_at)`
- 分区表：按时间分区 Outbox 表
- 读写分离：Outbox 查询使用只读副本

**JVM 优化：**

- 调整堆内存：`-Xms2g -Xmx4g`
- 使用 G1GC：`-XX:+UseG1GC`
- 调整 GC 参数：`-XX:MaxGCPauseMillis=200`

---

## 参考资料

- [Prometheus 官方文档](https://prometheus.io/docs/)
- [Grafana 官方文档](https://grafana.com/docs/)
- [OpenTelemetry 官方文档](https://opentelemetry.io/docs/)
- [Spring Boot Actuator 文档](https://docs.spring.io/spring-boot/docs/current/reference/html/actuator.html)
- [Micrometer 文档](https://micrometer.io/docs)
