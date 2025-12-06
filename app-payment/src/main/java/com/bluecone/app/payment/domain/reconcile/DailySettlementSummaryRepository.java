package com.bluecone.app.payment.domain.reconcile;

import java.time.LocalDate;
import java.util.List;

public interface DailySettlementSummaryRepository {

    void saveAll(List<DailySettlementSummary> summaries);

    List<DailySettlementSummary> findByTenantStoreAndDate(Long tenantId, Long storeId, LocalDate from, LocalDate to);
}
