package com.bluecone.app.ops.api.dto.forensics;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 计价快照信息
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PricingSnapshotSection {
    
    /**
     * 是否存在计价快照
     */
    private Boolean exists;
    
    /**
     * 报价单ID
     */
    private String quoteId;
    
    /**
     * 计价版本
     */
    private String pricingVersion;
    
    /**
     * 商品原价
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
     * 应付金额
     */
    private BigDecimal payableAmount;
    
    /**
     * 币种
     */
    private String currency;
    
    /**
     * 使用的优惠券ID
     */
    private Long appliedCouponId;
    
    /**
     * 使用的积分数量
     */
    private Integer appliedPoints;
    
    /**
     * 计价时间
     */
    private LocalDateTime pricingTime;
    
    /**
     * 扩展信息（JSON）
     */
    private String extInfo;
}
