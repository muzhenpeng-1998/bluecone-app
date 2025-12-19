package com.bluecone.app.notify.domain.policy;

import com.bluecone.app.notify.api.enums.NotificationChannel;

import java.util.List;
import java.util.Map;

/**
 * 通知策略接口
 * 定义不同业务类型的通知规则（渠道、频控等）
 */
public interface NotificationPolicy {
    
    /**
     * 获取业务类型
     */
    String getBizType();
    
    /**
     * 获取支持的渠道
     */
    List<NotificationChannel> getSupportedChannels();
    
    /**
     * 获取模板编码
     */
    String getTemplateCode();
    
    /**
     * 是否启用频控
     */
    boolean isRateLimitEnabled();
    
    /**
     * 获取每日发送上限（每用户每模板）
     */
    int getDailyLimit();
    
    /**
     * 是否启用免打扰时间
     */
    boolean isQuietHoursEnabled();
    
    /**
     * 获取免打扰开始时间（HH:mm）
     */
    String getQuietHoursStart();
    
    /**
     * 获取免打扰结束时间（HH:mm）
     */
    String getQuietHoursEnd();
    
    /**
     * 从事件载荷中提取模板变量
     */
    Map<String, Object> extractVariables(Object eventPayload);
}
