package com.bluecone.app.ops.api.dto.forensics;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 钱包流水记录
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WalletLedgerItem {
    
    /**
     * 流水ID
     */
    private Long id;
    
    /**
     * 用户ID
     */
    private Long userId;
    
    /**
     * 账户ID
     */
    private Long accountId;
    
    /**
     * 流水号
     */
    private String ledgerNo;
    
    /**
     * 业务类型
     */
    private String bizType;
    
    /**
     * 业务订单ID
     */
    private Long bizOrderId;
    
    /**
     * 业务订单号
     */
    private String bizOrderNo;
    
    /**
     * 变动金额
     */
    private BigDecimal amount;
    
    /**
     * 变动前余额
     */
    private BigDecimal balanceBefore;
    
    /**
     * 变动后余额
     */
    private BigDecimal balanceAfter;
    
    /**
     * 币种
     */
    private String currency;
    
    /**
     * 备注
     */
    private String remark;
    
    /**
     * 幂等键
     */
    private String idemKey;
    
    /**
     * 创建时间
     */
    private LocalDateTime createdAt;
}
