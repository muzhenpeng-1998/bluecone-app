package com.bluecone.app.payment.application.reconcile;

import com.bluecone.app.payment.domain.model.PaymentOrder;
import com.bluecone.app.payment.domain.model.PaymentRefundOrder;
import com.bluecone.app.payment.domain.reconcile.ChannelBillRecord;
import com.bluecone.app.payment.domain.reconcile.ChannelBillRecordRepository;
import com.bluecone.app.payment.domain.reconcile.ReconcileDiffType;
import com.bluecone.app.payment.domain.reconcile.ReconcileResult;
import com.bluecone.app.payment.domain.reconcile.ReconcileResultRepository;
import com.bluecone.app.payment.domain.repository.PaymentOrderRepository;
import com.bluecone.app.payment.domain.repository.PaymentRefundOrderRepository;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

@Service
public class ReconcileApplicationService {

    private static final Logger log = LoggerFactory.getLogger(ReconcileApplicationService.class);

    private final ChannelBillRecordRepository channelBillRecordRepository;
    private final ReconcileResultRepository reconcileResultRepository;
    private final PaymentOrderRepository paymentOrderRepository;
    private final PaymentRefundOrderRepository paymentRefundOrderRepository;

    public ReconcileApplicationService(ChannelBillRecordRepository channelBillRecordRepository,
                                       ReconcileResultRepository reconcileResultRepository,
                                       PaymentOrderRepository paymentOrderRepository,
                                       PaymentRefundOrderRepository paymentRefundOrderRepository) {
        this.channelBillRecordRepository = channelBillRecordRepository;
        this.reconcileResultRepository = reconcileResultRepository;
        this.paymentOrderRepository = paymentOrderRepository;
        this.paymentRefundOrderRepository = paymentRefundOrderRepository;
    }

    @Transactional
    public void reconcile(String channelCode, LocalDate billDate) {
        List<ChannelBillRecord> channelRecords = channelBillRecordRepository.findByChannelAndBillDate(channelCode, billDate);
        if (channelRecords.isEmpty()) {
            log.warn("[reconcile] no channel records channel={} billDate={}", channelCode, billDate);
            return;
        }
        reconcileResultRepository.deleteByChannelAndBillDate(channelCode, billDate);

        Map<String, List<ChannelBillRecord>> byMerchantOrder = indexByMerchantOrder(channelRecords);
        Map<String, List<ChannelBillRecord>> byMerchantRefund = indexByMerchantRefund(channelRecords);

        List<PaymentOrder> paymentOrders = paymentOrderRepository.findByPayDate(billDate);
        List<PaymentRefundOrder> refundOrders = paymentRefundOrderRepository.findByRefundDate(billDate);

        List<ReconcileResult> diffList = new ArrayList<>();
        reconcileLocalPayments(channelCode, billDate, paymentOrders, byMerchantOrder, diffList);
        reconcileLocalRefunds(channelCode, billDate, refundOrders, byMerchantRefund, diffList);
        reconcileChannelOnly(channelCode, billDate, channelRecords, paymentOrders, refundOrders, diffList);

        reconcileResultRepository.saveAll(diffList);
        log.info("[reconcile] finished channel={} billDate={} diffCount={}", channelCode, billDate, diffList.size());
    }

    private Map<String, List<ChannelBillRecord>> indexByMerchantOrder(List<ChannelBillRecord> records) {
        Map<String, List<ChannelBillRecord>> map = new HashMap<>();
        for (ChannelBillRecord r : records) {
            if (StringUtils.isBlank(r.getMerchantOrderNo())) {
                continue;
            }
            map.computeIfAbsent(r.getMerchantOrderNo(), k -> new ArrayList<>()).add(r);
        }
        return map;
    }

    private Map<String, List<ChannelBillRecord>> indexByMerchantRefund(List<ChannelBillRecord> records) {
        Map<String, List<ChannelBillRecord>> map = new HashMap<>();
        for (ChannelBillRecord r : records) {
            if (StringUtils.isBlank(r.getMerchantRefundNo())) {
                continue;
            }
            map.computeIfAbsent(r.getMerchantRefundNo(), k -> new ArrayList<>()).add(r);
        }
        return map;
    }

    private void reconcileLocalPayments(String channelCode,
                                        LocalDate billDate,
                                        List<PaymentOrder> paymentOrders,
                                        Map<String, List<ChannelBillRecord>> byMerchantOrder,
                                        List<ReconcileResult> diffs) {
        for (PaymentOrder order : paymentOrders) {
            if (order.getStatus() == null || !com.bluecone.app.payment.domain.enums.PaymentStatus.SUCCESS.equals(order.getStatus())) {
                continue;
            }
            String key = order.getPaymentNo() == null ? String.valueOf(order.getId()) : order.getPaymentNo();
            List<ChannelBillRecord> channel = byMerchantOrder.getOrDefault(key, List.of());
            if (channel.isEmpty()) {
                diffs.add(localOnly(channelCode, billDate, order, null));
                continue;
            }
            BigDecimal channelAmount = channel.stream()
                    .map(ChannelBillRecord::getAmount)
                    .filter(Objects::nonNull)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal localAmount = order.getPayableAmount() == null ? BigDecimal.ZERO : order.getPayableAmount();
            if (channelAmount.compareTo(localAmount) != 0) {
                diffs.add(amountMismatch(channelCode, billDate, order, channelAmount.subtract(localAmount), channel.get(0)));
            }
        }
    }

    private void reconcileLocalRefunds(String channelCode,
                                       LocalDate billDate,
                                       List<PaymentRefundOrder> refundOrders,
                                       Map<String, List<ChannelBillRecord>> byMerchantRefund,
                                       List<ReconcileResult> diffs) {
        for (PaymentRefundOrder refund : refundOrders) {
            if (!"SUCCESS".equalsIgnoreCase(refund.getStatus())) {
                continue;
            }
            String key = refund.getRefundNo();
            List<ChannelBillRecord> channel = byMerchantRefund.getOrDefault(key, List.of());
            if (channel.isEmpty()) {
                ReconcileResult result = new ReconcileResult();
                result.setChannelCode(channelCode);
                result.setBillDate(billDate);
                result.setDiffType(ReconcileDiffType.LOCAL_ONLY);
                result.setMerchantRefundNo(refund.getRefundNo());
                result.setRefundOrderId(refund.getId());
                result.setAmountDiff(refund.getRefundAmount());
                diffs.add(result);
                continue;
            }
            BigDecimal channelAmount = channel.stream()
                    .map(ChannelBillRecord::getAmount)
                    .filter(Objects::nonNull)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal localAmount = refund.getRefundAmount() == null ? BigDecimal.ZERO : refund.getRefundAmount();
            if (channelAmount.compareTo(localAmount) != 0) {
                ReconcileResult result = new ReconcileResult();
                result.setChannelCode(channelCode);
                result.setBillDate(billDate);
                result.setDiffType(ReconcileDiffType.AMOUNT_MISMATCH);
                result.setMerchantRefundNo(refund.getRefundNo());
                result.setRefundOrderId(refund.getId());
                result.setAmountDiff(channelAmount.subtract(localAmount));
                diffs.add(result);
            }
        }
    }

    private void reconcileChannelOnly(String channelCode,
                                      LocalDate billDate,
                                      List<ChannelBillRecord> channelRecords,
                                      List<PaymentOrder> paymentOrders,
                                      List<PaymentRefundOrder> refundOrders,
                                      List<ReconcileResult> diffs) {
        Set<String> localOrderNos = new HashSet<>();
        for (PaymentOrder order : paymentOrders) {
            String key = order.getPaymentNo() == null ? String.valueOf(order.getId()) : order.getPaymentNo();
            localOrderNos.add(key);
        }
        Set<String> localRefundNos = new HashSet<>();
        for (PaymentRefundOrder refund : refundOrders) {
            if (refund.getRefundNo() != null) {
                localRefundNos.add(refund.getRefundNo());
            }
        }
        for (ChannelBillRecord record : channelRecords) {
            if (StringUtils.isNotBlank(record.getMerchantOrderNo()) && localOrderNos.contains(record.getMerchantOrderNo())) {
                continue;
            }
            if (StringUtils.isNotBlank(record.getMerchantRefundNo()) && localRefundNos.contains(record.getMerchantRefundNo())) {
                continue;
            }
            ReconcileResult result = new ReconcileResult();
            result.setChannelCode(channelCode);
            result.setBillDate(billDate);
            result.setDiffType(ReconcileDiffType.CHANNEL_ONLY);
            result.setChannelTradeNo(record.getChannelTradeNo());
            result.setMerchantOrderNo(record.getMerchantOrderNo());
            result.setMerchantRefundNo(record.getMerchantRefundNo());
            result.setAmountDiff(record.getAmount());
            diffs.add(result);
        }
    }

    private ReconcileResult localOnly(String channelCode, LocalDate billDate, PaymentOrder order, ChannelBillRecord record) {
        ReconcileResult result = new ReconcileResult();
        result.setChannelCode(channelCode);
        result.setBillDate(billDate);
        result.setDiffType(ReconcileDiffType.LOCAL_ONLY);
        result.setPaymentOrderId(order.getId());
        result.setMerchantOrderNo(order.getPaymentNo());
        result.setChannelTradeNo(record == null ? null : record.getChannelTradeNo());
        result.setAmountDiff(order.getPayableAmount());
        return result;
    }

    private ReconcileResult amountMismatch(String channelCode, LocalDate billDate, PaymentOrder order, BigDecimal diff, ChannelBillRecord record) {
        ReconcileResult result = new ReconcileResult();
        result.setChannelCode(channelCode);
        result.setBillDate(billDate);
        result.setDiffType(ReconcileDiffType.AMOUNT_MISMATCH);
        result.setPaymentOrderId(order.getId());
        result.setMerchantOrderNo(order.getPaymentNo());
        result.setChannelTradeNo(record == null ? null : record.getChannelTradeNo());
        result.setAmountDiff(diff);
        return result;
    }
}
