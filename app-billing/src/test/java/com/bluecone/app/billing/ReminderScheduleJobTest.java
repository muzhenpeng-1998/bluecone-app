package com.bluecone.app.billing;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.bluecone.app.billing.dao.entity.BillingReminderTaskDO;
import com.bluecone.app.billing.dao.entity.TenantSubscriptionDO;
import com.bluecone.app.billing.dao.mapper.BillingReminderTaskMapper;
import com.bluecone.app.billing.dao.mapper.TenantSubscriptionMapper;
import com.bluecone.app.billing.domain.enums.ReminderStatus;
import com.bluecone.app.billing.domain.enums.ReminderType;
import com.bluecone.app.billing.domain.enums.SubscriptionStatus;
import com.bluecone.app.billing.metrics.BillingMetrics;
import com.bluecone.app.billing.scheduler.ReminderScheduleJob;
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
 * ReminderScheduleJob 单元测试
 * 测试提醒任务生成的幂等性和正确性
 */
class ReminderScheduleJobTest {
    
    @Mock
    private TenantSubscriptionMapper subscriptionMapper;
    
    @Mock
    private BillingReminderTaskMapper reminderTaskMapper;
    
    @Mock
    private BillingMetrics billingMetrics;
    
    @InjectMocks
    private ReminderScheduleJob reminderScheduleJob;
    
    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }
    
    @Test
    void testCreateReminderTask_Idempotent() {
        // 准备测试数据
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime expireAt = now.plusDays(7);
        
        TenantSubscriptionDO subscription = new TenantSubscriptionDO();
        subscription.setId(1L);
        subscription.setTenantId(100L);
        subscription.setCurrentPlanCode("PRO");
        subscription.setCurrentPlanName("专业版");
        subscription.setSubscriptionEndAt(expireAt);
        subscription.setStatus(SubscriptionStatus.ACTIVE.getCode());
        
        // 模拟已存在的提醒任务（幂等性检查）
        BillingReminderTaskDO existingTask = new BillingReminderTaskDO();
        existingTask.setId(1L);
        when(reminderTaskMapper.selectOne(any(LambdaQueryWrapper.class)))
                .thenReturn(existingTask);
        
        // 执行测试
        boolean created = reminderScheduleJob.createReminderTask(subscription, ReminderType.EXPIRE_7D, now);
        
        // 验证结果
        assertFalse(created, "已存在的任务不应该重复创建");
        verify(reminderTaskMapper, never()).insert(any());
        verify(billingMetrics, never()).recordReminderTaskCreated(any());
    }
    
    @Test
    void testCreateReminderTask_Success() {
        // 准备测试数据
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime expireAt = now.plusDays(7);
        
        TenantSubscriptionDO subscription = new TenantSubscriptionDO();
        subscription.setId(1L);
        subscription.setTenantId(100L);
        subscription.setCurrentPlanCode("PRO");
        subscription.setCurrentPlanName("专业版");
        subscription.setSubscriptionEndAt(expireAt);
        subscription.setStatus(SubscriptionStatus.ACTIVE.getCode());
        
        // 模拟不存在的提醒任务
        when(reminderTaskMapper.selectOne(any(LambdaQueryWrapper.class)))
                .thenReturn(null);
        
        // 执行测试
        boolean created = reminderScheduleJob.createReminderTask(subscription, ReminderType.EXPIRE_7D, now);
        
        // 验证结果
        assertTrue(created, "应该创建新任务");
        
        // 验证插入的任务数据
        ArgumentCaptor<BillingReminderTaskDO> taskCaptor = ArgumentCaptor.forClass(BillingReminderTaskDO.class);
        verify(reminderTaskMapper).insert(taskCaptor.capture());
        
        BillingReminderTaskDO insertedTask = taskCaptor.getValue();
        assertEquals(subscription.getId(), insertedTask.getSubscriptionId());
        assertEquals(subscription.getTenantId(), insertedTask.getTenantId());
        assertEquals(ReminderType.EXPIRE_7D.getCode(), insertedTask.getReminderType());
        assertEquals(ReminderStatus.PENDING.getCode(), insertedTask.getStatus());
        assertEquals(0, insertedTask.getRetryCount());
        assertEquals(3, insertedTask.getMaxRetryCount());
        
        // 验证指标记录
        verify(billingMetrics).recordReminderTaskCreated(ReminderType.EXPIRE_7D.getCode());
    }
    
    @Test
    void testScheduleReminders_NoSubscriptions() {
        // 模拟没有即将到期的订阅
        when(subscriptionMapper.selectList(any(LambdaQueryWrapper.class)))
                .thenReturn(Collections.emptyList());
        
        // 执行测试
        reminderScheduleJob.scheduleReminders();
        
        // 验证没有创建任务
        verify(reminderTaskMapper, never()).insert(any());
    }
    
    @Test
    void testScheduleReminders_WithSubscriptions() {
        // 准备测试数据
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime expireAt = now.plusDays(7);
        
        TenantSubscriptionDO subscription = new TenantSubscriptionDO();
        subscription.setId(1L);
        subscription.setTenantId(100L);
        subscription.setCurrentPlanCode("PRO");
        subscription.setCurrentPlanName("专业版");
        subscription.setSubscriptionEndAt(expireAt);
        subscription.setStatus(SubscriptionStatus.ACTIVE.getCode());
        
        // 模拟查询到即将到期的订阅
        when(subscriptionMapper.selectList(any(LambdaQueryWrapper.class)))
                .thenReturn(Arrays.asList(subscription))
                .thenReturn(Collections.emptyList()); // GRACE 订阅为空
        
        // 模拟不存在的提醒任务
        when(reminderTaskMapper.selectOne(any(LambdaQueryWrapper.class)))
                .thenReturn(null);
        
        // 执行测试
        reminderScheduleJob.scheduleReminders();
        
        // 验证创建了提醒任务（7/3/1/0天，共4个任务）
        verify(reminderTaskMapper, times(4)).insert(any());
    }
}
