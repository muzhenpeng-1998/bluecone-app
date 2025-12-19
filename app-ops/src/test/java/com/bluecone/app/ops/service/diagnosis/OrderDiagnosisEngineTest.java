package com.bluecone.app.ops.service.diagnosis;

import com.bluecone.app.ops.api.dto.forensics.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 订单诊断引擎单元测试
 */
class OrderDiagnosisEngineTest {
    
    private OrderDiagnosisEngine diagnosisEngine;
    
    @BeforeEach
    void setUp() {
        diagnosisEngine = new OrderDiagnosisEngine();
    }
    
    @Test
    void testDiagnose_NullView_ReturnsEmptyList() {
        List<DiagnosisItem> result = diagnosisEngine.diagnose(null);
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }
    
    @Test
    void testDiagnose_NullOrderSummary_ReturnsEmptyList() {
        OrderForensicsView view = OrderForensicsView.builder().build();
        List<DiagnosisItem> result = diagnosisEngine.diagnose(view);
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }
    
    @Test
    void testCheckMissingWalletCommit_PaidOrderWithoutLedger_ReturnsError() {
        // 构造已支付订单
        OrderSummarySection order = OrderSummarySection.builder()
            .orderId(123L)
            .status("PAID")
            .payStatus("PAY_SUCCESS")
            .payableAmount(new BigDecimal("100.00"))
            .build();
        
        // 没有钱包流水
        WalletSection walletSection = WalletSection.builder()
            .ledgers(Collections.emptyList())
            .build();
        
        OrderForensicsView view = OrderForensicsView.builder()
            .orderSummary(order)
            .walletSection(walletSection)
            .build();
        
        List<DiagnosisItem> result = diagnosisEngine.diagnose(view);
        
        assertFalse(result.isEmpty());
        DiagnosisItem diagnosis = result.stream()
            .filter(d -> "MISSING_WALLET_COMMIT".equals(d.getCode()))
            .findFirst()
            .orElse(null);
        
        assertNotNull(diagnosis);
        assertEquals("ERROR", diagnosis.getSeverity());
        assertTrue(diagnosis.getMessage().contains("钱包"));
    }
    
    @Test
    void testCheckMissingWalletCommit_PaidOrderWithCommitLedger_NoError() {
        // 构造已支付订单
        OrderSummarySection order = OrderSummarySection.builder()
            .orderId(123L)
            .status("PAID")
            .payStatus("PAY_SUCCESS")
            .payableAmount(new BigDecimal("100.00"))
            .build();
        
        // 有钱包提交流水
        WalletLedgerItem ledger = WalletLedgerItem.builder()
            .id(1L)
            .remark("订单支付扣款 - commit")
            .build();
        
        WalletSection walletSection = WalletSection.builder()
            .ledgers(List.of(ledger))
            .build();
        
        OrderForensicsView view = OrderForensicsView.builder()
            .orderSummary(order)
            .walletSection(walletSection)
            .build();
        
        List<DiagnosisItem> result = diagnosisEngine.diagnose(view);
        
        // 不应该有 MISSING_WALLET_COMMIT 错误
        boolean hasMissingWalletCommit = result.stream()
            .anyMatch(d -> "MISSING_WALLET_COMMIT".equals(d.getCode()));
        
        assertFalse(hasMissingWalletCommit);
    }
    
    @Test
    void testCheckMissingCouponRedemption_PaidOrderWithCouponButNoRedemption_ReturnsError() {
        // 构造已支付订单，使用了优惠券
        OrderSummarySection order = OrderSummarySection.builder()
            .orderId(123L)
            .status("PAID")
            .payStatus("PAY_SUCCESS")
            .couponId(456L)
            .build();
        
        // 有锁定记录，但没有核销记录
        CouponLockItem lock = CouponLockItem.builder()
            .id(1L)
            .couponId(456L)
            .lockStatus("LOCKED")
            .build();
        
        CouponSection couponSection = CouponSection.builder()
            .locks(List.of(lock))
            .redemptions(Collections.emptyList())
            .build();
        
        OrderForensicsView view = OrderForensicsView.builder()
            .orderSummary(order)
            .couponSection(couponSection)
            .build();
        
        List<DiagnosisItem> result = diagnosisEngine.diagnose(view);
        
        DiagnosisItem diagnosis = result.stream()
            .filter(d -> "MISSING_COUPON_REDEMPTION".equals(d.getCode()))
            .findFirst()
            .orElse(null);
        
        assertNotNull(diagnosis);
        assertEquals("ERROR", diagnosis.getSeverity());
        assertTrue(diagnosis.getMessage().contains("优惠券"));
    }
    
    @Test
    void testCheckOutboxDeliveryFailed_FailedEvent_ReturnsWarning() {
        OrderSummarySection order = OrderSummarySection.builder()
            .orderId(123L)
            .build();
        
        OutboxEventItem failedEvent = OutboxEventItem.builder()
            .id(1L)
            .eventId("evt-123")
            .eventType("order.paid")
            .status("FAILED")
            .retryCount(3)
            .maxRetryCount(10)
            .lastError("Connection timeout")
            .build();
        
        OutboxSection outboxSection = OutboxSection.builder()
            .events(List.of(failedEvent))
            .build();
        
        OrderForensicsView view = OrderForensicsView.builder()
            .orderSummary(order)
            .outboxSection(outboxSection)
            .build();
        
        List<DiagnosisItem> result = diagnosisEngine.diagnose(view);
        
        DiagnosisItem diagnosis = result.stream()
            .filter(d -> "OUTBOX_DELIVERY_FAILED".equals(d.getCode()))
            .findFirst()
            .orElse(null);
        
        assertNotNull(diagnosis);
        assertEquals("WARNING", diagnosis.getSeverity());
    }
    
    @Test
    void testCheckOutboxDeliveryFailed_DeadEvent_ReturnsError() {
        OrderSummarySection order = OrderSummarySection.builder()
            .orderId(123L)
            .build();
        
        OutboxEventItem deadEvent = OutboxEventItem.builder()
            .id(1L)
            .eventId("evt-123")
            .eventType("order.paid")
            .status("DEAD")
            .retryCount(10)
            .maxRetryCount(10)
            .lastError("Max retries exceeded")
            .build();
        
        OutboxSection outboxSection = OutboxSection.builder()
            .events(List.of(deadEvent))
            .build();
        
        OrderForensicsView view = OrderForensicsView.builder()
            .orderSummary(order)
            .outboxSection(outboxSection)
            .build();
        
        List<DiagnosisItem> result = diagnosisEngine.diagnose(view);
        
        DiagnosisItem diagnosis = result.stream()
            .filter(d -> "OUTBOX_DELIVERY_FAILED".equals(d.getCode()))
            .findFirst()
            .orElse(null);
        
        assertNotNull(diagnosis);
        assertEquals("ERROR", diagnosis.getSeverity());
        assertTrue(diagnosis.getMessage().contains("死信"));
    }
    
    @Test
    void testCheckLockTimeout_ExpiredCouponLock_ReturnsWarning() {
        OrderSummarySection order = OrderSummarySection.builder()
            .orderId(123L)
            .build();
        
        // 已过期的锁定
        CouponLockItem expiredLock = CouponLockItem.builder()
            .id(1L)
            .couponId(456L)
            .lockStatus("LOCKED")
            .lockTime(LocalDateTime.now().minusHours(2))
            .expireTime(LocalDateTime.now().minusHours(1))
            .build();
        
        CouponSection couponSection = CouponSection.builder()
            .locks(List.of(expiredLock))
            .build();
        
        OrderForensicsView view = OrderForensicsView.builder()
            .orderSummary(order)
            .couponSection(couponSection)
            .build();
        
        List<DiagnosisItem> result = diagnosisEngine.diagnose(view);
        
        DiagnosisItem diagnosis = result.stream()
            .filter(d -> "COUPON_LOCK_TIMEOUT".equals(d.getCode()))
            .findFirst()
            .orElse(null);
        
        assertNotNull(diagnosis);
        assertEquals("WARNING", diagnosis.getSeverity());
        assertTrue(diagnosis.getMessage().contains("超时"));
    }
    
    @Test
    void testCheckLockTimeout_ExpiredWalletFreeze_ReturnsWarning() {
        OrderSummarySection order = OrderSummarySection.builder()
            .orderId(123L)
            .build();
        
        // 已过期的冻结
        WalletFreezeItem expiredFreeze = WalletFreezeItem.builder()
            .id(1L)
            .freezeNo("FRZ-123")
            .status("FROZEN")
            .frozenAt(LocalDateTime.now().minusHours(2))
            .expiresAt(LocalDateTime.now().minusHours(1))
            .frozenAmount(new BigDecimal("100.00"))
            .build();
        
        WalletSection walletSection = WalletSection.builder()
            .freezes(List.of(expiredFreeze))
            .build();
        
        OrderForensicsView view = OrderForensicsView.builder()
            .orderSummary(order)
            .walletSection(walletSection)
            .build();
        
        List<DiagnosisItem> result = diagnosisEngine.diagnose(view);
        
        DiagnosisItem diagnosis = result.stream()
            .filter(d -> "WALLET_FREEZE_TIMEOUT".equals(d.getCode()))
            .findFirst()
            .orElse(null);
        
        assertNotNull(diagnosis);
        assertEquals("WARNING", diagnosis.getSeverity());
        assertTrue(diagnosis.getMessage().contains("冻结"));
    }
    
    @Test
    void testCheckDuplicateConsumption_MultipleSuccessLogs_ReturnsWarning() {
        OrderSummarySection order = OrderSummarySection.builder()
            .orderId(123L)
            .build();
        
        // 同一个消费者对同一个事件有多条成功记录
        ConsumeLogItem log1 = ConsumeLogItem.builder()
            .id(1L)
            .consumerName("WalletConsumer")
            .eventId("evt-123")
            .eventType("order.paid")
            .status("SUCCESS")
            .build();
        
        ConsumeLogItem log2 = ConsumeLogItem.builder()
            .id(2L)
            .consumerName("WalletConsumer")
            .eventId("evt-123")
            .eventType("order.paid")
            .status("SUCCESS")
            .build();
        
        ConsumeSection consumeSection = ConsumeSection.builder()
            .logs(List.of(log1, log2))
            .build();
        
        OrderForensicsView view = OrderForensicsView.builder()
            .orderSummary(order)
            .consumeSection(consumeSection)
            .build();
        
        List<DiagnosisItem> result = diagnosisEngine.diagnose(view);
        
        DiagnosisItem diagnosis = result.stream()
            .filter(d -> "DUPLICATE_CONSUMPTION".equals(d.getCode()))
            .findFirst()
            .orElse(null);
        
        assertNotNull(diagnosis);
        assertEquals("WARNING", diagnosis.getSeverity());
        assertTrue(diagnosis.getMessage().contains("重复消费"));
    }
    
    @Test
    void testCheckInconsistentAmounts_DifferentAmounts_ReturnsWarning() {
        OrderSummarySection order = OrderSummarySection.builder()
            .orderId(123L)
            .payableAmount(new BigDecimal("100.00"))
            .build();
        
        PricingSnapshotSection pricing = PricingSnapshotSection.builder()
            .exists(true)
            .payableAmount(new BigDecimal("95.00"))
            .build();
        
        OrderForensicsView view = OrderForensicsView.builder()
            .orderSummary(order)
            .pricingSnapshot(pricing)
            .build();
        
        List<DiagnosisItem> result = diagnosisEngine.diagnose(view);
        
        DiagnosisItem diagnosis = result.stream()
            .filter(d -> "AMOUNT_INCONSISTENT".equals(d.getCode()))
            .findFirst()
            .orElse(null);
        
        assertNotNull(diagnosis);
        assertEquals("WARNING", diagnosis.getSeverity());
        assertTrue(diagnosis.getMessage().contains("不一致"));
    }
}
