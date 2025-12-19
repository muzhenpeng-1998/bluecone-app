package com.bluecone.app.store.application;

import com.bluecone.app.core.exception.BusinessException;
import com.bluecone.app.store.api.StoreContextProvider;
import com.bluecone.app.store.api.dto.StoreBaseView;
import com.bluecone.app.store.api.dto.StoreOrderSnapshot;
import com.bluecone.app.store.domain.error.StoreErrorCode;
import com.bluecone.app.store.domain.model.StoreConfig;
import com.bluecone.app.store.domain.model.runtime.StoreRuntime;
import com.bluecone.app.store.domain.repository.StoreRepository;
import com.bluecone.app.store.domain.service.StoreOpenStateService;
import com.bluecone.app.store.application.service.StoreQueryService;
import java.time.LocalDate;
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
    private final StoreQueryService storeQueryService;

    @Override
    public StoreBaseView getStoreBase(Long tenantId, Long storeId) {
        StoreRuntime runtime = loadStoreRuntime(tenantId, storeId);
        return mapToStoreBaseView(runtime);
    }

    @Override
    public StoreOrderSnapshot getOrderSnapshot(Long tenantId, Long storeId, LocalDateTime now, String channelType) {
        // 使用 StoreConfig 获取完整信息，包括 configVersion 和营业时间
        StoreConfig config = storeQueryService.loadStoreConfig(tenantId, storeId);
        LocalDateTime targetTime = now != null ? now : LocalDateTime.now();
        return mapToStoreOrderSnapshot(config, targetTime, channelType);
    }

    private StoreRuntime loadStoreRuntime(Long tenantId, Long storeId) {
        return storeRepository.loadStoreRuntime(tenantId, storeId)
                .orElseThrow(() -> new BusinessException(StoreErrorCode.STORE_NOT_FOUND));
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

    /**
     * 将 StoreConfig 转换为 StoreOrderSnapshot，包含订单前置校验所需的最小字段集。
     *
     * @param config      门店配置聚合
     * @param now         当前时间
     * @param channelType 渠道类型（可选）
     * @return 订单视角的门店快照
     */
    private StoreOrderSnapshot mapToStoreOrderSnapshot(StoreConfig config, LocalDateTime now, String channelType) {
        StoreOrderSnapshot snapshot = new StoreOrderSnapshot();
        snapshot.setTenantId(config.getTenantId());
        snapshot.setStoreId(config.getStoreId());
        snapshot.setStorePublicId(config.getStorePublicId());
        snapshot.setStoreName(config.getName());
        snapshot.setStatus(config.getStatus());
        snapshot.setOpenForOrders(config.isOpenForOrders());
        snapshot.setConfigVersion(config.getConfigVersion());

        // 判断当前是否可接单（基于营业时间和接单开关）
        boolean isOpen = config.getOpeningSchedule() != null && config.getOpeningSchedule().isOpenAt(now);
        snapshot.setCurrentlyOpen(isOpen);
        snapshot.setCanAcceptOrder(config.isOpenForOrders() && "OPEN".equalsIgnoreCase(config.getStatus()) && isOpen);

        // 获取当天营业时间区间
        if (config.getOpeningSchedule() != null) {
            LocalDate today = now.toLocalDate();
            String todayOpeningHoursRange = config.getOpeningSchedule().getOpeningHoursRange(today);
            snapshot.setTodayOpeningHoursRange(todayOpeningHoursRange);
        }

        // 填充渠道信息（如果提供了 channelType）
        if (channelType != null && config.getChannels() != null) {
            config.getChannels().stream()
                    .filter(channel -> channelType.equalsIgnoreCase(channel.getChannelType()))
                    .findFirst()
                    .ifPresent(channel -> {
                        snapshot.setChannelType(channel.getChannelType());
                        snapshot.setChannelStatus(channel.getStatus());
                    });
        }

        return snapshot;
    }
}
