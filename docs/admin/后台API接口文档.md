# 商户后台 API 接口文档

## 通用说明

### 请求头

所有后台接口都需要以下请求头：

```http
X-Tenant-Id: {租户ID}
Authorization: Bearer {JWT Token}
```

### 响应格式

成功响应：
```json
{
  "code": 200,
  "message": "success",
  "data": { ... }
}
```

错误响应：
```json
{
  "code": 400,
  "message": "错误描述",
  "data": null
}
```

### 错误码

| 错误码 | 说明 |
|-------|------|
| 400 | 请求参数错误 |
| 401 | 未登录或Token过期 |
| 403 | 权限不足 |
| 404 | 资源不存在 |
| 500 | 服务器内部错误 |

---

## 一、门店管理

### 1.1 查询门店详情

**接口**: `GET /api/admin/stores/{id}`

**权限**: `store:view`

**请求参数**:
- Path: `id` - 门店ID

**响应示例**:
```json
{
  "id": 1,
  "name": "星巴克北京国贸店",
  "shortName": "国贸店",
  "address": "北京市朝阳区建国门外大街1号",
  "provinceCode": "110000",
  "cityCode": "110100",
  "districtCode": "110105",
  "longitude": 116.458,
  "latitude": 39.908,
  "contactPhone": "010-12345678",
  "logoUrl": "https://...",
  "coverUrl": "https://...",
  "status": "OPEN",
  "openForOrders": true,
  "createdAt": "2025-01-01T00:00:00",
  "updatedAt": "2025-01-01T00:00:00"
}
```

### 1.2 更新门店信息

**接口**: `PUT /api/admin/stores/{id}`

**权限**: `store:edit`

**请求参数**:
```json
{
  "name": "星巴克北京国贸店",
  "shortName": "国贸店",
  "address": "北京市朝阳区建国门外大街1号",
  "provinceCode": "110000",
  "cityCode": "110100",
  "districtCode": "110105",
  "longitude": 116.458,
  "latitude": 39.908,
  "contactPhone": "010-12345678",
  "logoUrl": "https://...",
  "coverUrl": "https://..."
}
```

**响应**: 同查询门店详情

---

## 二、商品管理

### 2.1 分页查询商品列表

**接口**: `GET /api/admin/products`

**权限**: `product:view`

**请求参数**:
- Query: `page` - 页码（默认1）
- Query: `size` - 每页大小（默认20）
- Query: `name` - 商品名称（可选，模糊搜索）
- Query: `status` - 商品状态（可选，0=草稿，1=启用，-1=禁用）

**响应示例**:
```json
{
  "records": [
    {
      "id": 1,
      "productCode": "P001",
      "name": "美式咖啡",
      "subtitle": "经典美式",
      "mainImage": "https://...",
      "unit": "杯",
      "status": 1,
      "statusDesc": "启用",
      "createdAt": "2025-01-01T00:00:00",
      "updatedAt": "2025-01-01T00:00:00"
    }
  ],
  "total": 100,
  "size": 20,
  "current": 1,
  "pages": 5
}
```

### 2.2 查询商品详情

**接口**: `GET /api/admin/products/{id}`

**权限**: `product:view`

**响应示例**:
```json
{
  "id": 1,
  "productCode": "P001",
  "name": "美式咖啡",
  "subtitle": "经典美式",
  "productType": "FOOD_BEVERAGE",
  "description": "经典美式咖啡，口感醇厚",
  "mainImage": "https://...",
  "mediaGallery": ["https://...", "https://..."],
  "unit": "杯",
  "status": "ENABLED",
  "sortOrder": 1,
  "skus": [
    {
      "id": 1,
      "skuCode": "P001-S",
      "name": "小杯",
      "basePrice": 25.00,
      "marketPrice": 30.00,
      "costPrice": 10.00,
      "barcode": "6901234567890",
      "defaultSku": true,
      "status": "ENABLED",
      "sortOrder": 1
    }
  ]
}
```

### 2.3 创建商品

**接口**: `POST /api/admin/products`

**权限**: `product:create`

**请求参数**:
```json
{
  "productCode": "P001",
  "name": "美式咖啡",
  "subtitle": "经典美式",
  "productType": "FOOD_BEVERAGE",
  "description": "经典美式咖啡，口感醇厚",
  "mainImage": "https://...",
  "mediaGallery": ["https://...", "https://..."],
  "unit": "杯",
  "sortOrder": 1,
  "skus": [
    {
      "skuCode": "P001-S",
      "name": "小杯",
      "basePrice": 25.00,
      "marketPrice": 30.00,
      "costPrice": 10.00,
      "barcode": "6901234567890",
      "defaultSku": true,
      "sortOrder": 1
    }
  ]
}
```

**响应**:
```json
{
  "productId": 1
}
```

### 2.4 更新商品信息

**接口**: `PUT /api/admin/products/{id}`

**权限**: `product:edit`

**请求参数**:
```json
{
  "name": "美式咖啡",
  "subtitle": "经典美式",
  "description": "经典美式咖啡，口感醇厚",
  "mainImage": "https://...",
  "mediaGallery": ["https://...", "https://..."],
  "unit": "杯",
  "sortOrder": 1
}
```

### 2.5 商品上线

**接口**: `POST /api/admin/products/{id}/online`

**权限**: `product:online`

### 2.6 商品下线

**接口**: `POST /api/admin/products/{id}/offline`

**权限**: `product:online`

---

## 三、订单管理

### 3.1 分页查询订单列表

**接口**: `GET /api/admin/orders`

**权限**: `order:view`

**请求参数**:
- Query: `page` - 页码（默认1）
- Query: `size` - 每页大小（默认20）
- Query: `status` - 订单状态（可选）
- Query: `storeId` - 门店ID（可选）
- Query: `startTime` - 开始时间（可选）
- Query: `endTime` - 结束时间（可选）

**响应示例**:
```json
{
  "records": [
    {
      "id": 1,
      "orderNo": "ord_xxx",
      "storeId": 1,
      "userId": 100,
      "bizType": "DINE_IN",
      "orderSource": "MINI_PROGRAM",
      "totalAmount": 50.00,
      "discountAmount": 5.00,
      "payableAmount": 45.00,
      "status": "PAID",
      "payStatus": "PAID",
      "createdAt": "2025-01-01T12:00:00",
      "updatedAt": "2025-01-01T12:05:00"
    }
  ],
  "total": 100,
  "size": 20,
  "current": 1,
  "pages": 5
}
```

### 3.2 查询订单详情

**接口**: `GET /api/admin/orders/{id}`

**权限**: `order:view`

**响应示例**:
```json
{
  "id": 1,
  "orderNo": "ord_xxx",
  "clientOrderNo": "client_xxx",
  "storeId": 1,
  "userId": 100,
  "bizType": "DINE_IN",
  "orderSource": "MINI_PROGRAM",
  "channel": "WECHAT",
  "totalAmount": 50.00,
  "discountAmount": 5.00,
  "payableAmount": 45.00,
  "currency": "CNY",
  "status": "PAID",
  "payStatus": "PAID",
  "orderRemark": "少糖",
  "forensicsUrl": "/api/admin/orders/1/forensics",
  "items": [
    {
      "id": 1,
      "productId": 1,
      "skuId": 1,
      "productName": "美式咖啡",
      "skuName": "小杯",
      "productCode": "P001",
      "quantity": 2,
      "unitPrice": 25.00,
      "discountAmount": 5.00,
      "payableAmount": 45.00,
      "attrsJson": "{}",
      "remark": "少糖"
    }
  ],
  "createdAt": "2025-01-01T12:00:00",
  "updatedAt": "2025-01-01T12:05:00"
}
```

### 3.3 查询订单诊断信息

**接口**: `GET /api/admin/orders/{id}/forensics`

**权限**: `order:view`

**说明**: 返回订单全链路诊断数据，包含计价快照、优惠券、钱包、Outbox事件、诊断结论等。

---

## 四、优惠券管理

### 4.1 查询模板列表

**接口**: `GET /api/admin/promo/templates`

**权限**: `coupon:view`

**请求参数**:
- Query: `status` - 状态筛选（可选，ONLINE=仅上线）

**响应示例**:
```json
[
  {
    "id": 1,
    "templateCode": "COUPON001",
    "templateName": "新用户券",
    "couponType": "DISCOUNT_AMOUNT",
    "discountAmount": 10.00,
    "minOrderAmount": 50.00,
    "validDays": 30,
    "totalQuantity": 1000,
    "issuedCount": 100,
    "status": "ONLINE",
    "createdAt": "2025-01-01T00:00:00"
  }
]
```

### 4.2 创建模板

**接口**: `POST /api/admin/promo/templates`

**权限**: `coupon:create`

**请求参数**:
```json
{
  "templateCode": "COUPON001",
  "templateName": "新用户券",
  "couponType": "DISCOUNT_AMOUNT",
  "discountAmount": 10.00,
  "discountRate": null,
  "minOrderAmount": 50.00,
  "maxDiscountAmount": null,
  "applicableScope": "ALL",
  "applicableScopeIds": null,
  "validDays": 30,
  "validStartTime": null,
  "validEndTime": null,
  "totalQuantity": 1000,
  "perUserLimit": 1,
  "description": "新用户专享优惠券",
  "termsOfUse": "仅限首次下单使用"
}
```

### 4.3 模板上线

**接口**: `POST /api/admin/promo/templates/{id}/publish`

**权限**: `coupon:online`

### 4.4 模板下线

**接口**: `POST /api/admin/promo/templates/{id}/offline`

**权限**: `coupon:online`

**请求参数**:
- Query: `reason` - 下线原因（可选）

### 4.5 手动发券

**接口**: `POST /api/admin/promo/grants`

**权限**: `coupon:grant`

**请求参数**:
```json
{
  "templateId": 1,
  "userIds": [100, 101, 102],
  "batchNo": "BATCH001",
  "operatorId": 1,
  "operatorName": "管理员",
  "grantReason": "活动发放",
  "idempotencyKey": "unique_key"
}
```

**响应**:
```json
{
  "total": 3,
  "successCount": 3,
  "failedCount": 0,
  "results": [
    {
      "userId": 100,
      "success": true,
      "couponId": 1001,
      "errorMessage": null
    }
  ]
}
```

---

## 五、钱包管理

### 5.1 查询用户余额

**接口**: `GET /api/admin/wallet/balances`

**权限**: `wallet:view`

**请求参数**:
- Query: `userId` - 用户ID（可选）
- Query: `page` - 页码（默认1）
- Query: `size` - 每页大小（默认20）

**响应示例**:
```json
{
  "records": [
    {
      "userId": 100,
      "availableBalance": 100.00,
      "frozenBalance": 0.00,
      "totalRecharged": 200.00,
      "totalConsumed": 100.00,
      "currency": "CNY",
      "status": "ACTIVE",
      "createdAt": "2025-01-01T00:00:00",
      "updatedAt": "2025-01-01T12:00:00"
    }
  ],
  "total": 100,
  "size": 20,
  "current": 1
}
```

### 5.2 查询充值记录

**接口**: `GET /api/admin/wallet/recharges`

**权限**: `wallet:view`

**请求参数**:
- Query: `userId` - 用户ID（可选）
- Query: `status` - 充值状态（可选）
- Query: `page` - 页码（默认1）
- Query: `size` - 每页大小（默认20）

**响应示例**:
```json
{
  "records": [
    {
      "id": 1,
      "rechargeId": "wrc_xxx",
      "userId": 100,
      "rechargeAmount": 100.00,
      "bonusAmount": 10.00,
      "totalAmount": 110.00,
      "currency": "CNY",
      "status": "SUCCESS",
      "payChannel": "WECHAT",
      "payNo": "wx_xxx",
      "rechargeRequestedAt": "2025-01-01T12:00:00",
      "rechargeCompletedAt": "2025-01-01T12:01:00",
      "createdAt": "2025-01-01T12:00:00"
    }
  ],
  "total": 50,
  "size": 20,
  "current": 1
}
```

---

## 六、概览仪表盘

### 6.1 查询经营数据概览

**接口**: `GET /api/admin/dashboard/summary`

**权限**: `dashboard:view`

**请求参数**:
- Query: `date` - 日期（可选，默认今天，格式：YYYY-MM-DD）

**响应示例**:
```json
{
  "date": "2025-01-01",
  "orderStats": {
    "totalCount": 100,
    "paidCount": 95,
    "canceledCount": 5,
    "gmv": 4500.00,
    "avgOrderAmount": 47.37
  },
  "couponStats": {
    "grantedCount": 50,
    "usedCount": 30,
    "totalDiscountAmount": 300.00
  },
  "walletStats": {
    "rechargeCount": 20,
    "rechargeAmount": 2000.00,
    "consumeCount": 50,
    "consumeAmount": 1500.00
  }
}
```

---

## 附录：权限清单

| 权限代码 | 权限名称 | 说明 |
|---------|---------|------|
| store:view | 查看门店 | 查看门店基本信息 |
| store:edit | 编辑门店 | 修改门店信息、营业时间 |
| product:view | 查看商品 | 查看商品列表和详情 |
| product:create | 创建商品 | 创建新商品 |
| product:edit | 编辑商品 | 修改商品信息 |
| product:online | 商品上下线 | 商品上线和下线操作 |
| order:view | 查看订单 | 查看订单列表和详情 |
| order:manage | 管理订单 | 接单、取消订单等操作 |
| coupon:view | 查看优惠券 | 查看优惠券模板和发放记录 |
| coupon:create | 创建优惠券 | 创建优惠券模板 |
| coupon:edit | 编辑优惠券 | 修改优惠券模板 |
| coupon:online | 优惠券上下线 | 优惠券模板上线和下线 |
| coupon:grant | 发放优惠券 | 手动发放优惠券给用户 |
| wallet:view | 查看钱包 | 查看用户余额和充值记录 |
| dashboard:view | 查看仪表盘 | 查看经营数据概览 |

---

**文档版本**: v1.0  
**最后更新**: 2025-12-19
