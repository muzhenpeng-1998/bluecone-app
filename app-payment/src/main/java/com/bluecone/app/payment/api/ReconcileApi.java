package com.bluecone.app.payment.api;

import com.bluecone.app.payment.api.dto.DailySettlementSummaryView;
import com.bluecone.app.payment.api.dto.ReconcileResultView;

import java.time.LocalDate;
import java.util.List;

public interface ReconcileApi {

    List<ReconcileResultView> listReconcileResults(String channelCode, LocalDate billDate);

    List<DailySettlementSummaryView> listDailySettlement(Long tenantId, Long storeId, LocalDate from, LocalDate to);
}
