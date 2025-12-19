package com.bluecone.app.promo.domain.model;

import com.bluecone.app.promo.api.enums.ApplicableScope;
import com.bluecone.app.promo.api.enums.CouponStatus;
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
 * 优惠券实例领域模型
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Coupon implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private Long tenantId;
    private Long templateId;
    private Long grantLogId;
    private String couponCode;
    private Long userId;
    private CouponType couponType;
    private BigDecimal discountAmount;
    private BigDecimal discountRate;
    private BigDecimal minOrderAmount;
    private BigDecimal maxDiscountAmount;
    private ApplicableScope applicableScope;
    private List<Long> applicableScopeIds;
    private LocalDateTime validStartTime;
    private LocalDateTime validEndTime;
    private CouponStatus status;
    private LocalDateTime grantTime;
    private LocalDateTime lockTime;
    private LocalDateTime useTime;
    private Long orderId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    /**
     * 检查券是否可用
     */
    public boolean isUsable() {
        return status == CouponStatus.ISSUED;
    }

    /**
     * 检查券是否已锁定
     */
    public boolean isLocked() {
        return status == CouponStatus.LOCKED;
    }

    /**
     * 检查券是否已使用
     */
    public boolean isUsed() {
        return status == CouponStatus.USED;
    }

    /**
     * 检查券是否在有效期内
     */
    public boolean isValid(LocalDateTime now) {
        return now.isAfter(validStartTime) && now.isBefore(validEndTime);
    }

    /**
     * 检查订单金额是否满足门槛
     */
    public boolean meetsThreshold(BigDecimal orderAmount) {
        return orderAmount != null && orderAmount.compareTo(minOrderAmount) >= 0;
    }

    /**
     * 检查是否适用于指定门店
     */
    public boolean isApplicableToStore(Long storeId) {
        if (applicableScope == ApplicableScope.ALL) {
            return true;
        }
        if (applicableScope == ApplicableScope.STORE) {
            return applicableScopeIds != null && applicableScopeIds.contains(storeId);
        }
        return false;
    }

    /**
     * 计算实际优惠金额
     */
    public BigDecimal calculateDiscount(BigDecimal orderAmount) {
        if (!meetsThreshold(orderAmount)) {
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

    /**
     * 锁定券
     */
    public void lock(Long orderId, LocalDateTime lockTime) {
        if (!isUsable()) {
            throw new IllegalStateException("券状态不是ISSUED，无法锁定");
        }
        this.status = CouponStatus.LOCKED;
        this.orderId = orderId;
        this.lockTime = lockTime;
    }

    /**
     * 释放券
     */
    public void release() {
        if (!isLocked()) {
            // 幂等：如果不是锁定状态，直接返回
            return;
        }
        this.status = CouponStatus.ISSUED;
        this.orderId = null;
        this.lockTime = null;
    }

    /**
     * 核销券
     */
    public void redeem(LocalDateTime useTime) {
        if (!isLocked()) {
            throw new IllegalStateException("券状态不是LOCKED，无法核销");
        }
        this.status = CouponStatus.USED;
        this.useTime = useTime;
    }
}
