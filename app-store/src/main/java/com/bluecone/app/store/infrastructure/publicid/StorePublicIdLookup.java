package com.bluecone.app.store.infrastructure.publicid;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.bluecone.app.core.publicid.api.PublicIdLookup;
import com.bluecone.app.id.api.ResourceType;
import com.bluecone.app.store.dao.entity.BcStore;
import com.bluecone.app.store.dao.mapper.BcStoreMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 门店 Public ID 查找实现。
 * 
 * <p>查询策略：</p>
 * <ul>
 *   <li>单查：SELECT id FROM bc_store WHERE tenant_id=? AND public_id=? AND is_deleted=0 LIMIT 1</li>
 *   <li>批量：SELECT id, public_id FROM bc_store WHERE tenant_id=? AND public_id IN (?, ...) AND is_deleted=0</li>
 * </ul>
 * 
 * <p>索引依赖：uk_bc_store_tenant_public_id (tenant_id, public_id)</p>
 * 
 * <p>性能指标：</p>
 * <ul>
 *   <li>单查响应时间：< 10ms</li>
 *   <li>批量查询（100 条）：< 50ms</li>
 * </ul>
 */
@Component
public class StorePublicIdLookup implements PublicIdLookup {

    private static final Logger log = LoggerFactory.getLogger(StorePublicIdLookup.class);

    private final BcStoreMapper bcStoreMapper;

    public StorePublicIdLookup(BcStoreMapper bcStoreMapper) {
        this.bcStoreMapper = bcStoreMapper;
    }

    @Override
    public ResourceType type() {
        return ResourceType.STORE;
    }

    @Override
    public Optional<Object> findInternalId(long tenantId, String publicId) {
        long startTime = System.currentTimeMillis();
        try {
            // 查询：走 (tenant_id, public_id) 索引
            LambdaQueryWrapper<BcStore> wrapper = new LambdaQueryWrapper<>();
            wrapper.select(BcStore::getId)  // 只查询主键字段，减少网络传输
                    .eq(BcStore::getTenantId, tenantId)
                    .eq(BcStore::getPublicId, publicId)
                    .eq(BcStore::getIsDeleted, false)
                    .last("LIMIT 1");

            BcStore store = bcStoreMapper.selectOne(wrapper);
            
            long elapsed = System.currentTimeMillis() - startTime;
            if (elapsed > 10) {
                log.warn("门店 Public ID 查询耗时过长：tenantId={}, publicId={}, elapsed={}ms",
                        tenantId, maskPublicId(publicId), elapsed);
            }

            return Optional.ofNullable(store != null ? store.getId() : null);
        } catch (Exception ex) {
            log.error("门店 Public ID 查询失败：tenantId={}, publicId={}",
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
            LambdaQueryWrapper<BcStore> wrapper = new LambdaQueryWrapper<>();
            wrapper.select(BcStore::getId, BcStore::getPublicId)  // 只查询必要字段
                    .eq(BcStore::getTenantId, tenantId)
                    .in(BcStore::getPublicId, publicIds)
                    .eq(BcStore::getIsDeleted, false);

            List<BcStore> stores = bcStoreMapper.selectList(wrapper);

            // 构造 publicId -> id 映射
            Map<String, Object> resultMap = new HashMap<>(stores.size());
            for (BcStore store : stores) {
                resultMap.put(store.getPublicId(), store.getId());
            }

            long elapsed = System.currentTimeMillis() - startTime;
            if (elapsed > 50) {
                log.warn("批量门店 Public ID 查询耗时过长：tenantId={}, count={}, elapsed={}ms",
                        tenantId, publicIds.size(), elapsed);
            }

            log.debug("批量门店 Public ID 查询完成：tenantId={}, requested={}, found={}, elapsed={}ms",
                    tenantId, publicIds.size(), resultMap.size(), elapsed);

            return resultMap;
        } catch (Exception ex) {
            log.error("批量门店 Public ID 查询失败：tenantId={}, count={}",
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

