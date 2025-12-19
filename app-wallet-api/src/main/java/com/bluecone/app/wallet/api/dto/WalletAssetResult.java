package com.bluecone.app.wallet.api.dto;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 钱包资产操作结果
 * 
 * @author bluecone
 * @since 2025-12-18
 */
public class WalletAssetResult implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    /**
     * 操作是否成功
     */
    private boolean success;
    
    /**
     * 账户ID
     */
    private Long accountId;
    
    /**
     * 冻结单号（freeze操作返回）
     */
    private String freezeNo;
    
    /**
     * 流水号（commit/revert操作返回）
     */
    private String ledgerNo;
    
    /**
     * 操作后的可用余额
     */
    private BigDecimal availableBalance;
    
    /**
     * 操作后的冻结余额
     */
    private BigDecimal frozenBalance;
    
    /**
     * 错误信息（失败时填充）
     */
    private String errorMessage;
    
    /**
     * 是否幂等重放（true=重复请求，false=首次请求）
     */
    private boolean idempotent;
    
    public WalletAssetResult() {
    }
    
    public static WalletAssetResult success(Long accountId, String freezeNo, String ledgerNo,
                                           BigDecimal availableBalance, BigDecimal frozenBalance,
                                           boolean idempotent) {
        WalletAssetResult result = new WalletAssetResult();
        result.success = true;
        result.accountId = accountId;
        result.freezeNo = freezeNo;
        result.ledgerNo = ledgerNo;
        result.availableBalance = availableBalance;
        result.frozenBalance = frozenBalance;
        result.idempotent = idempotent;
        return result;
    }
    
    public static WalletAssetResult failure(String errorMessage) {
        WalletAssetResult result = new WalletAssetResult();
        result.success = false;
        result.errorMessage = errorMessage;
        return result;
    }
    
    // Getters and Setters
    public boolean isSuccess() {
        return success;
    }
    
    public void setSuccess(boolean success) {
        this.success = success;
    }
    
    public Long getAccountId() {
        return accountId;
    }
    
    public void setAccountId(Long accountId) {
        this.accountId = accountId;
    }
    
    public String getFreezeNo() {
        return freezeNo;
    }
    
    public void setFreezeNo(String freezeNo) {
        this.freezeNo = freezeNo;
    }
    
    public String getLedgerNo() {
        return ledgerNo;
    }
    
    public void setLedgerNo(String ledgerNo) {
        this.ledgerNo = ledgerNo;
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
    
    public String getErrorMessage() {
        return errorMessage;
    }
    
    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }
    
    public boolean isIdempotent() {
        return idempotent;
    }
    
    public void setIdempotent(boolean idempotent) {
        this.idempotent = idempotent;
    }
}
