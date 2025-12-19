package com.bluecone.app.promo.api.enums;

/**
 * 优惠券发放来源
 */
public enum GrantSource {
    
    /**
     * 管理员手动发放
     */
    MANUAL_ADMIN,
    
    /**
     * 营销活动自动发放
     */
    CAMPAIGN,
    
    /**
     * 注册赠送
     */
    REGISTER,
    
    /**
     * 用户主动领取
     */
    USER_CLAIM
}
