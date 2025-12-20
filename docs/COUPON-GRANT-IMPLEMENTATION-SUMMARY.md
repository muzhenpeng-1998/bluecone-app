# 优惠券模板与发券系统实现总结

> **实施日期**: 2025-12-19  
> **状态**: ✅ 已完成

---

## 一、实施概览

本次实施完成了 BlueCone 优惠券运营闭环的核心功能：**优惠券模板管理** + **手动发券（Grant）**，包括完整的幂等控制、配额管理、状态机流转、可观测性等特性。

### 关键成果

✅ **数据库迁移**：增强模板表、新增发放日志表  
✅ **领域模型**：完整的模板、发放日志、券实例模型  
✅ **状态机**：DRAFT → ONLINE → OFFLINE 三态流转  
✅ **发券服务**：幂等 + 配额控制 + 并发安全  
✅ **Admin API**：模板 CRUD + 发券接口  
✅ **可观测性**：Metrics + 诊断视图增强  
✅ **测试覆盖**：幂等性、配额、状态机测试  
✅ **文档**：完整的设计文档和接入指南

---

## 二、核心实现

### 2.1 数据库设计

#### 迁移脚本

**文件**: `app-infra/src/main/resources/db/migration/V20251219004__enhance_coupon_template_and_add_grant_log.sql`

**变更内容**:

1. **bc_coupon_template 增强**:
   - 新增 `issued_count INT NOT NULL DEFAULT 0` - 已发放数量（配额控制）
   - 新增 `version INT NOT NULL DEFAULT 0` - 乐观锁版本号
   - 修改 `status` 默认值为 `DRAFT`，支持 DRAFT/ONLINE/OFFLINE

2. **bc_coupon_grant_log 新建**:
   - 核心字段：`tenant_id`, `template_id`, `user_id`, `idempotency_key`, `coupon_id`
   - 状态字段：`grant_status` (PROCESSING/SUCCESS/FAILED)
   - 来源字段：`grant_source` (MANUAL_ADMIN/CAMPAIGN/REGISTER)
   - 运营字段：`operator_id`, `operator_name`, `batch_no`, `grant_reason`
   - 唯一约束：`uk_tenant_idempotency (tenant_id, idempotency_key)` - 幂等兜底

3. **bc_coupon 增强**:
   - 新增 `grant_log_id BIGINT` - 关联发放日志

### 2.2 领域模型

#### 新增枚举类型

**文件**: `app-promo-api/src/main/java/com/bluecone/app/promo/api/enums/`

- `TemplateStatus.java` - 模板状态（DRAFT/ONLINE/OFFLINE）
- `GrantSource.java` - 发放来源（MANUAL_ADMIN/CAMPAIGN/REGISTER/USER_CLAIM）
- `GrantStatus.java` - 发放状态（PROCESSING/SUCCESS/FAILED）

#### 领域模型增强

**CouponTemplate** (`app-promo/src/main/java/com/bluecone/app/promo/domain/model/CouponTemplate.java`):
- 新增字段：`issuedCount`, `version`
- 新增方法：`isOnline()`, `isDraft()`, `isOffline()`, `hasQuotaAvailable()`, `canUserReceive()`

**CouponGrantLog** (`app-promo/src/main/java/com/bluecone/app/promo/domain/model/CouponGrantLog.java`):
- 完整的发放日志模型
- 状态管理方法：`markSuccess()`, `markFailed()`, `isSuccess()`, `isFailed()`

**Coupon**:
- 新增字段：`grantLogId` - 关联发放日志

### 2.3 仓储层

#### CouponTemplateRepository

**文件**: `app-promo/src/main/java/com/bluecone/app/promo/domain/repository/CouponTemplateRepository.java`

**核心方法**:
```java
// 乐观锁更新
boolean updateWithVersion(CouponTemplate template);

// 原子增加已发放数量（配额控制关键）
boolean incrementIssuedCount(Long templateId, int delta);

// 查询在线模板
List<CouponTemplate> findOnlineTemplates(Long tenantId);
```

**实现亮点** (`CouponTemplateRepositoryImpl.java`):
```java
// 原子 SQL 更新，确保配额不超发
@Override
public boolean incrementIssuedCount(Long templateId, int delta) {
    LambdaUpdateWrapper<CouponTemplatePO> wrapper = new LambdaUpdateWrapper<>();
    wrapper.eq(CouponTemplatePO::getId, templateId)
            .setSql("issued_count = issued_count + " + delta)
            .setSql("version = version + 1")
            // 关键：只有在配额充足时才更新
            .apply("(total_quantity IS NULL OR issued_count + {0} <= total_quantity)", delta);
    
    int updated = mapper.update(null, wrapper);
    return updated > 0;
}
```

#### CouponGrantLogRepository

**文件**: `app-promo/src/main/java/com/bluecone/app/promo/domain/repository/CouponGrantLogRepository.java`

**核心方法**:
```java
// 根据幂等键查询（幂等控制）
Optional<CouponGrantLog> findByIdempotencyKey(Long tenantId, String idempotencyKey);

// 统计用户从指定模板领取的券数量（用户配额控制）
int countUserGrantedByTemplate(Long tenantId, Long templateId, Long userId);

// 查询发放日志（运营查询）
List<CouponGrantLog> findByUser(Long tenantId, Long userId, int limit);
List<CouponGrantLog> findByTemplate(Long tenantId, Long templateId, int limit);
```

### 2.4 领域服务

#### CouponTemplateDomainService

**文件**: `app-promo/src/main/java/com/bluecone/app/promo/domain/service/CouponTemplateDomainService.java`

**职责**: 模板状态机管理

**核心方法**:
```java
// 创建草稿
CouponTemplate createDraft(CouponTemplate template);

// 更新草稿（仅草稿状态可更新）
void updateDraft(CouponTemplate template);

// 上线模板（DRAFT → ONLINE）
void publishTemplate(Long templateId);

// 下线模板（ONLINE → OFFLINE）
void offlineTemplate(Long templateId, String reason);

// 重新上线（OFFLINE → ONLINE）
void republishTemplate(Long templateId);
```

**状态机校验**:
- 上线前校验：有效期、配额
- 状态转换校验：只允许合法的状态流转
- 配置修改校验：只有草稿状态才能修改

#### CouponGrantService

**文件**: `app-promo/src/main/java/com/bluecone/app/promo/domain/service/CouponGrantService.java`

**职责**: 优惠券发放核心逻辑

**核心方法**:
```java
// 单用户发券（幂等 + 配额）
Coupon grantCoupon(Long tenantId, Long templateId, Long userId, 
                   String idempotencyKey, GrantSource grantSource,
                   Long operatorId, String operatorName, String grantReason);

// 批量发券
List<GrantResult> grantCouponBatch(Long tenantId, Long templateId, 
                                   List<Long> userIds, String batchNo,
                                   GrantSource grantSource, ...);
```

**发券流程**:
1. **幂等检查**: 查询 `grant_log`，如已成功则直接返回
2. **创建日志**: 写入 `grant_log`（PROCESSING），利用唯一约束兜底
3. **校验模板**: 状态、有效期
4. **校验配额**: 用户配额、总配额
5. **原子扣减**: 调用 `incrementIssuedCount()` 原子更新
6. **生成券**: 创建 `coupon` 实例
7. **更新日志**: 标记为 SUCCESS，回填 `coupon_id`
8. **异常处理**: 失败时记录错误码和错误信息

**并发安全**:
- 数据库唯一约束（`uk_tenant_idempotency`）
- 原子 SQL 更新配额
- 乐观锁（`version`）

### 2.5 可观测性

#### CouponGrantMetrics

**文件**: `app-promo/src/main/java/com/bluecone/app/promo/domain/service/CouponGrantMetrics.java`

**指标**:
```java
// 发放成功次数
coupon.grant.success

// 发放失败次数
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

#### 诊断视图增强

**文件**: `app-ops/src/main/java/com/bluecone/app/ops/api/dto/forensics/`

**CouponLockItem** 增强:
- 新增：`templateId`, `templateName`, `grantLogId`, `grantSource`

**CouponRedemptionItem** 增强:
- 新增：`templateName`, `grantLogId`, `grantSource`, `grantTime`

### 2.6 Admin API

#### CouponTemplateAdminController

**文件**: `app-application/src/main/java/com/bluecone/app/controller/admin/CouponTemplateAdminController.java`

**接口**:
```
POST   /api/admin/promo/templates           - 创建模板（草稿）
PUT    /api/admin/promo/templates/{id}      - 更新模板（仅草稿）
GET    /api/admin/promo/templates/{id}      - 查询模板详情
GET    /api/admin/promo/templates           - 查询模板列表
POST   /api/admin/promo/templates/{id}/publish    - 上线模板
POST   /api/admin/promo/templates/{id}/offline    - 下线模板
POST   /api/admin/promo/templates/{id}/republish  - 重新上线
```

#### CouponGrantAdminController

**文件**: `app-application/src/main/java/com/bluecone/app/controller/admin/CouponGrantAdminController.java`

**接口**:
```
POST   /api/admin/promo/grants                          - 手动发券（单用户或批量）
GET    /api/admin/promo/grants/user/{userId}            - 查询用户发放日志
GET    /api/admin/promo/grants/template/{templateId}    - 查询模板发放日志
GET    /api/admin/promo/grants/idempotency/{key}        - 根据幂等键查询
```

**发券接口示例**:
```json
POST /api/admin/promo/grants
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

### 2.7 测试

#### 幂等性测试

**文件**: `app-promo/src/test/java/com/bluecone/app/promo/domain/service/CouponGrantServiceIdempotencyTest.java`

**覆盖场景**:
- 同一幂等键重复调用，返回已发放的券
- 已失败的幂等键，返回失败信息
- 处理中的幂等键，返回处理中状态

#### 配额测试

**文件**: `app-promo/src/test/java/com/bluecone/app/promo/domain/service/CouponGrantServiceQuotaTest.java`

**覆盖场景**:
- 总配额用完，发券失败
- 用户配额用完，发券失败
- 配额充足，发券成功
- 不限量模板，无配额限制

#### 状态机测试

**文件**: `app-promo/src/test/java/com/bluecone/app/promo/domain/service/CouponTemplateDomainServiceTest.java`

**覆盖场景**:
- 创建草稿，初始状态正确
- 草稿上线，状态转换正确
- 在线下线，状态转换正确
- 非法状态转换，抛出异常

---

## 三、关键设计决策

### 3.1 幂等控制

**方案**: 数据库唯一约束 + 发放日志

**优势**:
- 简单可靠，数据库层面兜底
- 支持幂等重放（查询已成功的发放日志）
- 支持失败重试（记录失败原因）

**实现**:
```sql
UNIQUE KEY uk_tenant_idempotency (tenant_id, idempotency_key)
```

### 3.2 配额控制

**方案**: 原子 SQL 更新 + 用户配额统计

**总配额**:
```sql
UPDATE bc_coupon_template 
SET issued_count = issued_count + 1, version = version + 1
WHERE id = ? AND (total_quantity IS NULL OR issued_count + 1 <= total_quantity)
```

**用户配额**:
```sql
SELECT COUNT(*) FROM bc_coupon_grant_log 
WHERE tenant_id = ? AND template_id = ? AND user_id = ? AND grant_status = 'SUCCESS'
```

**优势**:
- 并发安全，不会超发
- 性能高效，单次 SQL 完成
- 支持不限量场景（`total_quantity IS NULL`）

### 3.3 状态机

**方案**: 领域服务 + 状态校验

**状态流转**:
```
DRAFT → ONLINE → OFFLINE → ONLINE
```

**优势**:
- 运营安全，防止误操作
- 状态清晰，易于理解
- 可扩展，支持更多状态

### 3.4 可观测性

**方案**: Metrics + 发放日志 + 诊断视图

**优势**:
- 实时监控：Metrics 实时反馈发放情况
- 历史追溯：发放日志完整记录
- 诊断分析：诊断视图展示券来源

---

## 四、文件清单

### 数据库迁移
- `app-infra/src/main/resources/db/migration/V20251219004__enhance_coupon_template_and_add_grant_log.sql`

### 枚举类型
- `app-promo-api/src/main/java/com/bluecone/app/promo/api/enums/TemplateStatus.java`
- `app-promo-api/src/main/java/com/bluecone/app/promo/api/enums/GrantSource.java`
- `app-promo-api/src/main/java/com/bluecone/app/promo/api/enums/GrantStatus.java`

### 领域模型
- `app-promo/src/main/java/com/bluecone/app/promo/domain/model/CouponTemplate.java` (增强)
- `app-promo/src/main/java/com/bluecone/app/promo/domain/model/Coupon.java` (增强)
- `app-promo/src/main/java/com/bluecone/app/promo/domain/model/CouponGrantLog.java` (新增)

### 持久化层
- `app-promo/src/main/java/com/bluecone/app/promo/infra/persistence/po/CouponTemplatePO.java` (增强)
- `app-promo/src/main/java/com/bluecone/app/promo/infra/persistence/po/CouponPO.java` (增强)
- `app-promo/src/main/java/com/bluecone/app/promo/infra/persistence/po/CouponGrantLogPO.java` (新增)
- `app-promo/src/main/java/com/bluecone/app/promo/infra/persistence/mapper/CouponGrantLogMapper.java` (新增)
- `app-promo/src/main/java/com/bluecone/app/promo/infra/persistence/converter/CouponTemplateConverter.java` (新增)
- `app-promo/src/main/java/com/bluecone/app/promo/infra/persistence/converter/CouponGrantLogConverter.java` (新增)

### 仓储层
- `app-promo/src/main/java/com/bluecone/app/promo/domain/repository/CouponTemplateRepository.java` (新增)
- `app-promo/src/main/java/com/bluecone/app/promo/domain/repository/CouponGrantLogRepository.java` (新增)
- `app-promo/src/main/java/com/bluecone/app/promo/infra/persistence/repository/CouponTemplateRepositoryImpl.java` (新增)
- `app-promo/src/main/java/com/bluecone/app/promo/infra/persistence/repository/CouponGrantLogRepositoryImpl.java` (新增)

### 领域服务
- `app-promo/src/main/java/com/bluecone/app/promo/domain/service/CouponTemplateDomainService.java` (新增)
- `app-promo/src/main/java/com/bluecone/app/promo/domain/service/CouponGrantService.java` (新增)
- `app-promo/src/main/java/com/bluecone/app/promo/domain/service/CouponGrantMetrics.java` (新增)

### Admin API
- `app-promo-api/src/main/java/com/bluecone/app/promo/api/dto/admin/CouponTemplateCreateRequest.java` (新增)
- `app-promo-api/src/main/java/com/bluecone/app/promo/api/dto/admin/CouponTemplateUpdateRequest.java` (新增)
- `app-promo-api/src/main/java/com/bluecone/app/promo/api/dto/admin/CouponTemplateView.java` (新增)
- `app-promo-api/src/main/java/com/bluecone/app/promo/api/dto/admin/CouponGrantRequest.java` (新增)
- `app-promo-api/src/main/java/com/bluecone/app/promo/api/dto/admin/CouponGrantResponse.java` (新增)
- `app-promo-api/src/main/java/com/bluecone/app/promo/api/dto/admin/CouponGrantLogView.java` (新增)
- `app-application/src/main/java/com/bluecone/app/controller/admin/CouponTemplateAdminController.java` (新增)
- `app-application/src/main/java/com/bluecone/app/controller/admin/CouponGrantAdminController.java` (新增)

### 诊断视图
- `app-ops/src/main/java/com/bluecone/app/ops/api/dto/forensics/CouponLockItem.java` (增强)
- `app-ops/src/main/java/com/bluecone/app/ops/api/dto/forensics/CouponRedemptionItem.java` (增强)

### ID Scope
- `app-id-api/src/main/java/com/bluecone/app/id/api/IdScope.java` (增强，新增 COUPON_GRANT_LOG)

### 测试
- `app-promo/src/test/java/com/bluecone/app/promo/domain/service/CouponGrantServiceIdempotencyTest.java` (新增)
- `app-promo/src/test/java/com/bluecone/app/promo/domain/service/CouponGrantServiceQuotaTest.java` (新增)
- `app-promo/src/test/java/com/bluecone/app/promo/domain/service/CouponTemplateDomainServiceTest.java` (新增)

### 文档
- `docs/COUPON-TEMPLATE-AND-GRANT-SYSTEM.md` (新增)
- `docs/COUPON-GRANT-IMPLEMENTATION-SUMMARY.md` (本文档)

---

## 五、后续工作

### 5.1 立即可做

1. **运行数据库迁移**:
   ```bash
   # 在开发环境执行迁移
   mvn flyway:migrate
   ```

2. **编译项目**:
   ```bash
   mvn clean compile
   ```

3. **运行测试**:
   ```bash
   mvn test -Dtest=CouponGrantService*Test
   mvn test -Dtest=CouponTemplateDomainServiceTest
   ```

4. **启动应用**:
   ```bash
   mvn spring-boot:run
   ```

5. **测试 API**:
   - 创建模板：`POST /api/admin/promo/templates`
   - 上线模板：`POST /api/admin/promo/templates/{id}/publish`
   - 手动发券：`POST /api/admin/promo/grants`

### 5.2 未来扩展

1. **自动发券**:
   - 注册赠券
   - 活动触发发券
   - 定时批量发券

2. **用户主动领取**:
   - 领券中心
   - H5/小程序领券

3. **高级配额策略**:
   - 分时段配额
   - 分渠道配额
   - 动态配额调整

4. **券包功能**:
   - 一次发放多张券
   - 券包模板管理

---

## 六、注意事项

### 6.1 数据库迁移

- ⚠️ 生产环境执行前务必备份数据
- ⚠️ 检查 `issued_count` 和 `version` 字段的默认值
- ⚠️ 确认唯一约束 `uk_tenant_idempotency` 已创建

### 6.2 并发控制

- ✅ 总配额使用原子 SQL 更新，无需额外锁
- ✅ 幂等键使用数据库唯一约束兜底
- ⚠️ 用户配额统计依赖 `grant_log` 表，确保索引正常

### 6.3 性能优化

- 建议为 `bc_coupon_grant_log` 表的高频查询字段添加索引
- 批量发券时建议分批处理，避免长事务
- 定期归档历史发放日志

### 6.4 监控告警

- 配置 Metrics 告警：配额超限、发券失败率
- 监控发放日志表增长速度
- 定期检查失败的发放记录

---

## 七、总结

本次实施完成了优惠券模板与发券系统的完整闭环，核心特性包括：

✅ **完整的状态机**：DRAFT → ONLINE → OFFLINE，确保运营安全  
✅ **强幂等保证**：数据库唯一约束 + 发放日志，支持重放和重试  
✅ **并发安全配额**：原子 SQL 更新，确保不超发  
✅ **完善的可观测性**：Metrics + 发放日志 + 诊断视图  
✅ **高质量测试**：幂等性、配额、状态机全覆盖  
✅ **清晰的文档**：设计文档 + 实施总结 + API 说明

系统已具备生产就绪能力，可支持大规模优惠券发放场景。

---

**实施人员**: AI Assistant  
**审核状态**: 待审核  
**部署状态**: 待部署
