# app-growth - 增长引擎模块

## 概述

增长引擎模块实现邀新归因、首单触发奖励、幂等对账和通知功能，是 BlueCone 平台的核心增长系统。

## 模块结构

```
app-growth/
├── src/main/java/com/bluecone/app/growth/
│   ├── application/              # 应用层
│   │   ├── facade/               # 门面实现
│   │   │   ├── GrowthInviteFacadeImpl.java
│   │   │   └── GrowthCampaignFacadeImpl.java
│   │   ├── reward/               # 奖励发放器
│   │   │   ├── CouponRewardIssuerImpl.java
│   │   │   ├── WalletRewardIssuerImpl.java
│   │   │   └── PointsRewardIssuerImpl.java
│   │   ├── GrowthApplicationService.java      # 核心应用服务
│   │   ├── CampaignManagementService.java     # 活动管理服务
│   │   └── GrowthEventConsumer.java           # 事件消费者
│   ├── controller/               # 控制器层
│   │   ├── GrowthInviteController.java
│   │   └── GrowthCampaignAdminController.java
│   ├── domain/                   # 领域层
│   │   ├── model/                # 领域模型
│   │   │   ├── GrowthCampaign.java
│   │   │   ├── InviteCode.java
│   │   │   ├── Attribution.java
│   │   │   └── RewardIssueLog.java
│   │   ├── repository/           # 仓储接口
│   │   │   ├── GrowthCampaignRepository.java
│   │   │   ├── InviteCodeRepository.java
│   │   │   ├── AttributionRepository.java
│   │   │   └── RewardIssueLogRepository.java
│   │   └── service/              # 领域服务
│   │       ├── InviteCodeGenerator.java
│   │       ├── RewardIssuanceService.java
│   │       └── GrowthMetrics.java
│   └── infrastructure/           # 基础设施层
│       ├── persistence/          # 持久化
│       │   ├── po/               # 持久化对象
│       │   │   ├── GrowthCampaignPO.java
│       │   │   ├── InviteCodePO.java
│       │   │   ├── AttributionPO.java
│       │   │   └── RewardIssueLogPO.java
│       │   └── mapper/           # MyBatis Mapper
│       │       ├── GrowthCampaignMapper.java
│       │       ├── InviteCodeMapper.java
│       │       ├── AttributionMapper.java
│       │       └── RewardIssueLogMapper.java
│       ├── repository/           # 仓储实现
│       │   ├── GrowthCampaignRepositoryImpl.java
│       │   ├── InviteCodeRepositoryImpl.java
│       │   ├── AttributionRepositoryImpl.java
│       │   └── RewardIssueLogRepositoryImpl.java
│       ├── converter/            # 转换器
│       │   └── GrowthConverter.java
│       └── config/               # 配置
│           └── GrowthModuleConfiguration.java
└── src/main/resources/
    └── spring.factories          # 自动配置
```

## 核心概念

### 1. 增长活动 (GrowthCampaign)

定义邀新活动的基本信息和奖励规则。

**关键属性：**
- `campaignCode`: 活动编码（唯一）
- `status`: 活动状态（DRAFT/ACTIVE/PAUSED/ENDED）
- `rulesJson`: 奖励规则（JSON格式）
- `startTime/endTime`: 活动时间范围

### 2. 邀请码 (InviteCode)

老客用于邀请新客的唯一标识。

**关键属性：**
- `inviteCode`: 8位唯一邀请码
- `inviterUserId`: 邀请人（老客）
- `invitesCount`: 邀请人数
- `successfulInvitesCount`: 成功邀请人数（完成首单）

### 3. 归因关系 (Attribution)

记录新客与老客的邀请关系。

**关键属性：**
- `inviterUserId`: 邀请人
- `inviteeUserId`: 被邀请人（新客）
- `status`: 归因状态（PENDING/CONFIRMED/INVALID）
- `firstOrderId`: 首单订单ID

**状态流转：**
```
PENDING（绑定时） → CONFIRMED（首单完成）
                 → INVALID（活动失效）
```

### 4. 奖励发放日志 (RewardIssueLog)

记录奖励发放的全过程，保证幂等性。

**关键属性：**
- `idempotencyKey`: 幂等键（全局唯一）
- `userId`: 奖励接收人
- `userRole`: 用户角色（INVITER/INVITEE）
- `rewardType`: 奖励类型（COUPON/WALLET_CREDIT/POINTS）
- `issueStatus`: 发放状态（PROCESSING/SUCCESS/FAILED）
- `resultId`: 发放结果ID（券ID/流水ID）

## 核心流程

### 邀请码生成流程

```java
@Transactional
public InviteCodeResponse getOrCreateInviteCode(Long tenantId, Long userId, String campaignCode) {
    // 1. 检查活动有效性
    GrowthCampaign campaign = campaignRepository.findByCode(tenantId, campaignCode)
            .orElseThrow(() -> new BusinessException("CAMPAIGN_NOT_FOUND", "活动不存在"));
    
    if (!campaign.isActive()) {
        throw new BusinessException("CAMPAIGN_NOT_ACTIVE", "活动未进行中");
    }
    
    // 2. 查询已有邀请码（幂等）
    Optional<InviteCode> existing = inviteCodeRepository.findByInviter(tenantId, campaignCode, userId);
    if (existing.isPresent()) {
        return buildResponse(existing.get());
    }
    
    // 3. 生成新邀请码
    String code = inviteCodeGenerator.generate(tenantId, campaignCode, userId);
    InviteCode inviteCode = InviteCode.builder()
            .id(idService.nextLong(IdScope.GROWTH))
            .tenantId(tenantId)
            .campaignCode(campaignCode)
            .inviteCode(code)
            .inviterUserId(userId)
            .build();
    
    // 4. 保存（唯一约束兜底）
    try {
        inviteCodeRepository.save(inviteCode);
    } catch (DuplicateKeyException e) {
        // 并发创建，重新查询
        inviteCode = inviteCodeRepository.findByInviter(tenantId, campaignCode, userId)
                .orElseThrow(() -> new BusinessException("INVITE_CODE_CREATE_FAILED"));
    }
    
    return buildResponse(inviteCode);
}
```

### 归因绑定流程

```java
@Transactional
public BindInviteResponse bindInviteCode(Long tenantId, Long userId, BindInviteRequest request) {
    // 1. 查询邀请码
    InviteCode inviteCode = inviteCodeRepository.findByInviteCode(request.getInviteCode())
            .orElseThrow(() -> new BusinessException("INVITE_CODE_NOT_FOUND"));
    
    // 2. 反作弊检查
    if (inviteCode.getInviterUserId().equals(userId)) {
        throw new BusinessException("SELF_INVITE_NOT_ALLOWED", "不能邀请自己");
    }
    
    // 3. 幂等检查
    Optional<Attribution> existing = attributionRepository
            .findByInvitee(tenantId, inviteCode.getCampaignCode(), userId);
    if (existing.isPresent()) {
        return buildResponse(existing.get(), "已绑定过该活动");
    }
    
    // 4. 创建归因（唯一约束兜底）
    Attribution attribution = Attribution.builder()
            .id(idService.nextLong(IdScope.GROWTH))
            .tenantId(tenantId)
            .campaignCode(inviteCode.getCampaignCode())
            .inviteCode(request.getInviteCode())
            .inviterUserId(inviteCode.getInviterUserId())
            .inviteeUserId(userId)
            .status(AttributionStatus.PENDING)
            .boundAt(LocalDateTime.now())
            .build();
    
    try {
        attributionRepository.save(attribution);
        inviteCodeRepository.incrementInvitesCount(inviteCode.getId());
        metrics.recordBind();
    } catch (DuplicateKeyException e) {
        attribution = attributionRepository.findByInvitee(tenantId, inviteCode.getCampaignCode(), userId)
                .orElseThrow(() -> new BusinessException("ATTRIBUTION_CREATE_FAILED"));
    }
    
    return buildResponse(attribution, "绑定成功");
}
```

### 首单触发奖励流程

```java
@Transactional
public void handleFirstOrderCompleted(Long tenantId, Long userId, Long orderId) {
    // 1. 首单判定
    int paidOrdersCount = attributionRepository.countPaidOrdersByUser(tenantId, userId);
    if (paidOrdersCount != 1) {
        log.info("非首单，跳过奖励发放");
        return;
    }
    
    // 2. 查询归因关系
    List<GrowthCampaign> campaigns = campaignRepository.findByTenantId(tenantId);
    for (GrowthCampaign campaign : campaigns) {
        Optional<Attribution> attrOpt = attributionRepository
                .findByInvitee(tenantId, campaign.getCampaignCode(), userId);
        
        if (attrOpt.isEmpty() || attrOpt.get().getStatus() != AttributionStatus.PENDING) {
            continue;
        }
        
        Attribution attribution = attrOpt.get();
        
        // 3. 确认归因
        attribution.confirm(orderId);
        attributionRepository.update(attribution);
        
        // 4. 更新成功邀请计数
        InviteCode inviteCode = inviteCodeRepository.findByInviteCode(attribution.getInviteCode())
                .orElse(null);
        if (inviteCode != null) {
            inviteCodeRepository.incrementSuccessfulInvitesCount(inviteCode.getId());
        }
        
        // 5. 解析奖励规则并发放
        CampaignRules rules = objectMapper.readValue(campaign.getRulesJson(), CampaignRules.class);
        
        // 发放被邀请人奖励
        for (RewardConfig reward : rules.getInviteeRewards()) {
            rewardIssuanceService.issueReward(tenantId, campaign.getCampaignCode(),
                    attribution.getId(), userId, UserRole.INVITEE, reward, orderId);
        }
        
        // 发放邀请人奖励
        for (RewardConfig reward : rules.getInviterRewards()) {
            rewardIssuanceService.issueReward(tenantId, campaign.getCampaignCode(),
                    attribution.getId(), attribution.getInviterUserId(), UserRole.INVITER, reward, orderId);
        }
    }
}
```

### 奖励发放流程（幂等）

```java
@Transactional
public RewardIssueLog issueReward(Long tenantId, String campaignCode, Long attributionId,
                                  Long userId, UserRole userRole, RewardConfig config,
                                  Long triggerOrderId) {
    // 1. 构造幂等键
    String idempotencyKey = buildIdempotencyKey(tenantId, campaignCode, 
            attributionId, userId, userRole, config.getType());
    
    // 2. 幂等检查
    Optional<RewardIssueLog> existing = rewardIssueLogRepository
            .findByIdempotencyKey(tenantId, idempotencyKey);
    
    if (existing.isPresent()) {
        RewardIssueLog log = existing.get();
        if (log.isSuccess()) {
            return log; // 已成功，幂等返回
        } else if (log.isFailed()) {
            throw new BusinessException(log.getErrorCode(), log.getErrorMessage());
        } else {
            throw new BusinessException("REWARD_ISSUE_IN_PROGRESS", "奖励发放处理中");
        }
    }
    
    // 3. 创建处理中日志
    RewardIssueLog log = createProcessingLog(tenantId, campaignCode, attributionId,
            userId, userRole, config, triggerOrderId, idempotencyKey);
    
    try {
        rewardIssueLogRepository.save(log);
    } catch (DuplicateKeyException e) {
        throw new BusinessException("DUPLICATE_REWARD_ISSUE", "重复的奖励发放请求");
    }
    
    // 4. 执行实际发放
    try {
        String resultId = doIssueReward(tenantId, userId, config, idempotencyKey);
        
        // 5. 更新为成功
        log.markSuccess(resultId);
        rewardIssueLogRepository.update(log);
        
        metrics.recordRewardIssued(config.getType().name());
        return log;
        
    } catch (Exception e) {
        // 6. 更新为失败
        log.markFailed("SYSTEM_ERROR", e.getMessage());
        rewardIssueLogRepository.update(log);
        metrics.recordRewardFailed("SYSTEM_ERROR");
        throw new BusinessException("REWARD_ISSUE_FAILED", "奖励发放失败");
    }
}
```

## 幂等键设计

### 格式定义

```
reward:{tenantId}:{campaignCode}:{attributionId}:{userId}:{userRole}:{rewardType}
```

### 示例

```
// 邀请人优惠券奖励
reward:1:INVITE_2025:100:1001:INVITER:COUPON

// 被邀请人储值奖励
reward:1:INVITE_2025:100:2001:INVITEE:WALLET_CREDIT

// 邀请人积分奖励
reward:1:INVITE_2025:100:1001:INVITER:POINTS
```

### 保证原理

1. **数据库唯一约束：** `uk_tenant_idempotency (tenant_id, idempotency_key)`
2. **状态机保护：** PROCESSING → SUCCESS/FAILED，不可逆
3. **幂等返回：** 已成功则返回 resultId，已失败则抛出原错误

## 依赖关系

### 内部依赖

```xml
<!-- 依赖其他模块的 API -->
<dependency>
    <groupId>com.bluecone</groupId>
    <artifactId>app-promo-api</artifactId>  <!-- 优惠券发放 -->
</dependency>
<dependency>
    <groupId>com.bluecone</groupId>
    <artifactId>app-member-api</artifactId>  <!-- 积分发放 -->
</dependency>
<dependency>
    <groupId>com.bluecone</groupId>
    <artifactId>app-wallet-api</artifactId>  <!-- 储值发放 -->
</dependency>
<dependency>
    <groupId>com.bluecone</groupId>
    <artifactId>app-notify-api</artifactId>  <!-- 通知发送 -->
</dependency>
```

### 被依赖

```xml
<!-- app-application 依赖此模块 -->
<dependency>
    <groupId>com.bluecone</groupId>
    <artifactId>app-growth</artifactId>
</dependency>
```

## 配置说明

### 自动配置

模块通过 `spring.factories` 自动配置：

```properties
org.springframework.boot.autoconfigure.EnableAutoConfiguration=\
com.bluecone.app.growth.infrastructure.config.GrowthModuleConfiguration
```

### 组件扫描

```java
@Configuration
@ComponentScan(basePackages = "com.bluecone.app.growth")
public class GrowthModuleConfiguration {
    // 自动装配奖励发放器
}
```

## 监控指标

### Prometheus Metrics

```
# 绑定事件总数
growth_bind_total

# 奖励发放成功总数（按类型）
growth_reward_issued_total{kind="all"}
growth_reward_issued_total{kind="coupon"}
growth_reward_issued_total{kind="wallet_credit"}
growth_reward_issued_total{kind="points"}

# 奖励发放失败总数（按错误码）
growth_reward_failed_total{error_code="..."}

# 奖励发放耗时（直方图）
growth_reward_issue_duration_bucket
growth_reward_issue_duration_sum
growth_reward_issue_duration_count
```

## 扩展点

### 1. 添加新的奖励类型

```java
// 1. 添加枚举
public enum RewardType {
    COUPON, WALLET_CREDIT, POINTS,
    NEW_TYPE  // 新类型
}

// 2. 实现发放器
@Component
public class NewTypeRewardIssuerImpl implements RewardIssuanceService.NewTypeRewardIssuer {
    @Override
    public String issue(Long tenantId, Long userId, String value, String idempotencyKey) {
        // 实现发放逻辑
    }
}

// 3. 注册发放器
rewardIssuanceService.setNewTypeRewardIssuer(newTypeRewardIssuer);
```

### 2. 添加新的反作弊规则

```java
// 在 GrowthApplicationService.bindInviteCode() 中添加检查
if (!antiFraudService.checkDevice(userId, deviceId)) {
    throw new BusinessException("DEVICE_FRAUD_DETECTED", "设备异常");
}
```

### 3. 自定义邀请码生成策略

```java
@Component
public class CustomInviteCodeGenerator extends InviteCodeGenerator {
    @Override
    public String generate(Long tenantId, String campaignCode, Long userId) {
        // 自定义生成逻辑
    }
}
```

## 已知限制

1. **单活动绑定：** 用户在同一租户的同一活动只能绑定一次
2. **储值发放器：** 需要 app-wallet 提供 CREDIT 接口
3. **通知未集成：** 需要调用 app-notify 发送通知

## 测试

运行测试：

```bash
mvn test -pl app-growth
```

## 相关文档

- [增长引擎设计文档](../docs/growth-engine-design.md)
- [快速开始指南](../docs/GROWTH_ENGINE_QUICK_START.md)
- [实现总结](../docs/GROWTH_ENGINE_IMPLEMENTATION_SUMMARY.md)

---

**模块版本：** 1.0.0-SNAPSHOT  
**最后更新：** 2025-12-19
