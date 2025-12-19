package com.bluecone.app.billing.domain.service;

import com.bluecone.app.billing.dao.entity.BillingInvoiceDO;
import com.bluecone.app.billing.dao.entity.PlanSkuDO;
import com.bluecone.app.billing.dao.entity.TenantSubscriptionDO;
import com.bluecone.app.billing.dao.mapper.BillingInvoiceMapper;
import com.bluecone.app.billing.dao.mapper.PlanSkuMapper;
import com.bluecone.app.billing.dao.mapper.TenantSubscriptionMapper;
import com.bluecone.app.billing.domain.enums.InvoiceStatus;
import com.bluecone.app.billing.domain.enums.PlanCode;
import com.bluecone.app.billing.domain.enums.SubscriptionStatus;
import com.bluecone.app.core.error.CommonErrorCode;
import com.bluecone.app.core.exception.BizException;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 订阅计费领域服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BillingDomainService {
    
    private final PlanSkuMapper planSkuMapper;
    private final BillingInvoiceMapper invoiceMapper;
    private final TenantSubscriptionMapper subscriptionMapper;
    
    /**
     * 获取所有可用的套餐 SKU
     */
    public List<PlanSkuDO> listActivePlanSkus() {
        return planSkuMapper.selectList(
            new LambdaQueryWrapper<PlanSkuDO>()
                .eq(PlanSkuDO::getStatus, "ACTIVE")
                .orderByAsc(PlanSkuDO::getSortOrder)
        );
    }
    
    /**
     * 根据 ID 获取套餐 SKU
     */
    public PlanSkuDO getPlanSkuById(Long id) {
        PlanSkuDO planSku = planSkuMapper.selectById(id);
        if (planSku == null) {
            throw new BizException(CommonErrorCode.BAD_REQUEST, "套餐 SKU 不存在");
        }
        return planSku;
    }
    
    /**
     * 创建账单（幂等）
     */
    @Transactional
    public BillingInvoiceDO createInvoice(Long tenantId, Long planSkuId, String idempotencyKey, String paymentChannel) {
        // 幂等性检查
        BillingInvoiceDO existingInvoice = invoiceMapper.selectOne(
            new LambdaQueryWrapper<BillingInvoiceDO>()
                .eq(BillingInvoiceDO::getIdempotencyKey, idempotencyKey)
        );
        if (existingInvoice != null) {
            log.info("[billing] 账单已存在，返回已有账单，idempotencyKey={}, invoiceId={}", idempotencyKey, existingInvoice.getId());
            return existingInvoice;
        }
        
        // 获取套餐 SKU
        PlanSkuDO planSku = getPlanSkuById(planSkuId);
        if (!"ACTIVE".equals(planSku.getStatus())) {
            throw new BizException(CommonErrorCode.BAD_REQUEST, "套餐 SKU 不可用");
        }
        
        // 生成账单号
        String invoiceNo = generateInvoiceNo(tenantId);
        
        // 创建账单
        BillingInvoiceDO invoice = new BillingInvoiceDO();
        invoice.setTenantId(tenantId);
        invoice.setInvoiceNo(invoiceNo);
        invoice.setIdempotencyKey(idempotencyKey);
        invoice.setPlanSkuId(planSkuId);
        invoice.setPlanCode(planSku.getPlanCode());
        invoice.setPlanName(planSku.getPlanName());
        invoice.setBillingPeriod(planSku.getBillingPeriod());
        invoice.setPeriodMonths(planSku.getPeriodMonths());
        invoice.setAmountFen(planSku.getPriceFen());
        invoice.setPaymentChannel(paymentChannel);
        invoice.setStatus(InvoiceStatus.PENDING.getCode());
        invoice.setCreatedAt(LocalDateTime.now());
        invoice.setUpdatedAt(LocalDateTime.now());
        
        invoiceMapper.insert(invoice);
        
        log.info("[billing] 账单创建成功，invoiceId={}, invoiceNo={}, tenantId={}, planCode={}, amountFen={}", 
                invoice.getId(), invoiceNo, tenantId, planSku.getPlanCode(), planSku.getPriceFen());
        
        return invoice;
    }
    
    /**
     * 标记账单为已支付（幂等）
     */
    @Transactional
    public BillingInvoiceDO markInvoiceAsPaid(Long invoiceId, String channelTradeNo, Long paidAmountFen, LocalDateTime paidAt) {
        BillingInvoiceDO invoice = invoiceMapper.selectById(invoiceId);
        if (invoice == null) {
            throw new BizException(CommonErrorCode.BAD_REQUEST, "账单不存在");
        }
        
        // 幂等性检查
        if (InvoiceStatus.PAID.getCode().equals(invoice.getStatus())) {
            log.info("[billing] 账单已支付，忽略重复通知，invoiceId={}, channelTradeNo={}", invoiceId, channelTradeNo);
            return invoice;
        }
        
        // 计算生效周期
        LocalDateTime effectiveStartAt = LocalDateTime.now();
        LocalDateTime effectiveEndAt = effectiveStartAt.plusMonths(invoice.getPeriodMonths());
        
        // 更新账单状态
        invoice.setStatus(InvoiceStatus.PAID.getCode());
        invoice.setChannelTradeNo(channelTradeNo);
        invoice.setPaidAmountFen(paidAmountFen);
        invoice.setPaidAt(paidAt);
        invoice.setEffectiveStartAt(effectiveStartAt);
        invoice.setEffectiveEndAt(effectiveEndAt);
        invoice.setUpdatedAt(LocalDateTime.now());
        
        invoiceMapper.updateById(invoice);
        
        log.info("[billing] 账单标记为已支付，invoiceId={}, channelTradeNo={}, paidAmountFen={}, effectiveStartAt={}, effectiveEndAt={}", 
                invoiceId, channelTradeNo, paidAmountFen, effectiveStartAt, effectiveEndAt);
        
        return invoice;
    }
    
    /**
     * 激活或更新订阅（幂等）
     */
    @Transactional
    public TenantSubscriptionDO activateSubscription(Long tenantId, BillingInvoiceDO invoice, PlanSkuDO planSku) {
        TenantSubscriptionDO subscription = subscriptionMapper.selectOne(
            new LambdaQueryWrapper<TenantSubscriptionDO>()
                .eq(TenantSubscriptionDO::getTenantId, tenantId)
        );
        
        if (subscription == null) {
            // 首次订阅
            subscription = new TenantSubscriptionDO();
            subscription.setTenantId(tenantId);
            subscription.setCurrentPlanCode(planSku.getPlanCode());
            subscription.setCurrentPlanName(planSku.getPlanName());
            subscription.setCurrentPlanLevel(planSku.getPlanLevel());
            subscription.setCurrentFeatures(planSku.getFeatures());
            subscription.setSubscriptionStartAt(invoice.getEffectiveStartAt());
            subscription.setSubscriptionEndAt(invoice.getEffectiveEndAt());
            subscription.setStatus(SubscriptionStatus.ACTIVE.getCode());
            subscription.setLastInvoiceId(invoice.getId());
            subscription.setLastPaidAt(invoice.getPaidAt());
            subscription.setCreatedAt(LocalDateTime.now());
            subscription.setUpdatedAt(LocalDateTime.now());
            
            subscriptionMapper.insert(subscription);
            
            log.info("[billing] 订阅创建成功，tenantId={}, planCode={}, startAt={}, endAt={}", 
                    tenantId, planSku.getPlanCode(), invoice.getEffectiveStartAt(), invoice.getEffectiveEndAt());
        } else {
            // 续费或升级
            subscription.setCurrentPlanCode(planSku.getPlanCode());
            subscription.setCurrentPlanName(planSku.getPlanName());
            subscription.setCurrentPlanLevel(planSku.getPlanLevel());
            subscription.setCurrentFeatures(planSku.getFeatures());
            
            // 如果当前订阅未过期，则在当前结束时间基础上延长
            LocalDateTime now = LocalDateTime.now();
            if (subscription.getSubscriptionEndAt().isAfter(now)) {
                subscription.setSubscriptionEndAt(subscription.getSubscriptionEndAt().plusMonths(invoice.getPeriodMonths()));
            } else {
                subscription.setSubscriptionStartAt(invoice.getEffectiveStartAt());
                subscription.setSubscriptionEndAt(invoice.getEffectiveEndAt());
            }
            
            subscription.setStatus(SubscriptionStatus.ACTIVE.getCode());
            subscription.setLastInvoiceId(invoice.getId());
            subscription.setLastPaidAt(invoice.getPaidAt());
            subscription.setUpdatedAt(LocalDateTime.now());
            
            subscriptionMapper.updateById(subscription);
            
            log.info("[billing] 订阅更新成功，tenantId={}, planCode={}, endAt={}", 
                    tenantId, planSku.getPlanCode(), subscription.getSubscriptionEndAt());
        }
        
        return subscription;
    }
    
    /**
     * 获取租户订阅
     */
    public TenantSubscriptionDO getSubscriptionByTenantId(Long tenantId) {
        return subscriptionMapper.selectOne(
            new LambdaQueryWrapper<TenantSubscriptionDO>()
                .eq(TenantSubscriptionDO::getTenantId, tenantId)
        );
    }
    
    /**
     * 根据 ID 获取账单
     */
    public BillingInvoiceDO getInvoiceById(Long invoiceId) {
        BillingInvoiceDO invoice = invoiceMapper.selectById(invoiceId);
        if (invoice == null) {
            throw new BizException(CommonErrorCode.BAD_REQUEST, "账单不存在");
        }
        return invoice;
    }
    
    /**
     * 降级到免费版
     */
    @Transactional
    public void downgradeToFree(Long tenantId) {
        TenantSubscriptionDO subscription = getSubscriptionByTenantId(tenantId);
        if (subscription == null) {
            log.warn("[billing] 租户订阅不存在，无法降级，tenantId={}", tenantId);
            return;
        }
        
        // 获取免费版套餐
        PlanSkuDO freePlan = planSkuMapper.selectOne(
            new LambdaQueryWrapper<PlanSkuDO>()
                .eq(PlanSkuDO::getPlanCode, PlanCode.FREE.getCode())
                .eq(PlanSkuDO::getStatus, "ACTIVE")
                .last("LIMIT 1")
        );
        
        if (freePlan == null) {
            throw new BizException(CommonErrorCode.SYSTEM_ERROR, "免费版套餐不存在");
        }
        
        subscription.setCurrentPlanCode(freePlan.getPlanCode());
        subscription.setCurrentPlanName(freePlan.getPlanName());
        subscription.setCurrentPlanLevel(freePlan.getPlanLevel());
        subscription.setCurrentFeatures(freePlan.getFeatures());
        subscription.setStatus(SubscriptionStatus.EXPIRED.getCode());
        subscription.setUpdatedAt(LocalDateTime.now());
        
        subscriptionMapper.updateById(subscription);
        
        log.info("[billing] 订阅降级到免费版，tenantId={}, oldPlanCode={}", tenantId, subscription.getCurrentPlanCode());
    }
    
    /**
     * 生成账单号
     */
    private String generateInvoiceNo(Long tenantId) {
        return "INV" + System.currentTimeMillis() + tenantId;
    }
}
