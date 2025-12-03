package com.bluecone.app.store.application;

import com.bluecone.app.store.application.command.ChangeStoreStatusCommand;
import com.bluecone.app.store.application.command.CreateStoreCommand;
import com.bluecone.app.store.application.command.ToggleOpenForOrdersCommand;
import com.bluecone.app.store.application.command.UpdateStoreBaseCommand;
import com.bluecone.app.store.application.command.UpdateStoreCapabilitiesCommand;
import com.bluecone.app.store.application.command.UpdateStoreOpeningHoursCommand;
import com.bluecone.app.store.application.command.UpdateStoreSpecialDaysCommand;
import com.bluecone.app.store.application.query.StoreDetailQuery;
import com.bluecone.app.store.application.query.StoreListQuery;
import com.bluecone.app.store.api.dto.StoreBaseView;
import com.bluecone.app.store.domain.exception.StoreConfigVersionConflictException;
import com.bluecone.app.store.domain.repository.StoreRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;

/**
 * 应用服务：承载写侧用例编排（创建/更新配置），并封装版本号控制。
 * <p>高隔离：上层通过 Facade 调用，不暴露底层 Mapper 与领域模型。</p>
 * <p>高稳定：通过 configVersion 做乐观锁，避免并发覆盖；异常统一抛出业务异常。</p>
 * <p>高并发：写侧成功后可触发缓存失效/刷新（预留 TODO）。</p>
 */
@Service
@RequiredArgsConstructor
public class StoreApplicationService {

    private final StoreRepository storeRepository;

    /**
     * 创建门店基础信息，后续可扩展为事务性聚合创建。
     */
    public void createStore(CreateStoreCommand command) {
        // TODO: 补充领域校验、唯一性校验、默认配置初始化等
        throw new UnsupportedOperationException("TODO implement createStore");
    }

    /**
     * 更新门店基础信息，使用 configVersion 做乐观锁。
     */
    public void updateStoreBase(UpdateStoreBaseCommand command) {
        // TODO: 通过 Mapper 更新基础信息，并在成功后 bumpConfigVersion
        // 未命中乐观锁时抛出 StoreConfigVersionConflictException
        throw new StoreConfigVersionConflictException("暂未实现，等待补充更新逻辑");
    }

    /**
     * 批量更新门店能力配置。
     */
    public void updateCapabilities(UpdateStoreCapabilitiesCommand command) {
        // TODO: 通过仓储更新能力，成功后 bumpConfigVersion，并刷新缓存
        throw new StoreConfigVersionConflictException("暂未实现，等待补充能力更新逻辑");
    }

    /**
     * 更新常规营业时间。
     */
    public void updateOpeningHours(UpdateStoreOpeningHoursCommand command) {
        // TODO: 仓储更新并 bumpConfigVersion
        throw new StoreConfigVersionConflictException("暂未实现，等待补充营业时间更新逻辑");
    }

    /**
     * 更新特殊日配置。
     */
    public void updateSpecialDays(UpdateStoreSpecialDaysCommand command) {
        // TODO: 仓储更新并 bumpConfigVersion
        throw new StoreConfigVersionConflictException("暂未实现，等待补充特殊日更新逻辑");
    }

    /**
     * 更新门店状态。
     */
    public void changeStatus(ChangeStoreStatusCommand command) {
        // TODO: 更新状态并 bumpConfigVersion
        throw new StoreConfigVersionConflictException("暂未实现，等待补充状态更新逻辑");
    }

    /**
     * 切换接单开关。
     */
    public void toggleOpenForOrders(ToggleOpenForOrdersCommand command) {
        // TODO: 更新 openForOrders 并 bumpConfigVersion
        throw new StoreConfigVersionConflictException("暂未实现，等待补充开关更新逻辑");
    }

    /**
     * 列表查询，用于后台管理等场景。
     */
    public List<StoreBaseView> list(StoreListQuery query) {
        // TODO: 复用现有查询 Service 或 Mapper，增加必要过滤
        return Collections.emptyList();
    }

    /**
     * 详情查询。
     */
    public StoreBaseView detail(StoreDetailQuery query) {
        // TODO: 复用查询服务获取基础信息
        return null;
    }
}
