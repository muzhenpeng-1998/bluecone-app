package com.bluecone.app.product.runtime.repository;

import com.bluecone.app.core.contextkit.SnapshotLoadKey;
import com.bluecone.app.core.contextkit.SnapshotRepository;
import com.bluecone.app.product.dao.entity.BcStoreMenuSnapshot;
import com.bluecone.app.product.dao.mapper.BcStoreMenuSnapshotMapper;
import com.bluecone.app.product.runtime.model.StoreMenuSnapshotData;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * 门店菜单快照缓存仓储（Prompt 08）。
 * <p>
 * 实现 {@link SnapshotRepository} 接口，用于 {@link com.bluecone.app.core.contextkit.SnapshotProvider} 的数据加载。
 * <p>
 * 职责：
 * <ul>
 *   <li>loadFull：从 {@code bc_store_menu_snapshot} 表读取完整的 {@code menu_json}</li>
 *   <li>loadVersion：从 {@code bc_store_menu_snapshot} 表读取 {@code version} 字段</li>
 * </ul>
 *
 * @author System
 * @since 2025-12-21
 */
@Repository
@RequiredArgsConstructor
@Slf4j
public class StoreMenuSnapshotCacheRepository implements SnapshotRepository<StoreMenuSnapshotData> {

    private final BcStoreMenuSnapshotMapper storeMenuSnapshotMapper;

    /**
     * 加载完整的菜单快照数据。
     * <p>
     * scopeId 格式：{storeId}:{channel}:{orderScene}
     *
     * @param loadKey 加载键
     * @return 菜单快照数据（包含 menu_json 和 version）
     */
    @Override
    public Optional<StoreMenuSnapshotData> loadFull(SnapshotLoadKey loadKey) {
        StoreMenuKey key = parseKey(loadKey);
        
        BcStoreMenuSnapshot snapshot = storeMenuSnapshotMapper.selectOne(new LambdaQueryWrapper<BcStoreMenuSnapshot>()
                .eq(BcStoreMenuSnapshot::getTenantId, loadKey.tenantId())
                .eq(BcStoreMenuSnapshot::getStoreId, key.storeId)
                .eq(BcStoreMenuSnapshot::getChannel, key.channel)
                .eq(BcStoreMenuSnapshot::getOrderScene, key.orderScene)
                .eq(BcStoreMenuSnapshot::getStatus, 1));

        if (snapshot == null) {
            log.debug("门店菜单快照不存在: tenantId={}, storeId={}, channel={}, orderScene={}", 
                    loadKey.tenantId(), key.storeId, key.channel, key.orderScene);
            return Optional.empty();
        }

        log.debug("加载门店菜单快照: tenantId={}, storeId={}, channel={}, orderScene={}, version={}", 
                loadKey.tenantId(), key.storeId, key.channel, key.orderScene, snapshot.getVersion());

        return Optional.of(new StoreMenuSnapshotData(
                snapshot.getMenuJson(),
                snapshot.getVersion() != null ? snapshot.getVersion() : 1L
        ));
    }

    /**
     * 仅加载 version 字段，用于版本校验。
     *
     * @param loadKey 加载键
     * @return version 值
     */
    @Override
    public Optional<Long> loadVersion(SnapshotLoadKey loadKey) {
        StoreMenuKey key = parseKey(loadKey);
        
        BcStoreMenuSnapshot snapshot = storeMenuSnapshotMapper.selectOne(new LambdaQueryWrapper<BcStoreMenuSnapshot>()
                .select(BcStoreMenuSnapshot::getVersion)
                .eq(BcStoreMenuSnapshot::getTenantId, loadKey.tenantId())
                .eq(BcStoreMenuSnapshot::getStoreId, key.storeId)
                .eq(BcStoreMenuSnapshot::getChannel, key.channel)
                .eq(BcStoreMenuSnapshot::getOrderScene, key.orderScene)
                .eq(BcStoreMenuSnapshot::getStatus, 1));

        if (snapshot == null || snapshot.getVersion() == null) {
            return Optional.empty();
        }

        return Optional.of(snapshot.getVersion());
    }

    /**
     * 解析 scopeId 为 StoreMenuKey。
     * <p>
     * scopeId 格式：{storeId}:{channel}:{orderScene}
     */
    private StoreMenuKey parseKey(SnapshotLoadKey loadKey) {
        String scopeId = String.valueOf(loadKey.scopeId());
        String[] parts = scopeId.split(":", 3);
        
        if (parts.length != 3) {
            throw new IllegalArgumentException("Invalid scopeId format, expected storeId:channel:orderScene, got: " + scopeId);
        }

        return new StoreMenuKey(
                Long.parseLong(parts[0]),
                parts[1],
                parts[2]
        );
    }

    /**
     * 门店菜单键（内部使用）。
     */
    private record StoreMenuKey(Long storeId, String channel, String orderScene) {
    }
}

