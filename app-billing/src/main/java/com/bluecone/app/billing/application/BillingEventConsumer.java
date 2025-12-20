package com.bluecone.app.billing.application;

import com.bluecone.app.billing.dao.entity.BillingInvoiceDO;
import com.bluecone.app.billing.dao.entity.PlanSkuDO;
import com.bluecone.app.billing.dao.entity.TenantSubscriptionDO;
import com.bluecone.app.billing.dao.mapper.BillingInvoiceMapper;
import com.bluecone.app.billing.dao.mapper.PlanSkuMapper;
import com.bluecone.app.billing.domain.service.BillingDomainService;
import com.bluecone.app.core.event.outbox.EventType;
import com.bluecone.app.infra.event.outbox.handler.OutboxEventHandler;
import com.bluecone.app.infra.event.outbox.OutboxEventDO;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 订阅计费事件消费者
 * 消费 INVOICE_PAID 事件，更新租户订阅状态
 */
@Slf4j
@Component
public class BillingEventConsumer implements OutboxEventHandler {
    
    private final BillingDomainService billingDomainService;
    private final BillingInvoiceMapper invoiceMapper;
    private final PlanSkuMapper planSkuMapper;
    private final ObjectMapper objectMapper;

    public BillingEventConsumer(BillingDomainService billingDomainService,
                               BillingInvoiceMapper invoiceMapper,
                               PlanSkuMapper planSkuMapper,
                               @Qualifier("redisObjectMapper") ObjectMapper objectMapper) {
        this.billingDomainService = billingDomainService;
        this.invoiceMapper = invoiceMapper;
        this.planSkuMapper = planSkuMapper;
        this.objectMapper = objectMapper;
    }
    
    @Override
    public boolean supports(OutboxEventDO event) {
        return EventType.INVOICE_PAID.getCode().equals(event.getEventType());
    }
    
    @Override
    public void handle(OutboxEventDO event) throws Exception {
        log.info("[billing-consumer] 消费 INVOICE_PAID 事件，eventId={}, aggregateId={}", 
                event.getId(), event.getAggregateId());
        
        try {
            // 解析事件载荷
            Map<String, Object> payload = objectMapper.readValue(event.getEventBody(), Map.class);
            Long invoiceId = getLongValue(payload, "invoiceId");
            Long tenantId = getLongValue(payload, "tenantId");
            Long planSkuId = getLongValue(payload, "planSkuId");
            
            if (invoiceId == null || tenantId == null || planSkuId == null) {
                log.error("[billing-consumer] 事件载荷缺少必要字段，eventId={}, payload={}", event.getId(), payload);
                return;
            }
            
            // 查询账单和套餐
            BillingInvoiceDO invoice = invoiceMapper.selectById(invoiceId);
            if (invoice == null) {
                log.error("[billing-consumer] 账单不存在，invoiceId={}", invoiceId);
                return;
            }
            
            PlanSkuDO planSku = planSkuMapper.selectById(planSkuId);
            if (planSku == null) {
                log.error("[billing-consumer] 套餐 SKU 不存在，planSkuId={}", planSkuId);
                return;
            }
            
            // 激活或更新订阅
            TenantSubscriptionDO subscription = billingDomainService.activateSubscription(tenantId, invoice, planSku);
            
            log.info("[billing-consumer] 订阅激活成功，tenantId={}, planCode={}, subscriptionId={}, endAt={}", 
                    tenantId, planSku.getPlanCode(), subscription.getId(), subscription.getSubscriptionEndAt());
            
        } catch (Exception e) {
            log.error("[billing-consumer] 消费 INVOICE_PAID 事件失败，eventId={}", event.getId(), e);
            throw e;
        }
    }
    
    private Long getLongValue(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value == null) {
            return null;
        }
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        try {
            return Long.valueOf(value.toString());
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
