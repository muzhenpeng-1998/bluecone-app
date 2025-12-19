package com.bluecone.app.wallet.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 钱包账本流水领域模型
 * 
 * @author bluecone
 * @since 2025-12-18
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WalletLedger implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    /**
     * 流水ID
     */
    private Long id;
    
    /**
     * 租户ID
     */
    private Long tenantId;
    
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
     * 业务单ID
     */
    private Long bizOrderId;
    
    /**
     * 业务单号
     */
    private String bizOrderNo;
    
    /**
     * 变更金额（正数=入账，负数=出账）
     */
    private BigDecimal amount;
    
    /**
     * 变更前余额
     */
    private BigDecimal balanceBefore;
    
    /**
     * 变更后余额
     */
    private BigDecimal balanceAfter;
    
    /**
     * 币种
     */
    @Builder.Default
    private String currency = "CNY";
    
    /**
     * 流水备注
     */
    private String remark;
    
    /**
     * 幂等键
     */
    private String idemKey;
    
    /**
     * 审计字段
     */
    private LocalDateTime createdAt;
    private Long createdBy;
    
    /**
     * 是否入账
     */
    public boolean isIncome() {
        return amount != null && amount.compareTo(BigDecimal.ZERO) > 0;
    }
    
    /**
     * 是否出账
     */
    public boolean isExpense() {
        return amount != null && amount.compareTo(BigDecimal.ZERO) < 0;
    }
}
