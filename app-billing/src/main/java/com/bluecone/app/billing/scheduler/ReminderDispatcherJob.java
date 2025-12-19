package com.bluecone.app.billing.scheduler;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.bluecone.app.billing.dao.entity.BillingDunningLogDO;
import com.bluecone.app.billing.dao.entity.BillingReminderTaskDO;
import com.bluecone.app.billing.dao.mapper.BillingDunningLogMapper;
import com.bluecone.app.billing.dao.mapper.BillingReminderTaskMapper;
import com.bluecone.app.billing.domain.enums.NotificationChannel;
import com.bluecone.app.billing.domain.enums.ReminderStatus;
import com.bluecone.app.billing.domain.enums.SendResult;
import com.bluecone.app.billing.notification.EmailNotificationSender;
import com.bluecone.app.billing.notification.InAppNotificationSender;
import com.bluecone.app.billing.notification.NotificationSender;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 提醒任务分发调度器
 * 定时扫描待发送的提醒任务，发送通知并记录日志
 * 支持指数退避重试（1分钟、5分钟、30分钟）
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ReminderDispatcherJob {
    
    private final BillingReminderTaskMapper reminderTaskMapper;
    private final BillingDunningLogMapper dunningLogMapper;
    private final InAppNotificationSender inAppNotificationSender;
    private final EmailNotificationSender emailNotificationSender;
    
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    
    // 指数退避重试间隔（分钟）
    private static final int[] RETRY_INTERVALS_MINUTES = {1, 5, 30};
    
    /**
     * 每分钟执行一次，扫描待发送的提醒任务
     */
    @Scheduled(cron = "0 * * * * ?")
    public void dispatchReminders() {
        log.info("[reminder-dispatcher-job] 开始扫描待发送的提醒任务");
        
        try {
            LocalDateTime now = LocalDateTime.now();
            
            // 查询待发送的提醒任务（状态为 PENDING 且到达重试时间）
            List<BillingReminderTaskDO> tasks = reminderTaskMapper.selectList(
                new LambdaQueryWrapper<BillingReminderTaskDO>()
                    .eq(BillingReminderTaskDO::getStatus, ReminderStatus.PENDING.getCode())
                    .le(BillingReminderTaskDO::getNextRetryAt, now)
                    .orderByAsc(BillingReminderTaskDO::getNextRetryAt)
                    .last("LIMIT 100") // 每次最多处理100条
            );
            
            if (tasks.isEmpty()) {
                log.debug("[reminder-dispatcher-job] 没有待发送的提醒任务");
                return;
            }
            
            log.info("[reminder-dispatcher-job] 发现 {} 个待发送的提醒任务", tasks.size());
            
            int successCount = 0;
            int failCount = 0;
            int retryCount = 0;
            
            for (BillingReminderTaskDO task : tasks) {
                try {
                    boolean success = dispatchTask(task, now);
                    if (success) {
                        successCount++;
                    } else if (task.getRetryCount() >= task.getMaxRetryCount()) {
                        failCount++;
                    } else {
                        retryCount++;
                    }
                } catch (Exception e) {
                    log.error("[reminder-dispatcher-job] 处理提醒任务失败，taskId={}", task.getId(), e);
                    retryCount++;
                }
            }
            
            log.info("[reminder-dispatcher-job] 提醒任务分发完成，总数={}, 成功={}, 失败={}, 待重试={}", 
                    tasks.size(), successCount, failCount, retryCount);
            
        } catch (Exception e) {
            log.error("[reminder-dispatcher-job] 提醒任务分发失败", e);
        }
    }
    
    /**
     * 分发单个提醒任务
     * 
     * @return true 如果发送成功，false 如果需要重试或已失败
     */
    @Transactional
    protected boolean dispatchTask(BillingReminderTaskDO task, LocalDateTime now) {
        log.info("[reminder-dispatcher-job] 开始发送提醒，taskId={}, tenantId={}, reminderType={}, retryCount={}", 
                task.getId(), task.getTenantId(), task.getReminderType(), task.getRetryCount());
        
        // 解析通知渠道
        List<NotificationChannel> channels = parseChannels(task.getNotificationChannels());
        
        // 计算剩余天数
        int daysRemaining = (int) ChronoUnit.DAYS.between(now.toLocalDate(), task.getExpireAt().toLocalDate());
        
        // 发送通知到各个渠道
        boolean allSuccess = true;
        StringBuilder errorMessages = new StringBuilder();
        
        for (NotificationChannel channel : channels) {
            try {
                NotificationSender.SendResult result = sendNotification(
                    channel,
                    task.getTenantId(),
                    task.getReminderType(),
                    task.getPlanName(),
                    task.getExpireAt().format(DATE_FORMATTER),
                    daysRemaining
                );
                
                // 记录 Dunning 日志
                recordDunningLog(task, channel, result, now);
                
                if (!result.isSuccess()) {
                    allSuccess = false;
                    errorMessages.append(channel.getCode()).append(": ").append(result.getMessage()).append("; ");
                }
                
            } catch (Exception e) {
                allSuccess = false;
                errorMessages.append(channel.getCode()).append(": ").append(e.getMessage()).append("; ");
                log.error("[reminder-dispatcher-job] 发送通知失败，taskId={}, channel={}", task.getId(), channel, e);
                
                // 记录失败日志
                recordDunningLog(task, channel, NotificationSender.SendResult.failure(e.getMessage()), now);
            }
        }
        
        // 更新任务状态
        if (allSuccess) {
            // 全部成功，标记为已发送
            task.setStatus(ReminderStatus.SENT.getCode());
            task.setSentAt(now);
            task.setUpdatedAt(now);
            reminderTaskMapper.updateById(task);
            
            log.info("[reminder-dispatcher-job] 提醒发送成功，taskId={}, tenantId={}", task.getId(), task.getTenantId());
            return true;
            
        } else {
            // 有失败，判断是否需要重试
            task.setRetryCount(task.getRetryCount() + 1);
            task.setLastError(errorMessages.toString());
            task.setUpdatedAt(now);
            
            if (task.getRetryCount() >= task.getMaxRetryCount()) {
                // 重试耗尽，标记为失败
                task.setStatus(ReminderStatus.FAILED.getCode());
                task.setNextRetryAt(null);
                
                log.warn("[reminder-dispatcher-job] 提醒发送失败（重试耗尽），taskId={}, tenantId={}, error={}", 
                        task.getId(), task.getTenantId(), errorMessages.toString());
            } else {
                // 计算下次重试时间（指数退避）
                int retryIntervalMinutes = RETRY_INTERVALS_MINUTES[Math.min(task.getRetryCount() - 1, RETRY_INTERVALS_MINUTES.length - 1)];
                task.setNextRetryAt(now.plusMinutes(retryIntervalMinutes));
                
                log.info("[reminder-dispatcher-job] 提醒发送失败，将在 {} 分钟后重试，taskId={}, retryCount={}, error={}", 
                        retryIntervalMinutes, task.getId(), task.getRetryCount(), errorMessages.toString());
            }
            
            reminderTaskMapper.updateById(task);
            return false;
        }
    }
    
    /**
     * 发送通知
     */
    private NotificationSender.SendResult sendNotification(
        NotificationChannel channel,
        Long tenantId,
        String reminderType,
        String planName,
        String expireAt,
        int daysRemaining
    ) {
        switch (channel) {
            case IN_APP:
                return inAppNotificationSender.send(channel, tenantId, reminderType, planName, expireAt, daysRemaining);
            case EMAIL:
                return emailNotificationSender.send(channel, tenantId, reminderType, planName, expireAt, daysRemaining);
            case SMS:
                // TODO: 实现短信通知
                return NotificationSender.SendResult.failure("短信通知暂未实现");
            default:
                return NotificationSender.SendResult.failure("不支持的通知渠道: " + channel);
        }
    }
    
    /**
     * 记录 Dunning 日志
     */
    private void recordDunningLog(
        BillingReminderTaskDO task,
        NotificationChannel channel,
        NotificationSender.SendResult result,
        LocalDateTime now
    ) {
        BillingDunningLogDO log = new BillingDunningLogDO();
        log.setReminderTaskId(task.getId());
        log.setSubscriptionId(task.getSubscriptionId());
        log.setTenantId(task.getTenantId());
        log.setReminderType(task.getReminderType());
        log.setNotificationChannel(channel.getCode());
        log.setRecipient(null); // TODO: 填充实际接收者
        log.setSendResult(result.isSuccess() ? SendResult.SUCCESS.getCode() : SendResult.FAILED.getCode());
        log.setErrorMessage(result.isSuccess() ? null : result.getMessage());
        log.setResponseData(result.getResponseData());
        log.setCreatedAt(now);
        
        dunningLogMapper.insert(log);
    }
    
    /**
     * 解析通知渠道
     */
    private List<NotificationChannel> parseChannels(String channelsStr) {
        if (channelsStr == null || channelsStr.isEmpty()) {
            return Arrays.asList(NotificationChannel.IN_APP);
        }
        
        return Arrays.stream(channelsStr.split(","))
                .map(String::trim)
                .map(NotificationChannel::fromCode)
                .filter(channel -> channel != null)
                .collect(Collectors.toList());
    }
}
