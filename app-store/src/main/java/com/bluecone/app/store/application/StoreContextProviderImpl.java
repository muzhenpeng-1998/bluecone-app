package com.bluecone.app.store.application;

import com.bluecone.app.core.exception.BizException;
import com.bluecone.app.store.api.StoreContextProvider;
import com.bluecone.app.store.api.dto.StoreBaseView;
import com.bluecone.app.store.api.dto.StoreOrderSnapshot;
import com.bluecone.app.store.domain.error.StoreErrorCode;
import com.bluecone.app.store.domain.model.runtime.StoreRuntime;
import com.bluecone.app.store.domain.repository.StoreRepository;
import com.bluecone.app.store.domain.service.StoreOpenStateService;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * StoreContextProvider 默认实现：基于门店运行时快照 + 营业状态服务。
 */
@Service
@RequiredArgsConstructor
public class StoreContextProviderImpl implements StoreContextProvider {

    private final StoreRepository storeRepository;
    private final StoreOpenStateService storeOpenStateService;

    @Override
    public StoreBaseView getStoreBase(Long tenantId, Long storeId) {
        StoreRuntime runtime = loadStoreRuntime(tenantId, storeId);
        return mapToStoreBaseView(runtime);
    }

    @Override
    public StoreOrderSnapshot getOrderSnapshot(Long tenantId, Long storeId, LocalDateTime now, String channelType) {
        StoreRuntime runtime = loadStoreRuntime(tenantId, storeId);
        LocalDateTime targetTime = now != null ? now : LocalDateTime.now();
        boolean canAcceptOrder = storeOpenStateService.isStoreOpenForOrder(runtime, targetTime);
        return mapToStoreOrderSnapshot(runtime, canAcceptOrder);
    }

    private StoreRuntime loadStoreRuntime(Long tenantId, Long storeId) {
        return storeRepository.loadStoreRuntime(tenantId, storeId)
                .orElseThrow(() -> new BizException(StoreErrorCode.STORE_NOT_FOUND));
    }

    private StoreBaseView mapToStoreBaseView(StoreRuntime runtime) {
        StoreBaseView view = new StoreBaseView();
        view.setTenantId(runtime.getTenantId());
        view.setStoreId(runtime.getStoreId());
        view.setStoreName(runtime.getStoreName());
        view.setName(runtime.getStoreName());
        view.setBizStatus(runtime.getBizStatus());
        Boolean forceClosed = runtime.getForceClosed();
        view.setOpenForOrders(forceClosed == null ? null : !forceClosed);
        view.setTakeoutEnabled(runtime.getTakeoutEnabled());
        view.setPickupEnabled(runtime.getPickupEnabled());
        view.setDineInEnabled(runtime.getDineInEnabled());
        return view;
    }

    private StoreOrderSnapshot mapToStoreOrderSnapshot(StoreRuntime runtime, boolean canAcceptOrder) {
        StoreOrderSnapshot snapshot = new StoreOrderSnapshot();
        snapshot.setTenantId(runtime.getTenantId());
        snapshot.setStoreId(runtime.getStoreId());
        snapshot.setStoreName(runtime.getStoreName());
        snapshot.setBizStatus(runtime.getBizStatus());
        snapshot.setCurrentlyOpen(canAcceptOrder);
        snapshot.setCanAcceptOrder(canAcceptOrder);
        Boolean forceClosed = runtime.getForceClosed();
        snapshot.setOpenForOrders(forceClosed == null ? null : !forceClosed);
        snapshot.setTakeoutEnabled(runtime.getTakeoutEnabled());
        snapshot.setPickupEnabled(runtime.getPickupEnabled());
        snapshot.setDineInEnabled(runtime.getDineInEnabled());
        return snapshot;
    }
}

