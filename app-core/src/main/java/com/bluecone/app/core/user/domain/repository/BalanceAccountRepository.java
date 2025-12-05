package com.bluecone.app.core.user.domain.repository;

import java.util.Optional;

import com.bluecone.app.core.user.domain.account.BalanceAccount;

/**
 * 储值账户仓储接口。
 */
public interface BalanceAccountRepository {

    Optional<BalanceAccount> findByTenantAndMember(Long tenantId, Long memberId);

    BalanceAccount save(BalanceAccount account);

    /**
     * 使用乐观锁保存，匹配 expectedVersion。
     */
    boolean saveWithVersion(BalanceAccount account, long expectedVersion);
}
