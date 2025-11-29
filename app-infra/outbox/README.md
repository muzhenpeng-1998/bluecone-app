# BlueCone Outbox（最强架构版）

## 流程
1. 业务调用 `DomainEventPublisher.publish()`（事务内）。  
2. Pipeline：Validate → Serialize → Persist → Metrics/Log。  
3. 事件以 `NEW` 状态写入 `bc_outbox_message`。  
4. 定时任务扫描 `NEW/FAILED` 且 `next_retry_at <= now`。  
5. 反序列化事件 → 路由 `EventHandler` → 执行。  
6. 成功：`PUBLISHED → DONE`；失败：`retry+1` 并回写 `FAILED/DEAD`。  
7. 清理任务定期删除过期的 `DONE/DEAD`。

## 状态机
```
NEW --dispatch--> PUBLISHED --success--> DONE
                     |--failure--> FAILED --超限--> DEAD
                                   \--重试--> NEW(或保持 FAILED 待重试)
```

## 重试机制
- 指数退避：`initialBackoffMillis * multiplier^retryCount`。  
- 超过 `maxRetries` 标记 `DEAD`，避免无限风暴。  
- `event_key` 唯一索引用于业务幂等。

## EventHandler 机制
- 复用 `EventHandler` 生态，路由器按事件 Class 映射。  
- 头部携带 `eventClass / eventType / traceId / tenantId / userId`，便于日志与链路追踪。  
- Handler 内保持无侵入：只关注业务逻辑。

## 扩展 MQ / CDC
- 增加新的 Sink/Dispatcher（Kafka/Redis Stream/Rabbit），复用 Outbox 表与 Pipeline。  
- 如需 CDC，可由 Debezium 订阅 Outbox 表推送到 MQ，无需改业务代码。

## 日志示例
- 入站：`[OutboxIn] eventType=order.paid eventId=... headers=...`  
- 投递成功：`[Outbox] delivered eventType=... outboxId=...`  
- 失败：`[Outbox] dispatch failed outboxId=... retry=... dead=false`

## 配置片段
```yaml
bluecone.outbox:
  enabled: true
  max-retries: 5
  initial-backoff-millis: 1000
  backoff-multiplier: 2.0
  dispatch-batch-size: 100
  clean-retention-days: 7
  publish-cron: "0/10 * * * * ?"
  clean-cron: "0 0 3 * * ?"
```
