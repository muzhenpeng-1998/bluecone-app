package com.bluecone.app.inventory.runtime.infrastructure;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.bluecone.app.core.contextkit.SnapshotLoadKey;
import com.bluecone.app.inventory.infra.mapper.InvPolicyMapper;
import com.bluecone.app.inventory.infra.po.InvPolicyDO;
import com.bluecone.app.inventory.runtime.api.InventoryPolicySnapshot;
import com.bluecone.app.inventory.runtime.api.InventoryScope;
import com.bluecone.app.inventory.runtime.spi.InventoryPolicyRepository;
import java.time.ZoneOffset;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

/**
 * 基于 bc_inv_policy 的库存策略快照仓储实现（门店维度）。
 *
 * <p>当前实现以门店维度聚合一条代表性策略，后续可根据业务需要扩展。</p>
 */
@Repository
@RequiredArgsConstructor
public class InventoryPolicySnapshotRepositoryImpl implements InventoryPolicyRepository {

    private final InvPolicyMapper invPolicyMapper;

    @Override
    public Optional<InventoryPolicySnapshot> loadFull(SnapshotLoadKey key) {
        InventoryScope scope = (InventoryScope) key.scopeId();
        Long storeId = scope.storeNumericId();
        if (storeId == null) {
            return Optional.empty();
        }

        InvPolicyDO policy = invPolicyMapper.selectOne(new LambdaQueryWrapper<InvPolicyDO>()
                .eq(InvPolicyDO::getTenantId, key.tenantId())
                .eq(InvPolicyDO::getStoreId, storeId)
                .eq(InvPolicyDO::getStatus, 1)
                .last("LIMIT 1"));
        if (policy == null) {
            return Optional.empty();
        }

        boolean enableInventory = policy.getStatus() != null && policy.getStatus() == 1;
        String deductMode = policy.getDeductMode();
        int safetyStockMode = 0;

        Instant updatedAt = policy.getUpdatedAt() != null
                ? policy.getUpdatedAt().toInstant(ZoneOffset.UTC)
                : null;
        long configVersion = deriveVersion(policy);

        Map<String, Object> ext = new HashMap<>();
        ext.put("oversellAllowed", policy.getOversellAllowed());
        ext.put("oversellLimit", policy.getOversellLimit());
        ext.put("maxDailySold", policy.getMaxDailySold());
        ext.put("storeNumericId", storeId);

        InventoryPolicySnapshot snapshot = new InventoryPolicySnapshot(
                key.tenantId(),
                scope.storeInternalId(),
                scope.storePublicId(),
                configVersion,
                enableInventory,
                deductMode,
                safetyStockMode,
                updatedAt,
                ext
        );
        return Optional.of(snapshot);
    }

    @Override
    public Optional<Long> loadVersion(SnapshotLoadKey key) {
        InventoryScope scope = (InventoryScope) key.scopeId();
        Long storeId = scope.storeNumericId();
        if (storeId == null) {
            return Optional.empty();
        }

        InvPolicyDO policy = invPolicyMapper.selectOne(new LambdaQueryWrapper<InvPolicyDO>()
                .select(InvPolicyDO::getUpdatedAt)
                .eq(InvPolicyDO::getTenantId, key.tenantId())
                .eq(InvPolicyDO::getStoreId, storeId)
                .eq(InvPolicyDO::getStatus, 1)
                .last("LIMIT 1"));
        if (policy == null || policy.getUpdatedAt() == null) {
            return Optional.empty();
        }
        return Optional.of(deriveVersion(policy));
    }

    private long deriveVersion(InvPolicyDO policy) {
        if (policy.getUpdatedAt() == null) {
            return 0L;
        }
        return policy.getUpdatedAt()
                .toInstant(ZoneOffset.UTC)
                .toEpochMilli();
    }
}
