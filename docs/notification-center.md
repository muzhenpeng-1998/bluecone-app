# 统一消息中心使用指南

## 概述

统一消息中心（Notification Hub）是 bluecone-app 的核心通知基础设施，提供以下能力：

- **多渠道通知**：支持站内信、邮件、微信订阅消息、短信（后两者预留）
- **模板化管理**：业务层只传变量，文案集中管理
- **幂等性保证**：通过 `tenantId:bizType:bizId:channel` 确保不重复发送
- **指数退避重试**：失败任务自动重试，间隔 2^n 分钟
- **频控与免打扰**：防止骚扰用户，支持每日上限、静默时间窗
- **审计与回溯**：所有发送记录可追溯，支持问题排查

## 架构设计

### 数据流

```
业务事件 (OutboxEvent)
    ↓
NotifyTaskCreator (消费 outbox，生成 notify_task)
    ↓
bc_notify_task (待发送任务队列)
    ↓
NotifyDispatcherJob (定时扫描，调度发送)
    ↓
RateLimitService (频控检查)
    ↓
NotificationChannel (渠道适配器)
    ↓
bc_notify_send_log (发送日志)
```

### 核心表结构

1. **bc_notify_template**：通知模板表
   - 存储模板文案（支持变量占位符 `{{varName}}`）
   - 支持租户级模板覆盖
   - 可配置频控策略

2. **bc_notify_task**：通知任务表
   - 从 outbox 事件生成的待发送任务
   - 支持状态流转：PENDING → SENDING → SENT/FAILED/RATE_LIMITED
   - 幂等键：`uk_idempotency_key`

3. **bc_notify_send_log**：发送日志表
   - 记录每次发送尝试（成功/失败）
   - 用于审计、频控统计、问题排查

4. **bc_notify_user_preference**：用户偏好表
   - 用户级渠道开关、免打扰时间
   - 可选，默认使用策略配置

## 快速开始

### 1. 创建通知模板

通过管理后台或直接插入数据库：

```sql
INSERT INTO bc_notify_template (
    template_code, template_name, biz_type, channel,
    title_template, content_template, status, priority
) VALUES (
    'INVOICE_PAID_REMINDER',
    '账单支付成功提醒',
    'INVOICE_PAID',
    'IN_APP',
    '账单支付成功',
    '您的账单 {{invoiceNo}} 已支付成功，订阅计划 {{planName}} 已生效至 {{effectiveEndAt}}',
    'ENABLED',
    50
);

INSERT INTO bc_notify_template (
    template_code, template_name, biz_type, channel,
    title_template, content_template, status, priority
) VALUES (
    'INVOICE_PAID_REMINDER',
    '账单支付成功邮件',
    'INVOICE_PAID',
    'EMAIL',
    '账单支付成功 - BlueCone',
    '尊敬的用户：\n\n您的账单 {{invoiceNo}} 已支付成功，金额 {{amount}} 元。\n\n订阅计划：{{planName}}\n生效期限：{{effectiveStartAt}} 至 {{effectiveEndAt}}\n\n感谢您的支持！',
    'ENABLED',
    50
);
```

### 2. 定义通知策略

在 `app-notify` 模块创建策略类：

```java
@Component
public class MyBusinessPolicy extends AbstractNotificationPolicy {
    
    @Override
    public String getBizType() {
        return "MY_BUSINESS";
    }
    
    @Override
    public String getTemplateCode() {
        return "MY_BUSINESS_TEMPLATE";
    }
    
    @Override
    public List<NotificationChannel> getSupportedChannels() {
        return Arrays.asList(NotificationChannel.IN_APP, NotificationChannel.EMAIL);
    }
    
    @Override
    public Map<String, Object> extractVariables(Object eventPayload) {
        // 从事件中提取模板变量
        Map<String, Object> variables = new HashMap<>();
        // ... 解析 eventPayload
        return variables;
    }
}
```

### 3. 业务层调用

#### 方式一：通过 OutboxEvent（推荐）

业务代码发布领域事件，由 `NotifyTaskCreator` 自动消费：

```java
@Service
public class BillingService {
    
    @Autowired
    private OutboxEventPublisher eventPublisher;
    
    @Transactional
    public void handleInvoicePaid(Invoice invoice) {
        // 业务逻辑
        invoice.markAsPaid();
        invoiceRepository.save(invoice);
        
        // 发布事件（写入 bc_outbox_event）
        InvoicePaidEvent event = new InvoicePaidEvent(
            invoice.getId(), 
            invoice.getInvoiceNo(), 
            invoice.getTenantId(),
            // ... 其他字段
        );
        eventPublisher.publish(event);
        
        // 消息中心会自动消费该事件，生成通知任务
    }
}
```

#### 方式二：直接调用 NotificationFacade

```java
@Service
public class MyBusinessService {
    
    @Autowired
    private NotificationFacade notificationFacade;
    
    public void sendNotification() {
        EnqueueNotificationRequest request = EnqueueNotificationRequest.builder()
                .bizType("MY_BUSINESS")
                .bizId("BIZ001")
                .tenantId(1L)
                .userId(100L)
                .templateCode("MY_BUSINESS_TEMPLATE")
                .channels(Arrays.asList(NotificationChannel.IN_APP, NotificationChannel.EMAIL))
                .variables(Map.of(
                    "orderNo", "ORDER001",
                    "amount", "99.00"
                ))
                .build();
        
        EnqueueNotificationResponse response = notificationFacade.enqueue(request);
        log.info("Created tasks: {}", response.getTaskIds());
    }
}
```

### 4. 查看发送状态

```java
String status = notificationFacade.getTaskStatus(taskId);
// 返回：PENDING / SENDING / SENT / FAILED / RATE_LIMITED / CANCELLED
```

## 通知策略说明

### 内置策略

| 策略 | 业务类型 | 渠道 | 每日上限 | 免打扰 |
|-----|---------|------|---------|-------|
| InvoicePaidPolicy | INVOICE_PAID | IN_APP + EMAIL | 5 | 22:00-08:00 |
| RenewalSuccessPolicy | RENEWAL_SUCCESS | IN_APP + EMAIL | 3 | 22:00-08:00 |
| OrderReadyPolicy | ORDER_READY | IN_APP | 20 | 否 |
| RefundSuccessPolicy | REFUND_SUCCESS | IN_APP | 10 | 22:00-08:00 |

### 频控规则

1. **每日上限**：同用户同渠道每日最多发送 N 条（根据策略配置）
2. **免打扰时间**：默认晚上 22:00 至早上 08:00 不发送（可通过用户偏好自定义）
3. **频控检查时机**：Dispatcher 调度任务前检查
4. **被限制任务**：状态标记为 `RATE_LIMITED`，不再重试

### 自定义策略

继承 `AbstractNotificationPolicy` 并实现以下方法：

```java
public class CustomPolicy extends AbstractNotificationPolicy {
    
    @Override
    public String getBizType() {
        return "CUSTOM_BIZ";
    }
    
    @Override
    public String getTemplateCode() {
        return "CUSTOM_TEMPLATE";
    }
    
    // 可选：覆盖默认配置
    @Override
    public int getDailyLimit() {
        return 15; // 自定义每日上限
    }
    
    @Override
    public boolean isQuietHoursEnabled() {
        return false; // 禁用免打扰
    }
}
```

## 渠道配置

### 站内信（IN_APP）

默认已实现，将通知写入站内信箱表或推送到 WebSocket。

### 邮件（EMAIL）

需要配置 Spring Mail：

```yaml
spring:
  mail:
    host: smtp.example.com
    port: 587
    username: noreply@bluecone.com
    password: ${MAIL_PASSWORD}
    properties:
      mail:
        smtp:
          auth: true
          starttls:
            enable: true
```

**注意**：需要从用户服务查询用户邮箱地址。

### 微信订阅消息（WECHAT）

预留接口，需实现：

1. 在 `WeChatNotificationChannel` 中集成微信小程序 API
2. 配置 AppID、AppSecret
3. 从用户服务查询 OpenID
4. 调用订阅消息发送接口

### 短信（SMS）

预留接口，需实现：

1. 在 `SmsNotificationChannel` 中集成短信服务商（阿里云/腾讯云）
2. 配置 AccessKey、SecretKey
3. 从用户服务查询手机号
4. 调用短信发送接口

## 幂等性保证

### 幂等键设计

```
idempotency_key = tenantId:bizType:bizId:channel
```

示例：
- `1:INVOICE_PAID:INV001:IN_APP`
- `1:INVOICE_PAID:INV001:EMAIL`

### 保证机制

1. 数据库唯一约束：`uk_idempotency_key`
2. `NotifyTaskCreator` 创建前检查是否已存在
3. 重复调用返回已有任务 ID，不创建新任务

### 场景

- Outbox 重复投递
- 业务代码重复调用
- 手动重放事件

## 重试策略

### 指数退避

- 第 1 次失败：2 分钟后重试
- 第 2 次失败：4 分钟后重试
- 第 3 次失败：8 分钟后重试
- 超过 `max_retry_count`（默认 3）：不再重试

### 失败原因

失败原因记录在 `notify_task.last_error` 和 `notify_send_log.error_message`：

- `EMAIL_ERROR`: 邮件发送失败
- `IN_APP_ERROR`: 站内信写入失败
- `NOT_IMPLEMENTED`: 渠道未实现
- `RATE_LIMITED`: 被频控限制

### 手动重试

更新任务状态为 `PENDING`，Dispatcher 会重新调度：

```sql
UPDATE bc_notify_task 
SET status = 'PENDING', retry_count = 0, next_retry_at = NULL
WHERE id = ?;
```

## 监控与告警

### Prometheus 指标

| 指标 | 说明 |
|-----|------|
| `notify_task_created_total{biz_type,channel}` | 任务创建总数 |
| `notify_task_sent_total{biz_type,channel}` | 发送成功总数 |
| `notify_task_failed_total{biz_type,channel,error_code}` | 发送失败总数 |
| `notify_task_rate_limited_total{biz_type,channel}` | 频控限制总数 |
| `notify_task_send_duration_seconds{biz_type,channel}` | 发送耗时 |
| `notify_task_status_count{status}` | 各状态任务数 |

### 告警规则（示例）

```yaml
# Prometheus Alert Rules
groups:
  - name: notification
    rules:
      - alert: HighNotificationFailureRate
        expr: |
          rate(notify_task_failed_total[5m]) / 
          rate(notify_task_sent_total[5m]) > 0.1
        for: 10m
        annotations:
          summary: "通知发送失败率过高（>10%）"
      
      - alert: TooManyPendingTasks
        expr: notify_task_status_count{status="PENDING"} > 1000
        for: 15m
        annotations:
          summary: "待发送任务堆积（>1000）"
```

## 常见问题

### 1. 任务一直处于 PENDING 状态

**原因**：Dispatcher 未启动或频控限制

**排查**：
- 检查 `@EnableScheduling` 是否启用
- 查看日志是否有 `dispatchPendingTasks` 输出
- 检查 `rate_limit_checked_at` 和 `rate_limit_passed` 字段

### 2. 模板变量未渲染

**原因**：变量名拼写错误或未传递

**解决**：
- 检查模板中占位符 `{{varName}}` 是否正确
- 确认 `variables` Map 包含所有必要字段
- 查看 `notify_task.variables` 字段值

### 3. 邮件发送失败

**原因**：SMTP 配置错误或用户无邮箱

**排查**：
- 检查 `spring.mail.*` 配置
- 查看 `notify_send_log.error_message`
- 确认用户服务返回有效邮箱地址

### 4. 如何禁止某类通知

**方法一**：禁用模板

```sql
UPDATE bc_notify_template 
SET status = 'DISABLED' 
WHERE template_code = 'XXX';
```

**方法二**：用户偏好关闭渠道

```sql
UPDATE bc_notify_user_preference 
SET channel_preferences = JSON_SET(channel_preferences, '$.EMAIL', false) 
WHERE user_id = ?;
```

## 最佳实践

1. **模板文案**：由产品/运营统一管理，不要散落在代码中
2. **变量命名**：使用清晰的变量名（如 `orderNo` 而非 `id`）
3. **策略配置**：根据业务重要性设置合理的频控上限
4. **日志审计**：定期归档 `bc_notify_send_log`，避免表过大
5. **性能优化**：Dispatcher 批量大小根据 QPS 调整（默认 100）
6. **告警配置**：监控失败率、堆积任务，及时发现问题

## 扩展开发

### 添加新渠道

1. 实现 `NotificationChannel` 接口
2. 注册为 Spring Bean（`@Component`）
3. 在 `NotificationChannelRegistry` 中自动加载
4. 配置渠道参数（AccessKey、模板ID等）

### 集成 Outbox 消费

在 `NotifyTaskCreator` 中添加事件消费逻辑：

```java
@EventListener
public void onInvoicePaid(InvoicePaidEvent event) {
    createTasks(
        InvoicePaidPolicy.BIZ_TYPE,
        event.getInvoiceNo(),
        event.getTenantId(),
        event.getUserId(),  // 需要从 metadata 获取
        event
    );
}
```

## 数据清理

定期归档或删除旧日志：

```sql
-- 删除 90 天前的发送日志
DELETE FROM bc_notify_send_log 
WHERE sent_at < DATE_SUB(NOW(), INTERVAL 90 DAY);

-- 删除已发送且超过 30 天的任务
DELETE FROM bc_notify_task 
WHERE status = 'SENT' 
AND sent_at < DATE_SUB(NOW(), INTERVAL 30 DAY);
```

## 总结

统一消息中心提供了完整的通知基础设施，业务模块只需：

1. **定义策略**：指定业务类型、渠道、频控规则
2. **创建模板**：配置文案模板和变量
3. **发布事件**：业务代码发布领域事件或直接调用 Facade

系统自动处理：

- 任务创建与幂等
- 多渠道发送
- 失败重试
- 频控与免打扰
- 审计与监控

遵循本文档的最佳实践，可构建稳定、高效的消息通知系统。
