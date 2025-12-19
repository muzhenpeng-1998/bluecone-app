package com.bluecone.app.notify.domain.policy;

import com.bluecone.app.notify.api.enums.NotificationChannel;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 账单支付成功通知策略
 */
@Component
public class InvoicePaidPolicy extends AbstractNotificationPolicy {
    
    public static final String BIZ_TYPE = "INVOICE_PAID";
    public static final String TEMPLATE_CODE = "INVOICE_PAID_REMINDER";
    
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
        return 5; // 每日最多5条账单通知
    }
    
    @Override
    public Map<String, Object> extractVariables(Object eventPayload) {
        // 从 InvoicePaidEvent 提取变量
        // 简化实现：假设 eventPayload 是 Map
        if (eventPayload instanceof Map) {
            Map<?, ?> payload = (Map<?, ?>) eventPayload;
            Map<String, Object> variables = new HashMap<>();
            variables.put("invoiceNo", payload.get("invoiceNo"));
            variables.put("planName", payload.get("planName"));
            variables.put("amount", formatAmount((Long) payload.get("paidAmountFen")));
            variables.put("effectiveStartAt", payload.get("effectiveStartAt"));
            variables.put("effectiveEndAt", payload.get("effectiveEndAt"));
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
