package com.bluecone.app.billing.api.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 续费订阅命令
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RenewSubscriptionCommand {
    
    /**
     * 租户ID（从请求头获取）
     */
    private Long tenantId;
    
    /**
     * 套餐 SKU ID（可选，不传则使用当前套餐）
     */
    private Long planSkuId;
    
    /**
     * 支付渠道：WECHAT/ALIPAY
     */
    @NotNull(message = "支付渠道不能为空")
    private String paymentChannel;
    
    /**
     * 幂等键（可选，不传则自动生成）
     */
    private String idempotencyKey;
}
