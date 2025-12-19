package com.bluecone.app.promo.api.enums;

/**
 * 优惠券锁定状态枚举
 */
public enum LockStatus {
    /**
     * 已锁定：券被订单锁定中
     */
    LOCKED,
    
    /**
     * 已释放：券锁定被释放（订单取消/超时）
     */
    RELEASED,
    
    /**
     * 已提交：券被核销（订单支付成功）
     */
    COMMITTED
}
