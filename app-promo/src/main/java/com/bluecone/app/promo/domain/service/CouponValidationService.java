package com.bluecone.app.promo.domain.service;

import com.bluecone.app.promo.api.enums.ApplicableScope;
import com.bluecone.app.promo.domain.model.Coupon;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 优惠券校验服务
 * 
 * <p>负责校验优惠券的可用性，包括：
 * <ul>
 *   <li>状态校验：券是否处于ISSUED状态</li>
 *   <li>有效期校验：当前时间是否在券的有效期内</li>
 *   <li>门槛校验：订单金额是否满足最低门槛</li>
 *   <li>适用范围校验：券是否适用于指定门店/商品/分类</li>
 * </ul>
 */
@Slf4j
@Service
public class CouponValidationService {

    /**
     * 校验优惠券是否可用
     * 
     * @param coupon 优惠券
     * @param orderAmount 订单金额
     * @param storeId 门店ID
     * @param skuIds 商品SKU ID列表（可选）
     * @param categoryIds 分类ID列表（可选）
     * @param now 当前时间
     * @return 校验结果
     */
    public ValidationResult validate(Coupon coupon, BigDecimal orderAmount, Long storeId, 
                                     List<Long> skuIds, List<Long> categoryIds, LocalDateTime now) {
        if (coupon == null) {
            return ValidationResult.invalid("优惠券不存在");
        }

        // 1. 状态校验
        if (!coupon.isUsable()) {
            return ValidationResult.invalid("优惠券状态不可用，当前状态：" + coupon.getStatus());
        }

        // 2. 有效期校验
        if (!coupon.isValid(now)) {
            if (now.isBefore(coupon.getValidStartTime())) {
                return ValidationResult.invalid("优惠券尚未生效");
            } else {
                return ValidationResult.invalid("优惠券已过期");
            }
        }

        // 3. 门槛金额校验
        if (!coupon.meetsThreshold(orderAmount)) {
            return ValidationResult.invalid(String.format("订单金额未达到使用门槛（满%.2f元可用）", 
                    coupon.getMinOrderAmount()));
        }

        // 4. 适用范围校验
        ValidationResult scopeResult = validateScope(coupon, storeId, skuIds, categoryIds);
        if (!scopeResult.isValid()) {
            return scopeResult;
        }

        // 5. 计算实际优惠金额
        BigDecimal discountAmount = coupon.calculateDiscount(orderAmount);
        
        return ValidationResult.valid(discountAmount);
    }

    /**
     * 校验适用范围
     */
    private ValidationResult validateScope(Coupon coupon, Long storeId, 
                                          List<Long> skuIds, List<Long> categoryIds) {
        ApplicableScope scope = coupon.getApplicableScope();
        
        if (scope == null) {
            return ValidationResult.invalid("优惠券适用范围配置错误");
        }

        switch (scope) {
            case ALL:
                // 全场通用，无需校验
                return ValidationResult.valid(BigDecimal.ZERO);
                
            case STORE:
                // 指定门店
                if (storeId == null) {
                    return ValidationResult.invalid("缺少门店信息");
                }
                if (!coupon.isApplicableToStore(storeId)) {
                    return ValidationResult.invalid("该优惠券不适用于当前门店");
                }
                return ValidationResult.valid(BigDecimal.ZERO);
                
            case SKU:
                // 指定商品（预留，暂时返回有效）
                // TODO: 校验订单中是否包含指定的SKU
                if (skuIds == null || skuIds.isEmpty()) {
                    return ValidationResult.invalid("订单中没有可用商品");
                }
                List<Long> applicableSkuIds = coupon.getApplicableScopeIds();
                if (applicableSkuIds == null || applicableSkuIds.isEmpty()) {
                    return ValidationResult.invalid("优惠券未配置适用商品");
                }
                boolean hasMatchingSku = skuIds.stream()
                        .anyMatch(applicableSkuIds::contains);
                if (!hasMatchingSku) {
                    return ValidationResult.invalid("该优惠券不适用于订单中的商品");
                }
                return ValidationResult.valid(BigDecimal.ZERO);
                
            case CATEGORY:
                // 指定分类（预留，暂时返回有效）
                // TODO: 校验订单中是否包含指定分类的商品
                if (categoryIds == null || categoryIds.isEmpty()) {
                    return ValidationResult.invalid("订单中没有可用分类商品");
                }
                List<Long> applicableCategoryIds = coupon.getApplicableScopeIds();
                if (applicableCategoryIds == null || applicableCategoryIds.isEmpty()) {
                    return ValidationResult.invalid("优惠券未配置适用分类");
                }
                boolean hasMatchingCategory = categoryIds.stream()
                        .anyMatch(applicableCategoryIds::contains);
                if (!hasMatchingCategory) {
                    return ValidationResult.invalid("该优惠券不适用于订单中的商品分类");
                }
                return ValidationResult.valid(BigDecimal.ZERO);
                
            default:
                return ValidationResult.invalid("未知的适用范围类型");
        }
    }

    /**
     * 校验结果
     */
    public static class ValidationResult {
        private final boolean valid;
        private final String reason;
        private final BigDecimal discountAmount;

        private ValidationResult(boolean valid, String reason, BigDecimal discountAmount) {
            this.valid = valid;
            this.reason = reason;
            this.discountAmount = discountAmount;
        }

        public static ValidationResult valid(BigDecimal discountAmount) {
            return new ValidationResult(true, null, discountAmount);
        }

        public static ValidationResult invalid(String reason) {
            return new ValidationResult(false, reason, BigDecimal.ZERO);
        }

        public boolean isValid() {
            return valid;
        }

        public String getReason() {
            return reason;
        }

        public BigDecimal getDiscountAmount() {
            return discountAmount;
        }
    }
}
