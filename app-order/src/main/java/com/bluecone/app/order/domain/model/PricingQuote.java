package com.bluecone.app.order.domain.model;

import com.bluecone.app.promo.api.dto.UsableCouponDTO;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;

/**
 * 订单定价报价
 * 
 * <p>包含订单定价的结果，包括：
 * <ul>
 *   <li>商品原价</li>
 *   <li>优惠金额（券抵扣）</li>
 *   <li>应付金额</li>
 *   <li>可用优惠券列表</li>
 *   <li>最优优惠券建议</li>
 * </ul>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PricingQuote implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 商品原价（总价）
     */
    private BigDecimal originalAmount;

    /**
     * 优惠金额（券抵扣）
     */
    private BigDecimal discountAmount;

    /**
     * 应付金额（原价 - 优惠）
     */
    private BigDecimal payableAmount;

    /**
     * 币种
     */
    @Builder.Default
    private String currency = "CNY";

    /**
     * 使用的优惠券信息（如果有）
     */
    private UsableCouponDTO appliedCoupon;

    /**
     * 可用优惠券列表
     */
    private List<UsableCouponDTO> availableCoupons;

    /**
     * 最优优惠券建议（优惠金额最大）
     */
    private UsableCouponDTO bestCoupon;

    /**
     * 定价明细列表（可选）
     */
    private List<PricingItemDetail> itemDetails;

    /**
     * 定价明细
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PricingItemDetail implements Serializable {
        private static final long serialVersionUID = 1L;

        /**
         * 商品SKU ID
         */
        private Long skuId;

        /**
         * 商品名称
         */
        private String skuName;

        /**
         * 数量
         */
        private Integer quantity;

        /**
         * 单价
         */
        private BigDecimal unitPrice;

        /**
         * 小计（单价 * 数量）
         */
        private BigDecimal subtotal;

        /**
         * 优惠金额（如果有）
         */
        private BigDecimal discountAmount;

        /**
         * 应付金额（小计 - 优惠）
         */
        private BigDecimal payableAmount;
    }
}
