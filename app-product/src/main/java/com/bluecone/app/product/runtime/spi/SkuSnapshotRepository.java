package com.bluecone.app.product.runtime.spi;

import com.bluecone.app.core.contextkit.SnapshotRepository;
import com.bluecone.app.core.contextkit.SnapshotLoadKey;
import com.bluecone.app.product.runtime.api.SkuSnapshot;

import java.util.Optional;

/**
 * SKU 快照仓储 SPI。
 */
public interface SkuSnapshotRepository extends SnapshotRepository<SkuSnapshot> {

    @Override
    Optional<SkuSnapshot> loadFull(SnapshotLoadKey key);

    @Override
    Optional<Long> loadVersion(SnapshotLoadKey key);
}

