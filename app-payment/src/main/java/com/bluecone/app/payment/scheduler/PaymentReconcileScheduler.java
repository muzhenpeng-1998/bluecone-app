package com.bluecone.app.payment.scheduler;

import com.bluecone.app.payment.application.reconcile.ChannelBillImportService;
import com.bluecone.app.payment.application.reconcile.ReconcileApplicationService;
import com.bluecone.app.payment.application.settlement.SettlementApplicationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;

@Component
public class PaymentReconcileScheduler {

    private static final Logger log = LoggerFactory.getLogger(PaymentReconcileScheduler.class);

    private final ChannelBillImportService billImportService;
    private final ReconcileApplicationService reconcileApplicationService;
    private final SettlementApplicationService settlementApplicationService;

    public PaymentReconcileScheduler(ChannelBillImportService billImportService,
                                     ReconcileApplicationService reconcileApplicationService,
                                     SettlementApplicationService settlementApplicationService) {
        this.billImportService = billImportService;
        this.reconcileApplicationService = reconcileApplicationService;
        this.settlementApplicationService = settlementApplicationService;
    }

    @Scheduled(cron = "0 0 3 * * ?")
    public void importAndReconcile() {
        LocalDate billDate = LocalDate.now().minusDays(1);
        for (String channel : List.of("WECHAT", "ALIPAY")) {
            try {
                log.info("[scheduler] start import & reconcile channel={} billDate={}", channel, billDate);
                billImportService.importBill(channel, billDate);
                reconcileApplicationService.reconcile(channel, billDate);
            } catch (Exception ex) {
                log.error("[scheduler] import/reconcile error channel={} billDate={}", channel, billDate, ex);
            }
        }
    }

    @Scheduled(cron = "0 0 4 * * ?")
    public void generateSettlement() {
        LocalDate billDate = LocalDate.now().minusDays(1);
        try {
            log.info("[scheduler] start settlement billDate={}", billDate);
            settlementApplicationService.generateDailySettlement(billDate);
        } catch (Exception ex) {
            log.error("[scheduler] settlement error billDate={}", billDate, ex);
        }
    }
}
