package com.bluecone.app.product.infrastructure.publicid;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.bluecone.app.core.publicid.api.PublicIdLookup;
import com.bluecone.app.id.api.ResourceType;
import com.bluecone.app.product.dao.entity.BcProductSku;
import com.bluecone.app.product.dao.mapper.BcProductSkuMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * SKU Public ID 查找实现。
 * 
 * <p>查询策略：</p>
 * <ul>
 *   <li>单查：SELECT id FROM bc_product_sku WHERE tenant_id=? AND public_id=? LIMIT 1</li>
 *   <li>批量：SELECT id, public_id FROM bc_product_sku WHERE tenant_id=? AND public_id IN (?, ...)</li>
 * </ul>
 * 
 * <p>索引依赖：uk_bc_product_sku_tenant_public_id (tenant_id, public_id)</p>
 * 
 * <p>使用场景：</p>
 * <ul>
 *   <li>订单详情：批量查询订单项关联的 SKU</li>
 *   <li>购物车：批量查询购物车中的 SKU</li>
 *   <li>商品详情：查询商品的默认 SKU</li>
 * </ul>
 */
@Component
public class SkuPublicIdLookup implements PublicIdLookup {

    private static final Logger log = LoggerFactory.getLogger(SkuPublicIdLookup.class);

    private final BcProductSkuMapper bcProductSkuMapper;

    public SkuPublicIdLookup(BcProductSkuMapper bcProductSkuMapper) {
        this.bcProductSkuMapper = bcProductSkuMapper;
    }

    @Override
    public ResourceType type() {
        return ResourceType.SKU;
    }

    @Override
    public Optional<Object> findInternalId(long tenantId, String publicId) {
        long startTime = System.currentTimeMillis();
        try {
            // 查询：走 (tenant_id, public_id) 索引
            LambdaQueryWrapper<BcProductSku> wrapper = new LambdaQueryWrapper<>();
            wrapper.select(BcProductSku::getId)  // 只查询主键字段
                    .eq(BcProductSku::getTenantId, tenantId)
                    .eq(BcProductSku::getPublicId, publicId)
                    .last("LIMIT 1");

            BcProductSku sku = bcProductSkuMapper.selectOne(wrapper);

            long elapsed = System.currentTimeMillis() - startTime;
            if (elapsed > 10) {
                log.warn("SKU Public ID 查询耗时过长：tenantId={}, publicId={}, elapsed={}ms",
                        tenantId, maskPublicId(publicId), elapsed);
            }

            return Optional.ofNullable(sku != null ? sku.getId() : null);
        } catch (Exception ex) {
            log.error("SKU Public ID 查询失败：tenantId={}, publicId={}",
                    tenantId, maskPublicId(publicId), ex);
            return Optional.empty();
        }
    }

    @Override
    public Map<String, Object> findInternalIds(long tenantId, List<String> publicIds) {
        if (publicIds == null || publicIds.isEmpty()) {
            return Map.of();
        }

        long startTime = System.currentTimeMillis();
        try {
            // 批量查询：走 (tenant_id, public_id) 索引 + IN 条件
            LambdaQueryWrapper<BcProductSku> wrapper = new LambdaQueryWrapper<>();
            wrapper.select(BcProductSku::getId, BcProductSku::getPublicId)  // 只查询必要字段
                    .eq(BcProductSku::getTenantId, tenantId)
                    .in(BcProductSku::getPublicId, publicIds);

            List<BcProductSku> skus = bcProductSkuMapper.selectList(wrapper);

            // 构造 publicId -> id 映射
            Map<String, Object> resultMap = new HashMap<>(skus.size());
            for (BcProductSku sku : skus) {
                resultMap.put(sku.getPublicId(), sku.getId());
            }

            long elapsed = System.currentTimeMillis() - startTime;
            if (elapsed > 50) {
                log.warn("批量 SKU Public ID 查询耗时过长：tenantId={}, count={}, elapsed={}ms",
                        tenantId, publicIds.size(), elapsed);
            }

            log.debug("批量 SKU Public ID 查询完成：tenantId={}, requested={}, found={}, elapsed={}ms",
                    tenantId, publicIds.size(), resultMap.size(), elapsed);

            return resultMap;
        } catch (Exception ex) {
            log.error("批量 SKU Public ID 查询失败：tenantId={}, count={}",
                    tenantId, publicIds.size(), ex);
            return Map.of();
        }
    }

    /**
     * 脱敏 publicId，避免日志泄露完整 ID。
     */
    private String maskPublicId(String publicId) {
        if (publicId == null || publicId.length() < 10) {
            return publicId;
        }
        int separatorIndex = publicId.indexOf('_');
        if (separatorIndex < 0) {
            return publicId.substring(0, Math.min(6, publicId.length())) + "***";
        }
        String prefix = publicId.substring(0, separatorIndex + 1);
        String payload = publicId.substring(separatorIndex + 1);
        if (payload.length() <= 6) {
            return publicId;
        }
        return prefix + payload.substring(0, 6) + "***";
    }
}

