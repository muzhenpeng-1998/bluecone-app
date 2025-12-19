package com.bluecone.app.ops.api.dto.forensics;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 订单诊断视图 - 聚合订单全链路数据用于运维诊断
 * 
 * 包含订单基本信息、计价快照、资产操作（优惠券/钱包/积分）、
 * Outbox 事件、消费日志以及自动诊断结论
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderForensicsView {
    
    /**
     * 订单摘要信息
     */
    private OrderSummarySection orderSummary;
    
    /**
     * 计价快照（如果存在）
     */
    private PricingSnapshotSection pricingSnapshot;
    
    /**
     * 优惠券相关操作
     */
    private CouponSection couponSection;
    
    /**
     * 钱包相关操作
     */
    private WalletSection walletSection;
    
    /**
     * 积分相关操作
     */
    private PointsSection pointsSection;
    
    /**
     * Outbox 事件列表
     */
    private OutboxSection outboxSection;
    
    /**
     * 消费日志列表
     */
    private ConsumeSection consumeSection;
    
    /**
     * 诊断结论列表
     */
    private List<DiagnosisItem> diagnosis;
}
