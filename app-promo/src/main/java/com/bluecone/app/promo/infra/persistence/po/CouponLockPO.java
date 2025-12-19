package com.bluecone.app.promo.infra.persistence.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 优惠券锁定记录表PO
 */
@Data
@TableName("bc_coupon_lock")
public class CouponLockPO implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.INPUT)
    private Long id;

    private Long tenantId;

    private Long couponId;

    private Long userId;

    private Long orderId;

    private String idempotencyKey;

    private String lockStatus;

    private LocalDateTime lockTime;

    private LocalDateTime releaseTime;

    private LocalDateTime commitTime;

    private LocalDateTime expireTime;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
