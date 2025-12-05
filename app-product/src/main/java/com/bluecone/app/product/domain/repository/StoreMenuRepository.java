package com.bluecone.app.product.domain.repository;

import com.bluecone.app.product.domain.enums.MenuScene;
import com.bluecone.app.product.domain.enums.SaleChannel;
import com.bluecone.app.product.domain.model.readmodel.StoreMenuSnapshot;
import java.util.Optional;

/**
 * 门店菜单快照的读模型仓储接口，对应 bc_store_menu_snapshot，支撑高并发菜单读取与失效控制。
 */
public interface StoreMenuRepository {

    Optional<StoreMenuSnapshot> findSnapshot(Long tenantId, Long storeId, SaleChannel channel, MenuScene scene);

    void saveSnapshot(StoreMenuSnapshot snapshot);

    void invalidateSnapshot(Long tenantId, Long storeId, SaleChannel channel, MenuScene scene);
}
