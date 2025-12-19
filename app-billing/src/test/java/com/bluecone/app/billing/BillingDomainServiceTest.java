package com.bluecone.app.billing;

import com.bluecone.app.billing.dao.entity.BillingInvoiceDO;
import com.bluecone.app.billing.dao.entity.PlanSkuDO;
import com.bluecone.app.billing.dao.entity.TenantSubscriptionDO;
import com.bluecone.app.billing.dao.mapper.BillingInvoiceMapper;
import com.bluecone.app.billing.dao.mapper.PlanSkuMapper;
import com.bluecone.app.billing.dao.mapper.TenantSubscriptionMapper;
import com.bluecone.app.billing.domain.enums.InvoiceStatus;
import com.bluecone.app.billing.domain.enums.SubscriptionStatus;
import com.bluecone.app.billing.domain.service.BillingDomainService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 订阅计费领域服务测试
 */
@SpringBootTest
@Transactional
class BillingDomainServiceTest {
    
    @Autowired
    private BillingDomainService billingDomainService;
    
    @Autowired
    private PlanSkuMapper planSkuMapper;
    
    @Autowired
    private BillingInvoiceMapper invoiceMapper;
    
    @Autowired
    private TenantSubscriptionMapper subscriptionMapper;
    
    @Test
    void testCreateInvoice_Idempotent() {
        // Given
        Long tenantId = 1001L;
        Long planSkuId = 1L;
        String idempotencyKey = "test-idempotency-key-" + System.currentTimeMillis();
        
        // When - 第一次创建
        BillingInvoiceDO invoice1 = billingDomainService.createInvoice(tenantId, planSkuId, idempotencyKey, "WECHAT");
        
        // When - 第二次创建（相同幂等键）
        BillingInvoiceDO invoice2 = billingDomainService.createInvoice(tenantId, planSkuId, idempotencyKey, "WECHAT");
        
        // Then - 应该返回相同的账单
        assertThat(invoice1.getId()).isEqualTo(invoice2.getId());
        assertThat(invoice1.getInvoiceNo()).isEqualTo(invoice2.getInvoiceNo());
    }
    
    @Test
    void testMarkInvoiceAsPaid_Idempotent() {
        // Given - 创建账单
        Long tenantId = 1002L;
        Long planSkuId = 1L;
        String idempotencyKey = "test-paid-" + System.currentTimeMillis();
        BillingInvoiceDO invoice = billingDomainService.createInvoice(tenantId, planSkuId, idempotencyKey, "WECHAT");
        
        String channelTradeNo = "WX-" + System.currentTimeMillis();
        Long paidAmountFen = 9900L;
        LocalDateTime paidAt = LocalDateTime.now();
        
        // When - 第一次标记为已支付
        BillingInvoiceDO paidInvoice1 = billingDomainService.markInvoiceAsPaid(
                invoice.getId(), channelTradeNo, paidAmountFen, paidAt);
        
        // When - 第二次标记为已支付（重复回调）
        BillingInvoiceDO paidInvoice2 = billingDomainService.markInvoiceAsPaid(
                invoice.getId(), channelTradeNo, paidAmountFen, paidAt);
        
        // Then - 状态应该是已支付
        assertThat(paidInvoice1.getStatus()).isEqualTo(InvoiceStatus.PAID.getCode());
        assertThat(paidInvoice2.getStatus()).isEqualTo(InvoiceStatus.PAID.getCode());
        assertThat(paidInvoice1.getChannelTradeNo()).isEqualTo(channelTradeNo);
    }
    
    @Test
    void testActivateSubscription_FirstTime() {
        // Given - 创建并支付账单
        Long tenantId = 1003L;
        Long planSkuId = 2L; // BASIC 套餐
        String idempotencyKey = "test-activate-" + System.currentTimeMillis();
        
        BillingInvoiceDO invoice = billingDomainService.createInvoice(tenantId, planSkuId, idempotencyKey, "WECHAT");
        invoice = billingDomainService.markInvoiceAsPaid(
                invoice.getId(), "WX-" + System.currentTimeMillis(), 9900L, LocalDateTime.now());
        
        PlanSkuDO planSku = planSkuMapper.selectById(planSkuId);
        
        // When - 激活订阅
        TenantSubscriptionDO subscription = billingDomainService.activateSubscription(tenantId, invoice, planSku);
        
        // Then - 订阅应该创建成功
        assertThat(subscription).isNotNull();
        assertThat(subscription.getTenantId()).isEqualTo(tenantId);
        assertThat(subscription.getCurrentPlanCode()).isEqualTo(planSku.getPlanCode());
        assertThat(subscription.getStatus()).isEqualTo(SubscriptionStatus.ACTIVE.getCode());
        assertThat(subscription.getLastInvoiceId()).isEqualTo(invoice.getId());
    }
    
    @Test
    void testActivateSubscription_Renewal() {
        // Given - 创建首次订阅
        Long tenantId = 1004L;
        Long planSkuId = 2L;
        
        // 首次订阅
        BillingInvoiceDO invoice1 = billingDomainService.createInvoice(
                tenantId, planSkuId, "test-renewal-1-" + System.currentTimeMillis(), "WECHAT");
        invoice1 = billingDomainService.markInvoiceAsPaid(
                invoice1.getId(), "WX-1-" + System.currentTimeMillis(), 9900L, LocalDateTime.now());
        PlanSkuDO planSku = planSkuMapper.selectById(planSkuId);
        TenantSubscriptionDO subscription1 = billingDomainService.activateSubscription(tenantId, invoice1, planSku);
        
        LocalDateTime firstEndAt = subscription1.getSubscriptionEndAt();
        
        // When - 续费
        BillingInvoiceDO invoice2 = billingDomainService.createInvoice(
                tenantId, planSkuId, "test-renewal-2-" + System.currentTimeMillis(), "WECHAT");
        invoice2 = billingDomainService.markInvoiceAsPaid(
                invoice2.getId(), "WX-2-" + System.currentTimeMillis(), 9900L, LocalDateTime.now());
        TenantSubscriptionDO subscription2 = billingDomainService.activateSubscription(tenantId, invoice2, planSku);
        
        // Then - 订阅结束时间应该延长
        assertThat(subscription2.getSubscriptionEndAt()).isAfter(firstEndAt);
        assertThat(subscription2.getLastInvoiceId()).isEqualTo(invoice2.getId());
    }
    
    @Test
    void testDowngradeToFree() {
        // Given - 创建付费订阅
        Long tenantId = 1005L;
        Long planSkuId = 2L;
        
        BillingInvoiceDO invoice = billingDomainService.createInvoice(
                tenantId, planSkuId, "test-downgrade-" + System.currentTimeMillis(), "WECHAT");
        invoice = billingDomainService.markInvoiceAsPaid(
                invoice.getId(), "WX-" + System.currentTimeMillis(), 9900L, LocalDateTime.now());
        PlanSkuDO planSku = planSkuMapper.selectById(planSkuId);
        billingDomainService.activateSubscription(tenantId, invoice, planSku);
        
        // When - 降级到免费版
        billingDomainService.downgradeToFree(tenantId);
        
        // Then - 订阅应该降级到免费版
        TenantSubscriptionDO subscription = billingDomainService.getSubscriptionByTenantId(tenantId);
        assertThat(subscription.getCurrentPlanCode()).isEqualTo("FREE");
        assertThat(subscription.getStatus()).isEqualTo(SubscriptionStatus.EXPIRED.getCode());
    }
}
