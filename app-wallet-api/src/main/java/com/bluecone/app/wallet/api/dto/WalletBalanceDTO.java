package com.bluecone.app.wallet.api.dto;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 钱包余额DTO
 * 
 * @author bluecone
 * @since 2025-12-18
 */
public class WalletBalanceDTO implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    /**
     * 账户ID
     */
    private Long accountId;
    
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
    private BigDecimal availableBalance;
    
    /**
     * 冻结余额
     */
    private BigDecimal frozenBalance;
    
    /**
     * 总余额（可用+冻结）
     */
    private BigDecimal totalBalance;
    
    /**
     * 币种
     */
    private String currency;
    
    /**
     * 账户状态：ACTIVE、FROZEN、CLOSED
     */
    private String status;
    
    /**
     * 版本号（乐观锁）
     */
    private Integer version;
    
    public WalletBalanceDTO() {
    }
    
    public WalletBalanceDTO(Long accountId, Long tenantId, Long userId, 
                           BigDecimal availableBalance, BigDecimal frozenBalance,
                           String currency, String status, Integer version) {
        this.accountId = accountId;
        this.tenantId = tenantId;
        this.userId = userId;
        this.availableBalance = availableBalance;
        this.frozenBalance = frozenBalance;
        this.totalBalance = availableBalance.add(frozenBalance);
        this.currency = currency;
        this.status = status;
        this.version = version;
    }
    
    // Getters and Setters
    public Long getAccountId() {
        return accountId;
    }
    
    public void setAccountId(Long accountId) {
        this.accountId = accountId;
    }
    
    public Long getTenantId() {
        return tenantId;
    }
    
    public void setTenantId(Long tenantId) {
        this.tenantId = tenantId;
    }
    
    public Long getUserId() {
        return userId;
    }
    
    public void setUserId(Long userId) {
        this.userId = userId;
    }
    
    public BigDecimal getAvailableBalance() {
        return availableBalance;
    }
    
    public void setAvailableBalance(BigDecimal availableBalance) {
        this.availableBalance = availableBalance;
    }
    
    public BigDecimal getFrozenBalance() {
        return frozenBalance;
    }
    
    public void setFrozenBalance(BigDecimal frozenBalance) {
        this.frozenBalance = frozenBalance;
    }
    
    public BigDecimal getTotalBalance() {
        return totalBalance;
    }
    
    public void setTotalBalance(BigDecimal totalBalance) {
        this.totalBalance = totalBalance;
    }
    
    public String getCurrency() {
        return currency;
    }
    
    public void setCurrency(String currency) {
        this.currency = currency;
    }
    
    public String getStatus() {
        return status;
    }
    
    public void setStatus(String status) {
        this.status = status;
    }
    
    public Integer getVersion() {
        return version;
    }
    
    public void setVersion(Integer version) {
        this.version = version;
    }
}
