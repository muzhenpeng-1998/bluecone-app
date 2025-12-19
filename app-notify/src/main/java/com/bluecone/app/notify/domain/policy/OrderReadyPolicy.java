package com.bluecone.app.notify.domain.policy;

import com.bluecone.app.notify.api.enums.NotificationChannel;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 订单待取货通知策略
 */
@Component
public class OrderReadyPolicy extends AbstractNotificationPolicy {
    
    public static final String BIZ_TYPE = "ORDER_READY";
    public static final String TEMPLATE_CODE = "ORDER_READY";
    
    @Override
    public String getBizType() {
        return BIZ_TYPE;
    }
    
    @Override
    public String getTemplateCode() {
        return TEMPLATE_CODE;
    }
    
    @Override
    public List<NotificationChannel> getSupportedChannels() {
        // 订单通知：站内信 + 预留微信订阅消息
        return Arrays.asList(NotificationChannel.IN_APP);
    }
    
    @Override
    public int getDailyLimit() {
        return 20; // 订单通知频率较高
    }
    
    @Override
    public boolean isQuietHoursEnabled() {
        return false; // 订单通知不受免打扰限制
    }
    
    @Override
    public Map<String, Object> extractVariables(Object eventPayload) {
        if (eventPayload instanceof Map) {
            Map<?, ?> payload = (Map<?, ?>) eventPayload;
            Map<String, Object> variables = new HashMap<>();
            variables.put("orderNo", payload.get("orderNo"));
            variables.put("productName", payload.get("productName"));
            variables.put("storeName", payload.get("storeName"));
            return variables;
        }
        return new HashMap<>();
    }
}
