package com.bluecone.app.wallet.infra.persistence.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 钱包冻结记录PO
 * 
 * @author bluecone
 * @since 2025-12-18
 */
@Data
@TableName("bc_wallet_freeze")
public class WalletFreezePO implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    @TableId(value = "id", type = IdType.INPUT)
    private Long id;
    
    private Long tenantId;
    
    private Long userId;
    
    private Long accountId;
    
    private String freezeNo;
    
    private String bizType;
    
    private Long bizOrderId;
    
    private String bizOrderNo;
    
    private BigDecimal frozenAmount;
    
    private String currency;
    
    private String status;
    
    private String idemKey;
    
    private LocalDateTime frozenAt;
    
    private LocalDateTime expiresAt;
    
    private LocalDateTime committedAt;
    
    private LocalDateTime releasedAt;
    
    private LocalDateTime revertedAt;
    
    @Version
    private Integer version;
    
    private LocalDateTime createdAt;
    
    private Long createdBy;
    
    private LocalDateTime updatedAt;
    
    private Long updatedBy;
}
