package com.bluecone.app.wallet.infra.persistence.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 钱包账本流水PO
 * 
 * @author bluecone
 * @since 2025-12-18
 */
@Data
@TableName("bc_wallet_ledger")
public class WalletLedgerPO implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    @TableId(value = "id", type = IdType.INPUT)
    private Long id;
    
    private Long tenantId;
    
    private Long userId;
    
    private Long accountId;
    
    private String ledgerNo;
    
    private String bizType;
    
    private Long bizOrderId;
    
    private String bizOrderNo;
    
    private BigDecimal amount;
    
    private BigDecimal balanceBefore;
    
    private BigDecimal balanceAfter;
    
    private String currency;
    
    private String remark;
    
    private String idemKey;
    
    private LocalDateTime createdAt;
    
    private Long createdBy;
}
