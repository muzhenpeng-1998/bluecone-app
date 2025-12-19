package com.bluecone.app.billing.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 创建账单命令
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateInvoiceCommand {
    
    /**
     * 租户ID
     */
    private Long tenantId;
    
    /**
     * 套餐 SKU ID
     */
    private Long planSkuId;
    
    /**
     * 幂等键（防止重复创建）
     */
    private String idempotencyKey;
    
    /**
     * 支付渠道：WECHAT/ALIPAY
     */
    private String paymentChannel;
}
