package com.bluecone.app.promo.api.dto;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 优惠券锁定命令
 */
public class CouponLockCommand implements Serializable {
    
    private Long tenantId;
    private Long userId;
    private Long couponId;
    private Long orderId;
    private BigDecimal orderAmount;
    
    /**
     * 幂等键（必填）
     */
    private String idempotencyKey;
    
    /**
     * 锁定过期时间（分钟），默认30分钟
     */
    private Integer lockExpireMinutes;

    public CouponLockCommand() {
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

    public BigDecimal getOrderAmount() {
        return orderAmount;
    }

    public void setOrderAmount(BigDecimal orderAmount) {
        this.orderAmount = orderAmount;
    }

    public String getIdempotencyKey() {
        return idempotencyKey;
    }

    public void setIdempotencyKey(String idempotencyKey) {
        this.idempotencyKey = idempotencyKey;
    }

    public Integer getLockExpireMinutes() {
        return lockExpireMinutes;
    }

    public void setLockExpireMinutes(Integer lockExpireMinutes) {
        this.lockExpireMinutes = lockExpireMinutes;
    }
}
