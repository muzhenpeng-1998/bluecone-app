package com.bluecone.app.order.domain.model;

import com.bluecone.app.pricing.api.dto.PricingLine;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 订单计价快照领域模型
 * 用于存储订单计价的完整快照，确保可追溯和可对账
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderPricingSnapshot {
    
    /**
     * 主键ID
     */
    private Long id;
    
    /**
     * 租户ID
     */
    private Long tenantId;
    
    /**
     * 订单ID
     */
    private Long orderId;
    
    /**
     * 报价单ID（用于幂等和追溯）
     */
    private String quoteId;
    
    /**
     * 计价版本
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
     * 使用的优惠券ID
     */
    private Long appliedCouponId;
    
    /**
     * 使用的积分数量
     */
    private Integer appliedPoints;
    
    /**
     * 计价明细行列表（可解释）
     */
    private List<PricingLine> breakdownLines;
    
    /**
     * 扩展信息
     */
    private String extInfo;
    
    /**
     * 计价时间
     */
    private LocalDateTime pricingTime;
    
    /**
     * 创建时间
     */
    private LocalDateTime createdAt;
    
    /**
     * 更新时间
     */
    private LocalDateTime updatedAt;
    
    /**
     * 创建人ID
     */
    private Long createdBy;
    
    /**
     * 更新人ID
     */
    private Long updatedBy;
    
    /**
     * 逻辑删除标记
     */
    private Boolean deleted;
}
