package com.bluecone.app.wallet.api.dto;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 钱包资产操作命令
 * 用于冻结、提交、释放、回退等操作
 * 
 * @author bluecone
 * @since 2025-12-18
 */
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
    
    public WalletAssetCommand() {
    }
    
    public WalletAssetCommand(Long tenantId, Long userId, BigDecimal amount, 
                             String bizType, Long bizOrderId, String idempotencyKey) {
        this.tenantId = tenantId;
        this.userId = userId;
        this.amount = amount;
        this.bizType = bizType;
        this.bizOrderId = bizOrderId;
        this.idempotencyKey = idempotencyKey;
    }
    
    // Getters and Setters
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
    
    public BigDecimal getAmount() {
        return amount;
    }
    
    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }
    
    public String getCurrency() {
        return currency;
    }
    
    public void setCurrency(String currency) {
        this.currency = currency;
    }
    
    public String getBizType() {
        return bizType;
    }
    
    public void setBizType(String bizType) {
        this.bizType = bizType;
    }
    
    public Long getBizOrderId() {
        return bizOrderId;
    }
    
    public void setBizOrderId(Long bizOrderId) {
        this.bizOrderId = bizOrderId;
    }
    
    public String getBizOrderNo() {
        return bizOrderNo;
    }
    
    public void setBizOrderNo(String bizOrderNo) {
        this.bizOrderNo = bizOrderNo;
    }
    
    public String getIdempotencyKey() {
        return idempotencyKey;
    }
    
    public void setIdempotencyKey(String idempotencyKey) {
        this.idempotencyKey = idempotencyKey;
    }
    
    public String getRemark() {
        return remark;
    }
    
    public void setRemark(String remark) {
        this.remark = remark;
    }
    
    public Long getOperatorId() {
        return operatorId;
    }
    
    public void setOperatorId(Long operatorId) {
        this.operatorId = operatorId;
    }
}
