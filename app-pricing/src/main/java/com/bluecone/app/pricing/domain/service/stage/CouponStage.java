package com.bluecone.app.pricing.domain.service.stage;

import com.bluecone.app.pricing.api.dto.PricingLine;
import com.bluecone.app.pricing.api.enums.ReasonCode;
import com.bluecone.app.pricing.domain.model.PricingContext;
import com.bluecone.app.pricing.domain.service.PricingStage;
import com.bluecone.app.promo.api.dto.CouponQueryContext;
import com.bluecone.app.promo.api.dto.UsableCouponDTO;
import com.bluecone.app.promo.api.facade.CouponQueryFacade;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

/**
 * Stage4: 优惠券阶段
 * 调用 CouponQueryFacade 校验与预估抵扣
 */
@Component
public class CouponStage implements PricingStage {
    
    private static final Logger log = LoggerFactory.getLogger(CouponStage.class);
    
    private final CouponQueryFacade couponQueryFacade;
    
    public CouponStage(CouponQueryFacade couponQueryFacade) {
        this.couponQueryFacade = couponQueryFacade;
    }
    
    @Override
    public void execute(PricingContext context) {
        log.debug("Executing CouponStage");
        
        Long couponId = context.getRequest().getCouponId();
        if (couponId == null) {
            log.debug("No coupon specified, skipping CouponStage");
            return;
        }
        
        try {
            // 构建查询上下文
            CouponQueryContext queryContext = new CouponQueryContext();
            queryContext.setTenantId(context.getRequest().getTenantId());
            queryContext.setUserId(context.getRequest().getUserId());
            queryContext.setStoreId(context.getRequest().getStoreId());
            queryContext.setOrderAmount(context.getCurrentAmount());
            
            // 查询可用优惠券
            List<UsableCouponDTO> usableCoupons = couponQueryFacade.listUsableCoupons(queryContext);
            
            // 查找指定的优惠券
            UsableCouponDTO targetCoupon = usableCoupons.stream()
                    .filter(c -> couponId.equals(c.getCouponId()))
                    .findFirst()
                    .orElse(null);
            
            if (targetCoupon == null) {
                log.warn("Coupon not found or not available: couponId={}", couponId);
                context.putContextData("coupon_unavailable_reason", "优惠券不存在或不可用");
                return;
            }
            
            if (!Boolean.TRUE.equals(targetCoupon.getUsable())) {
                log.warn("Coupon is not usable: couponId={}, reason={}", 
                        couponId, targetCoupon.getUnusableReason());
                context.putContextData("coupon_unavailable_reason", targetCoupon.getUnusableReason());
                return;
            }
            
            // 应用优惠券抵扣
            BigDecimal discountAmount = targetCoupon.getEstimatedDiscount();
            if (discountAmount != null && discountAmount.compareTo(BigDecimal.ZERO) > 0) {
                // 确保抵扣金额不超过当前金额
                if (discountAmount.compareTo(context.getCurrentAmount()) > 0) {
                    discountAmount = context.getCurrentAmount();
                }
                
                // 添加优惠券抵扣明细行
                context.addBreakdownLine(new PricingLine(
                        ReasonCode.COUPON_DISCOUNT,
                        String.format("优惠券抵扣: %s", targetCoupon.getDescription()),
                        discountAmount.negate(),
                        couponId,
                        "COUPON"
                ));
                
                // 更新上下文
                context.setCouponDiscountAmount(discountAmount);
                context.subtractAmount(discountAmount);
                context.setAppliedCouponId(couponId);
                
                log.debug("CouponStage completed: discountAmount={}", discountAmount);
            }
        } catch (Exception e) {
            log.error("Error in CouponStage", e);
            context.putContextData("coupon_error", e.getMessage());
        }
    }
    
    @Override
    public String getStageName() {
        return "CouponStage";
    }
    
    @Override
    public int getOrder() {
        return 4;
    }
}
