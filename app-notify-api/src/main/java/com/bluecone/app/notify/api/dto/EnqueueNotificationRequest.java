package com.bluecone.app.notify.api.dto;

import com.bluecone.app.notify.api.enums.NotificationChannel;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * 入队通知请求
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EnqueueNotificationRequest {
    
    /**
     * 业务类型（BILLING/ORDER/REFUND/SUBSCRIPTION 等）
     */
    @NotBlank(message = "业务类型不能为空")
    private String bizType;
    
    /**
     * 业务ID（订单号、账单号、退款单号）
     */
    @NotBlank(message = "业务ID不能为空")
    private String bizId;
    
    /**
     * 租户ID（可选）
     */
    private Long tenantId;
    
    /**
     * 用户ID
     */
    @NotNull(message = "用户ID不能为空")
    private Long userId;
    
    /**
     * 模板编码
     */
    @NotBlank(message = "模板编码不能为空")
    private String templateCode;
    
    /**
     * 通知渠道列表
     */
    @NotEmpty(message = "通知渠道不能为空")
    private List<NotificationChannel> channels;
    
    /**
     * 模板变量值
     */
    @NotNull(message = "模板变量不能为空")
    private Map<String, Object> variables;
    
    /**
     * 优先级（可选，默认50）
     */
    private Integer priority;
}
