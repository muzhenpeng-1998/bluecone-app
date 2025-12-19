package com.bluecone.app.ops.api.dto.forensics;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 钱包冻结记录
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WalletFreezeItem {
    
    /**
     * 冻结记录ID
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
     * 冻结单号
     */
    private String freezeNo;
    
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
     * 冻结金额
     */
    private BigDecimal frozenAmount;
    
    /**
     * 币种
     */
    private String currency;
    
    /**
     * 状态：FROZEN/COMMITTED/RELEASED/REVERTED
     */
    private String status;
    
    /**
     * 幂等键
     */
    private String idemKey;
    
    /**
     * 冻结时间
     */
    private LocalDateTime frozenAt;
    
    /**
     * 过期时间
     */
    private LocalDateTime expiresAt;
    
    /**
     * 提交时间
     */
    private LocalDateTime committedAt;
    
    /**
     * 释放时间
     */
    private LocalDateTime releasedAt;
    
    /**
     * 回退时间
     */
    private LocalDateTime revertedAt;
    
    /**
     * 创建时间
     */
    private LocalDateTime createdAt;
}
