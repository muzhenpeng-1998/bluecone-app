# app-notify - 统一消息中心

## 模块说明

`app-notify` 是 bluecone-app 的统一消息中心实现模块，提供多渠道通知能力。

## 功能特性

- ✅ 多渠道通知（站内信、邮件、微信、短信）
- ✅ 模板化管理（支持变量替换）
- ✅ 幂等性保证（唯一键约束）
- ✅ 指数退避重试（2^n 分钟）
- ✅ 频控与免打扰
- ✅ 审计与回溯
- ✅ Prometheus 指标

## 模块结构

```
app-notify/
├── domain/
│   ├── model/          # 领域模型（NotifyTask、NotifyTemplate 等）
│   ├── repository/     # 仓储接口
│   ├── service/        # 领域服务（NotifyTaskCreator、RateLimitService）
│   └── policy/         # 通知策略（InvoicePaidPolicy 等）
├── infrastructure/
│   ├── dao/            # MyBatis 数据对象与 Mapper
│   ├── repository/     # 仓储实现
│   └── converter/      # DO ↔ Domain 转换器
├── application/        # 应用层（Facade 实现）
├── channel/            # 渠道适配器（InApp、Email、WeChat、SMS）
├── scheduler/          # 调度任务（NotifyDispatcherJob）
└── config/             # 配置类（Metrics、Configuration）
```

## 核心组件

### NotifyTaskCreator

消费 outbox 事件，根据策略生成通知任务。

### NotifyDispatcherJob

定时调度任务，扫描 PENDING/FAILED 状态任务并发送。

### RateLimitService

频控检查，防止骚扰用户。

### NotificationChannel

渠道适配器接口，支持：
- `InAppNotificationChannel`：站内信
- `EmailNotificationChannel`：邮件
- `WeChatNotificationChannel`：微信订阅消息（预留）
- `SmsNotificationChannel`：短信（预留）

### NotificationPolicy

通知策略，定义业务类型的渠道、频控规则、变量提取逻辑。

## 依赖关系

```
app-notify
├── app-notify-api      # API 契约
├── app-core            # 核心基础设施
├── app-infra           # 数据库迁移
├── app-id-api          # ID 生成（可选）
└── spring-boot-starter-mail  # 邮件发送
```

## 使用示例

见 [docs/notification-center.md](../docs/notification-center.md)

## 测试

```bash
mvn test -pl app-notify
```

## 数据库迁移

迁移脚本位于 `app-infra/src/main/resources/db/migration/V20251219009__create_notification_tables.sql`

包含以下表：
- `bc_notify_template`
- `bc_notify_task`
- `bc_notify_send_log`
- `bc_notify_user_preference`

## 监控

启动应用后访问 `/actuator/prometheus` 查看指标：

```
notify_task_created_total{biz_type="INVOICE_PAID",channel="IN_APP"} 100
notify_task_sent_total{biz_type="INVOICE_PAID",channel="EMAIL"} 95
notify_task_failed_total{biz_type="ORDER_READY",channel="IN_APP",error_code="IN_APP_ERROR"} 5
```

## 配置

### 邮件配置

```yaml
spring:
  mail:
    host: smtp.example.com
    port: 587
    username: noreply@bluecone.com
    password: ${MAIL_PASSWORD}
```

### 调度频率

默认配置：
- 待发送任务：每分钟扫描一次
- 重试任务：每 5 分钟扫描一次

可在 `NotifyDispatcherJob` 中修改 `@Scheduled` cron 表达式。

## License

内部项目，禁止外部使用。
