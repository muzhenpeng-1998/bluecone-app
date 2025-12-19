package com.bluecone.app.promo.domain.model;

import com.bluecone.app.promo.api.enums.ApplicableScope;
import com.bluecone.app.promo.api.enums.CouponType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 优惠券模板领域模型
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CouponTemplate implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private Long tenantId;
    private String templateCode;
    private String templateName;
    private CouponType couponType;
    private BigDecimal discountAmount;
    private BigDecimal discountRate;
    private BigDecimal minOrderAmount;
    private BigDecimal maxDiscountAmount;
    private ApplicableScope applicableScope;
    private List<Long> applicableScopeIds;
    private Integer validDays;
    private LocalDateTime validStartTime;
    private LocalDateTime validEndTime;
    private Integer totalQuantity;
    private Integer perUserLimit;
    private String status;
    private String description;
    private String termsOfUse;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    /**
     * 是否激活
     */
    public boolean isActive() {
        return "ACTIVE".equals(status);
    }

    /**
     * 是否使用固定有效期
     */
    public boolean useFixedValidity() {
        return validStartTime != null && validEndTime != null;
    }

    /**
     * 计算券的实际优惠金额
     */
    public BigDecimal calculateDiscount(BigDecimal orderAmount) {
        if (orderAmount == null || orderAmount.compareTo(minOrderAmount) < 0) {
            return BigDecimal.ZERO;
        }

        if (couponType == CouponType.DISCOUNT_AMOUNT) {
            return discountAmount;
        } else if (couponType == CouponType.DISCOUNT_RATE) {
            BigDecimal discount = orderAmount.multiply(BigDecimal.valueOf(100).subtract(discountRate))
                    .divide(BigDecimal.valueOf(100), 2, BigDecimal.ROUND_HALF_UP);
            BigDecimal actualDiscount = orderAmount.subtract(discount);
            if (maxDiscountAmount != null && actualDiscount.compareTo(maxDiscountAmount) > 0) {
                return maxDiscountAmount;
            }
            return actualDiscount;
        }

        return BigDecimal.ZERO;
    }
}
