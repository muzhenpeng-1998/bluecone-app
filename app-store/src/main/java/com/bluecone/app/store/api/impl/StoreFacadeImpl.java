package com.bluecone.app.store.api.impl;

import com.bluecone.app.store.api.StoreFacade;
import com.bluecone.app.store.api.dto.StoreBaseView;
import com.bluecone.app.store.api.dto.StoreOrderAcceptResult;
import com.bluecone.app.store.api.dto.StoreOrderSnapshot;
import com.bluecone.app.store.application.StoreApplicationService;
import com.bluecone.app.store.application.command.ChangeStoreStatusCommand;
import com.bluecone.app.store.application.command.CreateStoreCommand;
import com.bluecone.app.store.application.command.ToggleOpenForOrdersCommand;
import com.bluecone.app.store.application.command.UpdateStoreBaseCommand;
import com.bluecone.app.store.application.command.UpdateStoreCapabilitiesCommand;
import com.bluecone.app.store.application.command.UpdateStoreOpeningHoursCommand;
import com.bluecone.app.store.application.command.UpdateStoreSpecialDaysCommand;
import com.bluecone.app.store.application.query.StoreDetailQuery;
import com.bluecone.app.store.application.query.StoreListQuery;
import com.bluecone.app.store.domain.service.StoreContextProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Facade 实现，对外模块（订单、用户等）只能通过此类访问门店能力，确保高隔离。
 * <p>读请求委派到 StoreContextProvider，写请求委派到 StoreApplicationService。</p>
 * <p>高稳定：内部可挂载缓存与降级策略，外部调用方无需感知。</p>
 */
@Service
@RequiredArgsConstructor
public class StoreFacadeImpl implements StoreFacade {

    private final StoreContextProvider storeContextProvider;
    private final StoreApplicationService storeApplicationService;

    @Override
    public StoreBaseView getStoreBase(Long tenantId, Long storeId) {
        return storeContextProvider.getStoreBase(tenantId, storeId);
    }

    @Override
    public StoreOrderSnapshot getOrderSnapshot(Long tenantId, Long storeId, LocalDateTime now, String channelType) {
        return storeContextProvider.getOrderSnapshot(tenantId, storeId, now, channelType);
    }

    @Override
    public StoreOrderAcceptResult checkOrderAcceptable(Long tenantId, Long storeId, String capability, LocalDateTime now, String channelType) {
        return storeContextProvider.checkOrderAcceptable(tenantId, storeId, capability, now, channelType);
    }

    @Override
    public List<StoreBaseView> list(StoreListQuery query) {
        return storeApplicationService.list(query);
    }

    @Override
    public StoreBaseView detail(StoreDetailQuery query) {
        return storeApplicationService.detail(query);
    }

    @Override
    public void createStore(CreateStoreCommand command) {
        storeApplicationService.createStore(command);
    }

    @Override
    public void updateStoreBase(UpdateStoreBaseCommand command) {
        storeApplicationService.updateStoreBase(command);
    }

    @Override
    public void updateCapabilities(UpdateStoreCapabilitiesCommand command) {
        storeApplicationService.updateCapabilities(command);
    }

    @Override
    public void updateOpeningHours(UpdateStoreOpeningHoursCommand command) {
        storeApplicationService.updateOpeningHours(command);
    }

    @Override
    public void updateSpecialDays(UpdateStoreSpecialDaysCommand command) {
        storeApplicationService.updateSpecialDays(command);
    }

    @Override
    public void changeStatus(ChangeStoreStatusCommand command) {
        storeApplicationService.changeStatus(command);
    }

    @Override
    public void toggleOpenForOrders(ToggleOpenForOrdersCommand command) {
        storeApplicationService.toggleOpenForOrders(command);
    }
}
