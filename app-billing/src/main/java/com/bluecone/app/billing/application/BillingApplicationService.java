package com.bluecone.app.billing.application;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.bluecone.app.billing.api.dto.*;
import com.bluecone.app.billing.dao.entity.BillingInvoiceDO;
import com.bluecone.app.billing.dao.entity.PlanSkuDO;
import com.bluecone.app.billing.dao.entity.TenantSubscriptionDO;
import com.bluecone.app.billing.dao.mapper.BillingInvoiceMapper;
import com.bluecone.app.billing.domain.service.BillingDomainService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 订阅计费应用服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BillingApplicationService {
    
    private final BillingDomainService billingDomainService;
    private final BillingInvoiceMapper invoiceMapper;
    private final ObjectMapper objectMapper;
    
    /**
     * 获取所有可用的套餐 SKU
     */
    public List<PlanSkuDTO> listPlans() {
        List<PlanSkuDO> planSkus = billingDomainService.listActivePlanSkus();
        return planSkus.stream()
                .map(this::toPlanSkuDTO)
                .collect(Collectors.toList());
    }
    
    /**
     * 创建账单
     */
    public CreateInvoiceResult createInvoice(CreateInvoiceCommand command) {
        // 创建账单
        BillingInvoiceDO invoice = billingDomainService.createInvoice(
                command.getTenantId(),
                command.getPlanSkuId(),
                command.getIdempotencyKey(),
                command.getPaymentChannel()
        );
        
        // 生成支付参数（这里简化处理，实际应该调用支付服务）
        Map<String, Object> paymentParams = new HashMap<>();
        paymentParams.put("invoiceId", invoice.getId());
        paymentParams.put("invoiceNo", invoice.getInvoiceNo());
        paymentParams.put("amountFen", invoice.getAmountFen());
        paymentParams.put("paymentChannel", invoice.getPaymentChannel());
        // TODO: 实际应该调用微信/支付宝支付服务，生成预支付参数
        
        return CreateInvoiceResult.builder()
                .invoiceId(invoice.getId())
                .invoiceNo(invoice.getInvoiceNo())
                .amountFen(invoice.getAmountFen())
                .paymentParams(paymentParams)
                .build();
    }
    
    /**
     * 分页查询账单
     */
    public Page<InvoiceDTO> listInvoices(Long tenantId, int pageNum, int pageSize) {
        Page<BillingInvoiceDO> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<BillingInvoiceDO> wrapper = new LambdaQueryWrapper<BillingInvoiceDO>()
                .eq(BillingInvoiceDO::getTenantId, tenantId)
                .orderByDesc(BillingInvoiceDO::getCreatedAt);
        
        Page<BillingInvoiceDO> result = invoiceMapper.selectPage(page, wrapper);
        
        Page<InvoiceDTO> dtoPage = new Page<>(result.getCurrent(), result.getSize(), result.getTotal());
        dtoPage.setRecords(result.getRecords().stream()
                .map(this::toInvoiceDTO)
                .collect(Collectors.toList()));
        
        return dtoPage;
    }
    
    /**
     * 获取租户订阅
     */
    public SubscriptionDTO getSubscription(Long tenantId) {
        TenantSubscriptionDO subscription = billingDomainService.getSubscriptionByTenantId(tenantId);
        if (subscription == null) {
            return null;
        }
        return toSubscriptionDTO(subscription);
    }
    
    /**
     * 续费订阅（生成续费账单）
     */
    public CreateInvoiceResult renewSubscription(RenewSubscriptionCommand command) {
        // 查询当前订阅
        TenantSubscriptionDO subscription = billingDomainService.getSubscriptionByTenantId(command.getTenantId());
        if (subscription == null) {
            throw new RuntimeException("订阅不存在");
        }
        
        // 确定续费的套餐 SKU
        Long planSkuId = command.getPlanSkuId();
        if (planSkuId == null) {
            // 如果没有指定套餐，使用当前套餐（同周期续费）
            planSkuId = subscription.getLastInvoiceId() != null ? 
                    billingDomainService.getInvoiceById(subscription.getLastInvoiceId()).getPlanSkuId() : null;
            
            if (planSkuId == null) {
                throw new RuntimeException("无法确定续费套餐，请指定 planSkuId");
            }
        }
        
        // 创建续费账单
        CreateInvoiceCommand invoiceCommand = new CreateInvoiceCommand();
        invoiceCommand.setTenantId(command.getTenantId());
        invoiceCommand.setPlanSkuId(planSkuId);
        invoiceCommand.setPaymentChannel(command.getPaymentChannel());
        invoiceCommand.setIdempotencyKey(command.getIdempotencyKey() != null ? 
                command.getIdempotencyKey() : "RENEW-" + java.util.UUID.randomUUID().toString());
        
        log.info("[billing] 生成续费账单，tenantId={}, planSkuId={}, idempotencyKey={}", 
                command.getTenantId(), planSkuId, invoiceCommand.getIdempotencyKey());
        
        return createInvoice(invoiceCommand);
    }
    
    private PlanSkuDTO toPlanSkuDTO(PlanSkuDO planSku) {
        Map<String, Object> features = parseFeatures(planSku.getFeatures());
        
        return PlanSkuDTO.builder()
                .id(planSku.getId())
                .planCode(planSku.getPlanCode())
                .planName(planSku.getPlanName())
                .planLevel(planSku.getPlanLevel())
                .billingPeriod(planSku.getBillingPeriod())
                .periodMonths(planSku.getPeriodMonths())
                .priceFen(planSku.getPriceFen())
                .originalPriceFen(planSku.getOriginalPriceFen())
                .features(features)
                .status(planSku.getStatus())
                .sortOrder(planSku.getSortOrder())
                .build();
    }
    
    private InvoiceDTO toInvoiceDTO(BillingInvoiceDO invoice) {
        return InvoiceDTO.builder()
                .id(invoice.getId())
                .tenantId(invoice.getTenantId())
                .invoiceNo(invoice.getInvoiceNo())
                .planSkuId(invoice.getPlanSkuId())
                .planCode(invoice.getPlanCode())
                .planName(invoice.getPlanName())
                .billingPeriod(invoice.getBillingPeriod())
                .periodMonths(invoice.getPeriodMonths())
                .amountFen(invoice.getAmountFen())
                .paidAmountFen(invoice.getPaidAmountFen())
                .paymentChannel(invoice.getPaymentChannel())
                .channelTradeNo(invoice.getChannelTradeNo())
                .paidAt(invoice.getPaidAt())
                .status(invoice.getStatus())
                .effectiveStartAt(invoice.getEffectiveStartAt())
                .effectiveEndAt(invoice.getEffectiveEndAt())
                .createdAt(invoice.getCreatedAt())
                .updatedAt(invoice.getUpdatedAt())
                .build();
    }
    
    private SubscriptionDTO toSubscriptionDTO(TenantSubscriptionDO subscription) {
        Map<String, Object> features = parseFeatures(subscription.getCurrentFeatures());
        
        // 计算剩余天数
        java.time.LocalDateTime now = java.time.LocalDateTime.now();
        long daysRemaining = java.time.temporal.ChronoUnit.DAYS.between(now.toLocalDate(), subscription.getSubscriptionEndAt().toLocalDate());
        
        // 判断是否在宽限期（GRACE 状态）
        boolean inGracePeriod = "GRACE".equals(subscription.getStatus());
        Integer graceDaysRemaining = null;
        
        if (inGracePeriod) {
            // 宽限期为7天，计算宽限期剩余天数
            java.time.LocalDateTime graceEndAt = subscription.getSubscriptionEndAt().plusDays(7);
            graceDaysRemaining = (int) java.time.temporal.ChronoUnit.DAYS.between(now.toLocalDate(), graceEndAt.toLocalDate());
        }
        
        return SubscriptionDTO.builder()
                .id(subscription.getId())
                .tenantId(subscription.getTenantId())
                .currentPlanCode(subscription.getCurrentPlanCode())
                .currentPlanName(subscription.getCurrentPlanName())
                .currentPlanLevel(subscription.getCurrentPlanLevel())
                .currentFeatures(features)
                .subscriptionStartAt(subscription.getSubscriptionStartAt())
                .subscriptionEndAt(subscription.getSubscriptionEndAt())
                .status(subscription.getStatus())
                .lastInvoiceId(subscription.getLastInvoiceId())
                .lastPaidAt(subscription.getLastPaidAt())
                .createdAt(subscription.getCreatedAt())
                .updatedAt(subscription.getUpdatedAt())
                .daysRemaining((int) daysRemaining)
                .inGracePeriod(inGracePeriod)
                .graceDaysRemaining(graceDaysRemaining)
                .build();
    }
    
    private Map<String, Object> parseFeatures(String featuresJson) {
        if (featuresJson == null || featuresJson.isEmpty()) {
            return new HashMap<>();
        }
        try {
            return objectMapper.readValue(featuresJson, new TypeReference<Map<String, Object>>() {});
        } catch (Exception e) {
            log.warn("[billing] 解析 features JSON 失败，featuresJson={}", featuresJson, e);
            return new HashMap<>();
        }
    }
}
