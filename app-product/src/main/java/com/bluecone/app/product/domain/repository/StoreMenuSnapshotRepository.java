package com.bluecone.app.product.domain.repository;

import com.bluecone.app.product.dao.entity.BcStoreMenuSnapshot;
import java.util.Optional;

/**
 * 门店菜单快照仓储抽象，领域层只关心按租户/门店/渠道/场景获取或保存快照，不暴露 MyBatis 细节。
 */
public interface StoreMenuSnapshotRepository {

    /**
     * 按租户、门店、渠道、场景查询快照。
     */
    Optional<BcStoreMenuSnapshot> findByTenantAndStoreAndChannelAndScene(Long tenantId, Long storeId, String channel, String orderScene);

    /**
     * 保存或更新快照：存在则更新 menuJson/version/generatedAt， 不存在则插入并从版本 1 开始。
     */
    void saveOrUpdateSnapshot(BcStoreMenuSnapshot snapshot);
}
