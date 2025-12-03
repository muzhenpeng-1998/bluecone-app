package com.bluecone.app.store.domain.service.impl;

import com.bluecone.app.store.api.dto.StoreBaseView;
import com.bluecone.app.store.api.dto.StoreOrderAcceptResult;
import com.bluecone.app.store.api.dto.StoreOrderSnapshot;
import com.bluecone.app.store.domain.model.StoreConfig;
import com.bluecone.app.store.domain.repository.StoreRepository;
import com.bluecone.app.store.domain.service.StoreContextProvider;
import com.bluecone.app.store.domain.service.StoreOpenStateService;
import com.bluecone.app.store.infrastructure.assembler.StoreSnapshotAssembler;
import com.bluecone.app.store.infrastructure.cache.StoreConfigCache;
import com.bluecone.app.store.infrastructure.cache.StoreContextCache;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * StoreContextProvider 实现，负责对外提供高性能的门店上下文访问接口。
 * <p>流程：先通过 StoreRepository 获取 StoreConfig（后续可接入多级缓存），再按场景组装视图/快照，最后委托 StoreOpenStateService 做校验。</p>
 * <p>高隔离：外部只见语义化方法，不暴露 Mapper/Entity。</p>
 * <p>高稳定：预留缓存降级点（Redis 失效回退 DB，DB 异常回退缓存快照）。</p>
 * <p>高并发：依赖 StoreConfig 的整体快照，避免订单链路多次拆表查询。</p>
 */
@Service
@RequiredArgsConstructor
public class StoreContextProviderImpl implements StoreContextProvider {

    private final StoreRepository storeRepository;
    private final StoreOpenStateService storeOpenStateService;
    private final StoreSnapshotAssembler storeSnapshotAssembler;
    private final StoreConfigCache storeConfigCache;
    private final StoreContextCache storeContextCache;

    @Override
    public StoreBaseView getStoreBase(Long tenantId, Long storeId) {
        StoreConfig config = loadConfigWithCache(tenantId, storeId);
        if (config == null) {
            return null;
        }
        return storeSnapshotAssembler.toBaseView(config);
    }

    @Override
    public StoreOrderSnapshot getOrderSnapshot(Long tenantId, Long storeId, LocalDateTime now, String channelType) {
        StoreConfig config = loadConfigWithCache(tenantId, storeId);
        if (config == null) {
            return null;
        }
        // 优先尝试上下文快照缓存，命中则直接返回
        StoreOrderSnapshot cached = storeContextCache.get(tenantId, storeId, channelType, config.getConfigVersion());
        if (cached != null) {
            return cached;
        }
        StoreOrderSnapshot snapshot = storeSnapshotAssembler.toOrderSnapshot(config, now, channelType);
        // 预留扩展点：可调用 StoreOpenStateService 补充当前营业态、特殊日信息，并将结果写入 snapshot
        // TODO: 引入多级缓存（本地 + Redis），key 可采用 configVersion + storeId 组合，防止版本回退
        storeContextCache.put(snapshot, tenantId, storeId, channelType);
        return snapshot;
    }

    @Override
    public StoreOrderAcceptResult checkOrderAcceptable(Long tenantId, Long storeId, String capability, LocalDateTime now, String channelType) {
        StoreConfig config = loadConfigWithCache(tenantId, storeId);
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

    private StoreConfig loadConfigWithCache(Long tenantId, Long storeId) {
        // 预留多级缓存扩展点：优先读本地缓存 -> Redis -> DB，命中后可根据 configVersion 做缓存穿透/回退控制
        // TODO: 引入 Redis 缓存时需处理缓存击穿、版本校验
        long configVersion = storeRepository.getConfigVersion(tenantId, storeId);
        StoreConfig cached = storeConfigCache.get(tenantId, storeId, configVersion);
        if (cached != null) {
            return cached;
        }
        StoreConfig config = storeRepository.loadFullConfig(tenantId, storeId);
        if (config != null) {
            storeConfigCache.put(config);
        }
        return config;
    }
}
