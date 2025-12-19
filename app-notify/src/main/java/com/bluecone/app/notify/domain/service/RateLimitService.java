package com.bluecone.app.notify.domain.service;

import com.bluecone.app.notify.domain.model.NotifyTask;
import com.bluecone.app.notify.domain.model.UserPreference;
import com.bluecone.app.notify.domain.policy.NotificationPolicy;
import com.bluecone.app.notify.domain.policy.NotificationPolicyRegistry;
import com.bluecone.app.notify.domain.repository.NotifySendLogRepository;
import com.bluecone.app.notify.domain.repository.UserPreferenceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Optional;

/**
 * 频控服务
 * 检查通知任务是否满足频控策略
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RateLimitService {
    
    private final NotificationPolicyRegistry policyRegistry;
    private final NotifySendLogRepository sendLogRepository;
    private final UserPreferenceRepository preferenceRepository;
    
    /**
     * 检查任务是否通过频控
     * 
     * @param task 通知任务
     * @return true=通过，false=被限制
     */
    public boolean checkRateLimit(NotifyTask task) {
        // 1. 获取策略
        Optional<NotificationPolicy> policyOpt = policyRegistry.getPolicy(task.getBizType());
        if (policyOpt.isEmpty()) {
            log.warn("No policy found for bizType={}, allowing by default", task.getBizType());
            return true;
        }
        
        NotificationPolicy policy = policyOpt.get();
        
        // 2. 检查免打扰时间
        if (policy.isQuietHoursEnabled() && isInQuietHours(task, policy)) {
            log.info("Task {} is in quiet hours, rate limited", task.getId());
            return false;
        }
        
        // 3. 检查每日发送上限
        if (policy.isRateLimitEnabled() && exceedsDailyLimit(task, policy)) {
            log.info("Task {} exceeds daily limit, rate limited", task.getId());
            return false;
        }
        
        return true;
    }
    
    /**
     * 判断是否在免打扰时间
     */
    private boolean isInQuietHours(NotifyTask task, NotificationPolicy policy) {
        // 1. 检查用户偏好
        Optional<UserPreference> prefOpt = preferenceRepository.findByUserId(task.getUserId(), task.getTenantId());
        if (prefOpt.isPresent() && prefOpt.get().isInQuietHours()) {
            return true;
        }
        
        // 2. 使用策略默认配置
        if (policy.isQuietHoursEnabled()) {
            LocalTime now = LocalTime.now();
            LocalTime start = LocalTime.parse(policy.getQuietHoursStart());
            LocalTime end = LocalTime.parse(policy.getQuietHoursEnd());
            
            // 处理跨天情况
            if (start.isAfter(end)) {
                return now.isAfter(start) || now.isBefore(end);
            } else {
                return now.isAfter(start) && now.isBefore(end);
            }
        }
        
        return false;
    }
    
    /**
     * 判断是否超过每日发送上限
     */
    private boolean exceedsDailyLimit(NotifyTask task, NotificationPolicy policy) {
        LocalDateTime startOfDay = LocalDateTime.now().withHour(0).withMinute(0).withSecond(0);
        
        // 统计今天已发送次数
        int sentToday = sendLogRepository.countByUserAndChannelSince(
                task.getUserId(), 
                task.getChannel(), 
                startOfDay
        );
        
        return sentToday >= policy.getDailyLimit();
    }
}
