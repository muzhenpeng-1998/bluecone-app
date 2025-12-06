package com.bluecone.app.payment.application.settlement;

import com.bluecone.app.payment.domain.model.PaymentOrder;
import com.bluecone.app.payment.domain.model.PaymentRefundOrder;
import com.bluecone.app.payment.domain.reconcile.DailySettlementSummary;
import com.bluecone.app.payment.domain.reconcile.DailySettlementSummaryRepository;
import com.bluecone.app.payment.domain.repository.PaymentOrderRepository;
import com.bluecone.app.payment.domain.repository.PaymentRefundOrderRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class SettlementApplicationService {

    private static final Logger log = LoggerFactory.getLogger(SettlementApplicationService.class);

    private final PaymentOrderRepository paymentOrderRepository;
    private final PaymentRefundOrderRepository paymentRefundOrderRepository;
    private final DailySettlementSummaryRepository dailySettlementSummaryRepository;

    public SettlementApplicationService(PaymentOrderRepository paymentOrderRepository,
                                        PaymentRefundOrderRepository paymentRefundOrderRepository,
                                        DailySettlementSummaryRepository dailySettlementSummaryRepository) {
        this.paymentOrderRepository = paymentOrderRepository;
        this.paymentRefundOrderRepository = paymentRefundOrderRepository;
        this.dailySettlementSummaryRepository = dailySettlementSummaryRepository;
    }

    @Transactional
    public void generateDailySettlement(LocalDate billDate) {
        List<PaymentOrder> payments = paymentOrderRepository.findSucceededByPayDate(billDate);
        List<PaymentRefundOrder> refunds = paymentRefundOrderRepository.findSucceededByRefundDate(billDate);

        Map<Key, DailySettlementSummary> summaryMap = new HashMap<>();
        aggregatePayments(billDate, payments, summaryMap);
        aggregateRefunds(billDate, refunds, summaryMap);

        List<DailySettlementSummary> summaries = new ArrayList<>(summaryMap.values());
        dailySettlementSummaryRepository.saveAll(summaries);
        log.info("[settlement] generated date={} count={}", billDate, summaries.size());
    }

    private void aggregatePayments(LocalDate billDate,
                                   List<PaymentOrder> payments,
                                   Map<Key, DailySettlementSummary> summaryMap) {
        for (PaymentOrder order : payments) {
            if (order.getStatus() == null || !com.bluecone.app.payment.domain.enums.PaymentStatus.SUCCESS.equals(order.getStatus())) {
                continue;
            }
            Key key = new Key(order.getTenantId(), order.getStoreId(), order.getChannel() == null ? null : order.getChannel().getCode());
            DailySettlementSummary summary = summaryMap.computeIfAbsent(key, k -> initSummary(billDate, k));
            BigDecimal amount = order.getPayableAmount() == null ? BigDecimal.ZERO : order.getPayableAmount();
            summary.setTotalPaidAmount(summary.getTotalPaidAmount().add(amount));
            summary.setPayCount(summary.getPayCount() + 1);
            summary.setNetAmount(summary.getTotalPaidAmount().subtract(summary.getTotalRefundedAmount()));
        }
    }

    private void aggregateRefunds(LocalDate billDate,
                                  List<PaymentRefundOrder> refunds,
                                  Map<Key, DailySettlementSummary> summaryMap) {
        for (PaymentRefundOrder refund : refunds) {
            if (!"SUCCESS".equalsIgnoreCase(refund.getStatus())) {
                continue;
            }
            Key key = new Key(refund.getTenantId(), null, null);
            // storeId/channelCode 不易从退款表获得，先置空占位。
            DailySettlementSummary summary = summaryMap.computeIfAbsent(key, k -> initSummary(billDate, k));
            BigDecimal amount = refund.getRefundAmount() == null ? BigDecimal.ZERO : refund.getRefundAmount();
            summary.setTotalRefundedAmount(summary.getTotalRefundedAmount().add(amount));
            summary.setRefundCount(summary.getRefundCount() + 1);
            summary.setNetAmount(summary.getTotalPaidAmount().subtract(summary.getTotalRefundedAmount()));
        }
    }

    private DailySettlementSummary initSummary(LocalDate billDate, Key key) {
        DailySettlementSummary summary = new DailySettlementSummary();
        summary.setTenantId(key.tenantId());
        summary.setStoreId(key.storeId());
        summary.setChannelCode(key.channelCode());
        summary.setBillDate(billDate);
        summary.setTotalPaidAmount(BigDecimal.ZERO);
        summary.setTotalRefundedAmount(BigDecimal.ZERO);
        summary.setNetAmount(BigDecimal.ZERO);
        summary.setPayCount(0);
        summary.setRefundCount(0);
        summary.setGeneratedAt(LocalDateTime.now());
        summary.setCreatedAt(LocalDateTime.now());
        summary.setUpdatedAt(LocalDateTime.now());
        return summary;
    }

    private record Key(Long tenantId, Long storeId, String channelCode) {
    }
}
