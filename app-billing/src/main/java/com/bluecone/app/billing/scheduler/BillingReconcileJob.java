package com.bluecone.app.billing.scheduler;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.bluecone.app.billing.dao.entity.BillingInvoiceDO;
import com.bluecone.app.billing.dao.entity.TenantSubscriptionDO;
import com.bluecone.app.billing.dao.mapper.BillingInvoiceMapper;
import com.bluecone.app.billing.dao.mapper.TenantSubscriptionMapper;
import com.bluecone.app.billing.domain.enums.InvoiceStatus;
import com.bluecone.app.billing.event.InvoicePaidEvent;
import com.bluecone.app.core.event.DomainEventPublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 订阅计费对账补偿任务
 * 定时扫描 invoice=PAID 但 subscription 未生效的情况，补发 Outbox 事件
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class BillingReconcileJob {
    
    private final BillingInvoiceMapper invoiceMapper;
    private final TenantSubscriptionMapper subscriptionMapper;
    private final DomainEventPublisher domainEventPublisher;
    
    /**
     * 每30分钟执行一次，扫描需要补偿的账单
     */
    @Scheduled(cron = "0 */30 * * * ?")
    public void reconcileBilling() {
        log.info("[billing-reconcile-job] 开始扫描需要补偿的订阅账单");
        
        try {
            // 查询已支付但可能未生效的账单（最近24小时内支付的）
            LocalDateTime oneDayAgo = LocalDateTime.now().minusDays(1);
            List<BillingInvoiceDO> paidInvoices = invoiceMapper.selectList(
                new LambdaQueryWrapper<BillingInvoiceDO>()
                    .eq(BillingInvoiceDO::getStatus, InvoiceStatus.PAID.getCode())
                    .gt(BillingInvoiceDO::getPaidAt, oneDayAgo)
            );
            
            if (paidInvoices.isEmpty()) {
                log.info("[billing-reconcile-job] 没有需要检查的已支付账单");
                return;
            }
            
            log.info("[billing-reconcile-job] 发现 {} 个已支付账单，开始检查订阅状态", paidInvoices.size());
            
            int compensateCount = 0;
            
            for (BillingInvoiceDO invoice : paidInvoices) {
                try {
                    // 检查订阅是否已生效
                    TenantSubscriptionDO subscription = subscriptionMapper.selectOne(
                        new LambdaQueryWrapper<TenantSubscriptionDO>()
                            .eq(TenantSubscriptionDO::getTenantId, invoice.getTenantId())
                            .eq(TenantSubscriptionDO::getLastInvoiceId, invoice.getId())
                    );
                    
                    if (subscription == null) {
                        // 订阅未生效，补发领域事件
                        log.warn("[billing-reconcile-job] 发现未生效的订阅账单，补发事件，invoiceId={}, tenantId={}", 
                                invoice.getId(), invoice.getTenantId());
                        
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
                                "billing-reconcile-job"
                        );
                        
                        domainEventPublisher.publish(event);
                        compensateCount++;
                        
                        log.info("[billing-reconcile-job] 补发事件成功，invoiceId={}, eventId={}", 
                                invoice.getId(), event.getEventId());
                    }
                } catch (Exception e) {
                    log.error("[billing-reconcile-job] 检查账单失败，invoiceId={}", invoice.getId(), e);
                }
            }
            
            log.info("[billing-reconcile-job] 对账补偿完成，检查数={}, 补偿数={}", paidInvoices.size(), compensateCount);
            
        } catch (Exception e) {
            log.error("[billing-reconcile-job] 对账补偿任务执行失败", e);
        }
    }
}
