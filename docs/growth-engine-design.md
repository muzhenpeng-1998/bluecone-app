# 增长引擎 E1：邀新归因 + 首单触发奖励 + 幂等对账 + 通知

## 概述

增长引擎 E1 是 BlueCone 平台的最小可用增长系统，支持：
- 邀请链接/邀请码生成
- 归因绑定（新客绑定老客）
- 首单触发奖励（券/储值/积分）
- 奖励发放幂等与可补偿
- 消息中心通知到账

## 系统架构

### 模块结构

```
app-growth-api/          # API 契约层
├── dto/                 # 数据传输对象
├── enums/               # 枚举定义
└── facade/              # 外部接口

app-growth/              # 核心实现层
├── domain/              # 领域层
│   ├── model/           # 领域模型
│   ├── repository/      # 仓储接口
│   └── service/         # 领域服务
├── application/         # 应用层
│   ├── reward/          # 奖励发放器
│   └── facade/          # 门面实现
├── infrastructure/      # 基础设施层
│   ├── persistence/     # 持久化
│   ├── repository/      # 仓储实现
│   └── config/          # 配置
└── controller/          # 控制器层
```

### 核心数据模型

#### 1. bc_growth_campaign - 增长活动表

```sql
CREATE TABLE bc_growth_campaign (
    id BIGINT PRIMARY KEY,
    tenant_id BIGINT NOT NULL,
    campaign_code VARCHAR(64) NOT NULL,    -- 活动编码（唯一）
    campaign_name VARCHAR(128) NOT NULL,
    campaign_type VARCHAR(32) NOT NULL,    -- INVITE-邀新
    status VARCHAR(32) NOT NULL,           -- DRAFT/ACTIVE/PAUSED/ENDED
    rules_json TEXT NOT NULL,              -- 奖励规则JSON
    start_time DATETIME NOT NULL,
    end_time DATETIME,
    UNIQUE KEY uk_tenant_code (tenant_id, campaign_code)
);
```

**rules_json 格式示例：**
```json
{
  "inviterRewards": [
    {
      "type": "COUPON",
      "value": "{\"templateId\": 123}",
      "description": "满50减10优惠券"
    },
    {
      "type": "POINTS",
      "value": "{\"points\": 100}",
      "description": "100积分"
    }
  ],
  "inviteeRewards": [
    {
      "type": "WALLET_CREDIT",
      "value": "{\"amount\": 1000}",
      "description": "10元储值"
    }
  ]
}
```

#### 2. bc_growth_invite_code - 邀请码表

```sql
CREATE TABLE bc_growth_invite_code (
    id BIGINT PRIMARY KEY,
    tenant_id BIGINT NOT NULL,
    campaign_code VARCHAR(64) NOT NULL,
    invite_code VARCHAR(32) NOT NULL,       -- 邀请码（全局唯一）
    inviter_user_id BIGINT NOT NULL,        -- 邀请人（老客）
    invites_count INT DEFAULT 0,            -- 邀请人数
    successful_invites_count INT DEFAULT 0, -- 成功邀请（完成首单）
    UNIQUE KEY uk_invite_code (invite_code),
    UNIQUE KEY uk_tenant_campaign_inviter (tenant_id, campaign_code, inviter_user_id)
);
```

#### 3. bc_growth_attribution - 归因关系表

```sql
CREATE TABLE bc_growth_attribution (
    id BIGINT PRIMARY KEY,
    tenant_id BIGINT NOT NULL,
    campaign_code VARCHAR(64) NOT NULL,
    invite_code VARCHAR(32) NOT NULL,
    inviter_user_id BIGINT NOT NULL,        -- 邀请人
    invitee_user_id BIGINT NOT NULL,        -- 被邀请人（新客）
    status VARCHAR(32) NOT NULL,            -- PENDING/CONFIRMED/INVALID
    bound_at DATETIME NOT NULL,             -- 绑定时间
    confirmed_at DATETIME,                  -- 确认时间（首单完成）
    first_order_id BIGINT,                  -- 首单ID
    UNIQUE KEY uk_tenant_campaign_invitee (tenant_id, campaign_code, invitee_user_id)
);
```

**关键约束：** 同一用户在同一活动只能绑定一次（uk_tenant_campaign_invitee）

#### 4. bc_growth_reward_issue_log - 奖励发放日志表

```sql
CREATE TABLE bc_growth_reward_issue_log (
    id BIGINT PRIMARY KEY,
    tenant_id BIGINT NOT NULL,
    campaign_code VARCHAR(64) NOT NULL,
    idempotency_key VARCHAR(256) NOT NULL, -- 幂等键（全局唯一）
    attribution_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    user_role VARCHAR(32) NOT NULL,        -- INVITER/INVITEE
    reward_type VARCHAR(32) NOT NULL,      -- COUPON/WALLET_CREDIT/POINTS
    reward_value VARCHAR(256) NOT NULL,    -- 奖励值JSON
    issue_status VARCHAR(32) NOT NULL,     -- PROCESSING/SUCCESS/FAILED
    result_id VARCHAR(128),                -- 发放结果ID（券ID/流水ID）
    error_code VARCHAR(64),
    error_message TEXT,
    trigger_order_id BIGINT NOT NULL,      -- 触发订单ID
    issued_at DATETIME,
    UNIQUE KEY uk_tenant_idempotency (tenant_id, idempotency_key)
);
```

**幂等键格式：**
```
reward:{tenantId}:{campaignCode}:{attributionId}:{userId}:{userRole}:{rewardType}
```

示例：
```
reward:1:INVITE_2025:100:1001:INVITER:COUPON
reward:1:INVITE_2025:100:2002:INVITEE:WALLET_CREDIT
```

## 核心流程

### 1. 邀请码生成流程

```
老客 → GET /api/growth/invite?campaignCode=INVITE_2025
     ↓
[检查活动有效性]
     ↓
[查询或生成邀请码] → bc_growth_invite_code
     ↓
返回: {inviteCode, inviteLink, invitesCount, successfulInvitesCount}
```

**邀请码生成规则：**
- 基于 `tenantId + campaignCode + userId` 生成 8 位字符串
- 使用 MD5 哈希 + Base64 映射到自定义字符集（去除易混淆字符）
- 确保全局唯一（数据库唯一约束）

### 2. 归因绑定流程

```
新客 → POST /api/growth/bind {inviteCode, campaignCode}
     ↓
[验证邀请码有效性]
     ↓
[反作弊检查]
  - 邀请人 ≠ 被邀请人
  - 同活动只能绑定一次
     ↓
[创建归因关系] → bc_growth_attribution (status=PENDING)
     ↓
[更新邀请计数] → bc_growth_invite_code.invites_count++
     ↓
返回: {success, attributionId, message}
```

### 3. 首单触发奖励流程

```
支付成功 → PAYMENT_SUCCESS 事件
     ↓
GrowthEventConsumer 消费事件
     ↓
[检查是否首单]
  - 统计该用户已支付订单数
  - 如果 count = 1，则为首单
     ↓
[查询归因关系]
  - 查询该用户的 PENDING 状态归因
     ↓
[确认归因] → bc_growth_attribution (status=CONFIRMED)
     ↓
[更新成功邀请计数] → bc_growth_invite_code.successful_invites_count++
     ↓
[解析活动规则] → campaign.rules_json
     ↓
并行发放奖励：
  ├─ 发放被邀请人奖励 (INVITEE)
  └─ 发放邀请人奖励 (INVITER)
```

### 4. 奖励发放流程（幂等）

```
RewardIssuanceService.issueReward()
     ↓
[构造幂等键]
  - reward:{tenantId}:{campaign}:{attribution}:{userId}:{role}:{type}
     ↓
[幂等检查] → bc_growth_reward_issue_log
  - 已存在且成功 → 返回已有结果
  - 已存在且失败 → 抛出原错误
  - 不存在 → 继续
     ↓
[创建发放日志] → status=PROCESSING
     ↓
[调用具体奖励发放器]
  ├─ CouponRewardIssuer
  │    └─ CouponGrantService.grantCoupon()
  ├─ WalletRewardIssuer
  │    └─ WalletDomainService.credit()
  └─ PointsRewardIssuer
       └─ PointsDomainService.earnPoints()
     ↓
[更新日志] → status=SUCCESS/FAILED
     ↓
[发送通知] → NotificationFacade.enqueue()
```

## 反作弊规则

### 1. 基础规则

- **自我邀请拦截：** `inviter_user_id ≠ invitee_user_id`
- **重复绑定拦截：** 唯一约束 `uk_tenant_campaign_invitee`
- **重复触发拦截：** 幂等键 `uk_tenant_idempotency`

### 2. 首单判定

```java
int paidOrdersCount = attributionRepository.countPaidOrdersByUser(tenantId, userId);
if (paidOrdersCount != 1) {
    // 非首单，跳过
    return;
}
```

**SQL 实现：**
```sql
SELECT COUNT(DISTINCT o.id) 
FROM bc_order o 
WHERE o.tenant_id = #{tenantId} 
  AND o.user_id = #{userId} 
  AND o.pay_status = 'PAID'
```

## 幂等保证

### 1. 邀请码生成幂等

- **唯一约束：** `uk_tenant_campaign_inviter`
- **并发处理：** `DuplicateKeyException` → 重新查询返回

### 2. 归因绑定幂等

- **唯一约束：** `uk_tenant_campaign_invitee`
- **并发处理：** 捕获异常 → 重新查询返回

### 3. 奖励发放幂等

- **唯一约束：** `uk_tenant_idempotency`
- **状态机：** PROCESSING → SUCCESS/FAILED
- **幂等返回：**
  - SUCCESS → 返回已有 `resultId`
  - FAILED → 抛出原错误
  - PROCESSING → 抛出"处理中"错误

## API 接口

### 1. 用户端接口

#### GET /api/growth/invite

获取或生成邀请码

**请求参数：**
- `campaignCode` (query, required): 活动编码

**响应示例：**
```json
{
  "code": 0,
  "data": {
    "inviteCode": "A3F8K2M9",
    "campaignCode": "INVITE_2025",
    "inviteLink": "https://app.bluecone.com/invite?code=A3F8K2M9&campaign=INVITE_2025",
    "invitesCount": 5,
    "successfulInvitesCount": 2
  }
}
```

#### POST /api/growth/bind

绑定邀请码

**请求体：**
```json
{
  "inviteCode": "A3F8K2M9",
  "campaignCode": "INVITE_2025"
}
```

**响应示例：**
```json
{
  "code": 0,
  "data": {
    "success": true,
    "attributionId": 1001,
    "campaignCode": "INVITE_2025",
    "inviteCode": "A3F8K2M9",
    "message": "绑定成功"
  }
}
```

### 2. 管理端接口

#### POST /admin/growth/campaigns

创建活动

**请求体：**
```json
{
  "campaignCode": "INVITE_2025",
  "campaignName": "2025春节邀新活动",
  "campaignType": "INVITE",
  "rules": {
    "inviterRewards": [
      {
        "type": "COUPON",
        "value": "{\"templateId\": 123}",
        "description": "满50减10优惠券"
      }
    ],
    "inviteeRewards": [
      {
        "type": "WALLET_CREDIT",
        "value": "{\"amount\": 1000}",
        "description": "10元储值"
      }
    ]
  },
  "startTime": "2025-01-01T00:00:00",
  "endTime": "2025-02-01T23:59:59",
  "description": "邀请好友下单，双方得奖励"
}
```

#### PUT /admin/growth/campaigns/{campaignCode}

更新活动（仅 DRAFT/PAUSED 状态可修改）

#### GET /admin/growth/campaigns/{campaignCode}

获取活动详情

#### GET /admin/growth/campaigns

获取活动列表

## 监控指标

### Prometheus Metrics

```
# 绑定事件总数
growth.bind.total

# 奖励发放成功总数（按类型）
growth.reward.issued.total{kind="all"}
growth.reward.issued.total{kind="coupon"}
growth.reward.issued.total{kind="wallet_credit"}
growth.reward.issued.total{kind="points"}

# 奖励发放失败总数（按错误码）
growth.reward.failed.total{error_code="..."}

# 奖励发放耗时
growth.reward.issue.duration
```

### 监控看板关键指标

- **绑定数：** `growth.bind.total`
- **发奖成功率：** `growth.reward.issued.total / (growth.reward.issued.total + growth.reward.failed.total)`
- **发奖耗时 P95：** `growth.reward.issue.duration{quantile="0.95"}`

## 测试场景

### 1. 幂等测试

```java
// 重复发放奖励，应返回相同结果
RewardIssueLog log1 = rewardIssuanceService.issueReward(...);
RewardIssueLog log2 = rewardIssuanceService.issueReward(...);
assert log1.getResultId().equals(log2.getResultId());
```

### 2. 首单判定测试

```java
// 用户首单 → 触发奖励
// 用户第二单 → 跳过
```

### 3. 反作弊测试

```java
// 自我邀请 → 抛出异常
// 重复绑定 → 返回已绑定
```

## 部署验收

### E1 验收标准

✅ **完整链路验证：**
1. 老客生成邀请码 → 获得 8 位邀请码
2. 新客绑定邀请码 → 归因关系创建（PENDING）
3. 新客下单并支付 → 归因确认（CONFIRMED）
4. 奖励自动发放 → 券/余额实际可用
5. 老客收到通知 → "您的好友已下单，奖励已到账"
6. 新客收到通知 → "首单奖励已到账"

✅ **幂等验证：**
- 重放 PAYMENT_SUCCESS 事件 → 不重复发奖
- 手动重试发奖接口 → 返回已有结果
- 数据库 `uk_tenant_idempotency` 约束生效

✅ **后台可见：**
- 活动列表：状态、时间范围、奖励规则
- 绑定数：`bc_growth_attribution` 记录数
- 发奖数：`bc_growth_reward_issue_log` SUCCESS 记录数
- 失败原因：`error_code` + `error_message`

## 已知限制与待优化

### 当前限制

1. **单活动绑定：** 用户在同一租户的同一活动只能绑定一次
2. **储值发放器未完全实现：** 需要 `app-wallet` 提供 CREDIT 接口
3. **通知未实现：** 需要集成 `app-notify` 模块
4. **邀请链接固定：** 需要前端 H5 页面支持

### 待优化点

1. **多活动支持：** 用户可同时参与多个邀新活动
2. **奖励延迟发放：** 支持订单完成后 N 天再发放
3. **阶梯奖励：** 邀请 1/5/10 人给予不同奖励
4. **作弊检测增强：** IP/设备指纹/地域/时间窗口等
5. **奖励回收：** 订单退款后回收奖励

## 技术栈

- **语言：** Java 21
- **框架：** Spring Boot 3.2.5
- **ORM：** MyBatis Plus 3.5.7
- **数据库：** MySQL 8.0
- **监控：** Micrometer + Prometheus
- **事件驱动：** Outbox Pattern

---

**文档版本：** E1-v1.0  
**更新时间：** 2025-12-19  
**维护人：** Growth Engine Team
