package com.bluecone.app.billing;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.bluecone.app.billing.application.SubscriptionPlanService;
import com.bluecone.app.billing.dao.entity.TenantSubscriptionDO;
import com.bluecone.app.billing.dao.mapper.TenantSubscriptionMapper;
import com.bluecone.app.billing.domain.enums.SubscriptionStatus;
import com.bluecone.app.billing.guard.PlanGuard;
import com.bluecone.app.core.exception.BizException;
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
 * PlanGuard 单元测试
 * 测试宽限期权限限制
 */
class PlanGuardTest {
    
    @Mock
    private TenantSubscriptionMapper subscriptionMapper;
    
    @Mock
    private SubscriptionPlanService subscriptionPlanService;
    
    @InjectMocks
    private PlanGuard planGuard;
    
    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }
    
    @Test
    void testCheckWritePermission_Active_Success() {
        // 准备测试数据：ACTIVE 状态
        TenantSubscriptionDO subscription = createSubscription(SubscriptionStatus.ACTIVE);
        when(subscriptionMapper.selectOne(any(LambdaQueryWrapper.class)))
                .thenReturn(subscription);
        
        // 执行测试：ACTIVE 状态应该允许写操作
        assertDoesNotThrow(() -> {
            planGuard.checkWritePermission(100L, "createStore");
        });
    }
    
    @Test
    void testCheckWritePermission_Grace_Restricted() {
        // 准备测试数据：GRACE 状态
        TenantSubscriptionDO subscription = createSubscription(SubscriptionStatus.GRACE);
        when(subscriptionMapper.selectOne(any(LambdaQueryWrapper.class)))
                .thenReturn(subscription);
        
        // 执行测试：GRACE 状态应该限制写操作
        BizException exception = assertThrows(BizException.class, () -> {
            planGuard.checkWritePermission(100L, "createStore");
        });
        
        assertTrue(exception.getMessage().contains("宽限期"));
    }
    
    @Test
    void testCheckWritePermission_Expired_Restricted() {
        // 准备测试数据：EXPIRED 状态
        TenantSubscriptionDO subscription = createSubscription(SubscriptionStatus.EXPIRED);
        when(subscriptionMapper.selectOne(any(LambdaQueryWrapper.class)))
                .thenReturn(subscription);
        
        // 执行测试：EXPIRED 状态应该限制写操作
        BizException exception = assertThrows(BizException.class, () -> {
            planGuard.checkWritePermission(100L, "createStore");
        });
        
        assertTrue(exception.getMessage().contains("已过期"));
    }
    
    @Test
    void testCheckAdvancedFeature_Grace_Restricted() {
        // 准备测试数据：GRACE 状态
        TenantSubscriptionDO subscription = createSubscription(SubscriptionStatus.GRACE);
        when(subscriptionMapper.selectOne(any(LambdaQueryWrapper.class)))
                .thenReturn(subscription);
        
        // 执行测试：GRACE 状态应该限制高级功能
        BizException exception = assertThrows(BizException.class, () -> {
            planGuard.checkAdvancedFeature(100L, "多仓库管理");
        });
        
        assertTrue(exception.getMessage().contains("宽限期"));
        assertTrue(exception.getMessage().contains("多仓库管理"));
    }
    
    @Test
    void testIsInGracePeriod() {
        // 准备测试数据：GRACE 状态
        TenantSubscriptionDO subscription = createSubscription(SubscriptionStatus.GRACE);
        when(subscriptionMapper.selectOne(any(LambdaQueryWrapper.class)))
                .thenReturn(subscription);
        
        // 执行测试
        boolean inGrace = planGuard.isInGracePeriod(100L);
        
        // 验证结果
        assertTrue(inGrace);
    }
    
    @Test
    void testIsSubscriptionValid_Active() {
        // 准备测试数据：ACTIVE 状态
        TenantSubscriptionDO subscription = createSubscription(SubscriptionStatus.ACTIVE);
        when(subscriptionMapper.selectOne(any(LambdaQueryWrapper.class)))
                .thenReturn(subscription);
        
        // 执行测试
        boolean valid = planGuard.isSubscriptionValid(100L);
        
        // 验证结果：ACTIVE 状态有效
        assertTrue(valid);
    }
    
    @Test
    void testIsSubscriptionValid_Grace() {
        // 准备测试数据：GRACE 状态
        TenantSubscriptionDO subscription = createSubscription(SubscriptionStatus.GRACE);
        when(subscriptionMapper.selectOne(any(LambdaQueryWrapper.class)))
                .thenReturn(subscription);
        
        // 执行测试
        boolean valid = planGuard.isSubscriptionValid(100L);
        
        // 验证结果：GRACE 状态也算有效（可以使用基本功能）
        assertTrue(valid);
    }
    
    @Test
    void testIsSubscriptionValid_Expired() {
        // 准备测试数据：EXPIRED 状态
        TenantSubscriptionDO subscription = createSubscription(SubscriptionStatus.EXPIRED);
        when(subscriptionMapper.selectOne(any(LambdaQueryWrapper.class)))
                .thenReturn(subscription);
        
        // 执行测试
        boolean valid = planGuard.isSubscriptionValid(100L);
        
        // 验证结果：EXPIRED 状态无效
        assertFalse(valid);
    }
    
    private TenantSubscriptionDO createSubscription(SubscriptionStatus status) {
        TenantSubscriptionDO subscription = new TenantSubscriptionDO();
        subscription.setId(1L);
        subscription.setTenantId(100L);
        subscription.setCurrentPlanCode("PRO");
        subscription.setCurrentPlanName("专业版");
        subscription.setSubscriptionStartAt(LocalDateTime.now().minusMonths(1));
        subscription.setSubscriptionEndAt(LocalDateTime.now().minusDays(1));
        subscription.setStatus(status.getCode());
        return subscription;
    }
}
