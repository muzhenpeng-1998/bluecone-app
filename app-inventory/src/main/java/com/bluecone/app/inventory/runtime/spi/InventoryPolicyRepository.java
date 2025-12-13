package com.bluecone.app.inventory.runtime.spi;

import com.bluecone.app.core.contextkit.SnapshotRepository;
import com.bluecone.app.inventory.runtime.api.InventoryPolicySnapshot;

/**
 * 库存策略快照仓储 SPI。
 *
 * <p>针对 ContextMiddlewareKit 的 SnapshotRepository 适配，提供门店维度的库存策略快照。</p>
 */
public interface InventoryPolicyRepository extends SnapshotRepository<InventoryPolicySnapshot> {
}

