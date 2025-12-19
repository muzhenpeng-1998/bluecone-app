package com.bluecone.app.promo.api.dto;

import java.io.Serializable;

/**
 * 优惠券释放命令
 */
public class CouponReleaseCommand implements Serializable {
    
    private Long tenantId;
    private Long userId;
    private Long couponId;
    private Long orderId;
    
    /**
     * 幂等键（必填，与锁定时相同）
     */
    private String idempotencyKey;

    public CouponReleaseCommand() {
    }

    public Long getTenantId() {
        return tenantId;
    }

    public void setTenantId(Long tenantId) {
        this.tenantId = tenantId;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public Long getCouponId() {
        return couponId;
    }

    public void setCouponId(Long couponId) {
        this.couponId = couponId;
    }

    public Long getOrderId() {
        return orderId;
    }

    public void setOrderId(Long orderId) {
        this.orderId = orderId;
    }

    public String getIdempotencyKey() {
        return idempotencyKey;
    }

    public void setIdempotencyKey(String idempotencyKey) {
        this.idempotencyKey = idempotencyKey;
    }
}
