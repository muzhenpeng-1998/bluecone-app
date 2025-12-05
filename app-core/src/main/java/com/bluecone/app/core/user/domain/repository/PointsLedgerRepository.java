package com.bluecone.app.core.user.domain.repository;

import com.bluecone.app.core.user.domain.account.PointsLedger;

import java.util.List;
import java.util.Optional;

/**
 * 积分流水仓储接口。
 */
public interface PointsLedgerRepository {

    Optional<PointsLedger> findByBiz(Long tenantId, String bizType, String bizId);

    List<PointsLedger> listByTenantAndMember(Long tenantId, Long memberId, int pageNo, int pageSize);

    PointsLedger save(PointsLedger ledger);
}
