package com.bluecone.app.ops.service;

import com.bluecone.app.infra.event.consume.EventConsumeLogMapper;
import com.bluecone.app.infra.event.consume.EventConsumeLogPO;
import com.bluecone.app.infra.event.outbox.OutboxEventMapper;
import com.bluecone.app.infra.event.outbox.OutboxEventPO;
import com.bluecone.app.member.infra.persistence.mapper.PointsLedgerMapper;
import com.bluecone.app.member.infra.persistence.po.PointsLedgerPO;
import com.bluecone.app.ops.api.dto.forensics.*;
import com.bluecone.app.ops.config.BlueconeOpsProperties;
import com.bluecone.app.ops.service.diagnosis.OrderDiagnosisEngine;
import com.bluecone.app.order.infra.persistence.mapper.OrderMapper;
import com.bluecone.app.order.infra.persistence.po.OrderPO;
import com.bluecone.app.promo.infra.persistence.mapper.CouponLockMapper;
import com.bluecone.app.promo.infra.persistence.mapper.CouponRedemptionMapper;
import com.bluecone.app.promo.infra.persistence.po.CouponLockPO;
import com.bluecone.app.promo.infra.persistence.po.CouponRedemptionPO;
import com.bluecone.app.wallet.infra.persistence.mapper.WalletFreezeMapper;
import com.bluecone.app.wallet.infra.persistence.mapper.WalletLedgerMapper;
import com.bluecone.app.wallet.infra.persistence.po.WalletFreezePO;
import com.bluecone.app.wallet.infra.persistence.po.WalletLedgerPO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 订单诊断查询服务
 * 
 * 聚合订单全链路数据，生成诊断视图
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrderForensicsQueryService {
    
    private final OrderMapper orderMapper;
    private final OutboxEventMapper outboxEventMapper;
    private final EventConsumeLogMapper consumeLogMapper;
    private final CouponLockMapper couponLockMapper;
    private final CouponRedemptionMapper couponRedemptionMapper;
    private final WalletFreezeMapper walletFreezeMapper;
    private final WalletLedgerMapper walletLedgerMapper;
    private final PointsLedgerMapper pointsLedgerMapper;
    private final OrderDiagnosisEngine diagnosisEngine;
    private final BlueconeOpsProperties opsProperties;
    
    /**
     * 查询订单诊断视图
     * 
     * @param tenantId 租户ID（必须）
     * @param orderId 订单ID
     * @return 诊断视图
     */
    public OrderForensicsView queryForensics(Long tenantId, Long orderId) {
        log.info("[OrderForensics] Query forensics: tenantId={}, orderId={}", tenantId, orderId);
        
        // 1. 查询订单基本信息
        OrderPO orderPO = orderMapper.selectById(orderId);
        if (orderPO == null) {
            throw new IllegalArgumentException("Order not found: " + orderId);
        }
        
        // 2. 验证租户隔离
        if (!tenantId.equals(orderPO.getTenantId())) {
            throw new IllegalArgumentException("Tenant mismatch: order belongs to different tenant");
        }
        
        // 3. 构建各个数据段
        OrderSummarySection orderSummary = buildOrderSummary(orderPO);
        PricingSnapshotSection pricingSnapshot = buildPricingSnapshot(tenantId, orderId);
        CouponSection couponSection = buildCouponSection(tenantId, orderId);
        WalletSection walletSection = buildWalletSection(tenantId, orderId);
        PointsSection pointsSection = buildPointsSection(tenantId, orderId);
        OutboxSection outboxSection = buildOutboxSection(tenantId, orderId);
        ConsumeSection consumeSection = buildConsumeSection(tenantId, outboxSection);
        
        // 4. 构建完整视图
        OrderForensicsView view = OrderForensicsView.builder()
            .orderSummary(orderSummary)
            .pricingSnapshot(pricingSnapshot)
            .couponSection(couponSection)
            .walletSection(walletSection)
            .pointsSection(pointsSection)
            .outboxSection(outboxSection)
            .consumeSection(consumeSection)
            .build();
        
        // 5. 执行诊断分析
        List<DiagnosisItem> diagnosis = diagnosisEngine.diagnose(view);
        view.setDiagnosis(diagnosis);
        
        log.info("[OrderForensics] Query completed: tenantId={}, orderId={}, diagnosisCount={}", 
                 tenantId, orderId, diagnosis.size());
        
        return view;
    }
    
    /**
     * 构建订单摘要信息
     */
    private OrderSummarySection buildOrderSummary(OrderPO orderPO) {
        return OrderSummarySection.builder()
            .orderId(orderPO.getId())
            .tenantId(orderPO.getTenantId())
            .storeId(orderPO.getStoreId())
            .userId(orderPO.getUserId())
            .orderNo(orderPO.getOrderNo())
            .clientOrderNo(orderPO.getClientOrderNo())
            .status(orderPO.getStatus())
            .payStatus(orderPO.getPayStatus())
            .bizType(orderPO.getBizType())
            .orderSource(orderPO.getOrderSource())
            .channel(orderPO.getChannel())
            .totalAmount(orderPO.getTotalAmount())
            .discountAmount(orderPO.getDiscountAmount())
            .payableAmount(orderPO.getPayableAmount())
            .currency(orderPO.getCurrency())
            .couponId(orderPO.getCouponId())
            .createdAt(orderPO.getCreatedAt())
            .updatedAt(orderPO.getUpdatedAt())
            .acceptedAt(orderPO.getAcceptedAt())
            .rejectedAt(orderPO.getRejectedAt())
            .closedAt(orderPO.getClosedAt())
            .closeReason(orderPO.getCloseReason())
            .rejectReasonCode(orderPO.getRejectReasonCode())
            .rejectReasonDesc(orderPO.getRejectReasonDesc())
            .remark(orderPO.getOrderRemark())
            .build();
    }
    
    /**
     * 构建计价快照信息
     * 注意：当前实现返回空快照，实际项目中需要查询 bc_order_pricing_snapshot 表
     */
    private PricingSnapshotSection buildPricingSnapshot(Long tenantId, Long orderId) {
        // TODO: 如果项目中有计价快照表，在此查询并填充数据
        // 目前返回不存在的快照
        return PricingSnapshotSection.builder()
            .exists(false)
            .build();
    }
    
    /**
     * 构建优惠券操作信息
     */
    private CouponSection buildCouponSection(Long tenantId, Long orderId) {
        int limit = getMaxItemLimit();
        
        // 查询优惠券锁定记录
        List<CouponLockPO> lockPOs = couponLockMapper.selectByOrderId(tenantId, orderId, limit);
        List<CouponLockItem> locks = lockPOs.stream()
            .map(this::convertCouponLock)
            .collect(Collectors.toList());
        
        // 查询优惠券核销记录
        List<CouponRedemptionPO> redemptionPOs = couponRedemptionMapper.selectByOrderId(tenantId, orderId, limit);
        List<CouponRedemptionItem> redemptions = redemptionPOs.stream()
            .map(this::convertCouponRedemption)
            .collect(Collectors.toList());
        
        return CouponSection.builder()
            .locks(locks)
            .redemptions(redemptions)
            .totalCount(locks.size() + redemptions.size())
            .build();
    }
    
    /**
     * 构建钱包操作信息
     */
    private WalletSection buildWalletSection(Long tenantId, Long orderId) {
        int limit = getMaxItemLimit();
        
        // 查询钱包冻结记录
        List<WalletFreezePO> freezePOs = walletFreezeMapper.selectByBizOrderIdForForensics(tenantId, orderId, limit);
        List<WalletFreezeItem> freezes = freezePOs.stream()
            .map(this::convertWalletFreeze)
            .collect(Collectors.toList());
        
        // 查询钱包流水记录
        List<WalletLedgerPO> ledgerPOs = walletLedgerMapper.selectByBizOrderIdForForensics(tenantId, orderId, limit);
        List<WalletLedgerItem> ledgers = ledgerPOs.stream()
            .map(this::convertWalletLedger)
            .collect(Collectors.toList());
        
        return WalletSection.builder()
            .freezes(freezes)
            .ledgers(ledgers)
            .totalCount(freezes.size() + ledgers.size())
            .build();
    }
    
    /**
     * 构建积分操作信息
     */
    private PointsSection buildPointsSection(Long tenantId, Long orderId) {
        int limit = getMaxItemLimit();
        
        // 查询积分流水记录（bizId 通常是订单ID的字符串形式）
        List<PointsLedgerPO> ledgerPOs = pointsLedgerMapper.selectByBizId(tenantId, String.valueOf(orderId), limit);
        List<PointsLedgerItem> ledgers = ledgerPOs.stream()
            .map(this::convertPointsLedger)
            .collect(Collectors.toList());
        
        return PointsSection.builder()
            .ledgers(ledgers)
            .totalCount(ledgers.size())
            .build();
    }
    
    /**
     * 构建 Outbox 事件信息
     */
    private OutboxSection buildOutboxSection(Long tenantId, Long orderId) {
        int limit = getMaxItemLimit();
        
        // 查询订单相关的所有 Outbox 事件
        List<OutboxEventPO> eventPOs = outboxEventMapper.selectByOrderId(tenantId, String.valueOf(orderId), limit);
        List<OutboxEventItem> events = eventPOs.stream()
            .map(this::convertOutboxEvent)
            .collect(Collectors.toList());
        
        return OutboxSection.builder()
            .events(events)
            .totalCount(events.size())
            .build();
    }
    
    /**
     * 构建消费日志信息
     */
    private ConsumeSection buildConsumeSection(Long tenantId, OutboxSection outboxSection) {
        if (outboxSection == null || outboxSection.getEvents() == null || outboxSection.getEvents().isEmpty()) {
            return ConsumeSection.builder()
                .logs(Collections.emptyList())
                .totalCount(0)
                .build();
        }
        
        int limit = getMaxItemLimit();
        
        // 收集所有事件ID
        List<String> eventIds = outboxSection.getEvents().stream()
            .map(OutboxEventItem::getEventId)
            .collect(Collectors.toList());
        
        if (eventIds.isEmpty()) {
            return ConsumeSection.builder()
                .logs(Collections.emptyList())
                .totalCount(0)
                .build();
        }
        
        // 查询这些事件的消费日志
        List<EventConsumeLogPO> logPOs = consumeLogMapper.selectByEventIds(tenantId, eventIds, limit);
        List<ConsumeLogItem> logs = logPOs.stream()
            .map(this::convertConsumeLog)
            .collect(Collectors.toList());
        
        return ConsumeSection.builder()
            .logs(logs)
            .totalCount(logs.size())
            .build();
    }
    
    // ========== 转换方法 ==========
    
    private CouponLockItem convertCouponLock(CouponLockPO po) {
        return CouponLockItem.builder()
            .id(po.getId())
            .couponId(po.getCouponId())
            .userId(po.getUserId())
            .orderId(po.getOrderId())
            .idempotencyKey(po.getIdempotencyKey())
            .lockStatus(po.getLockStatus())
            .lockTime(po.getLockTime())
            .releaseTime(po.getReleaseTime())
            .commitTime(po.getCommitTime())
            .expireTime(po.getExpireTime())
            .createdAt(po.getCreatedAt())
            .build();
    }
    
    private CouponRedemptionItem convertCouponRedemption(CouponRedemptionPO po) {
        return CouponRedemptionItem.builder()
            .id(po.getId())
            .couponId(po.getCouponId())
            .templateId(po.getTemplateId())
            .userId(po.getUserId())
            .orderId(po.getOrderId())
            .idempotencyKey(po.getIdempotencyKey())
            .originalAmount(po.getOriginalAmount())
            .discountAmount(po.getDiscountAmount())
            .finalAmount(po.getFinalAmount())
            .redemptionTime(po.getRedemptionTime())
            .createdAt(po.getCreatedAt())
            .build();
    }
    
    private WalletFreezeItem convertWalletFreeze(WalletFreezePO po) {
        return WalletFreezeItem.builder()
            .id(po.getId())
            .userId(po.getUserId())
            .accountId(po.getAccountId())
            .freezeNo(po.getFreezeNo())
            .bizType(po.getBizType())
            .bizOrderId(po.getBizOrderId())
            .bizOrderNo(po.getBizOrderNo())
            .frozenAmount(po.getFrozenAmount())
            .currency(po.getCurrency())
            .status(po.getStatus())
            .idemKey(po.getIdemKey())
            .frozenAt(po.getFrozenAt())
            .expiresAt(po.getExpiresAt())
            .committedAt(po.getCommittedAt())
            .releasedAt(po.getReleasedAt())
            .revertedAt(po.getRevertedAt())
            .createdAt(po.getCreatedAt())
            .build();
    }
    
    private WalletLedgerItem convertWalletLedger(WalletLedgerPO po) {
        return WalletLedgerItem.builder()
            .id(po.getId())
            .userId(po.getUserId())
            .accountId(po.getAccountId())
            .ledgerNo(po.getLedgerNo())
            .bizType(po.getBizType())
            .bizOrderId(po.getBizOrderId())
            .bizOrderNo(po.getBizOrderNo())
            .amount(po.getAmount())
            .balanceBefore(po.getBalanceBefore())
            .balanceAfter(po.getBalanceAfter())
            .currency(po.getCurrency())
            .remark(po.getRemark())
            .idemKey(po.getIdemKey())
            .createdAt(po.getCreatedAt())
            .build();
    }
    
    private PointsLedgerItem convertPointsLedger(PointsLedgerPO po) {
        return PointsLedgerItem.builder()
            .id(po.getId())
            .memberId(po.getMemberId())
            .direction(po.getDirection())
            .deltaPoints(po.getDeltaPoints())
            .beforeAvailable(po.getBeforeAvailable())
            .beforeFrozen(po.getBeforeFrozen())
            .afterAvailable(po.getAfterAvailable())
            .afterFrozen(po.getAfterFrozen())
            .bizType(po.getBizType())
            .bizId(po.getBizId())
            .idempotencyKey(po.getIdempotencyKey())
            .remark(po.getRemark())
            .createdAt(po.getCreatedAt())
            .build();
    }
    
    private OutboxEventItem convertOutboxEvent(OutboxEventPO po) {
        OutboxEventItem.OutboxEventItemBuilder builder = OutboxEventItem.builder()
            .id(po.getId())
            .aggregateType(po.getAggregateType())
            .aggregateId(po.getAggregateId())
            .eventType(po.getEventType())
            .eventId(po.getEventId())
            .status(po.getStatus())
            .retryCount(po.getRetryCount())
            .maxRetryCount(po.getMaxRetryCount())
            .nextRetryAt(po.getNextRetryAt())
            .lastError(truncateString(po.getLastError(), opsProperties.getMaxErrorMsgLen()))
            .createdAt(po.getCreatedAt())
            .updatedAt(po.getUpdatedAt())
            .sentAt(po.getSentAt());
        
        // 根据配置决定是否暴露载荷
        if (opsProperties.isExposePayload()) {
            builder.eventPayload(truncateString(po.getEventPayload(), opsProperties.getMaxPayloadLen()));
            builder.eventMetadata(truncateString(po.getEventMetadata(), opsProperties.getMaxPayloadLen()));
        }
        
        return builder.build();
    }
    
    private ConsumeLogItem convertConsumeLog(EventConsumeLogPO po) {
        return ConsumeLogItem.builder()
            .id(po.getId())
            .consumerName(po.getConsumerName())
            .eventId(po.getEventId())
            .eventType(po.getEventType())
            .status(po.getStatus())
            .idempotencyKey(po.getIdempotencyKey())
            .consumeResult(truncateString(po.getConsumeResult(), opsProperties.getMaxPayloadLen()))
            .errorMessage(truncateString(po.getErrorMessage(), opsProperties.getMaxErrorMsgLen()))
            .consumedAt(po.getConsumedAt())
            .createdAt(po.getCreatedAt())
            .build();
    }
    
    // ========== 辅助方法 ==========
    
    private int getMaxItemLimit() {
        // 默认每个 section 最多返回 50 条记录
        return Math.min(opsProperties.getMaxPageSize(), 50);
    }
    
    private String truncateString(String str, int maxLen) {
        if (str == null) {
            return null;
        }
        if (str.length() <= maxLen) {
            return str;
        }
        return str.substring(0, maxLen) + "...";
    }
}
