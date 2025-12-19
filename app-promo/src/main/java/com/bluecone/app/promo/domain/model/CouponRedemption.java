package com.bluecone.app.promo.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 优惠券核销记录领域模型
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CouponRedemption implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private Long tenantId;
    private Long couponId;
    private Long templateId;
    private Long userId;
    private Long orderId;
    private String idempotencyKey;
    private BigDecimal originalAmount;
    private BigDecimal discountAmount;
    private BigDecimal finalAmount;
    private LocalDateTime redemptionTime;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
