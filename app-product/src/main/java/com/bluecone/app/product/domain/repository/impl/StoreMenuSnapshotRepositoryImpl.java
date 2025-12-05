package com.bluecone.app.product.domain.repository.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.bluecone.app.product.dao.entity.BcStoreMenuSnapshot;
import com.bluecone.app.product.dao.mapper.BcStoreMenuSnapshotMapper;
import com.bluecone.app.product.domain.repository.StoreMenuSnapshotRepository;
import java.time.LocalDateTime;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

/**
 * 门店菜单快照仓储实现，负责快照的查询与 upsert。
 * <p>高并发读写分离：快照表作为冗余存储，读路径直接命中该表。</p>
 */
@Repository
@RequiredArgsConstructor
public class StoreMenuSnapshotRepositoryImpl implements StoreMenuSnapshotRepository {

    private final BcStoreMenuSnapshotMapper storeMenuSnapshotMapper;

    @Override
    public Optional<BcStoreMenuSnapshot> findByTenantAndStoreAndChannelAndScene(Long tenantId, Long storeId, String channel, String orderScene) {
        BcStoreMenuSnapshot entity = storeMenuSnapshotMapper.selectOne(new LambdaQueryWrapper<BcStoreMenuSnapshot>()
                .eq(BcStoreMenuSnapshot::getTenantId, tenantId)
                .eq(BcStoreMenuSnapshot::getStoreId, storeId)
                .eq(BcStoreMenuSnapshot::getChannel, channel)
                .eq(BcStoreMenuSnapshot::getOrderScene, orderScene)
                .eq(BcStoreMenuSnapshot::getStatus, 1));
        return Optional.ofNullable(entity);
    }

    @Override
    public void saveOrUpdateSnapshot(BcStoreMenuSnapshot snapshot) {
        if (snapshot == null) {
            return;
        }
        BcStoreMenuSnapshot existing = storeMenuSnapshotMapper.selectOne(new LambdaQueryWrapper<BcStoreMenuSnapshot>()
                .eq(BcStoreMenuSnapshot::getTenantId, snapshot.getTenantId())
                .eq(BcStoreMenuSnapshot::getStoreId, snapshot.getStoreId())
                .eq(BcStoreMenuSnapshot::getChannel, snapshot.getChannel())
                .eq(BcStoreMenuSnapshot::getOrderScene, snapshot.getOrderScene()));
        LocalDateTime now = LocalDateTime.now();
        if (existing == null) {
            snapshot.setVersion(snapshot.getVersion() != null ? snapshot.getVersion() : 1L);
            snapshot.setStatus(snapshot.getStatus() == null ? 1 : snapshot.getStatus());
            snapshot.setCreatedAt(snapshot.getCreatedAt() == null ? now : snapshot.getCreatedAt());
            snapshot.setUpdatedAt(now);
            storeMenuSnapshotMapper.insert(snapshot);
        } else {
            existing.setMenuJson(snapshot.getMenuJson());
            existing.setGeneratedAt(snapshot.getGeneratedAt() != null ? snapshot.getGeneratedAt() : now);
            existing.setStatus(snapshot.getStatus() == null ? existing.getStatus() : snapshot.getStatus());
            existing.setVersion(existing.getVersion() != null ? existing.getVersion() + 1 : 1L);
            existing.setUpdatedAt(now);
            storeMenuSnapshotMapper.updateById(existing);
        }
    }
}
