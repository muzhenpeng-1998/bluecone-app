package com.bluecone.app.core.contextkit;

import java.util.Optional;

/**
 * 通用快照仓储接口，支持加载完整快照与轻量版本号。
 */
public interface SnapshotRepository<T> {

    Optional<T> loadFull(SnapshotLoadKey key);

    Optional<Long> loadVersion(SnapshotLoadKey key);
}

