package com.bluecone.app.core.user.domain.repository;

import java.util.Optional;

import com.bluecone.app.core.user.domain.account.PointsAccount;

/**
 * 积分账户仓储接口。
 */
public interface PointsAccountRepository {

    Optional<PointsAccount> findByTenantAndMember(Long tenantId, Long memberId);

    PointsAccount save(PointsAccount account);

    /**
     * 使用乐观锁保存，匹配 expectedVersion。
     */
    boolean saveWithVersion(PointsAccount account, long expectedVersion);
}
