# ErrorCode/异常体系基线扫描报告

**项目**: bluecone-app  
**扫描时间**: 2025-12-19  
**目标**: 为错误码重构提供现状地图与迁移清单

---

## 1. 错误码定义现状

### 1.1 ErrorCode 接口/类/枚举统计

共发现 **11 个** ErrorCode 定义文件：

| 文件路径 | 类型 | 所在模块 | 实现方式 | 状态 |
|---------|------|---------|---------|------|
| `app-core/src/main/java/com/bluecone/app/core/exception/ErrorCode.java` | enum | app-core | 老版本 enum | ⚠️ 待淘汰 |
| `app-core/src/main/java/com/bluecone/app/core/error/ErrorCode.java` | interface | app-core | 新接口 | ✅ 标准接口 |
| `app-core/src/main/java/com/bluecone/app/core/error/CommonErrorCode.java` | enum | app-core | 实现新接口 | ✅ 新标准 |
| `app-core/src/main/java/com/bluecone/app/core/error/BizErrorCode.java` | enum | app-core | 实现新接口 | ✅ 新标准 |
| `app-core/src/main/java/com/bluecone/app/core/error/UserErrorCode.java` | enum | app-core | 实现新接口 | ✅ 新标准 |
| `app-core/src/main/java/com/bluecone/app/core/contextkit/ContextErrorCode.java` | enum | app-core | 无接口 | ⚠️ 独立体系 |
| `app-billing/src/main/java/com/bluecone/app/billing/domain/error/BillingErrorCode.java` | enum | app-billing | 实现新接口 | ✅ 新标准 |
| `app-order/src/main/java/com/bluecone/app/order/domain/error/OrderErrorCode.java` | enum | app-order | 实现新接口 | ✅ 新标准 |
| `app-order/src/main/java/com/bluecone/app/order/domain/enums/OrderErrorCode.java` | enum | app-order | 独立 | ⚠️ 重复定义 |
| `app-store/src/main/java/com/bluecone/app/store/domain/error/StoreErrorCode.java` | enum | app-store | 实现新接口 | ✅ 新标准 |
| `app-inventory/src/main/java/com/bluecone/app/inventory/domain/error/InventoryErrorCode.java` | enum | app-inventory | 实现新接口 | ✅ 新标准 |

**关键发现**:
- ✅ 已有新版 `ErrorCode` 接口 (`com.bluecone.app.core.error.ErrorCode`)
- ⚠️ 仍存在老版 `ErrorCode` enum (`com.bluecone.app.core.exception.ErrorCode`)，**两者并存造成混淆**
- ⚠️ `OrderErrorCode` 有两个版本（`domain/error/` 与 `domain/enums/`），需要合并
- ⚠️ `ContextErrorCode` 未实现标准接口，独立体系

---

## 2. 异常类现状

### 2.1 业务异常类

| 异常类 | 路径 | 状态 | 构造器支持 |
|--------|------|------|-----------|
| `BusinessException` | `app-core/.../core/exception/BusinessException.java` | ✅ 主力 | ✅ ErrorCode + String code/message |
| `BizException` | `app-core/.../core/exception/BizException.java` | ⚠️ @Deprecated | ✅ ErrorCode only |

**BusinessException 构造器现状**:
```java
// ✅ 新标准（推荐）
BusinessException(ErrorCode errorCode)
BusinessException(ErrorCode errorCode, String message)
BusinessException(ErrorCode errorCode, String message, Throwable cause)

// ⚠️ 老版本（允许 magic string，需淘汰）
BusinessException(String code, String message)
static BusinessException of(String code, String message)
```

### 2.2 其他异常类

共发现 **22 个** 自定义异常类（非 BusinessException/BizException）：

**按模块分类**:

| 模块 | 异常类数量 | 主要异常 |
|------|-----------|---------|
| app-core (publicid) | 6 | PublicIdInvalidException, PublicIdNotFoundException, PublicIdForbiddenException, PublicIdLookupMissingException |
| app-core (idempotency) | 3 | IdempotencyConflictException, IdempotencyInProgressException, IdempotencyStorageException |
| app-core (event) | 3 | EventConsumeConflictException, EventConsumeFailedException, EventConsumeInProgressException |
| app-core (idresolve) | 2 | PublicIdInvalidException, PublicIdNotFoundException |
| app-store | 1 | StoreConfigVersionConflictException |
| app-infra | 2 | EventDispatchException, RedisOperationException |
| app-id-api | 1 | ClockRollbackException |
| app-resource-api | 4 | ResourceException, ResourceUploadException, ResourceAccessDeniedException, ResourceNotFoundException |

**重点关注**: PublicId 相关异常有重复定义（`core.idresolve.api` vs `core.publicid.exception`）

---

## 3. 抛异常调用点统计

### 3.1 按异常类型统计

| 异常类型 | 数量 | 占比 | 备注 |
|---------|------|------|------|
| `throw new BusinessException(` | ~843 次 | ~90% | 主力异常 |
| `throw new BizException(` | ~14 次 | ~2% | 已废弃，需迁移 |
| `throw new IllegalArgumentException(` | ~50+ 次 | ~5% | 主要在域模型参数校验 |
| `throw new IllegalStateException(` | ~20+ 次 | ~2% | 主要在领域逻辑冲突 |
| 其他专用异常 | ~50 次 | ~1% | PublicId/Idempotency/Resource等 |

**总计**: 约 **850+ 个** throw 调用点

### 3.2 按 BusinessException 构造方式统计

| 构造方式 | 数量估算 | 风险等级 |
|---------|---------|---------|
| `new BusinessException(ErrorCode)` | ~700 | ✅ 安全 |
| `new BusinessException("STRING_CODE", ...)` | ~52 | ⚠️ 高风险（magic string） |
| `BusinessException.of("CODE", "msg")` | ~10 | ⚠️ 高风险（magic string） |

---

## 4. 常见错误码字面量位置

### 4.1 高频错误码分布

| 错误码字面量 | 出现文件数 | 主要位置 | 迁移优先级 |
|-------------|-----------|---------|-----------|
| `"INVALID_PARAM"` | 2 | GlobalExceptionHandler, ErrorCode enum | P0 |
| `"INTERNAL_ERROR"` | 2 | GlobalExceptionHandler, ErrorCode enum | P0 |
| `"AUTH_REQUIRED"` | 1 | ErrorCode enum (SecurityCurrentUserContext 使用 enum) | P0 |
| `"NOT_FOUND"` | 1 | ErrorCode enum | P1 |
| `"TOKEN_EXPIRED"` | 1 | ErrorCode enum | P1 |
| `"TOKEN_INVALID"` | 0 | 未直接使用 | P2 |
| `"TENANT_NOT_FOUND"` | 0 | 未直接字面量 | P2 |
| `"USER_NOT_FOUND"` | 0 | 未直接字面量 | P2 |
| `"ORDER_NOT_FOUND"` | 0 | 未直接字面量 | P2 |
| `"MISSING_PARAM"` | 0 | 未直接字面量 | P2 |

**关键发现**:
- ✅ 大部分业务域已使用结构化错误码（如 `OrderErrorCode.ORDER_NOT_FOUND`）
- ⚠️ `SecurityCurrentUserContext` 仍在使用老版 `ErrorCode.AUTH_REQUIRED` enum
- ⚠️ `GlobalExceptionHandler` 仍在引用老版 `ErrorCode` enum

---

## 5. 对外响应字段结构

### 5.1 ApiResponse 字段

当前对外错误响应字段（`app-core/src/main/java/com/bluecone/app/core/api/ApiResponse.java`）:

```json
{
  "code": "string",      // 错误码（成功时为 "OK"）
  "message": "string",   // 错误消息
  "data": null,          // 失败时为 null
  "traceId": "string",   // 从 MDC 注入
  "timestamp": "2025-12-19T10:00:00Z"
}
```

**字段命名**: ✅ 已统一使用 `code` / `message`（无 `errCode` / `errMsg` 历史包袱）

### 5.2 HTTP 状态码策略

根据 `GlobalExceptionHandler` 分析:

| 异常类型 | HTTP 状态码 | code 字段 | 策略 |
|---------|-----------|----------|------|
| BusinessException | 200 | ex.getCode() | ✅ 业务错误统一 200 |
| Exception | 500 | "INTERNAL_ERROR" | ✅ 系统错误 500 |
| PublicIdInvalidException | 400 | "INVALID_PARAM" | ✅ 语义化 HTTP |
| PublicIdNotFoundException | 404 | "NOT_FOUND" | ✅ 语义化 HTTP |
| PublicIdForbiddenException | 403 | "PUBLIC_ID_FORBIDDEN" | ✅ 语义化 HTTP |
| PublicIdLookupMissingException | 500 | "PUBLIC_ID_LOOKUP_MISSING" | ✅ 配置错误 500 |

**策略**: **混合策略** - 业务异常统一 200，技术异常语义化 HTTP

---

## 6. GlobalExceptionHandler 处理分支

文件路径: `app-application/src/main/java/com/bluecone/app/exception/GlobalExceptionHandler.java`

### 6.1 当前处理分支

| @ExceptionHandler | 映射到 | HTTP 状态 | code 来源 |
|------------------|--------|----------|---------|
| BusinessException | ApiResponse.fail(ex.getCode(), ex.getMessage()) | 200 | ✅ 从异常获取 |
| Exception | ApiResponse.fail("INTERNAL_ERROR", "...") | 500 | ⚠️ 引用老 enum |
| PublicIdInvalidException (idresolve) | ApiResponse.fail("INVALID_PARAM", ...) | 400 | ⚠️ 引用老 enum |
| PublicIdNotFoundException (idresolve) | ApiResponse.fail("NOT_FOUND", ...) | 404 | ⚠️ 引用老 enum |
| PublicIdInvalidException (governance) | ApiResponse.fail("PUBLIC_ID_INVALID", ...) | 400 | ⚠️ magic string |
| PublicIdNotFoundException (governance) | ApiResponse.fail("PUBLIC_ID_NOT_FOUND", ...) | 404 | ⚠️ magic string |
| PublicIdForbiddenException | ApiResponse.fail("PUBLIC_ID_FORBIDDEN", ...) | 403 | ⚠️ magic string |
| PublicIdLookupMissingException | ApiResponse.fail("PUBLIC_ID_LOOKUP_MISSING", ...) | 500 | ⚠️ magic string |

### 6.2 缺失的处理分支

当前 **未处理** 以下异常（会被通用 Exception handler 捕获）:
- ✅ `MethodArgumentNotValidException` (参数校验) - **需新增**
- ✅ `ConstraintViolationException` (参数校验) - **需新增**
- ✅ `BindException` (参数绑定) - **需新增**
- ✅ `HttpMessageNotReadableException` (JSON 解析) - **需新增**

---

## 7. 迁移清单

### 7.1 A 类：可直接迁移（低风险）

这些调用点只依赖字符串 code，可批量替换为新 ErrorCode 常量：

| 替换点 | 数量 | 替换方案 | 估计工时 |
|--------|------|---------|---------|
| `throw new BusinessException("INVALID_PARAM", ...)` | ~10 | → `ParamErrorCode.INVALID_PARAM` | 0.5h |
| `throw new BusinessException("..._NOT_FOUND", ...)` | ~20 | → 各域 `XXXErrorCode.XXX_NOT_FOUND` | 1h |
| `SecurityCurrentUserContext` 中 `ErrorCode.AUTH_REQUIRED` | 3 处 | → `AuthErrorCode.AUTH_REQUIRED` | 0.5h |
| `GlobalExceptionHandler` 中老 `ErrorCode` 引用 | 3 处 | → 新 ErrorCode 常量 | 0.5h |
| PublicId 系列 magic string | 4 处 | → `PublicIdErrorCode.*` | 0.5h |

**小计**: ~37 处，约 **3 小时**

### 7.2 B 类：需要小改（中风险）

这些需要调整但风险可控：

| 调整点 | 问题 | 方案 | 估计工时 |
|--------|------|------|---------|
| `ContextErrorCode` | 未实现 ErrorCode 接口 | 重构为实现接口，或废弃 | 1h |
| `OrderErrorCode` 重复定义 | `domain/error/` vs `domain/enums/` | 合并为一个，迁移引用 | 1.5h |
| `BizException` 残留 | ~14 处调用 | 替换为 `BusinessException` | 1h |
| PublicId 异常重复定义 | `idresolve.api` vs `publicid.exception` | 合并为统一体系 | 2h |
| 参数校验异常未映射 | 无 `@ExceptionHandler` | 新增映射到 `ParamErrorCode` | 1h |

**小计**: 约 **6.5 小时**

### 7.3 C 类：高风险（需谨慎）

这些可能被外部/前端强依赖，需要兼容性评估：

| 风险点 | 影响面 | 推荐方案 | 估计工时 |
|--------|--------|---------|---------|
| 老 `ErrorCode` enum code 值 | 前端可能硬编码判断 `AUTH_REQUIRED` 等 | **保持 code 值不变**，只改内部实现 | 4h |
| `ApiResponse` 字段名 | 前端依赖 `code` / `message` | ✅ 无需改动（已是标准） | 0h |
| HTTP 状态码策略 | 前端依赖 200/500 区分 | ✅ 保持现有策略（混合） | 0h |
| PublicId 错误码 | 可能有前端硬编码 | 统一为标准格式，但**保持旧 code 值** | 2h |
| BusinessException 支持 String 构造器 | 代码分散，难以一次性改完 | 先 deprecate，逐步迁移，**最后删除** | 10h |

**小计**: 约 **16 小时**

---

## 8. 重点问题与建议

### 8.1 核心问题

1. **双 ErrorCode 体系并存**  
   - 老版: `com.bluecone.app.core.exception.ErrorCode` (enum, 25 个常量)  
   - 新版: `com.bluecone.app.core.error.ErrorCode` (interface)  
   - **建议**: 立即废弃老 enum，全局替换为新接口体系

2. **BusinessException 允许 magic string**  
   - 现有 `BusinessException(String code, String message)` 构造器导致约 **52 处** magic string 泄漏  
   - **建议**: 标记 `@Deprecated`，强制新代码只使用 `ErrorCode` 参数

3. **OrderErrorCode 双版本**  
   - `domain/error/OrderErrorCode` (实现 ErrorCode 接口，4 个常量)  
   - `domain/enums/OrderErrorCode` (独立 enum，34 个常量)  
   - **建议**: 合并为一个，保留 `domain/error/` 版本（更符合 DDD）

4. **PublicId 异常未统一**  
   - 两套实现 (`idresolve.api` vs `publicid.exception`)  
   - GlobalExceptionHandler 需分别处理  
   - **建议**: 合并为一套，统一错误码

5. **参数校验异常未映射**  
   - `@Valid` 校验失败时被通用 Exception handler 捕获，返回 500  
   - **建议**: 新增 `@ExceptionHandler(MethodArgumentNotValidException.class)`，映射到 `ParamErrorCode.INVALID_PARAM`

### 8.2 推荐执行顺序

**阶段 0: 准备阶段（Prompt 0 ✅）**
- ✅ 完成基线扫描

**阶段 1: 建立新标准（Prompt 1, 2h）**
- ⚠️ **不要在 app-core 新增错误码类**（已有新标准接口 + 部分实现）
- ✅ 只需补充缺失的错误码类：`AuthErrorCode`, `ParamErrorCode`, `TenantErrorCode`, `PublicIdErrorCode`
- ✅ 废弃老 `ErrorCode` enum
- ✅ 新增 `ErrorCodeRegistry`（可选）

**阶段 2: 统一 BusinessException（Prompt 2, 1h）**
- ✅ 标记 String 构造器为 `@Deprecated`
- ✅ 更新 Javadoc，引导使用 ErrorCode 版本

**阶段 3: 统一 GlobalExceptionHandler（Prompt 3, 2h）**
- ✅ 新增参数校验异常映射
- ✅ 替换老 ErrorCode enum 引用为新常量
- ✅ 统一 PublicId 异常映射

**阶段 4: 迁移 AUTH_REQUIRED（Prompt 4, 1h）**
- ✅ 新增 `AuthErrorCode.AUTH_REQUIRED`
- ✅ 替换 `SecurityCurrentUserContext` 中的引用
- ✅ 废弃老 `ErrorCode.AUTH_REQUIRED`

**阶段 5: 迁移参数错误（Prompt 5, 1h）**
- ✅ 新增 `ParamErrorCode.INVALID_PARAM` / `MISSING_PARAM`
- ✅ 替换 magic string 调用点

**阶段 6: 迁移资源不存在（Prompt 6, 2h）**
- ✅ 补充各域 `XXX_NOT_FOUND` 错误码
- ✅ 替换通用 `NOT_FOUND` 为具体域错误码

**阶段 7: 删除 BizException（Prompt 7, 1h）**
- ✅ 替换 14 处调用点
- ✅ 删除 `BizException` 类

**阶段 8: 建立自动检测（Prompt 8, 2h）**
- ✅ ArchUnit 规则 或 ErrorCodeRegistry 检查

**阶段 9: 清理残留（Prompt 9, 2h）**
- ✅ 删除老 `ErrorCode` enum
- ✅ 合并 `OrderErrorCode` 双版本
- ✅ 统一 PublicId 异常体系
- ✅ 全局搜索 magic string 清零

**总计**: 约 **14 小时**（不含测试与回归验证）

---

## 9. 附录：错误码清单

### 9.1 老版 ErrorCode enum（待废弃）

路径: `app-core/src/main/java/com/bluecone/app/core/exception/ErrorCode.java`

```
INVALID_PARAM, PARAM_MISSING, PARAM_INVALID, NOT_FOUND, PERMISSION_DENIED,
TOKEN_EXPIRED, AUTH_REQUIRED, AUTH_INVALID_CREDENTIALS, AUTH_TOKEN_INVALID,
AUTH_SESSION_INVALID, AUTH_REFRESH_TOO_FREQUENT, UNAUTHORIZED, RATE_LIMITED,
IDEMPOTENT_REJECTED, SIGNATURE_INVALID, INTERNAL_ERROR, THIRD_PARTY_ERROR,
PAY_FAILED, STOCK_NOT_ENOUGH, INVALID_VERSION, UNSUPPORTED_VERSION
```

共 **25 个** 常量（全部 magic string，需迁移）

### 9.2 新版 ErrorCode 体系（已部分实现）

| 错误码类 | code 前缀 | 数量 | 示例 |
|---------|----------|------|------|
| CommonErrorCode | SYS-xxx | 5 | SYS-500-000, SYS-401-000 |
| BizErrorCode | BIZ-xxx | 4 | BIZ-404-001, BIZ-403-001 |
| UserErrorCode | USR-xxx | 3 | USR-404-001, USR-403-001 |
| OrderErrorCode | OR-xxx | 4 | OR-404-001, OR-400-001 |
| StoreErrorCode | ST-xxx | 18 | ST-404-001, ST-400-001 |
| BillingErrorCode | BL-xxx | 22 | BL-404-001, BL-403-010 |
| InventoryErrorCode | INV-xxx | 1 | INV-404-001 |

**缺失的错误码类**（需新增）:
- `AuthErrorCode` (AUTH-xxx)
- `ParamErrorCode` (PARAM-xxx)
- `TenantErrorCode` (TNT-xxx)
- `PublicIdErrorCode` (PID-xxx)
- `PaymentErrorCode` (PAY-xxx)
- `PromoErrorCode` (PROMO-xxx)

---

## 10. 执行检查清单

迁移完成后，确认以下检查项：

- [ ] 老 `ErrorCode` enum 已删除（`app-core/.../exception/ErrorCode.java`）
- [ ] `BizException` 已删除
- [ ] `BusinessException` String 构造器已删除
- [ ] `OrderErrorCode` 只剩一个版本（`domain/error/`）
- [ ] PublicId 异常已统一（只保留一套）
- [ ] `GlobalExceptionHandler` 无 magic string（全用 ErrorCode 常量）
- [ ] `SecurityCurrentUserContext` 无老 ErrorCode 引用
- [ ] 参数校验异常已映射到 `ParamErrorCode`
- [ ] 全局搜索 `throw new BusinessException("` 结果为 0
- [ ] 全局搜索 `throw new BizException(` 结果为 0
- [ ] ArchUnit 测试通过（ErrorCode 规则）
- [ ] 端到端测试通过（错误响应格式一致）
- [ ] 前端团队确认 code 值无 breaking change

---

## 11. 风险提示

⚠️ **迁移风险**:
1. **前端依赖**: 如果前端硬编码判断 `code === "AUTH_REQUIRED"`，需确保新错误码的 `code()` 返回值**完全一致**
2. **第三方集成**: Webhook 回调等外部系统可能依赖错误码格式，需兼容性测试
3. **日志/监控**: 告警规则可能依赖特定错误码字符串，需同步更新
4. **测试覆盖**: 约 850+ 个抛错点，需要充分的单元测试与集成测试

⚠️ **不要改的部分**:
- ✅ `ApiResponse` 字段名（`code` / `message`）
- ✅ HTTP 状态码策略（业务 200，系统 500）
- ✅ 对外暴露的 code 值（如 `"AUTH_REQUIRED"` → 新实现仍返回 `"AUTH_REQUIRED"`）

---

**报告生成完成**  
下一步: 执行 **Prompt 1 - 建立唯一错误码中心**
