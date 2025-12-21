package com.bluecone.app.billing.scheduler;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.bluecone.app.billing.dao.entity.BillingReminderTaskDO;
import com.bluecone.app.billing.dao.entity.TenantSubscriptionDO;
import com.bluecone.app.billing.dao.mapper.BillingReminderTaskMapper;
import com.bluecone.app.billing.dao.mapper.TenantSubscriptionMapper;
import com.bluecone.app.billing.domain.enums.ReminderStatus;
import com.bluecone.app.billing.domain.enums.ReminderType;
import com.bluecone.app.billing.domain.enums.SubscriptionStatus;
import com.bluecone.app.billing.metrics.BillingMetrics;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 订阅到期提醒任务生成调度器
 * 定时扫描即将到期的订阅，生成提醒任务（幂等）
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ReminderScheduleJob {
    
    private final TenantSubscriptionMapper subscriptionMapper;
    private final BillingReminderTaskMapper reminderTaskMapper;
    private final BillingMetrics billingMetrics;
    
    /**
     * 每小时执行一次，扫描需要生成提醒任务的订阅
     */
    @Scheduled(cron = "0 0 * * * ?")
    public void scheduleReminders() {
//        log.info("[reminder-schedule-job] 开始扫描需要生成提醒任务的订阅");
        
        try {
            LocalDateTime now = LocalDateTime.now();
            
            // 扫描 ACTIVE 状态的订阅，生成到期前提醒
            scheduleExpireReminders(now);
            
            // 扫描 GRACE 状态的订阅，生成宽限期提醒
            scheduleGraceReminders(now);
            
            log.info("[reminder-schedule-job] 提醒任务生成完成");
            
        } catch (Exception e) {
            log.error("[reminder-schedule-job] 提醒任务生成失败", e);
        }
    }
    
    /**
     * 生成到期前提醒任务（7/3/1/0天）
     */
    private void scheduleExpireReminders(LocalDateTime now) {
        // 查询未来8天内到期的 ACTIVE 订阅
        LocalDateTime endTime = now.plusDays(8);
        List<TenantSubscriptionDO> subscriptions = subscriptionMapper.selectList(
            new LambdaQueryWrapper<TenantSubscriptionDO>()
                .eq(TenantSubscriptionDO::getStatus, SubscriptionStatus.ACTIVE.getCode())
                .between(TenantSubscriptionDO::getSubscriptionEndAt, now, endTime)
        );
        
        if (subscriptions.isEmpty()) {
            log.info("[reminder-schedule-job] 没有即将到期的订阅");
            return;
        }
        
        log.info("[reminder-schedule-job] 发现 {} 个即将到期的订阅", subscriptions.size());
        
        int createdCount = 0;
        
        for (TenantSubscriptionDO subscription : subscriptions) {
            try {
                // 为每个订阅生成 7/3/1/0 天提醒任务
                for (ReminderType reminderType : ReminderType.values()) {
                    if (reminderType.isBeforeExpire()) {
                        boolean created = createReminderTask(subscription, reminderType, now);
                        if (created) {
                            createdCount++;
                        }
                    }
                }
            } catch (Exception e) {
                log.error("[reminder-schedule-job] 生成提醒任务失败，subscriptionId={}", subscription.getId(), e);
            }
        }
        
        log.info("[reminder-schedule-job] 到期前提醒任务生成完成，检查数={}, 创建数={}", subscriptions.size(), createdCount);
    }
    
    /**
     * 生成宽限期提醒任务
     */
    private void scheduleGraceReminders(LocalDateTime now) {
        // 查询 GRACE 状态的订阅
        List<TenantSubscriptionDO> subscriptions = subscriptionMapper.selectList(
            new LambdaQueryWrapper<TenantSubscriptionDO>()
                .eq(TenantSubscriptionDO::getStatus, SubscriptionStatus.GRACE.getCode())
        );
        
        if (subscriptions.isEmpty()) {
            log.info("[reminder-schedule-job] 没有宽限期订阅");
            return;
        }
        
        log.info("[reminder-schedule-job] 发现 {} 个宽限期订阅", subscriptions.size());
        
        int createdCount = 0;
        
        for (TenantSubscriptionDO subscription : subscriptions) {
            try {
                // 为宽限期订阅生成提醒任务
                boolean created = createReminderTask(subscription, ReminderType.GRACE_3D, now);
                if (created) {
                    createdCount++;
                }
            } catch (Exception e) {
                log.error("[reminder-schedule-job] 生成宽限期提醒任务失败，subscriptionId={}", subscription.getId(), e);
            }
        }
        
        log.info("[reminder-schedule-job] 宽限期提醒任务生成完成，检查数={}, 创建数={}", subscriptions.size(), createdCount);
    }
    
    /**
     * 创建提醒任务（幂等）
     * 
     * @return true 如果创建了新任务，false 如果任务已存在
     */
    @Transactional
    protected boolean createReminderTask(TenantSubscriptionDO subscription, ReminderType reminderType, LocalDateTime now) {
        // 计算提醒时间
        LocalDateTime remindAt = subscription.getSubscriptionEndAt().minusDays(reminderType.getDaysBeforeExpire());
        
        // 如果提醒时间还没到，跳过
        if (remindAt.isAfter(now.plusHours(1))) {
            return false;
        }
        
        // 幂等性检查：subscription_id + remind_at 唯一
        BillingReminderTaskDO existingTask = reminderTaskMapper.selectOne(
            new LambdaQueryWrapper<BillingReminderTaskDO>()
                .eq(BillingReminderTaskDO::getSubscriptionId, subscription.getId())
                .eq(BillingReminderTaskDO::getRemindAt, remindAt)
        );
        
        if (existingTask != null) {
            log.debug("[reminder-schedule-job] 提醒任务已存在，跳过，subscriptionId={}, reminderType={}, remindAt={}", 
                    subscription.getId(), reminderType.getCode(), remindAt);
            return false;
        }
        
        // 创建提醒任务
        BillingReminderTaskDO task = new BillingReminderTaskDO();
        task.setSubscriptionId(subscription.getId());
        task.setTenantId(subscription.getTenantId());
        task.setReminderType(reminderType.getCode());
        task.setRemindAt(remindAt);
        task.setExpireAt(subscription.getSubscriptionEndAt());
        task.setPlanCode(subscription.getCurrentPlanCode());
        task.setPlanName(subscription.getCurrentPlanName());
        task.setStatus(ReminderStatus.PENDING.getCode());
        task.setRetryCount(0);
        task.setMaxRetryCount(3);
        task.setNextRetryAt(now); // 立即可以发送
        task.setNotificationChannels("IN_APP,EMAIL"); // 默认站内通知 + 邮件
        task.setCreatedAt(now);
        task.setUpdatedAt(now);
        
        reminderTaskMapper.insert(task);
        
        // 记录指标
        billingMetrics.recordReminderTaskCreated(reminderType.getCode());
        
        log.info("[reminder-schedule-job] 提醒任务创建成功，subscriptionId={}, tenantId={}, reminderType={}, remindAt={}", 
                subscription.getId(), subscription.getTenantId(), reminderType.getCode(), remindAt);
        
        return true;
    }
}
