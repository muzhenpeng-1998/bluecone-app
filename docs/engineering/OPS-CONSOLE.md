# Minimal Ops Console

## 设计目标

- 只读：不提供任何重放、删除、清理等写操作，避免误操作影响生产。
- 低成本：依赖已有 Outbox / Idempotency / Consume 表和 JVM 指标，默认只做轻量统计（计数、简单聚合），并通过缓存避免高频 DB count。
- 强安全：默认关闭，通过独立 token 控制访问，仅暴露 /ops 下的只读接口。

## 如何开启

在应用配置中添加（建议使用环境变量注入 token）：

```yaml
bluecone:
  ops:
    console:
      enabled: true
      token: "请填强随机"
      allowLocalhost: false
      allowQueryToken: false
      cacheTtl: PT2S
```

说明：

- `enabled`：默认 `false`，不开启时 `/ops/**` 返回 404。
- `token`：访问 `/ops` API 的共享密钥，必须非空。
- `allowLocalhost`：默认为 `true`，允许本机（127.0.0.1 / ::1）免 token 访问，生产环境建议显式设为 `false`。
- `allowQueryToken`：是否允许从 `?token=xxx` 读取 token，默认 `false`；仅在临时排障时可打开。
- `cacheTtl`：`/ops/api/summary` 的缓存 TTL，默认 2 秒。

## 如何访问

1. 启动应用并确认配置已生效。
2. 浏览器访问：
   - `https://your-host/ops/console`
3. 首次访问时在页面顶部输入 token 并保存（保存在浏览器 localStorage，不回显原文）。
4. 页面会每 3 秒自动调用：
   - `GET /ops/api/summary`（携带 `X-Ops-Token` 头）

如果需要通过 URL 注入 token（便于远程排障），可在开启 `allowQueryToken=true` 后访问：

- `https://your-host/ops/console?token=YOUR_TOKEN`

前端会从 URL 读出 token 写入 localStorage，之后仅通过 Header 访问 API。

## 部署建议（微信云托管）

- 使用环境变量注入 token，例如：
  - `BLUECONE_OPS_CONSOLE_ENABLED=true`
  - `BLUECONE_OPS_CONSOLE_TOKEN=随机高熵字符串`
- 如有网关或接入层，建议对 `/ops/**` 做额外保护：
  - IP 白名单（仅运维跳板机）
  - VPC 内网访问限制
  - 限制暴露域名（不在主业务域名上开放）
- 不建议在公共网络上长期暴露 `/ops`，即使有 token 保护，也应结合网关策略。

## 指标解释

### Outbox

- `ready`：Outbox 中处于 `NEW` 状态的消息数量，即等待投递的 backlog。
- `processing`：当前实现没有持久化 PROCESSING 状态，固定显示 `-1` 表示 N/A。
- `failed`：`FAILED + DEAD` 状态的消息数量。
- `oldestAgeSeconds`：
  - 最老一条 `NEW` 消息距离当前时间的秒数。
  - 数值越大，说明 backlog 堆积时间越长，需要关注投递是否卡住。
- `recentErrors`：
  - 目前从消费侧表中选取最近失败记录的 error_msg（最多 5 条），用于快速感知近期开票错误。

### Consume

- `retryReady`：
  - 状态为 FAILED 且 `next_retry_at <= now` 的记录数量。
  - 值升高意味着大量失败记录已经达到重试时间但尚未被重试，可能是重试 Job 停止或落后。
- `inProgressLocks`：
  - 状态为 PROCESSING 且 `locked_until > now` 的记录数量。
  - 持续升高说明大量消费处于持锁状态，需关注是否存在长时间占锁或死锁。
- `groups`：
  - 按 `consumer_group` 维度统计过去 5 分钟的消费情况。
  - `Succ/Fail` 为 5 分钟内成功/失败比例（无数据时显示 NA）。
  - `Avg/P95` 为 5 分钟内消费时延的平均值和 P95，单位毫秒。

### Idempotency

- `conflictRate10m`：
  - 当前实现无法从幂等存储直接恢复冲突次数，固定返回 `-1` 表示 N/A。
- `inProgressRate5m`：
  - 最近 5 分钟内更新的幂等记录中，状态为 PROCESSING 的比例。
  - 比例较高可能说明有较多幂等请求仍在处理，需结合业务波峰判断。
- `avgLatencyMs5m`：
  - 最近 5 分钟内成功（SUCCEEDED）记录的平均处理耗时（createdAt → updatedAt）。

### Create（幂等创建）

- 当前版本尚未接入专用 create 指标，以下字段均返回 `-1` 表示 N/A：
  - `successRate5m`
  - `failureRate5m`
  - `avgTxLatencyMs5m`

后续如接入 create 级别的 Micrometer 指标，可在不改动 API 结构的前提下填充这些字段。

## 注意事项

- 所有 `/ops/**` 接口均为只读，不提供重放、删除、清理等操作。
- Token 不会在页面上回显，也不会写入前端日志。
- 服务端日志仅在校验失败时记录掩码后的 token（前 4 位 + `****`），避免敏感信息泄漏。
- 在未启用时（`enabled=false`），访问 `/ops/**` 统一返回 404，减少被扫描暴露的攻击面。

