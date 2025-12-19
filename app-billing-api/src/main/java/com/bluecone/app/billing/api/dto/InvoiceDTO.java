package com.bluecone.app.billing.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 账单 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InvoiceDTO {
    
    private Long id;
    private Long tenantId;
    
    private String invoiceNo;
    
    private Long planSkuId;
    private String planCode;
    private String planName;
    private String billingPeriod;
    private Integer periodMonths;
    
    private Long amountFen;
    private Long paidAmountFen;
    
    private String paymentChannel;
    private String channelTradeNo;
    private LocalDateTime paidAt;
    
    private String status;
    
    private LocalDateTime effectiveStartAt;
    private LocalDateTime effectiveEndAt;
    
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
