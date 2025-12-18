# Store API 使用示例

本文档提供门店（Store）模块的 REST API 调用示例，使用 curl 命令演示。

## 前置条件

1. 应用已启动（默认端口 80）
2. 已配置租户上下文（通过请求头 `X-Tenant-Id` 或认证 Token）
3. 数据库已初始化（Flyway 迁移已完成）

## 一、管理后台接口（AdminStoreController）

### 1. 创建门店

```bash
# 创建门店（基础信息）
curl -X POST http://localhost:80/api/admin/store \
  -H "Content-Type: application/json" \
  -H "X-Tenant-Id: 1001" \
  -H "Idempotency-Key: create-store-001" \
  -d '{
    "name": "测试门店",
    "shortName": "测试",
    "industryType": "FOOD",
    "cityCode": "330100",
    "openForOrders": true
  }'

# 响应示例：
# {
#   "code": "SUCCESS",
#   "message": "操作成功",
#   "data": {
#     "storePublicId": "sto_01HN8X5K9G3QRST2VW4XYZ"
#   }
# }
```

### 2. 查询门店详情

```bash
# 通过 storeId 查询
curl -X GET "http://localhost:80/api/admin/store/detail?storeId=123" \
  -H "X-Tenant-Id: 1001"

# 通过 storeCode 查询
curl -X GET "http://localhost:80/api/admin/store/detail?storeCode=STORE001" \
  -H "X-Tenant-Id: 1001"
```

### 3. 查询门店列表

```bash
# 查询所有门店
curl -X GET "http://localhost:80/api/admin/store/list" \
  -H "X-Tenant-Id: 1001"

# 按条件查询（城市、行业类型、状态、关键字）
curl -X GET "http://localhost:80/api/admin/store/list?cityCode=330100&industryType=FOOD&status=OPEN&keyword=测试" \
  -H "X-Tenant-Id: 1001"
```

### 4. 更新门店基础信息

```bash
# 更新门店名称等信息（需要提供 configVersion 做乐观锁）
curl -X PUT http://localhost:80/api/admin/store/base \
  -H "Content-Type: application/json" \
  -H "X-Tenant-Id: 1001" \
  -d '{
    "tenantId": 1001,
    "storeId": 123,
    "expectedConfigVersion": 1,
    "name": "测试门店（已更新）",
    "shortName": "测试更新"
  }'

# 注意：如果 configVersion 不匹配，会返回版本冲突错误：
# {
#   "code": "ST-409-001",
#   "message": "门店配置版本冲突，请刷新后重试"
# }
```

### 5. 更新门店能力配置

```bash
# 更新门店支持的能力（如堂食、外卖等）
curl -X PUT http://localhost:80/api/admin/store/capabilities \
  -H "Content-Type: application/json" \
  -H "X-Tenant-Id: 1001" \
  -d '{
    "tenantId": 1001,
    "storeId": 123,
    "expectedConfigVersion": 1,
    "capabilities": [
      {
        "capability": "DINE_IN",
        "enabled": true,
        "configJson": "{}"
      },
      {
        "capability": "TAKE_OUT",
        "enabled": true,
        "configJson": "{\"deliveryRadius\": 5000}"
      }
    ]
  }'
```

### 6. 更新常规营业时间

```bash
# 更新门店常规营业时间（周一至周五 9:00-18:00）
curl -X PUT http://localhost:80/api/admin/store/opening-hours \
  -H "Content-Type: application/json" \
  -H "X-Tenant-Id: 1001" \
  -d '{
    "tenantId": 1001,
    "storeId": 123,
    "expectedConfigVersion": 1,
    "schedule": {
      "regularHours": [
        {
          "weekday": 1,
          "startTime": "09:00:00",
          "endTime": "18:00:00",
          "periodType": "REGULAR"
        },
        {
          "weekday": 2,
          "startTime": "09:00:00",
          "endTime": "18:00:00",
          "periodType": "REGULAR"
        }
      ]
    }
  }'
```

### 7. 更新特殊日配置

```bash
# 配置节假日或特殊日期
curl -X PUT http://localhost:80/api/admin/store/special-days \
  -H "Content-Type: application/json" \
  -H "X-Tenant-Id: 1001" \
  -d '{
    "tenantId": 1001,
    "storeId": 123,
    "expectedConfigVersion": 1,
    "specialDays": [
      {
        "date": "2025-12-25",
        "specialType": "CLOSED",
        "remark": "圣诞节停业"
      },
      {
        "date": "2025-12-31",
        "specialType": "SPECIAL_TIME",
        "startTime": "10:00:00",
        "endTime": "22:00:00",
        "remark": "跨年延长营业"
      }
    ]
  }'
```

### 8. 切换门店状态

```bash
# 切换门店状态（OPEN/PAUSED/CLOSED）
curl -X PUT http://localhost:80/api/admin/store/status \
  -H "Content-Type: application/json" \
  -H "X-Tenant-Id: 1001" \
  -d '{
    "tenantId": 1001,
    "storeId": 123,
    "expectedConfigVersion": 1,
    "status": "PAUSED"
  }'
```

### 9. 切换接单开关

```bash
# 临时关闭接单（门店仍处于 OPEN 状态，但不接新订单）
curl -X PUT http://localhost:80/api/admin/store/open-for-orders \
  -H "Content-Type: application/json" \
  -H "X-Tenant-Id: 1001" \
  -d '{
    "tenantId": 1001,
    "storeId": 123,
    "expectedConfigVersion": 1,
    "openForOrders": false
  }'
```

## 二、商户侧接口（MerchantStoreController）

### 1. 查询门店详情（使用 PublicId）

```bash
# 使用 publicId 查询门店详情（自动解析并校验租户隔离）
curl -X GET "http://localhost:80/api/merchant/stores/sto_01HN8X5K9G3QRST2VW4XYZ" \
  -H "X-Tenant-Id: 1001"
```

### 2. 查询门店列表

```bash
# 查询所有门店
curl -X GET "http://localhost:80/api/merchant/stores" \
  -H "X-Tenant-Id: 1001"

# 按 storeId（publicId）过滤
curl -X GET "http://localhost:80/api/merchant/stores?storeId=sto_01HN8X5K9G3QRST2VW4XYZ" \
  -H "X-Tenant-Id: 1001"
```

## 三、开放接口（OpenStoreController）

### 1. 获取门店基础信息

```bash
curl -X GET "http://localhost:80/api/open/store/base?storeId=123" \
  -H "X-Tenant-Id: 1001"
```

### 2. 获取订单快照

```bash
# 获取门店订单快照（包含可接单状态、能力等）
curl -X GET "http://localhost:80/api/open/store/order-snapshot?storeId=123&channelType=WECHAT_MINI" \
  -H "X-Tenant-Id: 1001"
```

### 3. 检查是否可接单

```bash
# 检查门店是否可接指定类型的订单
curl -X GET "http://localhost:80/api/open/store/check-acceptable?storeId=123&capability=DINE_IN&channelType=WECHAT_MINI" \
  -H "X-Tenant-Id: 1001"

# 响应示例：
# {
#   "code": "SUCCESS",
#   "message": "操作成功",
#   "data": {
#     "acceptable": true,
#     "reasonCode": "OK",
#     "reasonMessage": "允许接单"
#   }
# }

# 如果不可接单，响应示例：
# {
#   "code": "SUCCESS",
#   "message": "操作成功",
#   "data": {
#     "acceptable": false,
#     "reasonCode": "ST-400-003",
#     "reasonMessage": "当前不在营业时间内"
#   }
# }
```

## 四、错误处理示例

### 版本冲突错误

```bash
# 第一次更新（成功，版本号变为 2）
curl -X PUT http://localhost:80/api/admin/store/base \
  -H "Content-Type: application/json" \
  -H "X-Tenant-Id: 1001" \
  -d '{
    "tenantId": 1001,
    "storeId": 123,
    "expectedConfigVersion": 1,
    "name": "第一次更新"
  }'

# 第二次更新使用旧版本号（失败）
curl -X PUT http://localhost:80/api/admin/store/base \
  -H "Content-Type: application/json" \
  -H "X-Tenant-Id: 1001" \
  -d '{
    "tenantId": 1001,
    "storeId": 123,
    "expectedConfigVersion": 1,
    "name": "第二次更新"
  }'

# 响应：
# {
#   "code": "ST-409-001",
#   "message": "门店配置版本冲突，请刷新后重试"
# }
```

### 门店不存在错误

```bash
curl -X GET "http://localhost:80/api/admin/store/detail?storeId=99999" \
  -H "X-Tenant-Id: 1001"

# 响应：
# {
#   "code": "ST-404-001",
#   "message": "门店不存在"
# }
```

## 五、完整流程示例

### 创建门店并配置完整信息

```bash
# 1. 创建门店
STORE_PUBLIC_ID=$(curl -s -X POST http://localhost:80/api/admin/store \
  -H "Content-Type: application/json" \
  -H "X-Tenant-Id: 1001" \
  -H "Idempotency-Key: create-store-$(date +%s)" \
  -d '{
    "name": "完整测试门店",
    "shortName": "完整测试",
    "industryType": "FOOD",
    "cityCode": "330100",
    "openForOrders": true
  }' | jq -r '.data.storePublicId')

echo "创建的门店 PublicId: $STORE_PUBLIC_ID"

# 2. 查询门店详情获取 storeId 和 configVersion
STORE_DETAIL=$(curl -s -X GET "http://localhost:80/api/admin/store/detail?storePublicId=$STORE_PUBLIC_ID" \
  -H "X-Tenant-Id: 1001")

STORE_ID=$(echo $STORE_DETAIL | jq -r '.data.storeId')
CONFIG_VERSION=$(echo $STORE_DETAIL | jq -r '.data.configVersion')

echo "门店 ID: $STORE_ID, 配置版本: $CONFIG_VERSION"

# 3. 更新能力配置
curl -X PUT http://localhost:80/api/admin/store/capabilities \
  -H "Content-Type: application/json" \
  -H "X-Tenant-Id: 1001" \
  -d "{
    \"tenantId\": 1001,
    \"storeId\": $STORE_ID,
    \"expectedConfigVersion\": $CONFIG_VERSION,
    \"capabilities\": [
      {\"capability\": \"DINE_IN\", \"enabled\": true},
      {\"capability\": \"TAKE_OUT\", \"enabled\": true}
    ]
  }"

# 4. 更新营业时间
curl -X PUT http://localhost:80/api/admin/store/opening-hours \
  -H "Content-Type: application/json" \
  -H "X-Tenant-Id: 1001" \
  -d "{
    \"tenantId\": 1001,
    \"storeId\": $STORE_ID,
    \"expectedConfigVersion\": $((CONFIG_VERSION + 1)),
    \"schedule\": {
      \"regularHours\": [
        {\"weekday\": 1, \"startTime\": \"09:00:00\", \"endTime\": \"18:00:00\", \"periodType\": \"REGULAR\"},
        {\"weekday\": 2, \"startTime\": \"09:00:00\", \"endTime\": \"18:00:00\", \"periodType\": \"REGULAR\"}
      ]
    }
  }"

# 5. 检查是否可接单
curl -X GET "http://localhost:80/api/open/store/check-acceptable?storeId=$STORE_ID&capability=DINE_IN" \
  -H "X-Tenant-Id: 1001"
```

## 六、注意事项

1. **租户隔离**：所有接口都需要通过 `X-Tenant-Id` 请求头或认证 Token 提供租户 ID
2. **乐观锁**：更新操作必须提供 `expectedConfigVersion`，避免并发覆盖
3. **幂等性**：创建门店接口支持 `Idempotency-Key` 请求头，确保重复请求不会创建多个门店
4. **PublicId 使用**：商户侧和开放接口推荐使用 `storePublicId`（格式：`sto_xxx`），避免暴露内部主键
5. **错误处理**：所有错误都通过统一的 `ApiResponse` 结构返回，包含错误码和错误消息

---

**最后更新**: 2025-12-16

