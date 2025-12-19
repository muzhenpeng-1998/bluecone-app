package com.bluecone.app.promo.api.dto.admin;

import com.bluecone.app.promo.api.enums.ApplicableScope;
import com.bluecone.app.promo.api.enums.CouponType;
import lombok.Data;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 创建优惠券模板请求
 */
@Data
public class CouponTemplateCreateRequest {

    @NotBlank(message = "模板编码不能为空")
    private String templateCode;

    @NotBlank(message = "模板名称不能为空")
    private String templateName;

    @NotNull(message = "券类型不能为空")
    private CouponType couponType;

    private BigDecimal discountAmount;

    private BigDecimal discountRate;

    @NotNull(message = "最低订单金额不能为空")
    private BigDecimal minOrderAmount;

    private BigDecimal maxDiscountAmount;

    @NotNull(message = "适用范围不能为空")
    private ApplicableScope applicableScope;

    private List<Long> applicableScopeIds;

    /**
     * 有效天数（领取后多少天内有效，与固定有效期二选一）
     */
    private Integer validDays;

    /**
     * 固定有效期开始时间（与有效天数二选一）
     */
    private LocalDateTime validStartTime;

    /**
     * 固定有效期结束时间（与有效天数二选一）
     */
    private LocalDateTime validEndTime;

    /**
     * 总发行量（null表示不限量）
     */
    private Integer totalQuantity;

    /**
     * 每人限领数量（null表示不限制）
     */
    private Integer perUserLimit;

    private String description;

    private String termsOfUse;
}
