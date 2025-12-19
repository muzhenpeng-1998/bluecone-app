package com.bluecone.app.billing;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.bluecone.app.billing.dao.entity.BillingInvoiceDO;
import com.bluecone.app.billing.dao.entity.PlanSkuDO;
import com.bluecone.app.billing.dao.mapper.BillingInvoiceMapper;
import com.bluecone.app.billing.dao.mapper.PlanSkuMapper;
import com.bluecone.app.billing.dao.mapper.TenantSubscriptionMapper;
import com.bluecone.app.billing.domain.enums.InvoiceStatus;
import com.bluecone.app.billing.domain.service.BillingDomainService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * 幂等性测试
 * 测试账单创建、支付回调等关键操作的幂等性
 */
class BillingIdempotencyTest {
    
    @Mock
    private BillingInvoiceMapper invoiceMapper;
    
    @Mock
    private PlanSkuMapper planSkuMapper;
    
    @Mock
    private TenantSubscriptionMapper subscriptionMapper;
    
    @InjectMocks
    private BillingDomainService billingDomainService;
    
    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }
    
    @Test
    void testCreateInvoice_Idempotent() {
        // 准备测试数据
        String idempotencyKey = "test-idempotency-key-001";
        Long tenantId = 100L;
        Long planSkuId = 1L;
        
        // 模拟已存在的账单
        BillingInvoiceDO existingInvoice = new BillingInvoiceDO();
        existingInvoice.setId(1L);
        existingInvoice.setInvoiceNo("INV001");
        existingInvoice.setIdempotencyKey(idempotencyKey);
        existingInvoice.setStatus(InvoiceStatus.PENDING.getCode());
        
        when(invoiceMapper.selectOne(any(LambdaQueryWrapper.class)))
                .thenReturn(existingInvoice);
        
        // 执行测试：使用相同的幂等键创建账单
        BillingInvoiceDO result = billingDomainService.createInvoice(tenantId, planSkuId, idempotencyKey, "WECHAT");
        
        // 验证结果：返回已存在的账单，不创建新账单
        assertNotNull(result);
        assertEquals(existingInvoice.getId(), result.getId());
        assertEquals(existingInvoice.getInvoiceNo(), result.getInvoiceNo());
        
        // 验证没有插入新记录
        verify(invoiceMapper, never()).insert(any());
    }
    
    @Test
    void testMarkInvoiceAsPaid_Idempotent() {
        // 准备测试数据
        Long invoiceId = 1L;
        String channelTradeNo = "wx_trade_001";
        
        // 模拟已支付的账单
        BillingInvoiceDO paidInvoice = new BillingInvoiceDO();
        paidInvoice.setId(invoiceId);
        paidInvoice.setInvoiceNo("INV001");
        paidInvoice.setStatus(InvoiceStatus.PAID.getCode());
        paidInvoice.setChannelTradeNo(channelTradeNo);
        paidInvoice.setPaidAt(LocalDateTime.now());
        
        when(invoiceMapper.selectById(invoiceId))
                .thenReturn(paidInvoice);
        
        // 执行测试：重复标记为已支付
        BillingInvoiceDO result = billingDomainService.markInvoiceAsPaid(
                invoiceId, channelTradeNo, 29900L, LocalDateTime.now()
        );
        
        // 验证结果：返回已支付的账单，不重复更新
        assertNotNull(result);
        assertEquals(InvoiceStatus.PAID.getCode(), result.getStatus());
        
        // 验证没有更新操作
        verify(invoiceMapper, never()).updateById(any());
    }
    
    @Test
    void testCreateInvoice_DifferentIdempotencyKey() {
        // 准备测试数据
        Long tenantId = 100L;
        Long planSkuId = 1L;
        
        // 模拟套餐 SKU
        PlanSkuDO planSku = new PlanSkuDO();
        planSku.setId(planSkuId);
        planSku.setPlanCode("PRO");
        planSku.setPlanName("专业版");
        planSku.setBillingPeriod("MONTHLY");
        planSku.setPeriodMonths(1);
        planSku.setPriceFen(29900L);
        planSku.setStatus("ACTIVE");
        
        when(planSkuMapper.selectById(planSkuId))
                .thenReturn(planSku);
        
        // 第一次创建：不存在
        when(invoiceMapper.selectOne(any(LambdaQueryWrapper.class)))
                .thenReturn(null);
        
        // 执行测试：使用不同的幂等键创建两个账单
        BillingInvoiceDO invoice1 = billingDomainService.createInvoice(
                tenantId, planSkuId, "key-001", "WECHAT"
        );
        BillingInvoiceDO invoice2 = billingDomainService.createInvoice(
                tenantId, planSkuId, "key-002", "WECHAT"
        );
        
        // 验证结果：应该创建两个不同的账单
        verify(invoiceMapper, times(2)).insert(any());
    }
}
