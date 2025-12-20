# OpenTelemetry 集成指南

本文档介绍如何将 OpenTelemetry 集成到 bluecone-app 中，支持分布式追踪（Distributed Tracing）。

## 目录

- [概述](#概述)
- [方案一：Javaagent 方式（推荐）](#方案一javaagent-方式推荐)
- [方案二：SDK 方式](#方案二sdk-方式)
- [配置腾讯云可观测平台](#配置腾讯云可观测平台)
- [验证和测试](#验证和测试)
- [故障排查](#故障排查)

---

## 概述

OpenTelemetry 是一个开源的可观测性框架，用于生成、收集和导出遥测数据（traces、metrics、logs）。

**支持的方式：**

1. **Javaagent 方式（推荐）**：无侵入，通过 JVM agent 自动注入追踪代码
2. **SDK 方式**：需要在代码中显式集成 OpenTelemetry SDK

**推荐使用 Javaagent 方式**，因为：
- 无需修改代码
- 自动支持常见框架（Spring Boot、JDBC、Redis、HTTP Client 等）
- 配置简单，易于维护

---

## 方案一：Javaagent 方式（推荐）

### 1. 下载 OpenTelemetry Javaagent

从 [OpenTelemetry Releases](https://github.com/open-telemetry/opentelemetry-java-instrumentation/releases) 下载最新版本的 `opentelemetry-javaagent.jar`。

```bash
# 下载到项目根目录
wget https://github.com/open-telemetry/opentelemetry-java-instrumentation/releases/download/v1.36.0/opentelemetry-javaagent.jar
```

### 2. 配置 JVM 启动参数

在启动应用时添加 `-javaagent` 参数：

```bash
java -javaagent:opentelemetry-javaagent.jar \
  -Dotel.service.name=bluecone-app \
  -Dotel.traces.exporter=otlp \
  -Dotel.metrics.exporter=none \
  -Dotel.logs.exporter=none \
  -Dotel.exporter.otlp.endpoint=http://your-otel-collector:4318 \
  -Dotel.exporter.otlp.protocol=http/protobuf \
  -Dotel.resource.attributes=service.version=1.0.0,deployment.environment=production \
  -jar target/bluecone-app.jar
```

### 3. 配置参数说明

| 参数 | 说明 | 示例值 |
|------|------|--------|
| `otel.service.name` | 服务名称 | `bluecone-app` |
| `otel.traces.exporter` | Trace 导出器类型 | `otlp`（推荐）、`zipkin`、`jaeger` |
| `otel.metrics.exporter` | Metrics 导出器类型 | `none`（使用 Prometheus）、`otlp` |
| `otel.logs.exporter` | Logs 导出器类型 | `none`（使用 Logback JSON） |
| `otel.exporter.otlp.endpoint` | OTLP 端点地址 | `http://otel-collector:4318` |
| `otel.exporter.otlp.protocol` | OTLP 协议 | `http/protobuf`（推荐）、`grpc` |
| `otel.resource.attributes` | 资源属性（标签） | `service.version=1.0.0,env=prod` |

### 4. Docker 部署示例

在 `Dockerfile` 中添加：

```dockerfile
FROM openjdk:21-slim

# 下载 OpenTelemetry Javaagent
ADD https://github.com/open-telemetry/opentelemetry-java-instrumentation/releases/download/v1.36.0/opentelemetry-javaagent.jar /app/opentelemetry-javaagent.jar

# 复制应用 JAR
COPY target/bluecone-app.jar /app/app.jar

# 设置工作目录
WORKDIR /app

# 启动应用
ENTRYPOINT ["java", \
  "-javaagent:/app/opentelemetry-javaagent.jar", \
  "-Dotel.service.name=bluecone-app", \
  "-Dotel.traces.exporter=otlp", \
  "-Dotel.metrics.exporter=none", \
  "-Dotel.logs.exporter=none", \
  "-Dotel.exporter.otlp.endpoint=${OTEL_EXPORTER_OTLP_ENDPOINT}", \
  "-Dotel.exporter.otlp.protocol=http/protobuf", \
  "-Dotel.resource.attributes=service.version=${APP_VERSION},deployment.environment=${ENVIRONMENT}", \
  "-jar", "app.jar"]
```

### 5. Kubernetes 部署示例

在 `deployment.yaml` 中添加：

```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: bluecone-app
spec:
  template:
    spec:
      initContainers:
      - name: download-otel-agent
        image: busybox:1.35
        command:
        - sh
        - -c
        - |
          wget -O /otel/opentelemetry-javaagent.jar \
            https://github.com/open-telemetry/opentelemetry-java-instrumentation/releases/download/v1.36.0/opentelemetry-javaagent.jar
        volumeMounts:
        - name: otel-agent
          mountPath: /otel
      
      containers:
      - name: bluecone-app
        image: bluecone-app:latest
        env:
        - name: JAVA_TOOL_OPTIONS
          value: >-
            -javaagent:/otel/opentelemetry-javaagent.jar
            -Dotel.service.name=bluecone-app
            -Dotel.traces.exporter=otlp
            -Dotel.metrics.exporter=none
            -Dotel.logs.exporter=none
            -Dotel.exporter.otlp.endpoint=http://otel-collector:4318
            -Dotel.exporter.otlp.protocol=http/protobuf
            -Dotel.resource.attributes=service.version=1.0.0,deployment.environment=production
        volumeMounts:
        - name: otel-agent
          mountPath: /otel
      
      volumes:
      - name: otel-agent
        emptyDir: {}
```

---

## 方案二：SDK 方式

如果无法使用 Javaagent（例如，受限的运行环境），可以使用 SDK 方式。

### 1. 添加依赖

依赖已在根 `pom.xml` 中定义，在需要的模块中添加：

```xml
<dependency>
    <groupId>io.opentelemetry</groupId>
    <artifactId>opentelemetry-api</artifactId>
</dependency>
<dependency>
    <groupId>io.opentelemetry</groupId>
    <artifactId>opentelemetry-sdk</artifactId>
</dependency>
<dependency>
    <groupId>io.opentelemetry</groupId>
    <artifactId>opentelemetry-exporter-otlp</artifactId>
</dependency>
```

### 2. 创建 OpenTelemetry 配置类

```java
package com.bluecone.app.infra.observability;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.exporter.otlp.trace.OtlpGrpcSpanExporter;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.opentelemetry.sdk.trace.export.BatchSpanProcessor;
import io.opentelemetry.semconv.resource.attributes.ResourceAttributes;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenTelemetryConfig {

    @Value("${otel.service.name:bluecone-app}")
    private String serviceName;

    @Value("${otel.exporter.otlp.endpoint:http://localhost:4317}")
    private String otlpEndpoint;

    @Bean
    public OpenTelemetry openTelemetry() {
        Resource resource = Resource.getDefault()
                .merge(Resource.create(Attributes.of(
                        ResourceAttributes.SERVICE_NAME, serviceName,
                        ResourceAttributes.SERVICE_VERSION, "1.0.0"
                )));

        SdkTracerProvider sdkTracerProvider = SdkTracerProvider.builder()
                .addSpanProcessor(BatchSpanProcessor.builder(
                        OtlpGrpcSpanExporter.builder()
                                .setEndpoint(otlpEndpoint)
                                .build()
                ).build())
                .setResource(resource)
                .build();

        return OpenTelemetrySdk.builder()
                .setTracerProvider(sdkTracerProvider)
                .buildAndRegisterGlobal();
    }

    @Bean
    public Tracer tracer(OpenTelemetry openTelemetry) {
        return openTelemetry.getTracer("com.bluecone.app", "1.0.0");
    }
}
```

### 3. 使用示例

```java
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Scope;

@Service
public class OrderService {
    
    private final Tracer tracer;
    
    public OrderService(Tracer tracer) {
        this.tracer = tracer;
    }
    
    public void createOrder(OrderRequest request) {
        Span span = tracer.spanBuilder("createOrder")
                .setAttribute("order.id", request.getOrderId())
                .setAttribute("tenant.id", request.getTenantId())
                .startSpan();
        
        try (Scope scope = span.makeCurrent()) {
            // 业务逻辑
            processOrder(request);
        } catch (Exception e) {
            span.recordException(e);
            throw e;
        } finally {
            span.end();
        }
    }
}
```

---

## 配置腾讯云可观测平台

腾讯云可观测平台（Application Performance Management, APM）支持 OpenTelemetry 协议。

### 1. 获取接入信息

1. 登录腾讯云控制台
2. 进入 **应用性能观测** 服务
3. 创建应用，获取以下信息：
   - **接入点（Endpoint）**：例如 `https://apm-receiver.ap-guangzhou.tencentcs.com:4318`
   - **Token**：用于身份验证

### 2. 配置 Javaagent 参数

```bash
java -javaagent:opentelemetry-javaagent.jar \
  -Dotel.service.name=bluecone-app \
  -Dotel.traces.exporter=otlp \
  -Dotel.exporter.otlp.endpoint=https://apm-receiver.ap-guangzhou.tencentcs.com:4318 \
  -Dotel.exporter.otlp.protocol=http/protobuf \
  -Dotel.exporter.otlp.headers=Authentication=Bearer\ YOUR_TOKEN \
  -Dotel.resource.attributes=service.version=1.0.0,deployment.environment=production \
  -jar target/bluecone-app.jar
```

### 3. 环境变量方式（推荐）

```bash
export OTEL_SERVICE_NAME=bluecone-app
export OTEL_TRACES_EXPORTER=otlp
export OTEL_EXPORTER_OTLP_ENDPOINT=https://apm-receiver.ap-guangzhou.tencentcs.com:4318
export OTEL_EXPORTER_OTLP_PROTOCOL=http/protobuf
export OTEL_EXPORTER_OTLP_HEADERS=Authentication=Bearer\ YOUR_TOKEN
export OTEL_RESOURCE_ATTRIBUTES=service.version=1.0.0,deployment.environment=production

java -javaagent:opentelemetry-javaagent.jar -jar target/bluecone-app.jar
```

---

## 验证和测试

### 1. 本地测试（使用 Jaeger）

使用 Docker 启动 Jaeger：

```bash
docker run -d --name jaeger \
  -p 16686:16686 \
  -p 4318:4318 \
  jaegertracing/all-in-one:latest
```

启动应用：

```bash
java -javaagent:opentelemetry-javaagent.jar \
  -Dotel.service.name=bluecone-app \
  -Dotel.traces.exporter=otlp \
  -Dotel.exporter.otlp.endpoint=http://localhost:4318 \
  -Dotel.exporter.otlp.protocol=http/protobuf \
  -jar target/bluecone-app.jar
```

访问 Jaeger UI：http://localhost:16686

### 2. 检查 Trace 数据

1. 触发一些 HTTP 请求（例如创建订单）
2. 在 Jaeger UI 中搜索 `bluecone-app` 服务
3. 查看 Trace 详情，验证：
   - Span 名称和层级关系
   - Span 属性（如 `http.method`、`http.url`、`db.statement`）
   - Trace ID 和 Span ID

### 3. 验证 Trace Header 透传

OpenTelemetry Javaagent 自动支持以下 Trace Header：

- **W3C Trace Context**（推荐）：`traceparent`、`tracestate`
- **B3 Propagation**：`X-B3-TraceId`、`X-B3-SpanId`

验证方法：

```bash
# 发送带有 traceparent 的请求
curl -H "traceparent: 00-0af7651916cd43dd8448eb211c80319c-b7ad6b7169203331-01" \
  http://localhost/api/orders

# 检查响应头中是否包含 traceparent
```

---

## 故障排查

### 1. Trace 数据未上报

**检查项：**

1. 确认 OTLP 端点地址正确
2. 检查网络连接（防火墙、DNS）
3. 查看应用日志，搜索 `otel` 关键字
4. 启用调试日志：

```bash
-Dotel.javaagent.debug=true
```

### 2. Trace ID 不一致

**原因：** 可能是多个 Trace Context 传播机制冲突。

**解决方法：** 显式指定传播机制：

```bash
-Dotel.propagators=tracecontext,baggage
```

### 3. 性能影响

OpenTelemetry Javaagent 的性能开销通常在 **5-10%** 以内。

**优化建议：**

1. 使用采样（Sampling）减少 Trace 数据量：

```bash
-Dotel.traces.sampler=parentbased_traceidratio
-Dotel.traces.sampler.arg=0.1  # 10% 采样率
```

2. 调整批处理参数：

```bash
-Dotel.bsp.schedule.delay=5000  # 批处理延迟（毫秒）
-Dotel.bsp.max.queue.size=2048  # 队列大小
-Dotel.bsp.max.export.batch.size=512  # 批量导出大小
```

### 4. 与 MDC 集成

OpenTelemetry Javaagent 自动将 Trace ID 和 Span ID 写入 MDC：

- `trace_id`：Trace ID
- `span_id`：Span ID
- `trace_flags`：Trace Flags

在 `logback-spring.xml` 中已配置 `traceId` 和 `spanId`，无需额外配置。

---

## 参考资料

- [OpenTelemetry Java Instrumentation](https://github.com/open-telemetry/opentelemetry-java-instrumentation)
- [OpenTelemetry Java SDK](https://github.com/open-telemetry/opentelemetry-java)
- [腾讯云应用性能观测](https://cloud.tencent.com/product/apm)
- [W3C Trace Context](https://www.w3.org/TR/trace-context/)
