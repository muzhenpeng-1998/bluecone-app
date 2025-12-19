package com.bluecone.app.billing.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * 创建账单结果
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateInvoiceResult {
    
    /**
     * 账单ID
     */
    private Long invoiceId;
    
    /**
     * 账单号
     */
    private String invoiceNo;
    
    /**
     * 应付金额（分）
     */
    private Long amountFen;
    
    /**
     * 支付参数（微信/支付宝支付参数）
     */
    private Map<String, Object> paymentParams;
}
