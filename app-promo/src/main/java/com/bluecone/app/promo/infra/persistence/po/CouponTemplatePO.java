package com.bluecone.app.promo.infra.persistence.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 优惠券模板表PO
 */
@Data
@TableName("bc_coupon_template")
public class CouponTemplatePO implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.INPUT)
    private Long id;

    private Long tenantId;

    private String templateCode;

    private String templateName;

    private String couponType;

    private BigDecimal discountAmount;

    private BigDecimal discountRate;

    private BigDecimal minOrderAmount;

    private BigDecimal maxDiscountAmount;

    private String applicableScope;

    private String applicableScopeIds;

    private Integer validDays;

    private LocalDateTime validStartTime;

    private LocalDateTime validEndTime;

    private Integer totalQuantity;

    private Integer perUserLimit;

    private Integer issuedCount;

    private Integer version;

    private String status;

    private String description;

    private String termsOfUse;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
