package com.bluecone.app.notify.domain.policy;

import com.bluecone.app.notify.api.enums.NotificationChannel;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 续费成功通知策略
 */
@Component
public class RenewalSuccessPolicy extends AbstractNotificationPolicy {
    
    public static final String BIZ_TYPE = "RENEWAL_SUCCESS";
    public static final String TEMPLATE_CODE = "RENEWAL_SUCCESS";
    
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
        // 支持站内信和邮件
        return Arrays.asList(NotificationChannel.IN_APP, NotificationChannel.EMAIL);
    }
    
    @Override
    public int getDailyLimit() {
        return 3; // 每日最多3条续费通知
    }
    
    @Override
    public Map<String, Object> extractVariables(Object eventPayload) {
        if (eventPayload instanceof Map) {
            Map<?, ?> payload = (Map<?, ?>) eventPayload;
            Map<String, Object> variables = new HashMap<>();
            variables.put("planName", payload.get("planName"));
            variables.put("newEndDate", payload.get("newEndDate"));
            return variables;
        }
        return new HashMap<>();
    }
}
