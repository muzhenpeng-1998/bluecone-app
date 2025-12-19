package com.bluecone.app.promo.application;

import com.bluecone.app.promo.api.dto.CouponQueryContext;
import com.bluecone.app.promo.api.dto.UsableCouponDTO;
import com.bluecone.app.promo.api.enums.CouponStatus;
import com.bluecone.app.promo.api.facade.CouponQueryFacade;
import com.bluecone.app.promo.domain.model.Coupon;
import com.bluecone.app.promo.domain.repository.CouponRepository;
import com.bluecone.app.promo.domain.service.CouponValidationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 优惠券查询门面实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CouponQueryFacadeImpl implements CouponQueryFacade {

    private final CouponRepository couponRepository;
    private final CouponValidationService validationService;

    @Override
    public List<UsableCouponDTO> listUsableCoupons(CouponQueryContext context) {
        if (context == null || context.getTenantId() == null || context.getUserId() == null) {
            return List.of();
        }

        LocalDateTime now = LocalDateTime.now();
        
        // 查询用户的所有已发放券
        List<Coupon> coupons = couponRepository.findUserCoupons(
                context.getTenantId(),
                context.getUserId(),
                List.of(CouponStatus.ISSUED),
                null // 不在查询时过滤有效期，在校验时做详细判断
        );

        if (coupons.isEmpty()) {
            return List.of();
        }

        // 校验每个券的可用性
        return coupons.stream()
                .map(coupon -> buildUsableCouponDTO(coupon, context, now))
                .sorted(Comparator.comparing(UsableCouponDTO::getUsable).reversed()
                        .thenComparing(UsableCouponDTO::getEstimatedDiscount, Comparator.reverseOrder()))
                .collect(Collectors.toList());
    }

    @Override
    public UsableCouponDTO bestCoupon(CouponQueryContext context) {
        List<UsableCouponDTO> usableCoupons = listUsableCoupons(context);
        
        // 返回可用且优惠金额最大的券
        return usableCoupons.stream()
                .filter(UsableCouponDTO::getUsable)
                .max(Comparator.comparing(UsableCouponDTO::getEstimatedDiscount))
                .orElse(null);
    }

    /**
     * 构建可用优惠券DTO
     */
    private UsableCouponDTO buildUsableCouponDTO(Coupon coupon, CouponQueryContext context, LocalDateTime now) {
        UsableCouponDTO dto = new UsableCouponDTO();
        
        // 基础信息
        dto.setCouponId(coupon.getId());
        dto.setTemplateId(coupon.getTemplateId());
        dto.setCouponCode(coupon.getCouponCode());
        dto.setCouponType(coupon.getCouponType() != null ? coupon.getCouponType().name() : null);
        dto.setDiscountAmount(coupon.getDiscountAmount());
        dto.setDiscountRate(coupon.getDiscountRate());
        dto.setMinOrderAmount(coupon.getMinOrderAmount());
        dto.setMaxDiscountAmount(coupon.getMaxDiscountAmount());
        dto.setApplicableScope(coupon.getApplicableScope() != null ? coupon.getApplicableScope().name() : null);
        dto.setValidStartTime(coupon.getValidStartTime());
        dto.setValidEndTime(coupon.getValidEndTime());
        dto.setDescription(generateDescription(coupon));

        // 校验可用性
        CouponValidationService.ValidationResult result = validationService.validate(
                coupon,
                context.getOrderAmount(),
                context.getStoreId(),
                context.getSkuIds(),
                context.getCategoryIds(),
                now
        );

        dto.setUsable(result.isValid());
        dto.setUnusableReason(result.getReason());
        dto.setEstimatedDiscount(result.isValid() ? result.getDiscountAmount() : BigDecimal.ZERO);

        return dto;
    }

    /**
     * 生成优惠券描述
     */
    private String generateDescription(Coupon coupon) {
        if (coupon.getCouponType() == null) {
            return "优惠券";
        }

        switch (coupon.getCouponType()) {
            case DISCOUNT_AMOUNT:
                if (coupon.getMinOrderAmount() != null && coupon.getMinOrderAmount().compareTo(BigDecimal.ZERO) > 0) {
                    return String.format("满%.0f减%.0f", 
                            coupon.getMinOrderAmount(), 
                            coupon.getDiscountAmount());
                } else {
                    return String.format("立减%.0f元", coupon.getDiscountAmount());
                }
                
            case DISCOUNT_RATE:
                String rateDesc = String.format("%.0f折", coupon.getDiscountRate());
                if (coupon.getMaxDiscountAmount() != null) {
                    rateDesc += String.format("（最高减%.0f）", coupon.getMaxDiscountAmount());
                }
                if (coupon.getMinOrderAmount() != null && coupon.getMinOrderAmount().compareTo(BigDecimal.ZERO) > 0) {
                    return String.format("满%.0f享%s", coupon.getMinOrderAmount(), rateDesc);
                }
                return rateDesc;
                
            default:
                return "优惠券";
        }
    }
}
