package com.bluecone.app.wallet.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 钱包资产操作命令
 * 用于冻结、提交、释放、回退等操作
 * 
 * @author bluecone
 * @since 2025-12-18
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WalletAssetCommand implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    /**
     * 租户ID（必填）
     */
    private Long tenantId;
    
    /**
     * 用户ID（必填）
     */
    private Long userId;
    
    /**
     * 操作金额（必填）
     */
    private BigDecimal amount;
    
    /**
     * 币种
     */
    @Builder.Default
    private String currency = "CNY";
    
    /**
     * 业务类型：ORDER_CHECKOUT、ORDER_PAY、REFUND等
     */
    private String bizType;
    
    /**
     * 业务单ID（订单ID等）
     */
    private Long bizOrderId;
    
    /**
     * 业务ID（字符串格式，兼容性字段）
     */
    private String bizId;
    
    /**
     * 业务单号（冗余）
     */
    private String bizOrderNo;
    
    /**
     * 幂等键（必填，格式：{tenantId}:{userId}:{bizOrderId}:{operationType}）
     */
    private String idempotencyKey;
    
    /**
     * 备注
     */
    private String remark;
    
    /**
     * 操作人ID
     */
    private Long operatorId;
}
