package com.bluecone.app.ops.service.diagnosis;

import com.bluecone.app.ops.api.dto.forensics.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 订单诊断引擎
 * 
 * 实现自动诊断规则，分析订单全链路数据，识别潜在问题
 */
@Slf4j
@Component
public class OrderDiagnosisEngine {
    
    /**
     * 执行诊断分析
     * 
     * @param view 订单诊断视图
     * @return 诊断结论列表
     */
    public List<DiagnosisItem> diagnose(OrderForensicsView view) {
        List<DiagnosisItem> findings = new ArrayList<>();
        
        if (view == null || view.getOrderSummary() == null) {
            return findings;
        }
        
        // Rule 1: Paid but missing wallet commit ledger
        checkMissingWalletCommit(view, findings);
        
        // Rule 2: Paid but missing coupon redemption
        checkMissingCouponRedemption(view, findings);
        
        // Rule 3: Outbox failed over threshold
        checkOutboxDeliveryFailed(view, findings);
        
        // Rule 4: Lock/freeze timeout exists
        checkLockTimeout(view, findings);
        
        // Rule 5: Repeated event idempotent hits
        checkDuplicateConsumption(view, findings);
        
        // Additional checks
        checkInconsistentAmounts(view, findings);
        
        return findings;
    }
    
    /**
     * Rule 1: 检查已支付订单是否缺失钱包提交流水
     */
    private void checkMissingWalletCommit(OrderForensicsView view, List<DiagnosisItem> findings) {
        OrderSummarySection order = view.getOrderSummary();
        
        // 只检查已支付且应付金额大于0的订单
        if (!"PAID".equals(order.getStatus()) && !"PAY_SUCCESS".equals(order.getPayStatus())) {
            return;
        }
        
        if (order.getPayableAmount() == null || order.getPayableAmount().compareTo(BigDecimal.ZERO) <= 0) {
            return;
        }
        
        WalletSection walletSection = view.getWalletSection();
        if (walletSection == null || walletSection.getLedgers() == null) {
            findings.add(createDiagnosis(
                "MISSING_WALLET_COMMIT",
                "ERROR",
                "订单已支付，但未找到钱包扣款流水",
                "检查 WalletEventConsumer 是否正常消费 order.paid 事件，查看消费日志中是否有失败记录",
                Map.of(
                    "orderId", order.getOrderId(),
                    "payableAmount", order.getPayableAmount(),
                    "orderStatus", order.getStatus(),
                    "payStatus", order.getPayStatus()
                )
            ));
            return;
        }
        
        // 检查是否存在 commit 相关的流水
        boolean hasCommitLedger = walletSection.getLedgers().stream()
            .anyMatch(ledger -> ledger.getRemark() != null && 
                               (ledger.getRemark().contains("commit") || 
                                ledger.getRemark().contains("提交") ||
                                ledger.getRemark().contains("扣款")));
        
        if (!hasCommitLedger) {
            findings.add(createDiagnosis(
                "MISSING_WALLET_COMMIT",
                "ERROR",
                "订单已支付，但未找到钱包提交（commit）流水记录",
                "检查 WalletEventConsumer 消费日志，确认 order.paid 事件是否被正确处理",
                Map.of(
                    "orderId", order.getOrderId(),
                    "payableAmount", order.getPayableAmount(),
                    "walletLedgerCount", walletSection.getLedgers().size()
                )
            ));
        }
    }
    
    /**
     * Rule 2: 检查已支付订单是否缺失优惠券核销记录
     */
    private void checkMissingCouponRedemption(OrderForensicsView view, List<DiagnosisItem> findings) {
        OrderSummarySection order = view.getOrderSummary();
        
        // 只检查已支付且使用了优惠券的订单
        if (!"PAID".equals(order.getStatus()) && !"PAY_SUCCESS".equals(order.getPayStatus())) {
            return;
        }
        
        if (order.getCouponId() == null) {
            return;
        }
        
        CouponSection couponSection = view.getCouponSection();
        if (couponSection == null) {
            findings.add(createDiagnosis(
                "MISSING_COUPON_REDEMPTION",
                "ERROR",
                "订单已支付且使用了优惠券，但未找到优惠券核销记录",
                "检查 CouponEventConsumer 是否正常消费 order.paid 事件，查看消费日志中是否有失败记录",
                Map.of(
                    "orderId", order.getOrderId(),
                    "couponId", order.getCouponId(),
                    "orderStatus", order.getStatus()
                )
            ));
            return;
        }
        
        // 检查是否存在锁定记录
        boolean hasLock = couponSection.getLocks() != null && !couponSection.getLocks().isEmpty();
        
        // 检查是否存在核销记录
        boolean hasRedemption = couponSection.getRedemptions() != null && !couponSection.getRedemptions().isEmpty();
        
        if (hasLock && !hasRedemption) {
            findings.add(createDiagnosis(
                "MISSING_COUPON_REDEMPTION",
                "ERROR",
                "订单已支付，优惠券已锁定，但未找到核销记录",
                "检查 CouponEventConsumer 消费日志，确认 order.paid 事件是否被正确处理",
                Map.of(
                    "orderId", order.getOrderId(),
                    "couponId", order.getCouponId(),
                    "lockCount", couponSection.getLocks().size()
                )
            ));
        }
    }
    
    /**
     * Rule 3: 检查 Outbox 事件投递失败
     */
    private void checkOutboxDeliveryFailed(OrderForensicsView view, List<DiagnosisItem> findings) {
        OutboxSection outboxSection = view.getOutboxSection();
        if (outboxSection == null || outboxSection.getEvents() == null) {
            return;
        }
        
        List<OutboxEventItem> failedEvents = outboxSection.getEvents().stream()
            .filter(event -> "FAILED".equals(event.getStatus()) || "DEAD".equals(event.getStatus()))
            .collect(Collectors.toList());
        
        if (!failedEvents.isEmpty()) {
            for (OutboxEventItem event : failedEvents) {
                String severity = "DEAD".equals(event.getStatus()) ? "ERROR" : "WARNING";
                String message = "DEAD".equals(event.getStatus()) 
                    ? String.format("Outbox 事件投递失败且已进入死信状态：%s", event.getEventType())
                    : String.format("Outbox 事件投递失败（重试中）：%s", event.getEventType());
                
                findings.add(createDiagnosis(
                    "OUTBOX_DELIVERY_FAILED",
                    severity,
                    message,
                    "检查 last_error 字段了解失败原因，考虑手动重新发布事件或修复下游服务后等待重试",
                    Map.of(
                        "eventId", event.getEventId(),
                        "eventType", event.getEventType(),
                        "status", event.getStatus(),
                        "retryCount", event.getRetryCount(),
                        "maxRetryCount", event.getMaxRetryCount(),
                        "lastError", event.getLastError() != null ? event.getLastError() : "N/A"
                    )
                ));
            }
        }
    }
    
    /**
     * Rule 4: 检查锁定/冻结超时
     */
    private void checkLockTimeout(OrderForensicsView view, List<DiagnosisItem> findings) {
        LocalDateTime now = LocalDateTime.now();
        
        // 检查优惠券锁定超时
        CouponSection couponSection = view.getCouponSection();
        if (couponSection != null && couponSection.getLocks() != null) {
            for (CouponLockItem lock : couponSection.getLocks()) {
                if ("LOCKED".equals(lock.getLockStatus()) && 
                    lock.getExpireTime() != null && 
                    lock.getExpireTime().isBefore(now)) {
                    
                    findings.add(createDiagnosis(
                        "COUPON_LOCK_TIMEOUT",
                        "WARNING",
                        String.format("优惠券锁定已超时：couponId=%d", lock.getCouponId()),
                        "锁定应该被 LockTimeoutReaperJob 自动释放，检查该定时任务是否正常运行",
                        Map.of(
                            "couponId", lock.getCouponId(),
                            "lockTime", lock.getLockTime(),
                            "expireTime", lock.getExpireTime(),
                            "lockStatus", lock.getLockStatus()
                        )
                    ));
                }
            }
        }
        
        // 检查钱包冻结超时
        WalletSection walletSection = view.getWalletSection();
        if (walletSection != null && walletSection.getFreezes() != null) {
            for (WalletFreezeItem freeze : walletSection.getFreezes()) {
                if ("FROZEN".equals(freeze.getStatus()) && 
                    freeze.getExpiresAt() != null && 
                    freeze.getExpiresAt().isBefore(now)) {
                    
                    findings.add(createDiagnosis(
                        "WALLET_FREEZE_TIMEOUT",
                        "WARNING",
                        String.format("钱包冻结已超时：freezeNo=%s", freeze.getFreezeNo()),
                        "冻结应该被 LockTimeoutReaperJob 自动释放，检查该定时任务是否正常运行",
                        Map.of(
                            "freezeNo", freeze.getFreezeNo(),
                            "frozenAmount", freeze.getFrozenAmount(),
                            "frozenAt", freeze.getFrozenAt(),
                            "expiresAt", freeze.getExpiresAt(),
                            "status", freeze.getStatus()
                        )
                    ));
                }
            }
        }
    }
    
    /**
     * Rule 5: 检查重复消费（幂等性冲突）
     */
    private void checkDuplicateConsumption(OrderForensicsView view, List<DiagnosisItem> findings) {
        ConsumeSection consumeSection = view.getConsumeSection();
        if (consumeSection == null || consumeSection.getLogs() == null) {
            return;
        }
        
        // 按 consumerName + eventId 分组，检查是否有重复消费
        Map<String, List<ConsumeLogItem>> groupedLogs = consumeSection.getLogs().stream()
            .collect(Collectors.groupingBy(log -> log.getConsumerName() + ":" + log.getEventId()));
        
        for (Map.Entry<String, List<ConsumeLogItem>> entry : groupedLogs.entrySet()) {
            List<ConsumeLogItem> logs = entry.getValue();
            
            // 检查是否有多条成功记录
            long successCount = logs.stream()
                .filter(log -> "SUCCESS".equals(log.getStatus()))
                .count();
            
            if (successCount > 1) {
                ConsumeLogItem firstLog = logs.get(0);
                findings.add(createDiagnosis(
                    "DUPLICATE_CONSUMPTION",
                    "WARNING",
                    String.format("检测到重复消费：%s 消费事件 %s 有 %d 条成功记录", 
                                  firstLog.getConsumerName(), firstLog.getEventType(), successCount),
                    "检查消费者的幂等性实现，确保 consumeLogService.isConsumed() 被正确调用",
                    Map.of(
                        "consumerName", firstLog.getConsumerName(),
                        "eventId", firstLog.getEventId(),
                        "eventType", firstLog.getEventType(),
                        "successCount", successCount
                    )
                ));
            }
        }
    }
    
    /**
     * 额外检查：金额不一致
     */
    private void checkInconsistentAmounts(OrderForensicsView view, List<DiagnosisItem> findings) {
        OrderSummarySection order = view.getOrderSummary();
        PricingSnapshotSection pricing = view.getPricingSnapshot();
        
        if (pricing == null || !Boolean.TRUE.equals(pricing.getExists())) {
            return;
        }
        
        // 检查订单应付金额与计价快照是否一致
        if (order.getPayableAmount() != null && pricing.getPayableAmount() != null) {
            BigDecimal diff = order.getPayableAmount().subtract(pricing.getPayableAmount()).abs();
            if (diff.compareTo(new BigDecimal("0.01")) > 0) {
                findings.add(createDiagnosis(
                    "AMOUNT_INCONSISTENT",
                    "WARNING",
                    String.format("订单应付金额与计价快照不一致：订单=%s, 快照=%s", 
                                  order.getPayableAmount(), pricing.getPayableAmount()),
                    "检查订单提交流程，确认计价快照是否在订单创建后被修改",
                    Map.of(
                        "orderPayableAmount", order.getPayableAmount(),
                        "pricingPayableAmount", pricing.getPayableAmount(),
                        "difference", diff
                    )
                ));
            }
        }
    }
    
    /**
     * 创建诊断结论
     */
    private DiagnosisItem createDiagnosis(String code, String severity, String message, 
                                          String suggestedAction, Map<String, Object> context) {
        return DiagnosisItem.builder()
            .code(code)
            .severity(severity)
            .message(message)
            .suggestedAction(suggestedAction)
            .context(context)
            .build();
    }
}
