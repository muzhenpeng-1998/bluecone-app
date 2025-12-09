package com.bluecone.app.product.domain.repository;

import com.bluecone.app.product.domain.model.menu.StoreSkuSnapshot;
import com.bluecone.app.product.domain.model.menu.UserStoreMenu;
import java.util.Collection;
import java.util.Map;

/**
 * 门店菜单仓储，提供面向用户的菜单视图及 SKU 权威定价。
 */
public interface StoreMenuRepository {

    /**
     * 加载某个门店在用户侧可见的菜单。
     */
    UserStoreMenu loadUserStoreMenu(Long tenantId, Long storeId);

    /**
     * 按门店 + SKU 列表加载权威 SKU 快照。
     */
    Map<Long, StoreSkuSnapshot> loadStoreSkuSnapshotMap(Long tenantId, Long storeId, Collection<Long> skuIds);
}
