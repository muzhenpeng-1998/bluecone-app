package com.bluecone.app.store.api.impl;

import com.bluecone.app.store.api.StoreFacade;
import com.bluecone.app.store.api.dto.StoreBaseView;
import com.bluecone.app.store.api.dto.StoreOrderAcceptResult;
import com.bluecone.app.store.api.dto.StoreOrderSnapshot;
import com.bluecone.app.store.application.command.ChangeStoreStatusCommand;
import com.bluecone.app.store.application.command.CreateStoreCommand;
import com.bluecone.app.store.application.command.ToggleOpenForOrdersCommand;
import com.bluecone.app.store.application.command.UpdateStoreBaseCommand;
import com.bluecone.app.store.application.command.UpdateStoreCapabilitiesCommand;
import com.bluecone.app.store.application.command.UpdateStoreOpeningHoursCommand;
import com.bluecone.app.store.application.command.UpdateStoreSpecialDaysCommand;
import com.bluecone.app.store.application.query.StoreDetailQuery;
import com.bluecone.app.store.application.query.StoreListQuery;
import com.bluecone.app.store.application.service.StoreCommandService;
import com.bluecone.app.store.application.service.StoreQueryService;
import com.bluecone.app.store.domain.model.StoreConfig;
import com.bluecone.app.store.api.StoreContextProvider;
import com.bluecone.app.store.domain.service.StoreOpenStateService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Facade 实现，对外模块（订单、用户等）只能通过此类访问门店能力，确保高隔离。
 * <p>读请求委派到 StoreContextProvider/StoreQueryService，写请求委派到 StoreCommandService。</p>
 * <p>高稳定：内部可挂载缓存与降级策略，外部调用方无需感知。</p>
 */
@Service
@RequiredArgsConstructor
public class StoreFacadeImpl implements StoreFacade {

    private final StoreContextProvider storeContextProvider;
    private final StoreOpenStateService storeOpenStateService;
    private final StoreQueryService storeQueryService;
    private final StoreCommandService storeCommandService;

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
        // 读流程：加载 StoreConfig 聚合后交给领域服务判断，确保规则集中
        StoreConfig config = storeQueryService.loadStoreConfig(tenantId, storeId);
        return storeOpenStateService.check(config, capability, now, channelType);
    }

    @Override
    public List<StoreBaseView> list(StoreListQuery query) {
        // 列表查询走应用层查询服务，避免外部依赖 Mapper
        return storeQueryService.listStores(query);
    }

    @Override
    public StoreBaseView detail(StoreDetailQuery query) {
        // 详情查询同样走应用层查询服务
        return storeQueryService.getStoreDetail(query);
    }

    @Override
    public String createStore(CreateStoreCommand command) {
        // Facade 不做复杂校验，直接交给写侧应用服务
        return storeCommandService.createStore(command);
    }

    @Override
    public void updateStoreBase(UpdateStoreBaseCommand command) {
        storeCommandService.updateStoreBase(command);
    }

    @Override
    public void updateCapabilities(UpdateStoreCapabilitiesCommand command) {
        storeCommandService.updateCapabilities(command);
    }

    @Override
    public void updateOpeningHours(UpdateStoreOpeningHoursCommand command) {
        storeCommandService.updateOpeningHours(command);
    }

    @Override
    public void updateSpecialDays(UpdateStoreSpecialDaysCommand command) {
        storeCommandService.updateSpecialDays(command);
    }

    @Override
    public void changeStatus(ChangeStoreStatusCommand command) {
        storeCommandService.changeStatus(command);
    }

    @Override
    public void toggleOpenForOrders(ToggleOpenForOrdersCommand command) {
        storeCommandService.toggleOpenForOrders(command);
    }
}
