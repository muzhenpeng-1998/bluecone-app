package com.bluecone.app.pricing.api.dto;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 计价报价单
 * 包含完整的计价结果和明细，可作为快照落库
 */
public class PricingQuote implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    /**
     * 报价单ID（用于幂等和追溯）
     */
    private String quoteId;
    
    /**
     * 计价版本（用于版本控制和对账）
     */
    private String pricingVersion;
    
    /**
     * 商品原价（基价+规格加价）
     */
    private BigDecimal originalAmount;
    
    /**
     * 会员优惠金额
     */
    private BigDecimal memberDiscountAmount;
    
    /**
     * 活动优惠金额
     */
    private BigDecimal promoDiscountAmount;
    
    /**
     * 优惠券抵扣金额
     */
    private BigDecimal couponDiscountAmount;
    
    /**
     * 积分抵扣金额
     */
    private BigDecimal pointsDiscountAmount;
    
    /**
     * 配送费
     */
    private BigDecimal deliveryFee;
    
    /**
     * 打包费
     */
    private BigDecimal packingFee;
    
    /**
     * 抹零金额
     */
    private BigDecimal roundingAmount;
    
    /**
     * 应付金额（最终金额）
     */
    private BigDecimal payableAmount;
    
    /**
     * 币种
     */
    private String currency;
    
    /**
     * 计价明细行列表（可解释）
     */
    private List<PricingLine> breakdownLines;
    
    /**
     * 使用的优惠券ID
     */
    private Long appliedCouponId;
    
    /**
     * 使用的积分数量
     */
    private Integer appliedPoints;
    
    /**
     * 不可用原因（如果计价失败）
     */
    private String unavailableReason;
    
    /**
     * 计价时间
     */
    private LocalDateTime pricingTime;
    
    /**
     * 扩展信息（JSON格式）
     */
    private String extInfo;
    
    public PricingQuote() {
        this.currency = "CNY";
        this.breakdownLines = new ArrayList<>();
        this.pricingTime = LocalDateTime.now();
        this.pricingVersion = "1.0.0";
    }
    
    public String getQuoteId() {
        return quoteId;
    }
    
    public void setQuoteId(String quoteId) {
        this.quoteId = quoteId;
    }
    
    public String getPricingVersion() {
        return pricingVersion;
    }
    
    public void setPricingVersion(String pricingVersion) {
        this.pricingVersion = pricingVersion;
    }
    
    public BigDecimal getOriginalAmount() {
        return originalAmount;
    }
    
    public void setOriginalAmount(BigDecimal originalAmount) {
        this.originalAmount = originalAmount;
    }
    
    public BigDecimal getMemberDiscountAmount() {
        return memberDiscountAmount;
    }
    
    public void setMemberDiscountAmount(BigDecimal memberDiscountAmount) {
        this.memberDiscountAmount = memberDiscountAmount;
    }
    
    public BigDecimal getPromoDiscountAmount() {
        return promoDiscountAmount;
    }
    
    public void setPromoDiscountAmount(BigDecimal promoDiscountAmount) {
        this.promoDiscountAmount = promoDiscountAmount;
    }
    
    public BigDecimal getCouponDiscountAmount() {
        return couponDiscountAmount;
    }
    
    public void setCouponDiscountAmount(BigDecimal couponDiscountAmount) {
        this.couponDiscountAmount = couponDiscountAmount;
    }
    
    public BigDecimal getPointsDiscountAmount() {
        return pointsDiscountAmount;
    }
    
    public void setPointsDiscountAmount(BigDecimal pointsDiscountAmount) {
        this.pointsDiscountAmount = pointsDiscountAmount;
    }
    
    public BigDecimal getDeliveryFee() {
        return deliveryFee;
    }
    
    public void setDeliveryFee(BigDecimal deliveryFee) {
        this.deliveryFee = deliveryFee;
    }
    
    public BigDecimal getPackingFee() {
        return packingFee;
    }
    
    public void setPackingFee(BigDecimal packingFee) {
        this.packingFee = packingFee;
    }
    
    public BigDecimal getRoundingAmount() {
        return roundingAmount;
    }
    
    public void setRoundingAmount(BigDecimal roundingAmount) {
        this.roundingAmount = roundingAmount;
    }
    
    public BigDecimal getPayableAmount() {
        return payableAmount;
    }
    
    public void setPayableAmount(BigDecimal payableAmount) {
        this.payableAmount = payableAmount;
    }
    
    public String getCurrency() {
        return currency;
    }
    
    public void setCurrency(String currency) {
        this.currency = currency;
    }
    
    public List<PricingLine> getBreakdownLines() {
        return breakdownLines;
    }
    
    public void setBreakdownLines(List<PricingLine> breakdownLines) {
        this.breakdownLines = breakdownLines;
    }
    
    public Long getAppliedCouponId() {
        return appliedCouponId;
    }
    
    public void setAppliedCouponId(Long appliedCouponId) {
        this.appliedCouponId = appliedCouponId;
    }
    
    public Integer getAppliedPoints() {
        return appliedPoints;
    }
    
    public void setAppliedPoints(Integer appliedPoints) {
        this.appliedPoints = appliedPoints;
    }
    
    public String getUnavailableReason() {
        return unavailableReason;
    }
    
    public void setUnavailableReason(String unavailableReason) {
        this.unavailableReason = unavailableReason;
    }
    
    public LocalDateTime getPricingTime() {
        return pricingTime;
    }
    
    public void setPricingTime(LocalDateTime pricingTime) {
        this.pricingTime = pricingTime;
    }
    
    public String getExtInfo() {
        return extInfo;
    }
    
    public void setExtInfo(String extInfo) {
        this.extInfo = extInfo;
    }
    
    /**
     * 添加计价明细行
     */
    public void addBreakdownLine(PricingLine line) {
        if (this.breakdownLines == null) {
            this.breakdownLines = new ArrayList<>();
        }
        this.breakdownLines.add(line);
    }
}
