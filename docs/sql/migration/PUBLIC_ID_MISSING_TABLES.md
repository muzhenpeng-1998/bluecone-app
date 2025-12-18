# Public ID 缺失表清单

本文档记录在 Public ID Governance Kit 落地过程中发现的缺少 `public_id` 字段的业务表。

## 已补齐的表

| 表名 | 资源类型 | 迁移脚本 | 状态 |
|------|---------|---------|------|
| bc_store | STORE | V20251213__add_internal_public_id__bc_store.sql | ✅ 已存在 |
| bc_order | ORDER | V20251213__add_internal_public_id__bc_order.sql | ✅ 已存在 |
| bc_user | USER | V20251213__add_internal_public_id__bc_user.sql | ✅ 已存在 |
| bc_payment_order | PAYMENT | V20251213__add_internal_public_id__bc_payment_order.sql | ✅ 已存在 |
| bc_product | PRODUCT | V20251215__add_public_id__bc_product.sql | ✅ 新增 |
| bc_product_sku | SKU | V20251215__add_public_id__bc_product_sku.sql | ✅ 新增 |

## 索引要求

所有表必须包含以下索引：

1. **唯一索引 (tenant_id, public_id)**
   - 用途：支持 PublicIdLookup 快速查询
   - 查询模式：`WHERE tenant_id=? AND public_id=?`
   - 性能要求：< 10ms

2. **唯一索引 (internal_id)**
   - 用途：支持 internal_id 反查
   - 查询模式：`WHERE internal_id=?`

## 数据迁移注意事项

### 新增字段后的数据填充

对于已有数据的表，需要执行数据填充脚本：

```sql
-- 示例：为 bc_product 填充 internal_id 和 public_id
UPDATE bc_product
SET 
    internal_id = UNHEX(REPLACE(UUID(), '-', '')),  -- 临时使用 UUID，生产环境需要使用 ULID
    public_id = CONCAT('prd_', UPPER(SUBSTRING(REPLACE(UUID(), '-', ''), 1, 26)))
WHERE internal_id IS NULL;
```

**注意**：生产环境应使用 IdService 生成真实的 ULID 和 PublicId，避免使用 UUID。

### 应用层代码同步

1. 更新实体类，添加 `internalId` 和 `publicId` 字段
2. 创建/更新时生成 ID：
   ```java
   Ulid128 internalId = idService.nextUlid();
   String publicId = idService.nextPublicId(ResourceType.PRODUCT);
   ```
3. 注册 PublicId 映射（可选，如使用映射表）：
   ```java
   publicIdRegistrar.register(tenantId, ResourceType.PRODUCT, publicId, internalId);
   ```

## 后续待补齐的表

以下表暂未评估是否需要 public_id，按需补充：

- bc_tenant
- bc_inventory_policy
- bc_menu_category
- bc_promotion
- bc_coupon

## 验证清单

- [ ] 执行迁移脚本
- [ ] 验证索引创建成功
- [ ] 填充已有数据的 public_id
- [ ] 更新实体类
- [ ] 实现 PublicIdLookup
- [ ] 编写单元测试
- [ ] 更新 API 文档

