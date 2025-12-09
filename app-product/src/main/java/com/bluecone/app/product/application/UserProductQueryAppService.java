package com.bluecone.app.product.application;

import com.bluecone.app.product.application.dto.UserStoreMenuView;
import com.bluecone.app.product.domain.model.menu.StoreSkuSnapshot;
import com.bluecone.app.product.domain.model.menu.UserStoreMenu;
import com.bluecone.app.product.domain.repository.StoreMenuRepository;
import java.util.Collection;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * 用户侧商品查询应用服务。
 */
@Service
@RequiredArgsConstructor
public class UserProductQueryAppService {

    private final StoreMenuRepository storeMenuRepository;

    public UserStoreMenuView getUserStoreMenu(Long tenantId, Long storeId) {
        UserStoreMenu menu = storeMenuRepository.loadUserStoreMenu(tenantId, storeId);
        return UserStoreMenuView.from(menu);
    }

    public Map<Long, StoreSkuSnapshot> getStoreSkuSnapshotMap(Long tenantId, Long storeId, Collection<Long> skuIds) {
        return storeMenuRepository.loadStoreSkuSnapshotMap(tenantId, storeId, skuIds);
    }
}
