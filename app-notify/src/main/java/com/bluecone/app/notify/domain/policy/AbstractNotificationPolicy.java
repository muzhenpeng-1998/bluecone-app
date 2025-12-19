package com.bluecone.app.notify.domain.policy;

import com.bluecone.app.notify.api.enums.NotificationChannel;

import java.util.Arrays;
import java.util.List;

/**
 * 通知策略抽象基类
 * 提供默认实现
 */
public abstract class AbstractNotificationPolicy implements NotificationPolicy {
    
    @Override
    public boolean isRateLimitEnabled() {
        return true; // 默认启用频控
    }
    
    @Override
    public int getDailyLimit() {
        return 10; // 默认每日10条
    }
    
    @Override
    public boolean isQuietHoursEnabled() {
        return true; // 默认启用免打扰
    }
    
    @Override
    public String getQuietHoursStart() {
        return "22:00"; // 默认晚上10点
    }
    
    @Override
    public String getQuietHoursEnd() {
        return "08:00"; // 默认早上8点
    }
    
    @Override
    public List<NotificationChannel> getSupportedChannels() {
        // 默认支持站内信和邮件
        return Arrays.asList(NotificationChannel.IN_APP, NotificationChannel.EMAIL);
    }
}
