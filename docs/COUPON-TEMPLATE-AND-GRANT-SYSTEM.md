# 优惠券模板与发券系统设计文档

> **版本**: 1.0  
> **日期**: 2025-12-19  
> **状态**: 已实现

---

## 一、系统概述

本文档描述 BlueCone 优惠券运营闭环的核心功能：**优惠券模板管理** + **手动发券（Grant）**，包括幂等控制、配额管理、状态机流转等关键特性。

### 1.1 核心目标

- **模板化管理**：运营人员可配置券模板，统一管理券规则（折扣、门槛、有效期、配额等）
- **状态机控制**：模板支持 DRAFT → ONLINE → OFFLINE 状态流转，确保运营安全
- **手动发券**：管理员可手动为用户发放优惠券（单用户或批量）
- **幂等保证**：基于 `idempotency_key` + 数据库唯一约束，确保同一发券请求不重复
- **配额控制**：支持总量配额（`total_quota`）和每人限领（`per_user_quota`），并发场景下不超发
- **可观测性**：完整的发放日志、指标监控、诊断视图

### 1.2 模块边界

- **app-promo**：优惠券核心领域，包含模板、发券、锁券、核销逻辑
- **app-promo-api**：对外 API 接口定义（DTOs、Enums、Facades）
- **app-application**：后台管理接口（CouponTemplateAdminController、CouponGrantAdminController）
- **app-order**：订单模块，仅依赖 `app-promo-api`，通过 Facade 调用优惠券服务
- **app-ops**：运维诊断模块，展示券的发放来源、模板信息

---

## 二、数据模型

### 2.1 优惠券模板表（bc_coupon_template）

```sql
CREATE TABLE bc_coupon_template (
    id BIGINT NOT NULL PRIMARY KEY COMMENT '模板ID',
    tenant_id BIGINT NOT NULL COMMENT '租户ID',
    template_code VARCHAR(64) NOT NULL COMMENT '模板编码（唯一）',
    template_name VARCHAR(128) NOT NULL COMMENT '模板名称',
    coupon_type VARCHAR(32) NOT NULL COMMENT '券类型：DISCOUNT_AMOUNT-满减券, DISCOUNT_RATE-折扣券',
    discount_amount DECIMAL(10, 2) DEFAULT NULL COMMENT '优惠金额（满减券）',
    discount_rate DECIMAL(5, 2) DEFAULT NULL COMMENT '折扣率（折扣券，如85表示85折）',
    min_order_amount DECIMAL(10, 2) NOT NULL DEFAULT 0 COMMENT '最低订单金额门槛',
    max_discount_amount DECIMAL(10, 2) DEFAULT NULL COMMENT '最高优惠金额（折扣券封顶）',
    applicable_scope VARCHAR(32) NOT NULL COMMENT '适用范围：ALL-全场, STORE-指定门店, SKU-指定商品, CATEGORY-指定分类',
    applicable_scope_ids TEXT DEFAULT NULL COMMENT '适用范围ID列表（JSON数组）',
    valid_days INT DEFAULT NULL COMMENT '有效天数（领取后多少天内有效，NULL表示使用固定时间范围）',
    valid_start_time DATETIME DEFAULT NULL COMMENT '固定有效期开始时间',
    valid_end_time DATETIME DEFAULT NULL COMMENT '固定有效期结束时间',
    total_quantity INT DEFAULT NULL COMMENT '总发行量（NULL表示不限量）',
    per_user_limit INT DEFAULT 1 COMMENT '每人限领数量',
    issued_count INT NOT NULL DEFAULT 0 COMMENT '已发放数量（用于配额控制）',
    version INT NOT NULL DEFAULT 0 COMMENT '版本号（乐观锁）',
    status VARCHAR(32) NOT NULL DEFAULT 'DRAFT' COMMENT '模板状态：DRAFT-草稿, ONLINE-上线, OFFLINE-下线',
    description TEXT DEFAULT NULL COMMENT '券描述',
    terms_of_use TEXT DEFAULT NULL COMMENT '使用条款',
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    INDEX idx_tenant_code (tenant_id, template_code),
    INDEX idx_tenant_status (tenant_id, status),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
```

**关键字段说明**：

- `issued_count`：已发放数量，用于总配额控制，通过原子 SQL 更新
- `version`：乐观锁版本号，用于并发控制
- `status`：模板状态，支持 DRAFT/ONLINE/OFFLINE 三种状态

### 2.2 优惠券发放日志表（bc_coupon_grant_log）

```sql
CREATE TABLE bc_coupon_grant_log (
    id BIGINT NOT NULL PRIMARY KEY COMMENT '发放日志ID',
    tenant_id BIGINT NOT NULL COMMENT '租户ID',
    template_id BIGINT NOT NULL COMMENT '模板ID',
    idempotency_key VARCHAR(128) NOT NULL COMMENT '幂等键（确保同一发券请求不重复）',
    user_id BIGINT NOT NULL COMMENT '领取用户ID',
    coupon_id BIGINT DEFAULT NULL COMMENT '生成的券ID（成功后回填）',
    grant_source VARCHAR(32) NOT NULL COMMENT '发放来源：MANUAL_ADMIN-管理员手动, CAMPAIGN-营销活动, REGISTER-注册赠送',
    grant_status VARCHAR(32) NOT NULL DEFAULT 'PROCESSING' COMMENT '发放状态：PROCESSING-处理中, SUCCESS-成功, FAILED-失败',
    operator_id BIGINT DEFAULT NULL COMMENT '操作人ID（管理员发券时）',
    operator_name VARCHAR(64) DEFAULT NULL COMMENT '操作人名称',
    batch_no VARCHAR(64) DEFAULT NULL COMMENT '批次号（批量发券时）',
    grant_reason TEXT DEFAULT NULL COMMENT '发放原因/备注',
    error_code VARCHAR(64) DEFAULT NULL COMMENT '失败错误码',
    error_message TEXT DEFAULT NULL COMMENT '失败错误信息',
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    UNIQUE KEY uk_tenant_idempotency (tenant_id, idempotency_key),
    INDEX idx_tenant_template (tenant_id, template_id),
    INDEX idx_tenant_user (tenant_id, user_id),
    INDEX idx_coupon (coupon_id),
    INDEX idx_batch (batch_no),
    INDEX idx_status (grant_status),
    INDEX idx_created (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
```

**关键设计**：

- `uk_tenant_idempotency`：唯一约束，确保幂等性（数据库层面兜底）
- `grant_status`：记录发放状态，支持失败重试和诊断
- `batch_no`：批量发券时的批次号，便于追踪

### 2.3 优惠券实例表（bc_coupon）

增强字段：

```sql
ALTER TABLE bc_coupon
    ADD COLUMN grant_log_id BIGINT DEFAULT NULL COMMENT '关联的发放日志ID' AFTER template_id,
    ADD INDEX idx_grant_log (grant_log_id);
```

---

## 三、核心流程

### 3.1 模板状态机

```
DRAFT（草稿）
  ↓ publish
ONLINE（上线，可发券）
  ↓ offline
OFFLINE（下线，停止发券）
  ↓ republish
ONLINE
```

**状态转换规则**：

1. **DRAFT → ONLINE**：
   - 只有草稿状态才能上线
   - 上线前校验：有效期未过期、配额未用完
   
2. **ONLINE → OFFLINE**：
   - 只有在线状态才能下线
   - 下线后不再发券，但已发放的券仍可使用
   
3. **OFFLINE → ONLINE**：
   - 只有下线状态才能重新上线
   - 重新上线前需再次校验有效期和配额

4. **DRAFT 状态修改**：
   - 只有草稿状态的模板才能修改配置
   - 上线后的模板不可修改，需下线后重新创建

### 3.2 发券流程（幂等 + 配额）

```java
// 伪代码
function grantCoupon(tenantId, templateId, userId, idempotencyKey) {
    // 1. 幂等检查：查询发放日志
    existingLog = grantLogRepository.findByIdempotencyKey(tenantId, idempotencyKey)
    if (existingLog.exists && existingLog.isSuccess) {
        metrics.recordIdempotentReplay()
        return existingLog.coupon  // 幂等返回
    }
    
    // 2. 创建发放日志（PROCESSING状态）
    grantLog = new GrantLog(tenantId, templateId, userId, idempotencyKey, PROCESSING)
    try {
        grantLogRepository.save(grantLog)  // 唯一约束兜底
    } catch (DuplicateKeyException) {
        throw BusinessException("DUPLICATE_GRANT_REQUEST")
    }
    
    try {
        // 3. 查询模板并校验状态
        template = templateRepository.findById(templateId)
        if (!template.isOnline()) {
            throw BusinessException("TEMPLATE_NOT_ONLINE")
        }
        
        // 4. 校验有效期
        if (template.isExpired()) {
            throw BusinessException("TEMPLATE_EXPIRED")
        }
        
        // 5. 校验用户配额
        userGrantedCount = grantLogRepository.countUserGrantedByTemplate(tenantId, templateId, userId)
        if (!template.canUserReceive(userGrantedCount)) {
            metrics.recordUserQuotaExceeded()
            throw BusinessException("USER_QUOTA_EXCEEDED")
        }
        
        // 6. 原子扣减总配额（关键：数据库原子更新）
        quotaDeducted = templateRepository.incrementIssuedCount(templateId, 1)
        if (!quotaDeducted) {
            metrics.recordQuotaExceeded()
            throw BusinessException("TOTAL_QUOTA_EXCEEDED")
        }
        
        // 7. 生成优惠券实例
        coupon = createCouponFromTemplate(template, userId, grantLog.id)
        couponRepository.save(coupon)
        
        // 8. 更新发放日志为成功
        grantLog.markSuccess(coupon.id)
        grantLogRepository.update(grantLog)
        
        metrics.recordSuccess()
        return coupon
        
    } catch (BusinessException e) {
        // 记录失败原因
        grantLog.markFailed(e.code, e.message)
        grantLogRepository.update(grantLog)
        metrics.recordFailure(e.code)
        throw e
    }
}
```

**关键点**：

1. **幂等键设计**：
   - 单用户发券：`idempotencyKey = request.idempotencyKey`（由调用方生成）
   - 批量发券：`idempotencyKey = batchNo:templateId:userId`（系统生成）

2. **配额控制**：
   - **总配额**：使用原子 SQL 更新 `issued_count`，确保不超发
   ```sql
   UPDATE bc_coupon_template 
   SET issued_count = issued_count + 1, version = version + 1
   WHERE id = ? AND (total_quantity IS NULL OR issued_count + 1 <= total_quantity)
   ```
   - **用户配额**：查询 `grant_log` 表统计用户已领取数量

3. **并发安全**：
   - 数据库唯一约束（`uk_tenant_idempotency`）兜底幂等
   - 原子 SQL 更新配额，避免超发
   - 乐观锁（`version`）用于模板更新

---

## 四、API 接口

### 4.1 模板管理接口

#### 创建模板（草稿）

```http
POST /api/admin/promo/templates
X-Tenant-Id: 1

{
  "templateCode": "NEWUSER2024",
  "templateName": "新用户专享券",
  "couponType": "DISCOUNT_AMOUNT",
  "discountAmount": 10.00,
  "minOrderAmount": 50.00,
  "applicableScope": "ALL",
  "validDays": 30,
  "totalQuantity": 10000,
  "perUserLimit": 1,
  "description": "新用户首单立减10元"
}
```

#### 上线模板

```http
POST /api/admin/promo/templates/{id}/publish
X-Tenant-Id: 1
```

#### 下线模板

```http
POST /api/admin/promo/templates/{id}/offline?reason=活动结束
X-Tenant-Id: 1
```

#### 查询模板列表

```http
GET /api/admin/promo/templates?status=ONLINE
X-Tenant-Id: 1
```

### 4.2 发券接口

#### 手动发券（单用户或批量）

```http
POST /api/admin/promo/grants
X-Tenant-Id: 1

{
  "templateId": 100,
  "userIds": [1001, 1002, 1003],
  "idempotencyKey": "MANUAL-20241219-001",
  "batchNo": "BATCH-001",
  "grantReason": "补偿发券",
  "operatorId": 999,
  "operatorName": "admin"
}
```

**响应**：

```json
{
  "total": 3,
  "successCount": 2,
  "failedCount": 1,
  "results": [
    {
      "userId": 1001,
      "success": true,
      "couponId": 5001,
      "errorMessage": null
    },
    {
      "userId": 1002,
      "success": true,
      "couponId": 5002,
      "errorMessage": null
    },
    {
      "userId": 1003,
      "success": false,
      "couponId": null,
      "errorMessage": "用户已达领取上限"
    }
  ]
}
```

#### 查询发放日志

```http
GET /api/admin/promo/grants/user/{userId}?limit=50
X-Tenant-Id: 1

GET /api/admin/promo/grants/template/{templateId}?limit=50
X-Tenant-Id: 1

GET /api/admin/promo/grants/idempotency/{idempotencyKey}
X-Tenant-Id: 1
```

---

## 五、可观测性

### 5.1 指标监控（Metrics）

```java
// 发放成功次数
coupon.grant.success

// 发放失败次数（带原因标签）
coupon.grant.failure

// 总配额超限次数
coupon.grant.quota_exceeded

// 用户配额超限次数
coupon.grant.user_quota_exceeded

// 幂等重放次数
coupon.grant.idempotent_replay

// 发放耗时
coupon.grant.duration
```

### 5.2 诊断视图

在订单诊断视图（`OrderForensicsView`）中增强优惠券信息：

```json
{
  "couponSection": {
    "locks": [
      {
        "id": 1,
        "couponId": 5001,
        "templateId": 100,
        "templateName": "新用户专享券",
        "grantLogId": 2001,
        "grantSource": "MANUAL_ADMIN",
        "lockStatus": "COMMITTED"
      }
    ],
    "redemptions": [
      {
        "id": 1,
        "couponId": 5001,
        "templateId": 100,
        "templateName": "新用户专享券",
        "grantLogId": 2001,
        "grantSource": "MANUAL_ADMIN",
        "grantTime": "2024-12-19T10:00:00",
        "discountAmount": 10.00
      }
    ]
  }
}
```

---

## 六、测试策略

### 6.1 幂等性测试

- **场景1**：同一 `idempotencyKey` 多次调用，只生成一张券
- **场景2**：并发请求，数据库唯一约束兜底
- **场景3**：失败后重试，返回失败信息

### 6.2 配额测试

- **场景1**：总配额用完，后续发券失败
- **场景2**：用户配额用完，该用户无法再领取
- **场景3**：并发发券，配额不超发（原子 SQL）
- **场景4**：不限量模板，无配额限制

### 6.3 状态机测试

- **场景1**：草稿 → 上线 → 下线 → 重新上线
- **场景2**：非法状态转换（如在线状态直接修改配置）
- **场景3**：上线前校验（有效期、配额）

---

## 七、运维指南

### 7.1 配额监控

定期监控模板配额使用情况：

```sql
SELECT 
    template_code,
    template_name,
    total_quantity,
    issued_count,
    ROUND(issued_count * 100.0 / total_quantity, 2) AS usage_rate
FROM bc_coupon_template
WHERE status = 'ONLINE' AND total_quantity IS NOT NULL
ORDER BY usage_rate DESC;
```

### 7.2 发放日志查询

查询失败的发放记录：

```sql
SELECT 
    id,
    template_id,
    user_id,
    grant_source,
    error_code,
    error_message,
    created_at
FROM bc_coupon_grant_log
WHERE grant_status = 'FAILED'
ORDER BY created_at DESC
LIMIT 100;
```

### 7.3 异常处理

1. **配额超发**：
   - 检查 `issued_count` 是否超过 `total_quantity`
   - 检查原子 SQL 是否正确执行

2. **幂等失败**：
   - 检查 `uk_tenant_idempotency` 唯一约束是否生效
   - 检查 `idempotencyKey` 生成逻辑

3. **状态异常**：
   - 检查模板状态流转日志
   - 检查状态机校验逻辑

---

## 八、未来扩展

### 8.1 自动发券

- 注册赠券：用户注册时自动发放
- 活动发券：营销活动触发自动发放
- 定时发券：定时任务批量发放

### 8.2 券包

- 支持一次发放多张不同类型的券
- 券包模板管理

### 8.3 用户主动领取

- 用户在 H5/小程序主动领取优惠券
- 领券中心展示

### 8.4 高级配额策略

- 分时段配额（如每天限量）
- 分渠道配额（如不同渠道独立配额）
- 动态配额调整

---

## 九、参考资料

- [幂等模板接入指南](./engineering/IDEMPOTENCY-TEMPLATE.md)
- [ID 契约规范](./engineering/ID-CONTRACT.md)
- [Outbox 模式快速参考](./OUTBOX-QUICK-REFERENCE.md)
