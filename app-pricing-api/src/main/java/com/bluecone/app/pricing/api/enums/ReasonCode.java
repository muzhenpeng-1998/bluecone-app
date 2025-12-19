package com.bluecone.app.pricing.api.enums;

/**
 * 计价原因码枚举
 * 用于标识计价明细行的业务含义
 */
public enum ReasonCode {
    
    /**
     * 商品基价
     */
    BASE_PRICE("商品基价"),
    
    /**
     * 规格加价
     */
    SPEC_SURCHARGE("规格加价"),
    
    /**
     * 会员价
     */
    MEMBER_PRICE("会员价"),
    
    /**
     * 时段价
     */
    TIME_SLOT_PRICE("时段价"),
    
    /**
     * 活动折扣
     */
    PROMO_DISCOUNT("活动折扣"),
    
    /**
     * 优惠券抵扣
     */
    COUPON_DISCOUNT("优惠券抵扣"),
    
    /**
     * 积分抵扣
     */
    POINTS_DISCOUNT("积分抵扣"),
    
    /**
     * 配送费
     */
    DELIVERY_FEE("配送费"),
    
    /**
     * 打包费
     */
    PACKING_FEE("打包费"),
    
    /**
     * 抹零
     */
    ROUNDING("抹零"),
    
    /**
     * 其他调整
     */
    OTHER_ADJUSTMENT("其他调整");
    
    private final String description;
    
    ReasonCode(String description) {
        this.description = description;
    }
    
    public String getDescription() {
        return description;
    }
}
