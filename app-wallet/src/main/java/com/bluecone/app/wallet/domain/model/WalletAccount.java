package com.bluecone.app.wallet.domain.model;

import com.bluecone.app.wallet.domain.enums.AccountStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 钱包账户领域模型
 * 
 * @author bluecone
 * @since 2025-12-18
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WalletAccount implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    /**
     * 账户ID
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
     * 可用余额
     */
    @Builder.Default
    private BigDecimal availableBalance = BigDecimal.ZERO;
    
    /**
     * 冻结余额
     */
    @Builder.Default
    private BigDecimal frozenBalance = BigDecimal.ZERO;
    
    /**
     * 累计充值金额
     */
    @Builder.Default
    private BigDecimal totalRecharged = BigDecimal.ZERO;
    
    /**
     * 累计消费金额
     */
    @Builder.Default
    private BigDecimal totalConsumed = BigDecimal.ZERO;
    
    /**
     * 币种
     */
    @Builder.Default
    private String currency = "CNY";
    
    /**
     * 账户状态
     */
    private AccountStatus status;
    
    /**
     * 乐观锁版本号
     */
    @Builder.Default
    private Integer version = 0;
    
    /**
     * 审计字段
     */
    private LocalDateTime createdAt;
    private Long createdBy;
    private LocalDateTime updatedAt;
    private Long updatedBy;
    
    /**
     * 检查账户是否活跃
     */
    public boolean isActive() {
        return AccountStatus.ACTIVE.equals(this.status);
    }
    
    /**
     * 检查可用余额是否足够
     */
    public boolean hasEnoughBalance(BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            return true;
        }
        return availableBalance != null && availableBalance.compareTo(amount) >= 0;
    }
    
    /**
     * 冻结余额（可用 -> 冻结）
     */
    public void freeze(BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("冻结金额必须大于0");
        }
        if (!hasEnoughBalance(amount)) {
            throw new IllegalStateException("可用余额不足，无法冻结");
        }
        this.availableBalance = this.availableBalance.subtract(amount);
        this.frozenBalance = this.frozenBalance.add(amount);
    }
    
    /**
     * 提交冻结（冻结 -> 扣除）
     */
    public void commitFreeze(BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("提交金额必须大于0");
        }
        if (this.frozenBalance.compareTo(amount) < 0) {
            throw new IllegalStateException("冻结余额不足，无法提交");
        }
        this.frozenBalance = this.frozenBalance.subtract(amount);
        this.totalConsumed = this.totalConsumed.add(amount);
    }
    
    /**
     * 释放冻结（冻结 -> 可用）
     */
    public void releaseFreeze(BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("释放金额必须大于0");
        }
        if (this.frozenBalance.compareTo(amount) < 0) {
            throw new IllegalStateException("冻结余额不足，无法释放");
        }
        this.frozenBalance = this.frozenBalance.subtract(amount);
        this.availableBalance = this.availableBalance.add(amount);
    }
    
    /**
     * 增加可用余额（充值、退款）
     */
    public void increaseAvailableBalance(BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("增加金额必须大于0");
        }
        this.availableBalance = this.availableBalance.add(amount);
    }
    
    /**
     * 减少可用余额（直接扣减，无冻结）
     */
    public void decreaseAvailableBalance(BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("减少金额必须大于0");
        }
        if (!hasEnoughBalance(amount)) {
            throw new IllegalStateException("可用余额不足，无法扣减");
        }
        this.availableBalance = this.availableBalance.subtract(amount);
    }
}
