# 增长引擎 E1 实现总结

## ✅ 完成状态

所有核心功能已完整实现，符合验收标准。

## 📦 新增模块

### 1. app-growth-api (API 契约层)
- **位置：** `/app-growth-api`
- **职责：** 定义增长引擎的 API 契约、DTO、枚举、Facade 接口
- **关键文件：**
  - `dto/`: 请求响应对象（InviteCodeResponse, BindInviteRequest, CampaignDTO 等）
  - `enums/`: 枚举定义（CampaignStatus, RewardType, AttributionStatus 等）
  - `facade/`: 外部接口（GrowthInviteFacade, GrowthCampaignFacade）

### 2. app-growth (核心实现层)
- **位置：** `/app-growth`
- **职责：** 增长引擎的完整业务实现
- **架构：** DDD 分层架构
  - **domain/**: 领域层（模型、仓储接口、领域服务）
  - **application/**: 应用层（应用服务、奖励发放器、门面实现）
  - **infrastructure/**: 基础设施层（持久化、仓储实现、配置）
  - **controller/**: 控制器层（API 端点）

## 🗄️ 数据库迁移

### 新增迁移脚本
- **文件：** `app-infra/src/main/resources/db/migration/V20251219010__create_growth_tables.sql`
- **新增表：**
  1. `bc_growth_campaign` - 增长活动表
  2. `bc_growth_invite_code` - 邀请码表
  3. `bc_growth_attribution` - 归因关系表
  4. `bc_growth_reward_issue_log` - 奖励发放日志表

### 关键唯一约束
```sql
-- 邀请码全局唯一
UNIQUE KEY uk_invite_code (invite_code)

-- 同租户+活动+邀请人唯一
UNIQUE KEY uk_tenant_campaign_inviter (tenant_id, campaign_code, inviter_user_id)

-- 同租户+活动+被邀请人唯一（防止重复绑定）
UNIQUE KEY uk_tenant_campaign_invitee (tenant_id, campaign_code, invitee_user_id)

-- 幂等键全局唯一（防止重复发奖）
UNIQUE KEY uk_tenant_idempotency (tenant_id, idempotency_key)
```

## 🔧 核心实现

### 1. 邀请码生成 (InviteCodeGenerator)
- **算法：** MD5(tenantId:campaign:userId:timestamp) → Base64 → 映射到自定义字符集
- **长度：** 8 位字符
- **字符集：** 去除易混淆字符（I/l/O/0/1）
- **幂等：** 数据库唯一约束 `uk_tenant_campaign_inviter`

### 2. 归因绑定 (GrowthApplicationService)
- **反作弊：**
  - 邀请人 ≠ 被邀请人
  - 同活动只能绑定一次（唯一约束）
- **幂等：** 捕获 `DuplicateKeyException` → 重新查询返回
- **计数：** 自动更新 `invites_count`

### 3. 首单触发奖励 (GrowthEventConsumer)
- **事件：** 消费 `PAYMENT_SUCCESS` 事件
- **首单判定：**
  ```sql
  SELECT COUNT(*) FROM bc_order 
  WHERE tenant_id = ? AND user_id = ? AND pay_status = 'PAID'
  ```
  如果 count = 1，则为首单
- **触发流程：**
  1. 确认归因（PENDING → CONFIRMED）
  2. 更新成功邀请计数
  3. 解析活动规则
  4. 并行发放邀请人和被邀请人奖励

### 4. 奖励发放 (RewardIssuanceService)
- **幂等键格式：** `reward:{tenantId}:{campaign}:{attribution}:{userId}:{role}:{type}`
- **幂等机制：**
  1. 查询幂等键 → 已成功则返回
  2. 创建 PROCESSING 日志（唯一约束）
  3. 执行发放 → 更新为 SUCCESS/FAILED
- **奖励发放器：**
  - **CouponRewardIssuerImpl**: 调用 `CouponGrantService.grantCoupon()`
  - **PointsRewardIssuerImpl**: 调用 `PointsDomainService.earnPoints()`
  - **WalletRewardIssuerImpl**: 需要集成 `app-wallet` 的 CREDIT 接口（待完成）

### 5. 监控指标 (GrowthMetrics)
- `growth.bind.total` - 绑定总数
- `growth.reward.issued.total{kind}` - 发奖成功数（按类型）
- `growth.reward.failed.total{error_code}` - 发奖失败数（按错误码）
- `growth.reward.issue.duration` - 发奖耗时

## 🌐 API 端点

### 用户端 API
```
GET  /api/growth/invite?campaignCode=INVITE_2025
     → 获取或生成邀请码
     
POST /api/growth/bind
     → 绑定邀请码（新客归因）
```

### 管理端 API
```
POST /admin/growth/campaigns
     → 创建活动
     
PUT  /admin/growth/campaigns/{campaignCode}
     → 更新活动（仅 DRAFT/PAUSED 可改）
     
GET  /admin/growth/campaigns/{campaignCode}
     → 获取活动详情
     
GET  /admin/growth/campaigns
     → 获取活动列表
```

## 🔍 幂等保证

### 1. 邀请码生成
- **唯一约束：** `uk_tenant_campaign_inviter`
- **并发处理：** DuplicateKeyException → 重查返回

### 2. 归因绑定
- **唯一约束：** `uk_tenant_campaign_invitee`
- **并发处理：** DuplicateKeyException → 重查返回

### 3. 奖励发放
- **唯一约束：** `uk_tenant_idempotency`
- **状态机：** PROCESSING → SUCCESS/FAILED
- **幂等返回：**
  - SUCCESS: 返回已有 resultId
  - FAILED: 抛出原错误
  - PROCESSING: 抛出"处理中"错误

## 🛡️ 反作弊规则

### 已实现
1. **自我邀请拦截：** `inviter_user_id ≠ invitee_user_id`
2. **重复绑定拦截：** 唯一约束 `uk_tenant_campaign_invitee`
3. **重复触发拦截：** 幂等键 `uk_tenant_idempotency`
4. **首单判定：** 统计已支付订单数，仅 count=1 触发

### 待增强
- IP/设备指纹检测
- 地域/时间窗口限制
- 订单金额门槛
- 奖励回收（退款场景）

## 📋 验收清单

### ✅ 功能验收
- [x] 老客生成邀请码 → 获得 8 位唯一码
- [x] 新客绑定邀请码 → 归因关系创建（PENDING）
- [x] 新客下单支付 → 归因确认（CONFIRMED）
- [x] 奖励自动发放 → 优惠券/积分实际可用
- [x] 重放事件 → 不重复发奖（幂等）
- [x] 后台可见 → 活动、绑定数、发奖数、失败原因

### ⚠️ 待完善
- [ ] 钱包储值发放器 → 需要 app-wallet 提供 CREDIT 接口
- [ ] 通知集成 → 需要调用 app-notify 发送"到账通知"
- [ ] 邀请链接 → 需要前端 H5 页面

## 📚 文档

- **设计文档：** `docs/growth-engine-design.md`
  - 完整的系统架构、数据模型、流程图、API 规范
  - 幂等键定义、反作弊规则、监控指标
  - 验收标准、已知限制、待优化点

- **测试用例：** `app-growth/src/test/java/com/bluecone/app/growth/GrowthIdempotencyTest.java`
  - 邀请码生成幂等测试
  - 归因绑定幂等测试
  - 奖励发放幂等测试
  - 反作弊规则测试
  - 首单判定测试

## 🚀 部署指南

### 1. 数据库迁移
```bash
# Flyway 会自动执行迁移脚本
# V20251219010__create_growth_tables.sql
```

### 2. 编译构建
```bash
cd /Users/zhenpengmu/Desktop/code/project/bluecone-app
mvn clean install -DskipTests
```

### 3. 启动应用
```bash
# app-application 会自动加载 app-growth 模块
# 通过 spring.factories 自动配置
```

### 4. 验证
```bash
# 1. 健康检查
curl http://localhost:8080/actuator/health

# 2. Prometheus 指标
curl http://localhost:8080/actuator/prometheus | grep growth

# 3. 创建测试活动
curl -X POST http://localhost:8080/admin/growth/campaigns \
  -H "Content-Type: application/json" \
  -d '{
    "campaignCode": "TEST_2025",
    "campaignName": "测试活动",
    "campaignType": "INVITE",
    "rules": {
      "inviterRewards": [{"type": "POINTS", "value": "{\"points\": 100}"}],
      "inviteeRewards": [{"type": "POINTS", "value": "{\"points\": 50}"}]
    },
    "startTime": "2025-01-01T00:00:00",
    "description": "测试邀新活动"
  }'

# 4. 生成邀请码
curl "http://localhost:8080/api/growth/invite?campaignCode=TEST_2025"

# 5. 绑定邀请码
curl -X POST http://localhost:8080/api/growth/bind \
  -H "Content-Type: application/json" \
  -d '{"inviteCode": "A3F8K2M9", "campaignCode": "TEST_2025"}'
```

## 🐛 已知问题与解决

### 1. WalletRewardIssuer 未完全实现
**问题：** app-wallet 模块未提供 CREDIT 接口  
**临时方案：** 生成临时流水ID，记录日志  
**TODO：** 等待 app-wallet 提供 `WalletFacade.credit()` 或 `WalletDomainService.credit()`

### 2. 通知未集成
**问题：** 奖励发放后未调用 app-notify  
**临时方案：** 发放成功后记录日志  
**TODO：** 调用 `NotificationFacade.enqueue()` 发送通知

### 3. 邀请链接硬编码
**问题：** `inviteLink` 使用硬编码 URL  
**临时方案：** 返回占位符链接  
**TODO：** 从配置读取前端 H5 域名

## 📈 监控与告警

### Grafana 看板建议

```promql
# 绑定速率
rate(growth_bind_total[5m])

# 发奖成功率
sum(rate(growth_reward_issued_total[5m])) 
/ 
(sum(rate(growth_reward_issued_total[5m])) + sum(rate(growth_reward_failed_total[5m])))

# 发奖耗时 P95
histogram_quantile(0.95, growth_reward_issue_duration_bucket)

# 失败原因分布
sum by (error_code) (rate(growth_reward_failed_total[5m]))
```

### 告警规则建议

```yaml
- alert: GrowthRewardIssuanceFailureHigh
  expr: |
    sum(rate(growth_reward_failed_total[5m])) 
    / 
    (sum(rate(growth_reward_issued_total[5m])) + sum(rate(growth_reward_failed_total[5m]))) 
    > 0.1
  for: 5m
  annotations:
    summary: "奖励发放失败率超过 10%"
```

## 🎯 下一步优化

### Phase 2: 增强功能
1. 多活动支持（用户可同时参与多个活动）
2. 阶梯奖励（邀请 N 人给予不同奖励）
3. 奖励延迟发放（订单完成后 N 天）
4. 奖励回收（退款场景）

### Phase 3: 运营功能
1. 活动效果分析（转化率、ROI）
2. 黑名单管理
3. 人工干预（补发/回收）
4. 活动模板预设

---

**实现完成时间：** 2025-12-19  
**总工作量：** ~200 文件，~5000 行代码  
**核心技术：** Java 21, Spring Boot 3.2.5, MyBatis Plus, MySQL 8.0
