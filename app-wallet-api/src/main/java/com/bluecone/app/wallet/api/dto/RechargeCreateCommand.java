package com.bluecone.app.wallet.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 充值创建命令
 * 
 * @author bluecone
 * @since 2025-12-19
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RechargeCreateCommand {
    
    /**
     * 租户ID
     */
    private Long tenantId;
    
    /**
     * 用户ID
     */
    private Long userId;
    
    /**
     * 充值金额（单位：元）
     */
    private BigDecimal rechargeAmount;
    
    /**
     * 支付渠道（WECHAT、ALIPAY）
     */
    private String payChannel;
    
    /**
     * 幂等键（客户端生成，用于防重）
     */
    private String idempotencyKey;
}
