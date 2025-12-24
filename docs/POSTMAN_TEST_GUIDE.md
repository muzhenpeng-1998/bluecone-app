# 菜单快照系统 Postman 测试指南

## 概述

本文档提供了测试"门店级 Epoch + 分类内商品排序 + 门店上架时间窗"功能的完整 Postman 测试链路。

## 前置条件

1. 系统已启动并可访问
2. 已获取有效的 Admin Token（用于后台管理接口）
3. 已创建租户（tenantId）
4. 已创建门店（storeId）

## 测试链路

### 步骤 1：创建商品分类

**接口：** `POST /api/admin/product-categories`

**权限：** `product-category:create`

**请求示例：**

```json
{
  "title": "热销推荐",
  "parentId": 0,
  "imageUrl": "https://example.com/category-icon.png",
  "sortOrder": 100,
  "enabled": true,
  "displayStartAt": null,
  "displayEndAt": null
}
```

**响应示例：**

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "categoryId": 10001
  }
}
```

**记录：** `categoryId = 10001`

---

### 步骤 2：创建商品聚合（绑定分类）

**接口：** `POST /api/admin/products`

**权限：** `product:create`

**请求示例：**

```json
{
  "name": "美式咖啡",
  "subtitle": "经典美式",
  "mainImage": "https://example.com/coffee.png",
  "sortOrder": 100,
  "enabled": true,
  "displayStartAt": null,
  "displayEndAt": null,
  "categoryIds": [10001],
  "skus": [
    {
      "name": "中杯",
      "basePrice": 15.00,
      "marketPrice": 18.00,
      "isDefault": true,
      "sortOrder": 100
    },
    {
      "name": "大杯",
      "basePrice": 18.00,
      "marketPrice": 22.00,
      "isDefault": false,
      "sortOrder": 90
    }
  ]
}
```

**响应示例：**

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "productId": 20001
  }
}
```

**记录：** `productId = 20001`

**重复此步骤创建 3-5 个商品，用于测试排序。**

---

### 步骤 3：门店上架商品（设置门店排序和展示时间窗）

**接口：** `POST /api/admin/stores/{storeId}/products/{productId}/config`

**权限：** `store-product:config`

**请求示例（立即上架）：**

```json
{
  "visible": true,
  "channel": "ALL",
  "sortOrder": 100,
  "displayStartAt": null,
  "displayEndAt": null
}
```

**请求示例（定时上架 - 未来才开始展示）：**

```json
{
  "visible": true,
  "channel": "ALL",
  "sortOrder": 100,
  "displayStartAt": "2025-12-25T00:00:00",
  "displayEndAt": "2025-12-31T23:59:59"
}
```

**响应示例：**

```json
{
  "code": 0,
  "message": "success"
}
```

**验收点：**
- 立即上架的商品应该在菜单中可见
- 定时上架的商品（未到时间）应该在菜单中不可见
- 到点后自动出现（通过快照重建/缓存回源都能正确）

---

### 步骤 4：触发菜单快照重建

**方式 1：自动触发（推荐）**

在步骤 3 中上架商品后，系统会自动触发该门店的菜单快照重建（afterCommit）。

**方式 2：手动触发（如果需要）**

可以通过修改商品信息、分类信息等操作触发重建。

**验收点：**
- 只有该门店的菜单快照会重建
- 同租户其他门店的缓存不受影响（门店级 Epoch）

---

### 步骤 5：调用 Open API 拉取菜单

**接口：** `GET /api/open/stores/{storeId}/menu`

**权限：** 无需权限（Open API）

**请求参数：**
- `storeId`: 门店ID
- `channel`: 渠道（可选，默认 ALL）
- `orderScene`: 订单场景（可选，默认 DEFAULT）

**响应示例：**

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "tenantId": 1,
    "storeId": 1001,
    "channel": "ALL",
    "orderScene": "DEFAULT",
    "categories": [
      {
        "categoryId": 10001,
        "name": "热销推荐",
        "iconUrl": "https://example.com/category-icon.png",
        "sortOrder": 100,
        "products": [
          {
            "productId": 20001,
            "name": "美式咖啡",
            "subtitle": "经典美式",
            "mainImage": "https://example.com/coffee.png",
            "storeSortOrder": 100,
            "productSortOrder": 100,
            "skus": [
              {
                "skuId": 30001,
                "name": "中杯",
                "price": 15.00,
                "originPrice": 18.00,
                "defaultSku": true
              },
              {
                "skuId": 30002,
                "name": "大杯",
                "price": 18.00,
                "originPrice": 22.00,
                "defaultSku": false
              }
            ],
            "optionGroups": []
          }
        ]
      }
    ]
  }
}
```

**验收点：**
- 菜单中只包含"立即上架"的商品
- 定时上架的商品（未到时间）不在菜单中
- 商品按 `storeSortOrder` 排序（门店排序优先级最高）

---

### 步骤 6：调整分类内商品排序

**接口：** `POST /api/admin/product-categories/{categoryId}/products/reorder`

**权限：** `product-category:edit`

**请求示例：**

```json
{
  "items": [
    {
      "productId": 20001,
      "sortOrder": 100
    },
    {
      "productId": 20002,
      "sortOrder": 90
    },
    {
      "productId": 20003,
      "sortOrder": 80
    }
  ]
}
```

**响应示例：**

```json
{
  "code": 0,
  "message": "success"
}
```

**验收点：**
- 系统会自动触发"上架了这些商品的门店"的菜单快照重建
- 只影响受影响的门店，不会全量重建

---

### 步骤 7：再次拉取菜单，验证排序变化

**接口：** `GET /api/open/stores/{storeId}/menu`

**验收点：**
- 商品排序已更新
- 排序规则：`storeSortOrder` > `categoryRelSortOrder` > `productSortOrder` > `productId`
- 只有该门店的菜单受影响，其他门店不受影响

---

### 步骤 8：验证门店级 Epoch（可选）

**目标：** 验证修改一个门店的菜单不会影响同租户其他门店的缓存。

**操作步骤：**

1. 创建两个门店（storeId1, storeId2）
2. 两个门店都上架同一个商品
3. 修改 storeId1 的商品配置（例如修改 sortOrder）
4. 观察日志：只有 storeId1 的菜单快照会重建
5. 拉取 storeId2 的菜单：缓存仍然有效（不会触发回源）

**验收点：**
- 日志中只有 storeId1 的 epoch 被 bump
- storeId2 的缓存命中率不受影响

---

### 步骤 9：验证展示时间窗（可选）

**目标：** 验证门店上架配置的 `displayStartAt` 和 `displayEndAt` 生效。

**操作步骤：**

1. 创建一个商品，设置 `displayStartAt = 未来时间`
2. 门店上架该商品
3. 拉取菜单：该商品不在菜单中
4. 修改系统时间或等待到点
5. 再次拉取菜单：该商品出现在菜单中

**验收点：**
- 未到展示时间的商品不在菜单中
- 到点后自动出现（通过快照重建/缓存回源都能正确）

---

## 完整测试链路总结

```
1. 创建分类 → categoryId
2. 创建商品（绑分类）→ productId
3. 门店上架（visible=true，设置 sort）→ 触发重建
4. 拉菜单 → 验证商品可见、排序正确
5. 调分类内商品 reorder → 触发重建
6. 再拉菜单 → 验证排序变化且只影响该门店
```

---

## 常见问题

### Q1: 菜单中看不到商品？

**可能原因：**
1. 商品未上架到门店（`visible=false`）
2. 商品的 `displayStartAt` 未到时间
3. 商品的 `status` 为禁用（`status=0`）
4. 分类的 `status` 为禁用（`status=0`）

### Q2: 排序不生效？

**可能原因：**
1. 排序规则：`storeSortOrder` > `categoryRelSortOrder` > `productSortOrder` > `productId`
2. 检查是否所有商品都设置了相同的 `storeSortOrder`（如果是，则按 `categoryRelSortOrder` 排序）

### Q3: 修改商品后其他门店的缓存也失效了？

**可能原因：**
1. 检查日志，确认是否使用了门店级 Epoch（namespace 应该是 `store:menu:snap:{storeId}`）
2. 检查是否误用了 `invalidateTenantMenus`（该方法已废弃）

---

## 日志关键字

**门店级 Epoch：**
```
门店菜单快照缓存已失效（门店级 Epoch Bump）: tenantId=1, storeId=1001, namespace=store:menu:snap:1001, newEpoch=2, reason=...
```

**菜单快照重建：**
```
开始重建门店菜单快照（门店级 Epoch）: tenantId=1, storeId=1001, reason=...
门店菜单快照重建完成（门店级 Epoch）: tenantId=1, storeId=1001, reason=...
```

**展示时间窗过滤：**
```
门店无可售商品配置（已过滤展示时间窗）: tenantId=1, storeId=1001, channel=ALL, now=2025-12-22T10:00:00
```

---

## 性能验证

**缓存命中率：**
- 修改一个门店的菜单后，其他门店的缓存命中率应保持不变
- 观察 Redis 监控：只有受影响门店的缓存 key 被更新

**重建性能：**
- 单店重建时间应在 100ms 以内（取决于商品数量）
- 批量重建时应并发执行（best-effort）

---

## 联系方式

如有问题，请联系 BlueCone Team。

