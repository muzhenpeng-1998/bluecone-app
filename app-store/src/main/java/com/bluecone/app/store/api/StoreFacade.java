package com.bluecone.app.store.api;

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

import java.time.LocalDateTime;
import java.util.List;

/**
 * 对外暴露的门店能力 Facade，其他模块只能依赖该接口，禁止直接依赖 Mapper/ServiceImpl。
 * <p>高隔离：隐藏领域模型与持久化细节。</p>
 * <p>高稳定：内部可封装缓存/降级，调用方无需关心。</p>
 */
public interface StoreFacade {

    // 读侧
    StoreBaseView getStoreBase(Long tenantId, Long storeId);

    StoreOrderSnapshot getOrderSnapshot(Long tenantId, Long storeId, LocalDateTime now, String channelType);

    StoreOrderAcceptResult checkOrderAcceptable(Long tenantId, Long storeId, String capability, LocalDateTime now, String channelType);

    List<StoreBaseView> list(StoreListQuery query);

    StoreBaseView detail(StoreDetailQuery query);

    // 写侧
    void createStore(CreateStoreCommand command);

    void updateStoreBase(UpdateStoreBaseCommand command);

    void updateCapabilities(UpdateStoreCapabilitiesCommand command);

    void updateOpeningHours(UpdateStoreOpeningHoursCommand command);

    void updateSpecialDays(UpdateStoreSpecialDaysCommand command);

    void changeStatus(ChangeStoreStatusCommand command);

    void toggleOpenForOrders(ToggleOpenForOrdersCommand command);
}
