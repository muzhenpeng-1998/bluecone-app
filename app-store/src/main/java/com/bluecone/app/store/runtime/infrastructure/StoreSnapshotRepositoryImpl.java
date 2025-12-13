package com.bluecone.app.store.runtime.infrastructure;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.bluecone.app.id.core.Ulid128;
import com.bluecone.app.store.dao.entity.BcStore;
import com.bluecone.app.store.dao.mapper.BcStoreMapper;
import com.bluecone.app.store.runtime.api.StoreSnapshot;
import com.bluecone.app.store.runtime.spi.StoreSnapshotRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * 基于 bc_store 的门店运行态快照仓储实现。
 */
@Repository
@RequiredArgsConstructor
public class StoreSnapshotRepositoryImpl implements StoreSnapshotRepository {

    private final BcStoreMapper bcStoreMapper;

    @Override
    public Optional<StoreSnapshot> loadSnapshot(long tenantId, Ulid128 storeInternalId) {
        BcStore store = bcStoreMapper.selectOne(new LambdaQueryWrapper<BcStore>()
                .eq(BcStore::getTenantId, tenantId)
                .eq(BcStore::getInternalId, storeInternalId)
                .eq(BcStore::getIsDeleted, false));
        if (store == null) {
            return Optional.empty();
        }
        int status = "OPEN".equalsIgnoreCase(store.getStatus()) ? 1 : 0;
        boolean openForOrders = Boolean.TRUE.equals(store.getOpenForOrders());
        long configVersion = store.getConfigVersion() != null ? store.getConfigVersion() : 0L;

        Map<String, Object> ext = new HashMap<>();
        if (store.getCityCode() != null) {
            ext.put("cityCode", store.getCityCode());
        }
        if (store.getIndustryType() != null) {
            ext.put("industryType", store.getIndustryType().name());
        }
        if (store.getId() != null) {
            ext.put("storeId", store.getId());
        }

        StoreSnapshot snapshot = new StoreSnapshot(
                store.getTenantId(),
                store.getInternalId(),
                store.getPublicId(),
                store.getName(),
                status,
                openForOrders,
                null,
                configVersion,
                store.getUpdatedAt() != null ? store.getUpdatedAt().toInstant(ZoneOffset.UTC) : null,
                ext
        );
        return Optional.of(snapshot);
    }

    @Override
    public Optional<Long> loadConfigVersion(long tenantId, Ulid128 storeInternalId) {
        BcStore store = bcStoreMapper.selectOne(new LambdaQueryWrapper<BcStore>()
                .select(BcStore::getConfigVersion)
                .eq(BcStore::getTenantId, tenantId)
                .eq(BcStore::getInternalId, storeInternalId)
                .eq(BcStore::getIsDeleted, false));
        if (store == null || store.getConfigVersion() == null) {
            return Optional.empty();
        }
        return Optional.of(store.getConfigVersion());
    }
}
