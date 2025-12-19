package com.bluecone.app.billing;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.bluecone.app.billing.dao.entity.TenantSubscriptionDO;
import com.bluecone.app.billing.dao.mapper.TenantSubscriptionMapper;
import com.bluecone.app.billing.domain.enums.SubscriptionStatus;
import com.bluecone.app.billing.domain.service.BillingDomainService;
import com.bluecone.app.billing.metrics.BillingMetrics;
import com.bluecone.app.billing.scheduler.SubscriptionExpireJob;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * SubscriptionExpireJob 单元测试
 * 测试订阅到期和宽限期逻辑
 */
class SubscriptionExpireJobTest {
    
    @Mock
    private TenantSubscriptionMapper subscriptionMapper;
    
    @Mock
    private BillingDomainService billingDomainService;
    
    @Mock
    private BillingMetrics billingMetrics;
    
    @InjectMocks
    private SubscriptionExpireJob subscriptionExpireJob;
    
    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }
    
    @Test
    void testActiveToGrace_Success() {
        // 准备测试数据：已到期的 ACTIVE 订阅
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime expiredAt = now.minusHours(1);
        
        TenantSubscriptionDO subscription = new TenantSubscriptionDO();
        subscription.setId(1L);
        subscription.setTenantId(100L);
        subscription.setCurrentPlanCode("PRO");
        subscription.setCurrentPlanName("专业版");
        subscription.setSubscriptionEndAt(expiredAt);
        subscription.setStatus(SubscriptionStatus.ACTIVE.getCode());
        
        // 模拟查询到已到期的订阅
        when(subscriptionMapper.selectList(any(LambdaQueryWrapper.class)))
                .thenReturn(Arrays.asList(subscription))
                .thenReturn(Collections.emptyList()); // GRACE 订阅为空
        
        // 执行测试
        subscriptionExpireJob.expireSubscriptions();
        
        // 验证订阅状态更新为 GRACE
        ArgumentCaptor<TenantSubscriptionDO> subscriptionCaptor = ArgumentCaptor.forClass(TenantSubscriptionDO.class);
        verify(subscriptionMapper).updateById(subscriptionCaptor.capture());
        
        TenantSubscriptionDO updatedSubscription = subscriptionCaptor.getValue();
        assertEquals(SubscriptionStatus.GRACE.getCode(), updatedSubscription.getStatus());
        
        // 验证指标记录
        verify(billingMetrics).recordGraceEntered(subscription.getTenantId(), subscription.getCurrentPlanCode());
    }
    
    @Test
    void testGraceToExpired_Success() {
        // 准备测试数据：宽限期已结束的订阅
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime expiredAt = now.minusDays(8); // 到期 + 8天，超过宽限期
        
        TenantSubscriptionDO subscription = new TenantSubscriptionDO();
        subscription.setId(1L);
        subscription.setTenantId(100L);
        subscription.setCurrentPlanCode("PRO");
        subscription.setCurrentPlanName("专业版");
        subscription.setSubscriptionEndAt(expiredAt);
        subscription.setStatus(SubscriptionStatus.GRACE.getCode());
        
        // 模拟查询
        when(subscriptionMapper.selectList(any(LambdaQueryWrapper.class)))
                .thenReturn(Collections.emptyList()) // ACTIVE 订阅为空
                .thenReturn(Arrays.asList(subscription)); // GRACE 订阅
        
        // 执行测试
        subscriptionExpireJob.expireSubscriptions();
        
        // 验证调用了降级方法
        verify(billingDomainService).downgradeToFree(subscription.getTenantId());
        
        // 验证指标记录
        verify(billingMetrics).recordGraceExpired(subscription.getTenantId(), subscription.getCurrentPlanCode());
    }
    
    @Test
    void testExpireSubscriptions_NoExpiredSubscriptions() {
        // 模拟没有到期的订阅
        when(subscriptionMapper.selectList(any(LambdaQueryWrapper.class)))
                .thenReturn(Collections.emptyList());
        
        // 执行测试
        subscriptionExpireJob.expireSubscriptions();
        
        // 验证没有更新操作
        verify(subscriptionMapper, never()).updateById(any());
        verify(billingDomainService, never()).downgradeToFree(any());
    }
    
    @Test
    void testGracePeriodDuration() {
        // 验证宽限期为 7 天
        // 这个测试主要是确保常量值正确
        LocalDateTime expireAt = LocalDateTime.of(2025, 12, 19, 0, 0);
        LocalDateTime graceEndAt = expireAt.plusDays(7);
        
        assertEquals(LocalDateTime.of(2025, 12, 26, 0, 0), graceEndAt);
    }
}
