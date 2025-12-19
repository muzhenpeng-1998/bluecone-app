package com.bluecone.app.order.application.service;

import com.bluecone.app.order.domain.model.PricingContext;
import com.bluecone.app.order.domain.model.PricingQuote;
import com.bluecone.app.promo.api.dto.CouponQueryContext;
import com.bluecone.app.promo.api.dto.UsableCouponDTO;
import com.bluecone.app.promo.api.facade.CouponQueryFacade;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 订单优惠券定价服务
 * 
 * <p>职责：
 * <ul>
 *   <li>计算订单原价</li>
 *   <li>查询可用优惠券</li>
 *   <li>计算优惠金额</li>
 *   <li>生成定价报价</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrderCouponPricingService {

    private final CouponQueryFacade couponQueryFacade;

    /**
     * 计算订单定价报价（包含优惠券信息）
     * 
     * @param context 定价上下文
     * @return 定价报价
     */
    public PricingQuote calculatePricing(PricingContext context) {
        if (context == null) {
            throw new IllegalArgumentException("定价上下文不能为空");
        }

        // 1. 计算商品原价
        BigDecimal originalAmount = calculateOriginalAmount(context);

        // 2. 构建优惠券查询上下文
        CouponQueryContext couponContext = buildCouponQueryContext(context, originalAmount);

        // 3. 查询可用优惠券
        List<UsableCouponDTO> availableCoupons = queryCoupons(couponContext);

        // 4. 确定应用的优惠券
        UsableCouponDTO appliedCoupon = determineAppliedCoupon(context, availableCoupons);

        // 5. 查询最优优惠券建议
        UsableCouponDTO bestCoupon = queryBestCoupon(couponContext);

        // 6. 计算优惠金额和应付金额
        BigDecimal discountAmount = appliedCoupon != null && appliedCoupon.getUsable() 
                ? appliedCoupon.getEstimatedDiscount() 
                : BigDecimal.ZERO;
        BigDecimal payableAmount = originalAmount.subtract(discountAmount);

        // 7. 构建定价报价
        return PricingQuote.builder()
                .originalAmount(originalAmount)
                .discountAmount(discountAmount)
                .payableAmount(payableAmount)
                .currency("CNY")
                .appliedCoupon(appliedCoupon)
                .availableCoupons(availableCoupons)
                .bestCoupon(bestCoupon)
                .build();
    }

    /**
     * 计算商品原价（不含优惠）
     */
    private BigDecimal calculateOriginalAmount(PricingContext context) {
        if (context.getItems() == null || context.getItems().isEmpty()) {
            return BigDecimal.ZERO;
        }

        return context.getItems().stream()
                .map(item -> {
                    BigDecimal unitPrice = item.getUnitPrice() != null ? item.getUnitPrice() : BigDecimal.ZERO;
                    int quantity = item.getQuantity() != null ? item.getQuantity() : 0;
                    return unitPrice.multiply(BigDecimal.valueOf(quantity));
                })
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * 构建优惠券查询上下文
     */
    private CouponQueryContext buildCouponQueryContext(PricingContext context, BigDecimal orderAmount) {
        CouponQueryContext couponContext = new CouponQueryContext();
        couponContext.setTenantId(context.getTenantId());
        couponContext.setUserId(context.getUserId());
        couponContext.setStoreId(context.getStoreId());
        couponContext.setOrderAmount(orderAmount);

        // 提取SKU ID列表
        if (context.getItems() != null && !context.getItems().isEmpty()) {
            List<Long> skuIds = context.getItems().stream()
                    .map(item -> item.getSkuId())
                    .filter(id -> id != null)
                    .distinct()
                    .collect(Collectors.toList());
            couponContext.setSkuIds(skuIds);

            // TODO: 提取分类ID列表（需要从商品信息中获取）
            // couponContext.setCategoryIds(categoryIds);
        }

        return couponContext;
    }

    /**
     * 查询可用优惠券列表
     */
    private List<UsableCouponDTO> queryCoupons(CouponQueryContext context) {
        try {
            return couponQueryFacade.listUsableCoupons(context);
        } catch (Exception e) {
            log.error("查询可用优惠券失败: tenantId={}, userId={}", 
                    context.getTenantId(), context.getUserId(), e);
            return List.of();
        }
    }

    /**
     * 查询最优优惠券
     */
    private UsableCouponDTO queryBestCoupon(CouponQueryContext context) {
        try {
            return couponQueryFacade.bestCoupon(context);
        } catch (Exception e) {
            log.error("查询最优优惠券失败: tenantId={}, userId={}", 
                    context.getTenantId(), context.getUserId(), e);
            return null;
        }
    }

    /**
     * 确定应用的优惠券
     * 
     * <p>逻辑：
     * <ul>
     *   <li>如果指定了优惠券ID，则使用指定的券</li>
     *   <li>如果未指定，则不自动应用（由用户选择）</li>
     * </ul>
     */
    private UsableCouponDTO determineAppliedCoupon(PricingContext context, List<UsableCouponDTO> availableCoupons) {
        if (context.getCouponId() == null || availableCoupons == null || availableCoupons.isEmpty()) {
            return null;
        }

        // 查找指定的优惠券
        return availableCoupons.stream()
                .filter(coupon -> context.getCouponId().equals(coupon.getCouponId()))
                .findFirst()
                .orElse(null);
    }
}
