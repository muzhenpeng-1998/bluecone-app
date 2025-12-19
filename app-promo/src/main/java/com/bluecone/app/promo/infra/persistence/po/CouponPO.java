package com.bluecone.app.promo.infra.persistence.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 优惠券实例表PO
 */
@Data
@TableName("bc_coupon")
public class CouponPO implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.INPUT)
    private Long id;

    private Long tenantId;

    private Long templateId;

    private Long grantLogId;

    private String couponCode;

    private Long userId;

    private String couponType;

    private BigDecimal discountAmount;

    private BigDecimal discountRate;

    private BigDecimal minOrderAmount;

    private BigDecimal maxDiscountAmount;

    private String applicableScope;

    private String applicableScopeIds;

    private LocalDateTime validStartTime;

    private LocalDateTime validEndTime;

    private String status;

    private LocalDateTime grantTime;

    private LocalDateTime lockTime;

    private LocalDateTime useTime;

    private Long orderId;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
