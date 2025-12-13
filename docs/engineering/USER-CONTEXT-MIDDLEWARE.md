# UserContextMiddleware（用户上下文中间件）

用户上下文中间件在用户侧接口链路中统一完成：

- 从鉴权凭证（JWT / dev header）解析当前用户主体（tenantId + userInternalId）；
- 基于 `ContextMiddlewareKit` 加载并缓存用户运行态快照 `UserSnapshot`；
- 对必须登录的路径进行统一的登录态校验与用户状态校验；
- 将用户上下文注入 `ApiContext` 与 MDC（脱敏）。

> 注意：这里承载的是“用户状态/会员信息快照”，**不是账号密码或敏感隐私数据**，手机号/邮箱不会写入 MDC。

---

## 1. 快照契约：UserSnapshot

模块：`app-core`

路径：`app-core/src/main/java/com/bluecone/app/core/user/runtime/api/UserSnapshot.java`

```java
public record UserSnapshot(
        long tenantId,
        Ulid128 userId,
        int status,          // 1 normal / 0 frozen / -1 deleted
        boolean phoneBound,
        String memberLevel,
        long configVersion,
        Instant updatedAt,
        Map<String, Object> ext
) {}
```

语义约定：

- `tenantId`：租户 ID；
- `userId`：用户内部主键（Ulid128），当前内部仍以 Long userId 为主，暂通过简单映射构造；
- `status`：
  - `1`：正常；
  - `0`：冻结/封禁；
  - `-1`：已删除/注销；
- `phoneBound`：是否绑定手机号（按 identity 表是否有 phone 字段推导）；
- `memberLevel`：会员等级标识（示例：会员等级 id 或 code）；
- `configVersion`：配置版本号：
  - 当前实现以身份/会员/画像三者中最新的 `updated_at` 时间戳（epochMilli）派生，满足单调递增即可；
- `updatedAt`：最新更新时间（UTC Instant）；
- `ext`：扩展字段，当前包含：
  - `memberStatus`：会员状态（来自 `bc_tenant_member.status`）；
  - `nickname`：用户昵称；
  - `numericUserId`：数值型 userId（用于回填 `ApiContext.userId`）。

---

## 2. 用户主体解析 SPI：UserPrincipalResolver

路径：`app-core/src/main/java/com/bluecone/app/core/user/runtime/spi/UserPrincipalResolver.java`

```java
public interface UserPrincipalResolver {

    Optional<UserPrincipal> resolve(HttpServletRequest req);

    record UserPrincipal(
            long tenantId,
            Ulid128 userId,
            String authType,
            String subject
    ) {}
}
```

职责：

- 从 `HttpServletRequest` 中解析当前请求的用户主体；
- 返回值可能为空（匿名请求）；
- `authType` 用于标记认证类型（如 `JWT`、`DEV_HEADER` 等）；
- `subject` 为认证系统中的 subject 标识（如 userId 字符串），仅用于内部追踪，不写入 MDC。

### 2.1 默认实现：UserPrincipalResolverImpl（JWT）

路径：`app-application/src/main/java/com/bluecone/app/core/user/runtime/UserPrincipalResolverImpl.java`

实现策略：

1. 从 Header `Authorization: Bearer <token>` 读取访问令牌；
2. 调用 `TokenProvider.parseAccessToken` 解析为 `TokenUserContext`；
3. 若包含 `tenantId` 与 `userId`：
   - 构造 `Ulid128 internalId = new Ulid128(userId, 0L)`（当前作为简化映射，后续可接入真正的 `internal_id` 列）；  
   - 返回 `UserPrincipal(tenantId, internalId, "JWT", String.valueOf(userId))`。

当 Header 不存在/无效时返回 `Optional.empty()`，由上层决定是否允许匿名。

### 2.2 dev-only 实现：DevHeaderUserPrincipalResolver

路径：`app-application/src/main/java/com/bluecone/app/core/user/runtime/DevHeaderUserPrincipalResolver.java`

特性：

- 仅在 `dev` Profile 启用（`@Profile("dev")`）；
- 从 Header 解析：
  - `X-Tenant-Id`；
  - `X-User-Id`；
- 成功时构造 `UserPrincipal(tenantId, new Ulid128(userId, 0L), "DEV_HEADER", userHeader)`；
- 用于本地/开发环境快速模拟登录态，在生产环境不可用。

---

## 3. Repository SPI 与 SnapshotProvider

### 3.1 SPI：UserSnapshotRepository

路径：`app-core/src/main/java/com/bluecone/app/core/user/runtime/spi/UserSnapshotRepository.java`

```java
public interface UserSnapshotRepository extends SnapshotRepository<UserSnapshot> {
}
```

### 3.2 仓储实现：UserSnapshotRepositoryImpl

模块：`app-infra`

路径：`app-infra/src/main/java/com/bluecone/app/infra/user/runtime/UserSnapshotRepositoryImpl.java`

依赖：

- `UserIdentityMapper`（`bc_user_identity`）；
- `TenantMemberMapper`（`bc_tenant_member`）；
- `UserProfileMapper`（`bc_user_profile`）。

实现细节：

- 当前内部仍以 Long userId 为主键，`SnapshotLoadKey.scopeId` 中传入的 `Ulid128` 简化为：

```java
Ulid128 internalId = (Ulid128) key.scopeId();
Long userId = internalId != null ? internalId.getLeastSignificantBits() : null;
```

- `loadFull`：
  - 按 userId 加载 `UserIdentityDO`；
  - 按 `(tenantId, userId)` 加载 `TenantMemberDO`；
  - 按 userId 加载 `UserProfileDO`；
  - 组合生成 `UserSnapshot`：
    - `status`：来自 identity.status（1/0/-1），缺省视为 1；
    - `phoneBound`：identity.phone 是否非空；
    - `memberLevel`：简单用 `member.level_id` 字符串表示；
    - `configVersion`：使用 identity/member/profile 的最新 `updated_at` 时间戳（毫秒）；  
    - `updatedAt`：上述 `updated_at` 中的最大值；
    - `ext`：写入 `memberStatus`、`nickname`、`numericUserId` 等。
- `loadVersion`：
  - 轻量查询 identity 的 `updated_at`，转为 epochMilli 作为版本号；
  - 未查到或字段为空时返回 `Optional.empty()`。

### 3.3 SnapshotProvider 行为

用户中间件复用 `ContextMiddlewareKit` 的通用 `SnapshotProvider<T>`：

- 缓存键：`user:snap:{tenantId}:{scopeId.toString()}`；
- 逻辑与 Store/Inventory 保持一致：
  - L1/L2 缓存；
  - 负缓存：`loadFull` 返回 empty 时写入 `NegativeValue`；
  - 版本校验：命中缓存后在一定时间窗口/采样率下使用 `loadVersion` 轻量校验，如版本不一致则执行 reload。

单测：`UserSnapshotProviderVersionReloadTest`（`app-core/src/test/java/com/bluecone/app/core/user/runtime/UserSnapshotProviderVersionReloadTest.java`）  
模拟两次 `loadFull` 返回不同 `memberLevel`，两次 `loadVersion` 返回 1 / 2，验证版本不一致触发 reload。

---

## 4. Resolver 与 Middleware

### 4.1 UserContextResolver

路径：`app-application/src/main/java/com/bluecone/app/application/middleware/UserContextResolver.java`

配置类：`UserContextProperties`（`bluecone.user.context.*`）

流程：

1. 判定是否启用：
   - `bluecone.user.context.enabled=true`；
   - 请求路径命中 `includePaths` 且不在 `excludePaths`。
2. 调用 `UserPrincipalResolver.resolve(request)`：
   - 若返回 `Optional.empty()`：
     - 若路径命中 `allowAnonymousPaths` → 注入匿名：`ApiContext.userId=null`，`USER_SNAPSHOT` 为空；
     - 否则抛出 `BizException(CommonErrorCode.UNAUTHORIZED)`（HTTP 401）。
3. 对于解析出的 `UserPrincipal`：

```java
SnapshotLoadKey loadKey = new SnapshotLoadKey(tenantId, "user:snap", principal.userId());
UserSnapshot snapshot = snapshotProvider.getOrLoad(
        loadKey, snapshotRepository, cache, versionChecker, serde, kitProperties);
```

   - `snapshot == null` → `BizException(UserErrorCode.USER_NOT_FOUND)`（USR-404-001）；
   - `snapshot.status == 0` → `BizException(UserErrorCode.USER_FROZEN)`（USR-403-001）；
   - `snapshot.status == -1` → `BizException(UserErrorCode.USER_DELETED)`（USR-410-001）。
4. 注入 ApiContext：
   - 从 `snapshot.ext.numericUserId` 回填数值型 userId：`apiCtx.setUserId(numericUserId)`；
   - `apiCtx.putAttribute("USER_SNAPSHOT", snapshot)`；
5. 写入 MDC（隐私保护）：
   - `userId`：使用 `Ulid128` 字符串进行掩码：前 3 + `...` + 后 3，例如 `abc...xyz`；
   - `authType`：写入认证类型（`JWT` / `DEV_HEADER` 等）；
   - 不写入手机号/email 等敏感字段。

### 4.2 UserMiddleware（接入 ApiGateway）

路径：`app-application/src/main/java/com/bluecone/app/gateway/middleware/UserMiddleware.java`

- 依赖：
  - `UserContextResolver`；
  - `UserContextProperties`。
- 行为：
  - 判定路径是否需要用户上下文；
  - 若需要，调用 `userContextResolver.resolve(ctx)`；
  - 然后继续链路 `chain.next(ctx)`。

### 4.3 AutoConfiguration 与网关顺序

AutoConfiguration：`app-application/src/main/java/com/bluecone/app/config/UserContextAutoConfiguration.java`

- 条件：
  - `bluecone.user.context.enabled=true`；
  - 存在 `UserPrincipalResolver`、`UserSnapshotRepository`、`ContextCache`、`ObjectMapper`、`VersionChecker`。
- 注册：
  - `UserContextResolver`；
  - `UserMiddleware`。

网关接入：`app-application/src/main/java/com/bluecone/app/gateway/ApiGateway.java`

- 在 `TenantMiddleware` 和 `StoreMiddleware` 之后插入用户中间件：

```java
if (contract.isTenantRequired()) {
    chain.add(tenantMiddleware);
}
// After tenant binding, resolve store context if needed
chain.add(storeMiddleware);
// User context middleware (optional)
try {
    UserMiddleware userMiddleware = applicationContext.getBean(UserMiddleware.class);
    chain.add(userMiddleware);
} catch (BeansException ignored) {
}
```

若未启用 user context 或缺少 Bean，则不会插入 `UserMiddleware`。

---

## 5. 测试覆盖

### 5.1 匿名路径允许无 user

单测：`UserContextResolverAnonymousTest.anonymousPathShouldAllowNoUser`  
路径：`app-application/src/test/java/com/bluecone/app/application/middleware/UserContextResolverAnonymousTest.java`

- 配置：
  - `bluecone.user.context.enabled=true`；
  - `includePaths=["/api/mini/**"]`；
  - `allowAnonymousPaths=["/api/mini/public/**"]`。
- 行为：
  - `UserPrincipalResolver.resolve` 返回 `Optional.empty()`；
  - 请求路径 `/api/mini/public/ping`；
  - 断言：`ApiContext.userId == null`，`USER_SNAPSHOT` 属性为空，且未调用 `UserSnapshotRepository`。

### 5.2 必须登录路径无 user -> 401

单测：`UserContextResolverAnonymousTest.nonAnonymousPathWithoutUserShouldThrowUnauthorized`

- 路径 `/api/mini/secure/data` 不在 `allowAnonymousPaths`；
- `UserPrincipalResolver.resolve` 返回 empty；
- 断言：抛出 `BizException(CommonErrorCode.UNAUTHORIZED)`，且不调用仓储。

### 5.3 user not found -> 404 + 负缓存

单测：`UserContextResolverAnonymousTest.userNotFoundShouldWriteNegativeCacheAndThrow`

- `UserPrincipalResolver.resolve` 返回合法 principal；
- `UserSnapshotRepository.loadFull` 返回 `Optional.empty()`；
- `SnapshotProvider` 写入负缓存（由通用实现保证）；
- 断言：
  - `BizException(UserErrorCode.USER_NOT_FOUND)` 抛出；
  - `loadFull` 只调用一次（第二次命中负缓存时不会重复访问仓储）。

### 5.4 version 不一致 reload

单测：`UserSnapshotProviderVersionReloadTest`

- 通过自定义 `AlwaysCheckVersionChecker` 总是触发版本校验；
- 模拟：
  - 第一次 `loadFull` 返回 memberLevel = `L1`，版本号 1；
  - 第二次版本校验发现 DB 版本为 2，`reloadAndFill` 返回 memberLevel = `L2`；
- 断言：
  - `getOrLoad` 第一次返回 `L1`，第二次返回 `L2`；
  - `loadFull` 被调用两次。

---

## 6. 匿名/登录策略与隐私日志规范

- 对于允许匿名访问的路径：
  - 在 `allowAnonymousPaths` 中显式配置；
  - 不要求提供鉴权头，`UserContextMiddleware` 不会抛 401；
  - `ApiContext.userId` 为 null，业务代码需自行做“未登录”分支处理。
- 对于必须登录路径：
  - 未携带或无法解析有效用户 → 统一抛出 `CommonErrorCode.UNAUTHORIZED`（SYS-401-000）；
  - 下游只需关注业务错误码，不需要再重复做登录校验。
- 隐私日志规范：
  - 仅将经过掩码的 userId（基于内部 ULID 字符串）写入 MDC；
  - 不在 MDC 中写入手机号、邮箱、unionId 等敏感字段；
  - `authType` 字段用于区分认证来源（JWT / dev header 等）。

---

## 7. dev-only Header 策略

- `DevHeaderUserPrincipalResolver` 仅在 `dev` Profile 下启用，解析：
  - `X-Tenant-Id`；
  - `X-User-Id`。
- 生产环境中不会注册该 Bean，保证不会因为 Header 被伪造而绕过正常鉴权。
- 建议在本地调试时配合 MockMvc / Postman 设置上述 Header，即可测试用户上下文链路。

---

## 8. 缓存策略总结

- 采用 `ContextMiddlewareKit` 统一机制：
  - L1 Caffeine 缓存：减少重复 DB 访问；
  - 配置化 TTL 与负缓存 TTL；
  - 版本校验窗口 + 采样：兼顾一致性与 DB 压力；
  - L2 Redis 如全局启用 `contextKitCache` 时可复用。
- 在高 QPS 场景中，可通过调整 `ContextKitProperties`：
  - `versionCheckWindow`：加大窗口降低版本校验频率；
  - `versionCheckSampleRate`：控制采样比例；
  - `l1Ttl` / `negativeTtl`：平衡命中率与刷新时效性。

> 最终效果：UserContextMiddleware 为每个请求提供一个稳定的“用户状态/会员信息快照”，不重复侵入业务服务、避免敏感信息泄露，并利用统一的快照缓存/版本失效框架保证性能与一致性。 

