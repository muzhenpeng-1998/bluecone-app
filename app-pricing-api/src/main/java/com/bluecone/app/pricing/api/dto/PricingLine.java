package com.bluecone.app.pricing.api.dto;

import com.bluecone.app.pricing.api.enums.ReasonCode;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 计价明细行
 * 用于记录计价过程中的每一步调整，确保可解释性和可追溯性
 */
public class PricingLine implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    /**
     * 原因码（业务含义）
     */
    private ReasonCode reasonCode;
    
    /**
     * 明细描述（中文）
     */
    private String description;
    
    /**
     * 金额（正数表示增加，负数表示减少）
     */
    private BigDecimal amount;
    
    /**
     * 关联的业务ID（如优惠券ID、活动ID等）
     */
    private Long relatedId;
    
    /**
     * 关联的业务类型（如 COUPON、PROMO、POINTS 等）
     */
    private String relatedType;
    
    /**
     * 扩展信息（JSON格式）
     */
    private String extInfo;
    
    public PricingLine() {
    }
    
    public PricingLine(ReasonCode reasonCode, String description, BigDecimal amount) {
        this.reasonCode = reasonCode;
        this.description = description;
        this.amount = amount;
    }
    
    public PricingLine(ReasonCode reasonCode, String description, BigDecimal amount, Long relatedId, String relatedType) {
        this.reasonCode = reasonCode;
        this.description = description;
        this.amount = amount;
        this.relatedId = relatedId;
        this.relatedType = relatedType;
    }
    
    public ReasonCode getReasonCode() {
        return reasonCode;
    }
    
    public void setReasonCode(ReasonCode reasonCode) {
        this.reasonCode = reasonCode;
    }
    
    public String getDescription() {
        return description;
    }
    
    public void setDescription(String description) {
        this.description = description;
    }
    
    public BigDecimal getAmount() {
        return amount;
    }
    
    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }
    
    public Long getRelatedId() {
        return relatedId;
    }
    
    public void setRelatedId(Long relatedId) {
        this.relatedId = relatedId;
    }
    
    public String getRelatedType() {
        return relatedType;
    }
    
    public void setRelatedType(String relatedType) {
        this.relatedType = relatedType;
    }
    
    public String getExtInfo() {
        return extInfo;
    }
    
    public void setExtInfo(String extInfo) {
        this.extInfo = extInfo;
    }
}
