package com.bluecone.app.promo.api.dto.admin;

import com.bluecone.app.promo.api.enums.ApplicableScope;
import com.bluecone.app.promo.api.enums.CouponType;
import com.bluecone.app.promo.api.enums.TemplateStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 优惠券模板视图
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CouponTemplateView {

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
    private Integer issuedCount;
    private Integer version;
    private TemplateStatus status;
    private String description;
    private String termsOfUse;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    /**
     * 配额使用率（百分比）
     */
    public Double getQuotaUsageRate() {
        if (totalQuantity == null || totalQuantity == 0) {
            return null;
        }
        return (double) issuedCount / totalQuantity * 100;
    }

    /**
     * 剩余配额
     */
    public Integer getRemainingQuota() {
        if (totalQuantity == null) {
            return null;
        }
        return Math.max(0, totalQuantity - issuedCount);
    }
}
