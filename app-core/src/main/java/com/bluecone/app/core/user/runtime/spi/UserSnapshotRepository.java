package com.bluecone.app.core.user.runtime.spi;

import com.bluecone.app.core.contextkit.SnapshotRepository;
import com.bluecone.app.core.user.runtime.api.UserSnapshot;

/**
 * 用户运行态快照仓储接口，用于从底表组装 UserSnapshot。
 */
public interface UserSnapshotRepository extends SnapshotRepository<UserSnapshot> {
}

