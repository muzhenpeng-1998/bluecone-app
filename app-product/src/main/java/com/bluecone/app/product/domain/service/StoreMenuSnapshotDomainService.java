package com.bluecone.app.product.domain.service;

import com.bluecone.app.product.dao.entity.BcStoreMenuSnapshot;
import com.bluecone.app.product.domain.model.menu.StoreMenuSnapshotModel;
import com.bluecone.app.product.domain.repository.StoreMenuSnapshotRepository;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 菜单快照领域服务：负责“构建 + 落库”的完整流程。
 * <p>后续可挂接商品/门店配置变更事件触发重建，也可在此扩展 Redis 缓存写入。</p>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class StoreMenuSnapshotDomainService {

    private final StoreMenuSnapshotBuilderService builderService;
    private final StoreMenuSnapshotRepository storeMenuSnapshotRepository;

    /**
     * 重建并保存指定门店/渠道/场景的菜单快照。
     */
    public BcStoreMenuSnapshot rebuildAndSaveSnapshot(Long tenantId, Long storeId, String channel, String orderScene) {
        StoreMenuSnapshotModel model = builderService.buildStoreMenuSnapshot(tenantId, storeId, channel, orderScene);
        String menuJson = builderService.buildMenuJson(model);
        BcStoreMenuSnapshot entity = new BcStoreMenuSnapshot();
        entity.setTenantId(tenantId);
        entity.setStoreId(storeId);
        entity.setChannel(channel);
        entity.setOrderScene(orderScene);
        entity.setMenuJson(menuJson);
        entity.setGeneratedAt(LocalDateTime.now());
        entity.setStatus(1);
        storeMenuSnapshotRepository.saveOrUpdateSnapshot(entity);
        BcStoreMenuSnapshot latest = storeMenuSnapshotRepository
                .findByTenantAndStoreAndChannelAndScene(tenantId, storeId, channel, orderScene)
                .orElse(entity);
        log.info("菜单快照已重建并保存, tenantId={}, storeId={}, channel={}, scene={}", tenantId, storeId, channel, orderScene);
        return latest;
    }
}
