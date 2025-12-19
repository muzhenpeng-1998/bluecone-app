package com.bluecone.app.promo.api.enums;

/**
 * 优惠券状态枚举
 */
public enum CouponStatus {
    /**
     * 已发放：券已发给用户，可以使用
     */
    ISSUED,
    
    /**
     * 已锁定：券被订单锁定，等待支付
     */
    LOCKED,
    
    /**
     * 已使用：券已核销，订单已支付
     */
    USED,
    
    /**
     * 已过期：券已超过有效期
     */
    EXPIRED
}
