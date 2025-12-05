package com.bluecone.app.core.user.domain.repository;

import com.bluecone.app.core.user.domain.account.BalanceLedger;

import java.util.List;
import java.util.Optional;

/**
 * 储值流水仓储接口。
 */
public interface BalanceLedgerRepository {

    Optional<BalanceLedger> findByBiz(Long tenantId, String bizType, String bizId);

    List<BalanceLedger> listByTenantAndMember(Long tenantId, Long memberId, int pageNo, int pageSize);

    BalanceLedger save(BalanceLedger ledger);
}
