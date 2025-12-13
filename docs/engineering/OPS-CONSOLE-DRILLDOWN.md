# Minimal Ops Console Drill-down

## 能力范围（只读）

- 在 Step 14 的 `/ops/console` 与 `/ops/api/summary` 基础上，提供只读下钻能力：
  - Outbox：按状态（READY/FAILED/PROCESSING/SENT）查看最近 N 条记录，支持 cursor 分页。
  - Consume：按 `consumer_group` + 状态（PROCESSING/SUCCEEDED/FAILED/RETRY）查看最近 N 条记录。
  - Idempotency：查看最近的幂等失败记录（视为“冲突”样例），仅返回 key/hash 前缀。
- 所有接口均为 GET，只读，不提供任何写操作：
  - 不提供重放、删除、清理、立即重试等操作。

## 安全说明

- 下钻 API 与控制台页面与 Step 14 共用同一访问控制：
  - 默认关闭：`bluecone.ops.console.enabled=false` 时，访问 `/ops/**` 一律返回 404。
  - 开启后必须在请求中携带正确的 token（`X-Ops-Token` 或 `Authorization: Bearer`），否则返回 404。
- `token` 必须是强随机字符串，建议通过环境变量注入，不直接写入配置文件。
- 默认不返回 payload：
  - Drill-down 的列表接口默认不包含 `payload_json` / `headers_json` 等大字段。
  - 若后续需要返回 payload，应通过 `bluecone.ops.console.exposePayload=true` 显式开启，并在响应中做截断与脱敏。
- 幂等相关字段：
  - 不返回原始 `idem_key`、`request_hash`，仅返回前缀（默认 8 位）用于排障。

## 常用排障路径

### 1. Outbox FAILED 增加

1. 在 `/ops/console` 的 Summary 页面观察：
   - `Outbox.failed` 是否持续升高。
2. 切换到 Outbox Tab：
   - 选择 Status=`FAILED`，点击行查看详情。
   - 重点关注：
     - `eventType`：失败集中在哪些事件类型。
     - `retryCount`：是否已经重试多次。
     - `nextRetryAt`：下次重试时间。
     - `createdAt`：失败记录集中出现的时间窗口。
3. 根据 `eventType` + `errorMsg`，结合业务日志进一步排查原因。

### 2. oldest_age 升高

1. 在 Summary 中看到 `Outbox.oldestAgeSeconds` 不断升高：
   - 说明最老的一条 READY (NEW) 记录长期未被处理。
2. 切换到 Outbox Tab：
   - 选择 Status=`READY`。
   - 按 CreatedAt 查看最早的几条记录，关注：
     - `eventType`：是否集中在某一类业务。
     - `createdAt`：堆积开始时间。
3. 若 READY 数量不大，但 oldest_age 很高，说明某些特定消息被“饿死”，需检查消费者/任务调度逻辑。

### 3. Consume FAILED / RETRY

1. 在 Summary 中查看 `Consume.retryReady` 与 `Consume.groups`：
   - 若 `retryReady` 持续升高，说明很多 FAILED 记录已到达可重试时间但尚未被消费。
2. 切换到 Consume Tab：
   - 从下拉框选择特定 `consumer_group`。
   - 选择 Status=`FAILED` 或 `RETRY`：
     - `FAILED`：所有失败记录。
     - `RETRY`：状态 FAILED 且 `nextRetryAt <= now`，即“可重试”记录。
   - 行点击查看详情，关注：
     - `eventType`：失败集中在哪些事件类型。
     - `retryCount`：是否已经接近重试上限。
     - `nextRetryAt`：当前退避策略是否合理。
     - `lockedUntil` / `lockedBy`：是否存在长期持锁未释放。

### 4. retry_ready 升高

- Summary 中 `retryReady` 升高一般意味着：
  - 消费端在失败后做了退避，但调度/重试任务没有及时触发。
  - 或有大量失败堆积，重试能力不足（重试间隔过长或并行度不足）。
- 建议：
  - 在 Consume Tab 中按 `RETRY` 查看具体失败记录。
  - 检查重试任务调度（如定时 Job 是否及时运行）。

## 性能与防滥用说明

- Limit 上限：
  - 由 `bluecone.ops.console.maxPageSize` 控制，默认 100。
  - 下钻 API 的 `limit` 参数会被 clamp 到 `[1, maxPageSize]`。
- Cursor 分页：
  - 使用自增主键 `id` 作为 cursor，格式为字符串 `"{lastId}"`。
  - 查询语义：`WHERE id < lastId ORDER BY id DESC LIMIT limit`。
  - `nextCursor` 为本页最小 id；前端“加载更多”时携带此 cursor 继续向后翻页。
- 列表刷新策略：
  - Summary 页面仍保持每 3 秒自动刷新。
  - Drill-down 列表默认不自动刷新，仅在用户点击切换 Tab、改变筛选条件或点击“Load more”时请求。
  - 这样可以避免在排障时对数据库产生过大压力。
- drillCacheTtl：
  - `bluecone.ops.console.drillCacheTtl` 默认 `PT1S`。
  - 后端会对相同 path+query（不含 token）进行短 TTL 缓存，避免同一页面被频繁刷新时重复执行相同 SQL。

## 配置示例

```yaml
bluecone:
  ops:
    console:
      enabled: true
      token: "换成强随机"
      cacheTtl: PT2S
      drillCacheTtl: PT1S
      maxPageSize: 100
      exposePayload: false
      maxErrorMsgLen: 200
      maxPayloadLen: 2000
```

说明：

- `enabled` / `token`：同 OPS-CONSOLE（Step 14），控制入口开关与访问密钥。
- `cacheTtl`：Summary 接口缓存 TTL。
- `drillCacheTtl`：Drill-down 接口缓存 TTL（建议 1s）。
- `maxPageSize`：列表最大行数上限，防止一次拉取过多数据。
- `exposePayload`：
  - 默认 `false`，不返回 payload。
  - 若设为 `true`，应仅在受控环境（内网 + 网关限流 + 强 token）下开启，并保证前端对 payload 做最小化展示。
- `maxErrorMsgLen` / `maxPayloadLen`：
  - 后端会对 `errorMsg` 与 payload 字段做截断，避免异常/请求体过长。

## 注意事项

- 所有 Drill-down API 均为只读 GET 请求，不会修改任何数据库状态。
- 默认不展示 payload，避免在浏览器中加载大体积或敏感数据，仅展示必要元信息（eventType、错误摘要、重试信息等）。
- cursor / limit 均通过后端校验，非法参数（非数字 cursor、不符合约束的 status/group）将返回 400。
- 为减少耦合，Drill-down 的 DTO 结构在 `app-ops` 模块中定义，底层查询通过 app-infra 中的只读 Repository 完成，并确保：
  - 使用主键或索引字段过滤。
  - 仅 select 必要列。
  - limit 受配置限制。

