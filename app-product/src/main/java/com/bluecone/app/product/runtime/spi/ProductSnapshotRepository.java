package com.bluecone.app.product.runtime.spi;

import com.bluecone.app.core.contextkit.SnapshotRepository;
import com.bluecone.app.core.contextkit.SnapshotLoadKey;
import com.bluecone.app.product.runtime.api.ProductSnapshot;

import java.util.Optional;

/**
 * 商品快照仓储 SPI。
 */
public interface ProductSnapshotRepository extends SnapshotRepository<ProductSnapshot> {

    @Override
    Optional<ProductSnapshot> loadFull(SnapshotLoadKey key);

    @Override
    Optional<Long> loadVersion(SnapshotLoadKey key);
}

