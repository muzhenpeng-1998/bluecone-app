package com.bluecone.app.promo.api.dto.admin;

import com.bluecone.app.promo.api.enums.ApplicableScope;
import com.bluecone.app.promo.api.enums.CouponType;
import lombok.Data;

import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 更新优惠券模板请求（仅草稿状态可更新）
 */
@Data
public class CouponTemplateUpdateRequest {

    @NotNull(message = "模板ID不能为空")
    private Long id;

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

    private String description;

    private String termsOfUse;
}
