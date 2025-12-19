package com.bluecone.app.billing.application;

import com.bluecone.app.billing.dao.entity.BillingInvoiceDO;
import com.bluecone.app.billing.dao.entity.PlanSkuDO;
import com.bluecone.app.billing.dao.mapper.BillingInvoiceMapper;
import com.bluecone.app.billing.dao.mapper.PlanSkuMapper;
import com.bluecone.app.billing.domain.enums.InvoiceStatus;
import com.bluecone.app.billing.domain.service.BillingDomainService;
import com.bluecone.app.billing.event.InvoicePaidEvent;
import com.bluecone.app.core.event.DomainEventPublisher;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * 订阅计费支付回调服务
 * 处理订阅账单的支付回调，更新账单状态并写入 Outbox
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BillingPaymentCallbackService {
    
    private final BillingDomainService billingDomainService;
    private final BillingInvoiceMapper invoiceMapper;
    private final PlanSkuMapper planSkuMapper;
    private final DomainEventPublisher domainEventPublisher;
    
    /**
     * 处理订阅账单支付成功回调
     * 
     * @param invoiceId 账单ID
     * @param channelTradeNo 渠道交易号（微信/支付宝交易号）
     * @param paidAmountFen 实付金额（分）
     * @param paidAt 支付时间
     */
    @Transactional
    public void handleInvoicePaid(Long invoiceId, String channelTradeNo, Long paidAmountFen, LocalDateTime paidAt) {
        log.info("[billing-callback] 处理订阅账单支付回调，invoiceId={}, channelTradeNo={}, paidAmountFen={}", 
                invoiceId, channelTradeNo, paidAmountFen);
        
        // 查询账单
        BillingInvoiceDO invoice = invoiceMapper.selectById(invoiceId);
        if (invoice == null) {
            log.warn("[billing-callback] 账单不存在，invoiceId={}", invoiceId);
            return;
        }
        
        // 幂等性检查
        if (InvoiceStatus.PAID.getCode().equals(invoice.getStatus())) {
            log.info("[billing-callback] 账单已支付，忽略重复通知，invoiceId={}, channelTradeNo={}", invoiceId, channelTradeNo);
            return;
        }
        
        // 标记账单为已支付
        invoice = billingDomainService.markInvoiceAsPaid(invoiceId, channelTradeNo, paidAmountFen, paidAt);
        
        // 发布领域事件（同事务）
        InvoicePaidEvent event = new InvoicePaidEvent(
                invoice.getId(),
                invoice.getInvoiceNo(),
                invoice.getTenantId(),
                invoice.getPlanSkuId(),
                invoice.getPlanCode(),
                invoice.getPlanName(),
                invoice.getBillingPeriod(),
                invoice.getPeriodMonths(),
                invoice.getPaidAmountFen(),
                invoice.getPaidAt(),
                invoice.getEffectiveStartAt(),
                invoice.getEffectiveEndAt(),
                MDC.get("traceId")
        );
        
        domainEventPublisher.publish(event);
        
        log.info("[billing-callback] 账单支付回调处理完成，invoiceId={}, eventId={}", invoiceId, event.getEventId());
    }
    
    /**
     * 根据渠道交易号查询账单并处理支付回调
     * 用于微信/支付宝支付回调，通过 channelTradeNo 查找账单
     */
    @Transactional
    public void handleInvoicePaidByChannelTradeNo(String channelTradeNo, Long paidAmountFen, LocalDateTime paidAt) {
        log.info("[billing-callback] 根据渠道交易号处理订阅账单支付回调，channelTradeNo={}, paidAmountFen={}", 
                channelTradeNo, paidAmountFen);
        
        // 根据 channelTradeNo 查询账单
        BillingInvoiceDO invoice = invoiceMapper.selectOne(
            new LambdaQueryWrapper<BillingInvoiceDO>()
                .eq(BillingInvoiceDO::getChannelTradeNo, channelTradeNo)
        );
        
        if (invoice != null) {
            handleInvoicePaid(invoice.getId(), channelTradeNo, paidAmountFen, paidAt);
        } else {
            log.warn("[billing-callback] 未找到对应的订阅账单，channelTradeNo={}", channelTradeNo);
        }
    }
    
    /**
     * 判断是否为订阅账单支付（根据 outTradeNo 或 attach 字段）
     */
    public boolean isSubscriptionInvoicePayment(String outTradeNo, String attach) {
        // 方案1: 通过 outTradeNo 前缀判断（如 INV 开头）
        if (outTradeNo != null && outTradeNo.startsWith("INV")) {
            return true;
        }
        
        // 方案2: 通过 attach 字段判断（如包含 "subscription" 或 "invoice"）
        if (attach != null && (attach.contains("subscription") || attach.contains("invoice"))) {
            return true;
        }
        
        return false;
    }
    
    /**
     * 从 outTradeNo 解析账单ID
     */
    public Long parseInvoiceIdFromOutTradeNo(String outTradeNo) {
        // 假设 outTradeNo 格式为：INV{timestamp}{tenantId}
        // 或者直接使用账单ID作为 outTradeNo
        try {
            // 如果 outTradeNo 是纯数字，直接解析为账单ID
            return Long.valueOf(outTradeNo);
        } catch (NumberFormatException e) {
            log.warn("[billing-callback] 无法从 outTradeNo 解析账单ID，outTradeNo={}", outTradeNo);
            return null;
        }
    }
}
