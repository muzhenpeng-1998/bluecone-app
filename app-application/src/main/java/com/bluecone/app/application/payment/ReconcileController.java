package com.bluecone.app.application.payment;

import com.bluecone.app.payment.api.ReconcileApi;
import com.bluecone.app.payment.api.dto.DailySettlementSummaryView;
import com.bluecone.app.payment.api.dto.ReconcileResultView;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/payments/reconcile")
public class ReconcileController {

    private final ReconcileApi reconcileApi;

    public ReconcileController(ReconcileApi reconcileApi) {
        this.reconcileApi = reconcileApi;
    }

    @GetMapping("/diffs")
    public List<ReconcileResultView> listDiffs(@RequestParam("channel") String channel,
                                               @RequestParam("billDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate billDate) {
        return reconcileApi.listReconcileResults(channel, billDate);
    }

    @GetMapping("/settlements")
    public List<DailySettlementSummaryView> listSettlement(@RequestParam("tenantId") Long tenantId,
                                                           @RequestParam("storeId") Long storeId,
                                                           @RequestParam("from") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                                                           @RequestParam("to") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return reconcileApi.listDailySettlement(tenantId, storeId, from, to);
    }
}
