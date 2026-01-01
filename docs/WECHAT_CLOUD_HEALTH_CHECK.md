# 微信云托管健康检查配置指南

## 问题描述

应用部署到微信云托管后，出现以下错误：

```
Liveness probe failed: dial tcp 10.19.11.147:80: connect: connection refused
Readiness probe failed: dial tcp 10.19.11.147:80: connect: connection refused
```

## 原因分析

Spring Boot 应用启动需要时间（通常 10-30 秒），包括：
- 加载 Spring 上下文
- 初始化数据库连接池
- 扫描 MyBatis Mapper
- 初始化各种 Bean

如果 Kubernetes 健康检查探针的 `initialDelaySeconds`（初始延迟）设置过短，探针会在应用未完全启动时就开始检查，导致连接失败，容器被 K8s 反复重启。

## 解决方案

### 方案一：在微信云托管控制台配置健康检查（推荐）

1. 登录 [微信云托管控制台](https://cloud.weixin.qq.com/cloudrun)
2. 进入您的服务 → 版本管理 → 编辑版本
3. 找到 **健康检查** 配置区域
4. 配置以下参数：

#### Liveness Probe（存活探针）

```yaml
检查路径: /internal/actuator/health/liveness
检查端口: 80
初始延迟: 60 秒          # initialDelaySeconds - 给应用足够的启动时间
检查间隔: 10 秒          # periodSeconds
超时时间: 5 秒           # timeoutSeconds
失败阈值: 3 次           # failureThreshold - 连续失败 3 次才重启
```

#### Readiness Probe（就绪探针）

```yaml
检查路径: /internal/actuator/health/readiness
检查端口: 80
初始延迟: 30 秒          # initialDelaySeconds - 比 liveness 短一些
检查间隔: 5 秒           # periodSeconds
超时时间: 3 秒           # timeoutSeconds
失败阈值: 3 次           # failureThreshold
```

### 方案二：优化应用启动速度

如果无法修改云托管的健康检查配置，可以优化应用启动速度：

#### 1. 禁用不必要的自动配置

在 `application-prod.yml` 中添加：

```yaml
spring:
  autoconfigure:
    exclude:
      - org.springframework.boot.autoconfigure.data.redis.RedisRepositoriesAutoConfiguration
      # 如果不使用，可以排除其他自动配置
```

#### 2. 延迟初始化非关键 Bean

```yaml
spring:
  main:
    lazy-initialization: false  # 保持默认，但可以针对特定 Bean 设置 @Lazy
```

#### 3. 优化 MyBatis 配置

确保已经移除重复的 `@MapperScan`（已在前面修复）。

#### 4. 调整 JVM 参数

在 Dockerfile 或环境变量中优化 JVM 启动参数：

```bash
JAVA_OPTS="-XX:+UseG1GC \
    -XX:MaxRAMPercentage=75.0 \
    -XX:InitialRAMPercentage=50.0 \
    -XX:+TieredCompilation \
    -XX:TieredStopAtLevel=1 \
    -Xverify:none \
    -XX:+UseContainerSupport"
```

### 方案三：使用 Startup Probe（启动探针）

如果微信云托管支持 Kubernetes 1.18+ 的 Startup Probe，可以使用它来处理慢启动应用：

```yaml
startupProbe:
  httpGet:
    path: /internal/actuator/health/liveness
    port: 80
  initialDelaySeconds: 0
  periodSeconds: 10
  failureThreshold: 30        # 最多等待 300 秒（10秒 × 30次）
  timeoutSeconds: 5

livenessProbe:
  httpGet:
    path: /internal/actuator/health/liveness
    port: 80
  periodSeconds: 10
  timeoutSeconds: 5
  failureThreshold: 3

readinessProbe:
  httpGet:
    path: /internal/actuator/health/readiness
    port: 80
  periodSeconds: 5
  timeoutSeconds: 3
  failureThreshold: 3
```

## 推荐配置总结

### 快速修复（最小改动）

在微信云托管控制台将 **Liveness Probe 初始延迟** 设置为 **60 秒**。

### 生产环境推荐配置

| 探针类型 | 路径 | 初始延迟 | 检查间隔 | 超时时间 | 失败阈值 |
|---------|------|---------|---------|---------|---------|
| Startup | `/internal/actuator/health/liveness` | 0s | 10s | 5s | 30 |
| Liveness | `/internal/actuator/health/liveness` | - | 10s | 5s | 3 |
| Readiness | `/internal/actuator/health/readiness` | - | 5s | 3s | 3 |

## 验证方法

### 1. 查看应用启动日志

在微信云托管控制台查看日志，确认应用成功启动：

```json
{"message":"Started Application in 25.123 seconds","level":"INFO"}
```

### 2. 手动测试健康检查端点

```bash
# 进入容器
kubectl exec -it <pod-name> -- sh

# 测试健康检查
curl http://localhost:80/internal/actuator/health/liveness
curl http://localhost:80/internal/actuator/health/readiness
```

预期响应：

```json
{
  "status": "UP"
}
```

### 3. 查看 Pod 事件

```bash
kubectl describe pod <pod-name>
```

如果配置正确，不应该看到 `Liveness probe failed` 或 `Readiness probe failed` 事件。

## 常见问题

### Q1: 为什么 initialDelaySeconds 要设置这么长？

**A**: Spring Boot 应用启动需要：
- 加载类和依赖（5-10 秒）
- 初始化 Spring 上下文（5-10 秒）
- 连接数据库和 Redis（2-5 秒）
- 执行 Flyway 数据库迁移（可能很长）
- 初始化业务 Bean（5-10 秒）

总计可能需要 20-60 秒，所以建议设置 60 秒以上。

### Q2: Liveness 和 Readiness 有什么区别？

**A**:
- **Liveness Probe**: 检查应用是否存活。如果失败，K8s 会**重启容器**。
- **Readiness Probe**: 检查应用是否就绪。如果失败，K8s 会**停止向该 Pod 转发流量**，但不会重启。

### Q3: 如何知道应用启动需要多长时间？

**A**: 查看日志中的启动时间：

```json
{"@timestamp":"2026-01-01T23:02:18.032341866+08:00","message":"Starting Application..."}
{"@timestamp":"2026-01-01T23:02:43.123456789+08:00","message":"Started Application in 25.091 seconds"}
```

启动时间 = 25 秒，建议 `initialDelaySeconds` 设置为 **40-60 秒**（留有余量）。

## 参考资料

- [Kubernetes 健康检查文档](https://kubernetes.io/docs/tasks/configure-pod-container/configure-liveness-readiness-startup-probes/)
- [Spring Boot Actuator 健康检查](https://docs.spring.io/spring-boot/docs/current/reference/html/actuator.html#actuator.endpoints.health)
- [微信云托管文档](https://cloud.weixin.qq.com/cloudrun/docs)
- 项目内文档：`docs/observability-config-examples.yml` - 包含完整的 K8s 部署配置示例

## 相关文件

- `app-application/src/main/resources/application.yml` - Spring Boot Actuator 配置
- `docs/observability-config-examples.yml` - Kubernetes 部署配置示例
- `Dockerfile` - 容器构建配置

