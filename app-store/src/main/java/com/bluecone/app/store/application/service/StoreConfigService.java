package com.bluecone.app.store.application.service;

import com.bluecone.app.infra.cache.annotation.Cached;
import com.bluecone.app.infra.cache.profile.CacheProfileName;
import com.bluecone.app.store.domain.model.StoreConfig;
import com.bluecone.app.store.domain.repository.StoreRepository;
import org.springframework.stereotype.Service;

/**
 * 门店配置应用服务。
 * <p>职责：基于 StoreRepository 提供带缓存的 StoreConfig 访问能力。</p>
 * <p>高并发：使用 configVersion 作为 key 维度做版本化缓存，避免脏读。</p>
 */
@Service
public class StoreConfigService {

    private final StoreRepository storeRepository;

    public StoreConfigService(StoreRepository storeRepository) {
        this.storeRepository = storeRepository;
    }

    /**
     * 加载指定版本的门店配置，并使用多级缓存。
     *
     * @param tenantId      租户 ID
     * @param storeId       门店 ID
     * @param configVersion 配置版本号（来自 bc_store.config_version）
     * @return 门店配置聚合
     */
    @Cached(profile = CacheProfileName.STORE_CONFIG, key = "#tenantId + ':' + #storeId + ':' + #configVersion")
    public StoreConfig loadConfig(Long tenantId, Long storeId, Long configVersion) {
        // 直接委派给仓储，缓存由 AOP 负责
        return storeRepository.loadFullConfig(tenantId, storeId);
    }
}
