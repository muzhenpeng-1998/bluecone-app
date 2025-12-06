package com.bluecone.app.payment.application.reconcile;

import com.bluecone.app.payment.api.ReconcileApi;
import com.bluecone.app.payment.api.dto.DailySettlementSummaryView;
import com.bluecone.app.payment.api.dto.ReconcileResultView;
import com.bluecone.app.payment.domain.reconcile.DailySettlementSummaryRepository;
import com.bluecone.app.payment.domain.reconcile.ReconcileResultRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ReconcileQueryApplicationService implements ReconcileApi {

    private final ReconcileResultRepository reconcileResultRepository;
    private final DailySettlementSummaryRepository dailySettlementSummaryRepository;

    public ReconcileQueryApplicationService(ReconcileResultRepository reconcileResultRepository,
                                            DailySettlementSummaryRepository dailySettlementSummaryRepository) {
        this.reconcileResultRepository = reconcileResultRepository;
        this.dailySettlementSummaryRepository = dailySettlementSummaryRepository;
    }

    @Override
    public List<ReconcileResultView> listReconcileResults(String channelCode, LocalDate billDate) {
        return reconcileResultRepository.findByChannelAndBillDate(channelCode, billDate)
                .stream()
                .map(ReconcileResultView::fromDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<DailySettlementSummaryView> listDailySettlement(Long tenantId, Long storeId, LocalDate from, LocalDate to) {
        return dailySettlementSummaryRepository.findByTenantStoreAndDate(tenantId, storeId, from, to)
                .stream()
                .map(DailySettlementSummaryView::fromDomain)
                .collect(Collectors.toList());
    }
}
