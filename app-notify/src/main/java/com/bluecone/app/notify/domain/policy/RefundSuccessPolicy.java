package com.bluecone.app.notify.domain.policy;

import com.bluecone.app.notify.api.enums.NotificationChannel;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 退款成功通知策略
 */
@Component
public class RefundSuccessPolicy extends AbstractNotificationPolicy {
    
    public static final String BIZ_TYPE = "REFUND_SUCCESS";
    public static final String TEMPLATE_CODE = "REFUND_SUCCESS";
    
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
        // 退款通知：站内信
        return Arrays.asList(NotificationChannel.IN_APP);
    }
    
    @Override
    public int getDailyLimit() {
        return 10;
    }
    
    @Override
    public Map<String, Object> extractVariables(Object eventPayload) {
        if (eventPayload instanceof Map) {
            Map<?, ?> payload = (Map<?, ?>) eventPayload;
            Map<String, Object> variables = new HashMap<>();
            variables.put("orderNo", payload.get("orderNo"));
            variables.put("refundAmount", formatAmount((Long) payload.get("refundAmountFen")));
            return variables;
        }
        return new HashMap<>();
    }
    
    private String formatAmount(Long amountFen) {
        if (amountFen == null) {
            return "0.00";
        }
        return String.format("%.2f", amountFen / 100.0);
    }
}
