# 门店创建接口文档

## 📋 接口概述

手动创建门店的管理后台接口，支持完整的Token验证和权限校验机制。

## 🔗 接口信息

- **接口路径**: `POST /api/admin/stores`
- **权限要求**: `store:create`
- **认证方式**: Bearer Token（JWT）
- **租户隔离**: 通过 `X-Tenant-Id` 请求头实现

## 🔐 安全机制

### 1. Token验证
- 请求头必须携带有效的 `Authorization: Bearer {token}`
- Token由用户登录后获取，包含用户身份和权限信息
- Token过期后需要重新登录获取新Token

### 2. 权限验证
- 使用 `@RequireAdminPermission("store:create")` 注解实现
- 系统会自动拦截请求，验证Token中的用户是否拥有 `store:create` 权限
- 权限信息缓存5分钟，提升验证性能

### 3. 租户隔离
- 通过 `X-Tenant-Id` 请求头指定租户
- 创建的门店自动归属到指定租户
- 防止跨租户数据访问

## 📝 请求示例

### 请求头
```http
POST /api/admin/stores HTTP/1.1
Host: api.bluecone.com
Content-Type: application/json
X-Tenant-Id: 10001
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiaWF0IjoxNTE2MjM5MDIyfQ.SflKxwRJSMeKKF2QT4fwpMeJf36POk6yJV_adQssw5c
```

### 请求体（完整示例）
```json
{
  "name": "BlueCone咖啡朝阳门店",
  "shortName": "朝阳店",
  "storeCode": "BJ001",
  "industryType": "COFFEE",
  "cityCode": "110100",
  "openForOrders": true,
  "address": "北京市朝阳区建国路88号",
  "provinceCode": "110000",
  "districtCode": "110105",
  "contactPhone": "010-12345678",
  "longitude": 116.407526,
  "latitude": 39.904030,
  "logoUrl": "https://cdn.bluecone.com/stores/logo/bj001.jpg",
  "coverUrl": "https://cdn.bluecone.com/stores/cover/bj001.jpg"
}
```

### 请求体（最小示例）
```json
{
  "name": "BlueCone咖啡朝阳门店",
  "industryType": "COFFEE"
}
```

## 📤 响应示例

### 成功响应（200 OK）
```json
{
  "publicId": "sto_01HQZXYZ123456789ABCDEFG",
  "storeNo": 1001,
  "storeId": 12345,
  "message": "门店创建成功"
}
```

### 错误响应

#### 401 Unauthorized - Token缺失或无效
```json
{
  "code": "UNAUTHORIZED",
  "message": "未授权访问，请先登录",
  "timestamp": "2025-12-20T10:30:00Z"
}
```

#### 403 Forbidden - 无权限
```json
{
  "code": "FORBIDDEN",
  "message": "权限不足，需要 store:create 权限",
  "timestamp": "2025-12-20T10:30:00Z"
}
```

#### 400 Bad Request - 参数错误
```json
{
  "code": "INVALID_PARAMETER",
  "message": "参数校验失败",
  "errors": [
    {
      "field": "name",
      "message": "门店名称不能为空"
    },
    {
      "field": "industryType",
      "message": "行业类型不能为空"
    }
  ],
  "timestamp": "2025-12-20T10:30:00Z"
}
```

#### 409 Conflict - 门店编码重复
```json
{
  "code": "STORE_CONFIG_CONFLICT",
  "message": "门店编码已存在",
  "timestamp": "2025-12-20T10:30:00Z"
}
```

## 📋 字段说明

### 请求字段

| 字段名 | 类型 | 必填 | 说明 | 示例 |
|--------|------|------|------|------|
| name | String | ✅ | 门店全称 | "BlueCone咖啡朝阳门店" |
| shortName | String | ❌ | 门店简称，不传则使用name | "朝阳店" |
| storeCode | String | ❌ | 门店编码（租户内唯一），不传则自动使用publicId | "BJ001" |
| industryType | Enum | ✅ | 行业类型：COFFEE/FOOD/BAKERY/OTHER | "COFFEE" |
| cityCode | String | ❌ | 城市代码（国标行政区划代码） | "110100" |
| openForOrders | Boolean | ❌ | 是否开启接单，默认true | true |
| address | String | ❌ | 详细地址 | "朝阳区建国路88号" |
| provinceCode | String | ❌ | 省份代码 | "110000" |
| districtCode | String | ❌ | 区县代码 | "110105" |
| contactPhone | String | ❌ | 联系电话 | "010-12345678" |
| longitude | BigDecimal | ❌ | 经度（GCJ-02坐标系） | 116.407526 |
| latitude | BigDecimal | ❌ | 纬度（GCJ-02坐标系） | 39.904030 |
| logoUrl | String | ❌ | Logo图片URL | "https://cdn.example.com/logo.jpg" |
| coverUrl | String | ❌ | 封面图片URL | "https://cdn.example.com/cover.jpg" |

### 响应字段

| 字段名 | 类型 | 说明 | 示例 |
|--------|------|------|------|
| publicId | String | 对外公开ID，用于商户侧API | "sto_01HQZXYZ123456789ABCDEFG" |
| storeNo | Long | 门店数字编号，用于展示 | 1001 |
| storeId | Long | 内部Long主键，用于管理后台 | 12345 |
| message | String | 操作结果消息 | "门店创建成功" |

## 🔄 业务流程

1. **Token校验**：Spring Security自动验证Token有效性
2. **权限校验**：AdminPermissionAspect拦截并验证 `store:create` 权限
3. **租户隔离**：从请求头获取租户ID，确保门店归属正确租户
4. **参数校验**：验证必填字段（名称、行业类型等）
5. **唯一性校验**：检查门店编码在租户内是否唯一
6. **生成ID**：自动生成内部ID（ULID）、对外ID（publicId）、门店编号（storeNo）
7. **创建门店**：写入门店主表，初始化配置版本号为1
8. **初始化配置**：创建默认能力配置（堂食、自取）和营业时间（08:00-20:00）
9. **记录审计日志**：异步记录创建操作，包含操作人和创建数据
10. **返回结果**：返回publicId和storeNo供后续使用

## 🎯 默认配置

创建门店时，系统会自动初始化以下默认配置：

### 1. 门店状态
- 状态：`OPEN`（营业中）
- 接单开关：`true`（开启接单）
- 配置版本：`1`

### 2. 能力配置
- **堂食（DINE_IN）**：默认开启
- **自取（PICKUP）**：默认开启
- **外卖（TAKE_OUT）**：默认关闭（需手动开启）

### 3. 营业时间
- **周一至周日**：08:00 - 20:00
- 支持后续通过营业时间接口修改

## ⚠️ 注意事项

1. **Token必需**：请求头必须携带有效的 Authorization Token，否则返回 401
2. **权限必需**：Token用户必须拥有 `store:create` 权限，否则返回 403
3. **租户隔离**：门店自动归属到请求头中的租户ID，不可跨租户创建
4. **编码唯一**：storeCode在同一租户内必须唯一，如不传则自动使用publicId
5. **行业类型**：必须是有效的枚举值（COFFEE/FOOD/BAKERY/OTHER）
6. **默认状态**：新建门店默认状态为 OPEN，接单开关默认开启
7. **事务保证**：门店创建和配置初始化在同一事务内，保证数据一致性
8. **审计日志**：所有创建操作都会记录审计日志，便于后续追溯

## 🧪 测试用例

### 使用 curl 测试

```bash
# 1. 最小参数创建门店
curl -X POST "https://api.bluecone.com/api/admin/stores" \
  -H "Content-Type: application/json" \
  -H "X-Tenant-Id: 10001" \
  -H "Authorization: Bearer YOUR_TOKEN_HERE" \
  -d '{
    "name": "测试门店",
    "industryType": "COFFEE"
  }'

# 2. 完整参数创建门店
curl -X POST "https://api.bluecone.com/api/admin/stores" \
  -H "Content-Type: application/json" \
  -H "X-Tenant-Id: 10001" \
  -H "Authorization: Bearer YOUR_TOKEN_HERE" \
  -d '{
    "name": "BlueCone咖啡朝阳门店",
    "shortName": "朝阳店",
    "storeCode": "BJ001",
    "industryType": "COFFEE",
    "cityCode": "110100",
    "openForOrders": true,
    "address": "北京市朝阳区建国路88号",
    "provinceCode": "110000",
    "districtCode": "110105",
    "contactPhone": "010-12345678",
    "longitude": 116.407526,
    "latitude": 39.904030
  }'
```

### 使用 Postman 测试

1. **设置请求方法**：POST
2. **设置URL**：`https://api.bluecone.com/api/admin/stores`
3. **设置Headers**：
   - `Content-Type: application/json`
   - `X-Tenant-Id: 10001`
   - `Authorization: Bearer YOUR_TOKEN_HERE`
4. **设置Body**（选择raw JSON）：参考上面的请求体示例
5. **发送请求**

## 🔗 相关接口

- `GET /api/admin/stores/{id}` - 查询门店详情
- `PUT /api/admin/stores/{id}` - 更新门店基本信息
- `PUT /api/admin/stores/{id}/opening-hours` - 更新营业时间

## 📞 技术支持

如有问题，请联系技术支持团队。

