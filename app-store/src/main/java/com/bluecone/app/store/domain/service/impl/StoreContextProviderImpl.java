package com.bluecone.app.store.domain.service.impl;

import com.bluecone.app.core.exception.BizException;
import com.bluecone.app.infra.cache.annotation.Cached;
import com.bluecone.app.infra.cache.profile.CacheProfileName;
import com.bluecone.app.store.api.dto.StoreBaseView;
import com.bluecone.app.store.api.dto.StoreOrderAcceptResult;
import com.bluecone.app.store.api.dto.StoreOrderSnapshot;
import com.bluecone.app.store.application.service.StoreConfigService;
import com.bluecone.app.store.dao.entity.BcStore;
import com.bluecone.app.store.dao.service.IBcStoreService;
import com.bluecone.app.store.domain.model.StoreConfig;
import com.bluecone.app.store.domain.error.StoreErrorCode;
import com.bluecone.app.store.domain.service.StoreContextProvider;
import com.bluecone.app.store.domain.service.StoreOpenStateService;
import com.bluecone.app.store.domain.service.assembler.StoreViewAssembler;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * StoreContextProvider 实现，负责对外提供高性能的门店上下文访问接口。
 * <p>流程：先查 bc_store 拿 configVersion，再通过 StoreConfigService（多级缓存包装）加载 StoreConfig 快照，最后委托 StoreOpenStateService 做校验。</p>
 * <p>高隔离：外部只见语义化方法，不暴露 Mapper/Entity。</p>
 * <p>高稳定：多级缓存兜底，Redis/L1 异常时仍可回退 DB。</p>
 * <p>高并发：依赖 StoreConfig 的整体快照，避免订单链路多次拆表查询。</p>
 */
@Service
@RequiredArgsConstructor
public class StoreContextProviderImpl implements StoreContextProvider {

    private final IBcStoreService bcStoreService;
    private final StoreConfigService storeConfigService;
    private final StoreOpenStateService storeOpenStateService;
    private final StoreViewAssembler storeViewAssembler;

    @Override
    @Cached(profile = CacheProfileName.STORE_BASE, key = "#tenantId + ':' + #storeId")
    public StoreBaseView getStoreBase(Long tenantId, Long storeId) {
        BcStore entity = loadStoreStrict(tenantId, storeId);
        StoreConfig config = storeConfigService.loadConfig(tenantId, storeId, entity.getConfigVersion());
        if (config != null) {
            // 通过装配器生成只读视图，避免外部依赖领域对象
            return storeViewAssembler.toStoreBaseView(config);
        }
        return storeViewAssembler.toStoreBaseView(entity);
    }

    @Override
    @Cached(profile = CacheProfileName.STORE_SNAPSHOT, key = "#tenantId + ':' + #storeId + ':' + (#channelType == null ? '' : #channelType)")
    public StoreOrderSnapshot getOrderSnapshot(Long tenantId, Long storeId, LocalDateTime now, String channelType) {
        BcStore entity = loadStoreStrict(tenantId, storeId);
        StoreConfig config = storeConfigService.loadConfig(tenantId, storeId, entity.getConfigVersion());
        if (config == null) {
            return null;
        }
        StoreOrderSnapshot snapshot = storeViewAssembler.toOrderSnapshot(config, now, channelType);
        return snapshot;
    }

    @Override
    public StoreOrderAcceptResult checkOrderAcceptable(Long tenantId, Long storeId, String capability, LocalDateTime now, String channelType) {
        BcStore entity = findStoreEntity(tenantId, storeId);
        StoreConfig config = entity == null ? null : storeConfigService.loadConfig(tenantId, storeId, entity.getConfigVersion());
        // 未命中聚合则返回不可接单，避免上层 NPE
        if (config == null) {
            return StoreOrderAcceptResult.builder()
                    .acceptable(false)
                    .reasonCode("STORE_NOT_FOUND")
                    .reasonMessage("门店不存在或已删除")
                    .build();
        }
        // 通过领域服务做集中校验，避免上层重复实现规则
        return storeOpenStateService.check(config, capability, now, channelType);
    }

    private BcStore loadStoreStrict(Long tenantId, Long storeId) {
        BcStore entity = findStoreEntity(tenantId, storeId);
        if (entity == null) {
            throw new BizException(StoreErrorCode.STORE_NOT_FOUND);
        }
        return entity;
    }

    private BcStore findStoreEntity(Long tenantId, Long storeId) {
        return bcStoreService.lambdaQuery()
                .eq(BcStore::getTenantId, tenantId)
                .eq(BcStore::getId, storeId)
                .eq(BcStore::getIsDeleted, false)
                .one();
    }
}
