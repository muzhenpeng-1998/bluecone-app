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
    /**
     * 获取门店基础信息视图。高频场景可结合 StoreContextProvider 缓存快照，避免多次查表。
     */
    StoreBaseView getStoreBase(Long tenantId, Long storeId);

    /**
     * 获取订单视角快照，包含能力/渠道等高频字段，支持高并发读取。
     */
    StoreOrderSnapshot getOrderSnapshot(Long tenantId, Long storeId, LocalDateTime now, String channelType);

    /**
     * 校验是否可接单，封装门店状态、营业时间、渠道绑定等规则。
     */
    StoreOrderAcceptResult checkOrderAcceptable(Long tenantId, Long storeId, String capability, LocalDateTime now, String channelType);

    /**
     * 列表查询，供后台等读侧使用。
     */
    List<StoreBaseView> list(StoreListQuery query);

    /**
     * 详情查询，返回只读基础信息。
     */
    StoreBaseView detail(StoreDetailQuery query);

    // 写侧
    /**
     * 创建门店及其默认配置。
     */
    void createStore(CreateStoreCommand command);

    /**
     * 更新门店基础信息，需依赖 configVersion 做并发保护。
     */
    void updateStoreBase(UpdateStoreBaseCommand command);

    /**
     * 批量更新门店能力配置。
     */
    void updateCapabilities(UpdateStoreCapabilitiesCommand command);

    /**
     * 更新常规营业时间。
     */
    void updateOpeningHours(UpdateStoreOpeningHoursCommand command);

    /**
     * 更新特殊日配置。
     */
    void updateSpecialDays(UpdateStoreSpecialDaysCommand command);

    /**
     * 切换门店状态（OPEN/PAUSED/CLOSED）。
     */
    void changeStatus(ChangeStoreStatusCommand command);

    /**
     * 切换接单开关。
     */
    void toggleOpenForOrders(ToggleOpenForOrdersCommand command);
}
