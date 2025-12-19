package com.bluecone.app.billing.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 订阅 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SubscriptionDTO {
    
    private Long id;
    private Long tenantId;
    
    private String currentPlanCode;
    private String currentPlanName;
    private Integer currentPlanLevel;
    
    private Map<String, Object> currentFeatures;
    
    private LocalDateTime subscriptionStartAt;
    private LocalDateTime subscriptionEndAt;
    
    private String status;
    
    private Long lastInvoiceId;
    private LocalDateTime lastPaidAt;
    
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    /**
     * 剩余天数（正数表示未到期，0表示今天到期，负数表示已过期）
     */
    private Integer daysRemaining;
    
    /**
     * 是否在宽限期
     */
    private Boolean inGracePeriod;
    
    /**
     * 宽限期剩余天数（仅在宽限期内有值）
     */
    private Integer graceDaysRemaining;
}
