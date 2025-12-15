package com.bluecone.app.store.runtime.spi;

import com.bluecone.app.id.core.Ulid128;
import com.bluecone.app.core.store.StoreSnapshot;

import java.util.Optional;

/**
 * 门店运行态快照仓储接口，用于从底表组装 StoreSnapshot。
 */
public interface StoreSnapshotRepository {

    /**
     * 加载完整门店快照。
     */
    Optional<StoreSnapshot> loadSnapshot(long tenantId, Ulid128 storeInternalId);

    /**
     * 轻量加载版本号，用于缓存版本校验。
     */
    Optional<Long> loadConfigVersion(long tenantId, Ulid128 storeInternalId);
}

