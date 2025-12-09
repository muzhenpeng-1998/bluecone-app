# 资源中心（Resource Center）设计与使用说明

## 1. 设计目标与场景

### 1.1 设计目标
- **多租户高隔离**：所有资源记录、绑定、Redis 会话都附带 `tenant_id`，资源查询会校验 `TenantContext`；跨租户访问会抛 `ResourceAccessDeniedException` 并输出告警日志。
- **高并发**：大文件从客户端直传 OSS，应用层只做“控制平面”逻辑（校验、策略、落库），避免承载大流量，缩短响应 latency。
- **高稳定性**：上传过程可重试与幂等（通过 uploadToken、storageKey、hashSha256），配额限流阻止某一租户滥用资源；失败日志/指标可用于告警。
- **易扩展**：业务依赖的是 `app-resource-api.ResourceClient`，真正实现集中在 `app-resource`，对接新的 profile 或存储实现时无需改动业务代码。

业务不会直接引用 OSS SDK，而是通过统一的 ResourceCenter 接口：这样可以统一处理多租户、配额、签名、CDN 域名，OS SDK 细节集中在 `StorageClient` 中，也方便后续切换 MinIO/COS 或加缓存策略。

### 1.2 典型使用场景
- 门店 Logo / 环境图
- 商品主图 / 图集
- 装修 Banner
- 导出报表（PDF/Excel）
- 用户头像

## 2. 模块结构与依赖关系

### 2.1 模块列表
- `app-resource-api`：面向业务暴露的接口、DTO、枚举、异常。（业务仅依赖此模块）
- `app-resource`：ResourceClient 的默认实现，封装事务、MyBatis、Redis 会话、配额、对象/绑定表、指标、日志。
- `app-infra`：提供底层基础设施，包括 Redis 操作、StorageClient 抽象、Aliyun OSS 实现、配置等。
- `app-application`：HTTP 层（`ResourceController`）和常驻的门户接口，调用 `ResourceClient` 提供前端用的 API。
- 业务模块（`app-store`、`app-product` 等）：通过 `app-resource-api` 的 `ResourceClient` 操作资源，不需要接触 OSS 细节。

### 2.2 依赖拓扑图
```
  app-application
        |
        v
 app-resource-api <--- app-store / app-product / app-order ...
        ^
        |
    app-resource  ---->  app-infra.storage (StorageClient + Aliyun OSS)
```
业务模块只能依赖 `app-resource-api`，真正的业务逻辑（事务、配额、Redis、MyBatis）在 `app-resource`；`app-infra` 提供 StorageClient 与 RedisOps 等基础能力。

## 3. 数据模型与表结构

### 3.1 bc_res_object：资源物理对象表
| 字段 | 描述 |
| --- | --- |
| `id` | 主键，MyBatis-Plus `ASSIGN_ID`，兼容雪花/ULID。
| `tenant_id` | 所属租户，所有操作都必须带上当前 `TenantContext`。
| `profile_code` | 资源档位（如 `STORE_LOGO`），对应 `ResourceProfileCode`。
| `storage_provider` | 存储提供者（如 `ALIYUN_OSS`），用于指标打点。
| `storage_key` | OSS 中的 object path（含环境/租户/owner/ULID）。
| `size_bytes` | 文件大小，完结后从 `completeUpload` 调用写入。
| `content_type` | MIME 类型。
| `file_ext` | 扩展名。
| `hash_sha256` | SHA-256，用于秒传/去重。
| `access_level` | 存取级别（1=PRIVATE, 2=PUBLIC_READ, 3=INTERNAL）。
| `status` | 状态位，当前始终置 1。
| `ext_json` | 预留 JSON 扩展字段。
| `created_at` / `updated_at` | 记录创建/更新时间。

每条 `bc_res_object` 记录对应 OSS 中的一个“物理对象”，可通过多个 `bc_res_binding` 复用。

### 3.2 bc_res_binding：资源绑定表
| 字段 | 描述 |
| --- | --- |
| `tenant_id` | 租户隔离。
| `owner_type` | 归属类型（`STORE`、`PRODUCT` 等）。
| `owner_id` | 业务实体 ID。
| `purpose` | 用途（`MAIN_LOGO`、`GALLERY` 等）。
| `resource_object_id` | 指向 `bc_res_object.id`。
| `sort_order` | 排序值，越小优先。
| `is_main` | 是否主资源（用于 `getMainResource`）。
| `created_at` / `created_by` | 绑定元数据。

绑定表实现一对多、多对一复用，例如一个门店多张图对应多条 binding，其中 `is_main=1` 表示主图，且 `ResourceClientImpl` 会在设为主图时重置其他绑定的 `is_main`。

### 3.3 Redis 会话与配额 Key 约定
- 上传会话：`res:upload:{uploadToken}`，内容为 `UploadSession` 记录（tenantId、profileCode、ownerType/Id、purpose、storageKey、expectedSize/hash、contentType、expiresAt）
  - TTL：由 `StorageUploadPolicy.expiresAt` 控制，若未返回则至少 `spec.expireSeconds`（`ResourceProfilesLoader` 中定义，生成时会将 `expiresAt` 存入记录）。
- 租户配额（每日指标）：
  - `res:quota:{tenantId}:cnt:{yyyyMMdd}` → 上传次数。
  - `res:quota:{tenantId}:bytes:{yyyyMMdd}` → 上传字节。
  - TTL：当前时间到次日 02:00（`quotaKeyTtl()` 中计算），便于隔日归零。

## 4. 核心域模型与接口

### 4.1 ResourceClient 接口（app-resource-api）
| 方法 | 说明 |
| --- | --- |
| `requestUploadPolicy(ResourceUploadRequest)` | 业务传 profile/owner/文件描述，校验 profile、配额、扩展名，返回直传策略 + uploadToken。 |
| `completeUpload(String uploadToken, String storageKey, long sizeBytes, String hashSha256)` | 上传完成时调用，读取 Redis 会话、确保同一租户、创建/复用 `bc_res_object`、落 `bc_res_binding`、扣配额。 |
| `getMainResource(ownerType, ownerId, purpose)` | 获取指定 owner 的主资源（按照 `is_main` + `sort_order` 选第一条）。 |
| `listResources(ResourceQuery)` | 查询当前 owner 下所有资源。 |
| `bindExistingObject(BindResourceCommand)` | 复用已有 `resource_object_id` 与 owner 建立 binding（支持设主图、排序）。 |
| `unbindResource(UnbindResourceCommand)` | 根据条件删除 binding。 |

### 4.2 DTO / VO 与枚举说明
- `ResourceUploadRequest`：包含 `profileCode`、`ownerType`/`ownerId`、`fileName`、`contentType`、`sizeBytes`、`hashSha256`。
- `UploadPolicyView`：返回 `uploadToken`、`uploadUrl`、`formFields`（现在是空 map）与 `expiresAt`。
- `ResourceHandle`：`objectId`（`bc_res_object.id`）、`profileCode`、`url`、`sizeBytes`、`contentType`。
- `ResourceQuery`：owner/purpose + 分页（可选）和排序控制。
- `BindResourceCommand` & `UnbindResourceCommand`：业务主动管理绑定，支持设为主图、排序权重。
- 枚举示例：
  - `ResourceOwnerType`: `STORE`、`PRODUCT`、`USER`、`TENANT`、`SYSTEM`。
  - `ResourcePurpose`: `MAIN_LOGO`、`GALLERY`、`BANNER`、`AVATAR`、`DETAIL_IMAGE`、`AUXILIARY`。
  - `ResourceProfileCode`: `STORE_LOGO`、`PRODUCT_IMAGE`、`EXPORT_REPORT`、`SYSTEM_TEMP`、`USER_AVATAR`。

### 4.3 资源 Profile（ResourceProfileSpec）
`ResourceProfileSpec` 定义了 profileCode、purpose、bucket/basePath、accessLevel、maxSizeBytes、允许扩展名/类型、策略过期秒数。当前在 `ResourceProfilesLoader` 中硬编码，示例如下：
```yaml
STORE_LOGO:
  bucketName: bluecone-static
  basePath: "store-logo"
  purpose: MAIN_LOGO
  accessLevel: PUBLIC_READ
  maxSizeBytes: 2MB
  allowedExtensions: [jpg, jpeg, png, webp]
  expireSeconds: 900
PRODUCT_IMAGE:
  bucketName: bluecone-static
  basePath: "product-images"
  purpose: GALLERY
  accessLevel: PUBLIC_READ
  maxSizeBytes: 5MB
  expireSeconds: 1200
EXPORT_REPORT:
  bucketName: bluecone-static
  basePath: "reports"
  accessLevel: PRIVATE
  maxSizeBytes: 10MB
  expireSeconds: 600
```
实际的 `basePath` 会在 `buildStorageKey` 中拼接日期、ownerType、ownerId、ULID，如 `prod/123/store-logo/2024/05/10/store-42/01HPZZ...`。
新增 profile 时通常只需：
1. 在 `ResourceProfilesLoader` /配置中新增 `ResourceProfileSpec`。
2. 若必要，在 `ResourceProfileCode` 枚举里新增常量。
3. 业务模块调用 `ResourceClient` 时用新 profileCode，若需特殊配额在 `bluecone.resource.quota.tenant-overrides` 中调整。

## 5. 存储与访问策略（StorageClient + CDN）

### 5.1 StorageClient 抽象
`StorageClient`（`app-infra.storage`）屏蔽存储 SDK，主要方法为：
- `generateUploadPolicy(GenerateUploadPolicyRequest)`
- `generateDownloadUrl(GenerateDownloadUrlRequest)`
- `deleteObject(String bucketName, String storageKey)`
`app-resource` 通过 `StorageClientDelegate` 进一步封装，未来可统一加监控/重试。

### 5.2 Aliyun OSS 实现与 CDN 域名
当前默认实现是 `AliyunOssStorageClient`：
- 配置前缀 `bluecone.storage.aliyun`：`endpoint`/`accessKeyId`/`accessKeySecret`、`defaultBucket`、`defaultExpireSeconds`、`cdnDomain`、`publicDomain`。
- 上传策略依赖 `GeneratePresignedUrlRequest`，返回 `PUT` URL 给前端直传。
- 下载 URL：
  - `PUBLIC_READ` 直接通过 `publicDomain`（有则优先）或 `cdnDomain` 拼接 `storageKey`。
  - `PRIVATE` 先生成带签名的 URL，然后把 host 替换为 `cdnDomain`，保留签名 query，避免暴露 OSS 原始域名。
- `cdnDomain` 可是 CDN CNAME，也可为空（此时返回 OSS 默认域名）；`publicDomain` 仅对公共读资源生效。

### 5.3 访问级别（AccessLevel）
- `PRIVATE`：必须通过 `generateDownloadUrl` 生成签名 URL，数据过期后 URL 失效。
- `PUBLIC_READ`：可以直接拼接 CDN 域名，适用于 Logo、商品主图。
- `INTERNAL`：预留给服务端内部消费，不通过 CDN 暴露。
不同 profile 在 `ResourceProfilesLoader` 中绑定 `AccessLevel`，`ResourceClientImpl` 会调用 `mapAccessLevel` 将其存入 `bc_res_object.access_level`。

## 6. 上传与绑定全流程

### 6.1 前端直传 OSS 的整体流程
1. 客户端请求 `/api/resource/upload/policy`，携带 `profileCode`、`ownerType`/`ownerId`、`fileName`、`contentType`、`sizeBytes`、`hashSha256`。
2. 后端 `ResourceClientImpl.requestUploadPolicy` 校验 profile、配额，调用 `StorageClient.generateUploadPolicy` 生成直传策略，写入 Redis 会话 (`UploadSessionStore`) 并返回 `uploadToken` + 上传 URL。
3. 前端直接 PUT 文件到 OSS（走 CDN，绕过应用层）。
4. 上传成功后，前端调用 `/api/resource/upload/complete`，带上 `uploadToken`、`storageKey`、`sizeBytes`、`hashSha256`。
5. 后端校验 token、storageKey、租户，去重/保存 `bc_res_object`、写入 `bc_res_binding`、更新配额、清 Redis，会返回 `ResourceHandle`。
6. 业务查询资源（`getMainResource` / `listResources`）时，ResourceCenter 会再生成下载 URL（带 CDN/签名）给业务，业务无需懂 OSS 与 CDN。

### 6.2 获取上传策略 (`requestUploadPolicy`)
- 校验入参：profile/owner/文件名必填。
- 校验 profile 规则（`ResourceProfileSpec`）：最大大小、允许扩展名/Content-Type。
- 检查配额：调用 `TenantResourceQuotaService.assertCanUpload`，包含每日次数与字节限额。
- 生成 `storageKey`（环境/tenant/basePath/日期/owner/ULID），调用 `StorageClient.generateUploadPolicy`。
- 根据策略有效期保存 `UploadSession`（包含预计大小、hash、purpose、profile、owner、expiresAt），Redis TTL 至少 `spec.expireSeconds`。

### 6.3 完成上传 (`completeUpload`)
- 从 Redis 读取 `UploadSession`，检查 tenantId、`uploadToken`、`storageKey`、是否过期。
- 校验实际大小不超过预期，实际 hash 不提供时使用预期 hash。
- 如果 hash 有值，尝试 `findByTenantProfileHash` 去重：已有对象复用，避免重复上传。
- 若需新建，写 `bc_res_object`（storageKey、size、contentType、ext、hash、accessLevel、status=1）。
- 建立 binding：如果已存在则刷新 `sort_order`/`is_main`；否则插入新记录，并在设主图时重置其他 binding 的 `is_main`。
- `TenantResourceQuotaService.consumeQuota`：次数 + 字节累加，key TTL 延伸至次日 02:00。
- 删除 Redis 会话，返回 `ResourceHandle`（`getDownloadUrl` 会通过 CDN/签名）。

### 6.4 绑定 / 解绑已有资源
- `bindExistingObject`：业务可把已有 `resource_object_id` 绑定到任意 owner，支持排序/主图标记。
- `unbindResource`：按 owner/purpose/`resourceObjectId`/`sortOrder`/`isMain` 删除 binding。
- 该逻辑允许多个业务共享同一个 `bc_res_object`，例如多个门店共用同一张素材图。

## 7. 多租户隔离与配额控制

### 7.1 租户隔离策略
- 所有表（`bc_res_object`、`bc_res_binding`）都带 `tenant_id`，查询/写入必须使用当前 `TenantContext.getTenantId()`，不信任外部传参。
- `ResourceClientImpl.ensureTenant`、`ensureOwnerInfo`、`resolveTenantId` 系列方法确保请求与 Redis 会话、绑定等都在同一租户下；跨租户访问抛 `ResourceAccessDeniedException`，并写 warn 日志。

### 7.2 日配额模型（TenantResourceQuotaService）
- 配置项 `bluecone.resource.quota`：
  - `defaultDailyUploadCountLimit`（默认 2000 次）
  - `defaultDailyUploadBytesLimit`（默认 10GiB）
  - `tenantOverrides` 可按租户自定义 `dailyUploadCountLimit`/`dailyUploadBytesLimit`。
- `assertCanUpload`：在 `requestUploadPolicy` 之前检查当前计数/字节是否超过配额，超出则抛 `ResourceUploadException`（代码 `RES_QUOTA_DAILY_*`），并记录 Micrometer `resource.quota.exceeded` + warn 日志。
- `consumeQuota`：上传完成后再 `INCR` 次数与字节，首次写入时附加 TTL（到次日下午 02:00）。

### 7.3 错误码约定与前端处理建议
| 错误码 | 含义 | 处理建议 |
| --- | --- | --- |
| `RES_QUOTA_DAILY_COUNT_EXCEEDED` | 上传次数超过当日限额 | 提示“今日上传额度已用完”，引导联系客服/升级套餐。 |
| `RES_QUOTA_DAILY_BYTES_EXCEEDED` | 当天传输总字节超过限额 | 同上，建议提示可上传的剩余大小。
| `RES_UPLOAD_SESSION_EXPIRED` | uploadToken 未知或 TTL 过期 | 提示“上传信息过期，请重新获取策略”。
| `RES_UPLOAD_INVALID_ARGUMENT` | 缺少 owner、profile、文件名，或 storageKey/hash 校验失败 | 检查前端传参、文件 metadata。
| `RES_UPLOAD_STORAGE_ERROR` | OSS 策略/签名失败或 `StorageClient` 抛异常 | 提示稍后重试，服务端关注告警。

## 8. 读路径与业务集成示例（以门店 Logo 为例）

### 8.1 获取主资源（`getMainResource`）
- `ResourceClient.getMainResource(ResourceOwnerType.STORE, storeId, ResourcePurpose.MAIN_LOGO)` 会按 `is_main DESC`、`sort_order ASC` 取最优 binding，若无资源返回 `null`。
- 返回的 `ResourceHandle.url` 已经替换为 CDN/签名地址，业务模块无需关心 bucket/OSS。

### 8.2 列表查询（`listResources`）
- `listResources(ResourceQuery)` 适用于商品图集、门店环境图等场景，支持分页、自定义排序字段。
- `ResourceHandle` 列表顺序同样遵循 `is_main` + `sort_order`，可用于前端展示。

### 8.3 门店模块接入示例（伪代码）
```java
// 申请上传门店 Logo
UploadPolicyView policy = resourceClient.requestUploadPolicy(
        new ResourceUploadRequest(
                ResourceProfileCode.STORE_LOGO,
                ResourceOwnerType.STORE,
                storeId,
                fileName,
                contentType,
                sizeBytes,
                hashSha256));
// 前端使用 policy 直传 OSS

// 上传完成后确认资源，返回 ResourceHandle
ResourceHandle handle = resourceClient.completeUpload(uploadToken, storageKey, sizeBytes, hashSha256);

// 查询门店详情时获取 logo URL
ResourceHandle logoHandle = resourceClient.getMainResource(
        ResourceOwnerType.STORE,
        storeId,
        ResourcePurpose.MAIN_LOGO);
String logoUrl = logoHandle != null ? logoHandle.url() : null;
```
门店模块完全不关心 bucket 名称、OSS SDK、CDN 签名等细节，只依赖 `ResourceClient`。若多个门店共享图，亦可通过 `bindExistingObject` 复用已有 `resource_object_id`。

## 9. Metrics 与日志

### 9.1 指标列表（Micrometer）
| 指标 | 描述 | 典型用途 |
| --- | --- | --- |
| `resource.upload.policy.requests`（tag: profile/tenant/result） | `requestUploadPolicy` 请求量，`result` 为 `success/fail` | Grafana 可看某个租户或 profile 的策略下发成功率。|
| `resource.upload.completed`（tag: profile/tenant/provider/result） | `completeUpload` 成功/失败次数 | 监控 storage provider（如 `ALIYUN_OSS`）是否异常。|
| `resource.upload.bytes`（tag: profile） | 上传字节量（summary） | 看每日大型文件流量趋势。|
| `resource.download.url.generated`（tag: profile/accessLevel） | 生成下载 URL 的次数 | 判断某 profile 是否频繁被访问。|
| `resource.quota.exceeded`（tag: tenant/profile/type） | 配额触发次数 | 监控是否常态限流，观察 `type=count/bytes`。|

### 9.2 日志字段与排障建议
- `[ResourceUploadPolicy]`：输出 `tenant`、`ownerType/Id`、`profile`、`fileName`、`sizeBytes`、`uploadToken`、`storageKey`。
- `[ResourceUploadComplete]`：包含 `tenant`、`owner`、`profile`、`resourceId`、`storageProvider`、`storageKey`、`sizeBytes`。
- `[ResourceQuotaExceeded]`：告警级别，包含配额类型、当前值与上限。
- `[ResourceBinding]` / `[ResourceUnbind]`：记录 binding/解绑详情。

日志中禁止记录 `AccessKeyId/Secret`、完整带签名 URL，但可以记录 `storageKey`、`tenantId`、`profileCode`、`ownerType/Id` 方便关联问题。

## 10. 如何扩展与演进

### 10.1 新增资源 Profile
1. 在 `ResourceProfilesLoader` 或配置文件中添加新的 `ResourceProfileSpec`，定义 `bucket/basePath/accessLevel/maxSize/expireSeconds`。
2. 如有需要，在 `ResourceProfileCode` 枚举里新增常量，同时同步 `ResourceProfileSpec` 的 key。
3. 业务调用 `ResourceClient` 时直接使用新的 profileCode。
4. 若新 profile 对应特殊配额，可在 `application.yml` 中扩展 `bluecone.resource.quota.tenant-overrides`。

### 10.2 接入新存储实现（MinIO / COS）
- 在 `app-infra.storage` 下新增对应的 `StorageClient` 实现（`MinioStorageClient` / `CosStorageClient`），实现 `generateUploadPolicy`/`generateDownloadUrl`/`deleteObject`。
- 在 `StorageProperties` 里通过 `bluecone.storage.provider` 指定 `MINIO` 或 `LOCAL`，并通过新配置类注册对应 bean。
- `app-resource` 只依赖 `StorageClient`，无需改动；`ResourceClientImpl` 仍可通过 `StorageClientDelegate` 访问。

### 10.3 自建存储 / 多云迁移注意事项
- `storageKey` 语义应与存储实现解耦，不要将 provider 名称、Region 等硬编码在 key 中；便于切换存储时复用 `storage_key`。
- 迁移时可遍历 `bc_res_object`，根据 `storage_provider`/`storage_key` 批量同步数据到新存储，再统一更新 `storage_provider` 和 `bc_res_binding`。
- 注意签名算法与 CDN 域名切换可能导致 URL 缓存失效，必要时缩短 `expireSeconds` 以强制刷新。

## 11. FAQ 与常见坑
- **为什么我上传成功了，但查询不到资源？** 先确认 `completeUpload` 是否调用成功、`UploadSession` 是否过期，上传后 `bc_res_binding` 必须写入；还要确认当前 `TenantContext` 与 `session.tenantId` 一致。
- **为什么 URL 一会儿能访问一会儿不能？** 检查 `ResourceProfileSpec` 的 `AccessLevel`：PRIVATE 会生成短期签名 URL，过期后访问失败；也可能是 CDN 缓存或 `expiresAt` 设置过短。
- **如何限制某个大客户不要一直上传大文件？** 在 `application.yml` 中给该租户添加 `bluecone.resource.quota.tenant-overrides.{tenantId}`，降低 `dailyUploadByteLimit` 或 `dailyUploadCountLimit`，并在前端提示“超出额度”。
- **绑定同一对象到多个业务时需要注意什么？** `bindExistingObject` 会验证 owner/tenant，并可借助 `isMain`/`sortOrder` 控制显示；若要替换主图，先调用 `bindExistingObject` 带 `isMain=true`，系统会把其它 binding 的 `is_main` 重置。
