package com.bluecone.app.promo.infra.persistence.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 优惠券核销记录表PO
 */
@Data
@TableName("bc_coupon_redemption")
public class CouponRedemptionPO implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.INPUT)
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
