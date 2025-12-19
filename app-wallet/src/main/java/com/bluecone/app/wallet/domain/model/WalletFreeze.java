package com.bluecone.app.wallet.domain.model;

import com.bluecone.app.wallet.domain.enums.FreezeStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 钱包冻结记录领域模型
 * 
 * @author bluecone
 * @since 2025-12-18
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WalletFreeze implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    /**
     * 冻结ID
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
     * 冻结单号
     */
    private String freezeNo;
    
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
     * 冻结金额
     */
    private BigDecimal frozenAmount;
    
    /**
     * 币种
     */
    @Builder.Default
    private String currency = "CNY";
    
    /**
     * 冻结状态
     */
    private FreezeStatus status;
    
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
     * 审计字段
     */
    @Builder.Default
    private Integer version = 0;
    private LocalDateTime createdAt;
    private Long createdBy;
    private LocalDateTime updatedAt;
    private Long updatedBy;
    
    /**
     * 检查是否可以提交
     */
    public boolean canCommit() {
        return FreezeStatus.FROZEN.equals(this.status);
    }
    
    /**
     * 检查是否可以释放
     */
    public boolean canRelease() {
        return FreezeStatus.FROZEN.equals(this.status);
    }
    
    /**
     * 标记为已提交
     */
    public void markCommitted() {
        if (!canCommit()) {
            throw new IllegalStateException("冻结状态不允许提交：" + this.status);
        }
        this.status = FreezeStatus.COMMITTED;
        this.committedAt = LocalDateTime.now();
    }
    
    /**
     * 标记为已释放
     */
    public void markReleased() {
        if (!canRelease()) {
            throw new IllegalStateException("冻结状态不允许释放：" + this.status);
        }
        this.status = FreezeStatus.RELEASED;
        this.releasedAt = LocalDateTime.now();
    }
    
    /**
     * 检查是否已过期
     */
    public boolean isExpired() {
        return expiresAt != null && LocalDateTime.now().isAfter(expiresAt);
    }
}
