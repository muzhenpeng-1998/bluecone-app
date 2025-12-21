package com.bluecone.app.notify.scheduler;

import com.bluecone.app.notify.channel.NotificationChannel;
import com.bluecone.app.notify.channel.NotificationChannelRegistry;
import com.bluecone.app.notify.domain.model.NotifyTask;
import com.bluecone.app.notify.domain.model.NotifySendLog;
import com.bluecone.app.notify.domain.repository.NotifySendLogRepository;
import com.bluecone.app.notify.domain.repository.NotifyTaskRepository;
import com.bluecone.app.notify.domain.service.RateLimitService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

/**
 * 通知调度任务
 * 扫描 PENDING/FAILED 任务并发送
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class NotifyDispatcherJob {
    
    private final NotifyTaskRepository taskRepository;
    private final NotifySendLogRepository sendLogRepository;
    private final NotificationChannelRegistry channelRegistry;
    private final RateLimitService rateLimitService;
    
    private static final int BATCH_SIZE = 100;
    
    /**
     * 调度待发送任务（每分钟执行一次）
     */
    @Scheduled(cron = "0 * * * * ?")
    public void dispatchPendingTasks() {
//        log.info("Start dispatching pending tasks");
        
        List<NotifyTask> tasks = taskRepository.findPendingTasks(BATCH_SIZE);
//        log.info("Found {} pending tasks", tasks.size());
        
        for (NotifyTask task : tasks) {
            try {
                dispatchTask(task);
            } catch (Exception e) {
                log.error("Failed to dispatch task {}", task.getId(), e);
            }
        }
        
//        log.info("Finished dispatching pending tasks");
    }
    
    /**
     * 调度失败重试任务（每5分钟执行一次）
     */
    @Scheduled(cron = "0 */5 * * * ?")
    public void dispatchRetryTasks() {
//        log.info("Start dispatching retry tasks");
        
        List<NotifyTask> tasks = taskRepository.findTasksForRetry(BATCH_SIZE);
//        log.info("Found {} tasks for retry", tasks.size());
        
        for (NotifyTask task : tasks) {
            try {
                dispatchTask(task);
            } catch (Exception e) {
                log.error("Failed to dispatch retry task {}", task.getId(), e);
            }
        }
        
//        log.info("Finished dispatching retry tasks");
    }
    
    /**
     * 调度单个任务
     */
    private void dispatchTask(NotifyTask task) {
        log.debug("Dispatching task {}: channel={}, userId={}", task.getId(), task.getChannel(), task.getUserId());
        
        // 1. 频控检查
        if (!rateLimitService.checkRateLimit(task)) {
            task.markAsRateLimited();
            taskRepository.update(task);
            log.info("Task {} failed rate limit check", task.getId());
            return;
        }
        
        task.passRateLimitCheck();
        
        // 2. 获取渠道适配器
        Optional<NotificationChannel> channelOpt = channelRegistry.getChannel(task.getChannel());
        if (channelOpt.isEmpty()) {
            log.error("No channel adapter found for {}", task.getChannel());
            task.markAsFailed("No channel adapter found");
            taskRepository.update(task);
            return;
        }
        
        NotificationChannel channel = channelOpt.get();
        
        // 3. 获取接收方地址
        String recipient = resolveRecipient(task);
        if (recipient == null) {
            log.error("Failed to resolve recipient for task {}", task.getId());
            task.markAsFailed("Failed to resolve recipient");
            taskRepository.update(task);
            return;
        }
        
        // 4. 标记为发送中
        task.markAsSending();
        taskRepository.update(task);
        
        // 5. 发送通知
        NotificationChannel.SendResult result = channel.send(task, recipient);
        
        // 6. 记录日志
        NotifySendLog sendLog;
        if (result.isSuccess()) {
            sendLog = NotifySendLog.success(task, recipient, result.getDurationMs());
            task.markAsSent();
        } else {
            sendLog = NotifySendLog.failure(task, recipient, result.getErrorCode(), 
                                          result.getErrorMessage(), result.getDurationMs());
            task.markAsFailed(result.getErrorMessage());
        }
        
        sendLogRepository.save(sendLog);
        taskRepository.update(task);
        
//        log.info("Task {} dispatched: success={}, durationMs={}",
//                task.getId(), result.isSuccess(), result.getDurationMs());
    }
    
    /**
     * 解析接收方地址
     * TODO: 从用户信息中获取邮箱/手机号/OpenID
     */
    private String resolveRecipient(NotifyTask task) {
        switch (task.getChannel()) {
            case EMAIL:
                // TODO: 从用户服务查询邮箱
                return "user" + task.getUserId() + "@example.com";
            case SMS:
                // TODO: 从用户服务查询手机号
                return "13800000000";
            case WECHAT:
                // TODO: 从用户服务查询 OpenID
                return "wechat_openid_" + task.getUserId();
            case IN_APP:
                // 站内信直接使用 userId
                return String.valueOf(task.getUserId());
            default:
                return null;
        }
    }
}
