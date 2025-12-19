package com.bluecone.app.promo.api.enums;

/**
 * 优惠券适用范围枚举
 */
public enum ApplicableScope {
    /**
     * 全场通用
     */
    ALL,
    
    /**
     * 指定门店
     */
    STORE,
    
    /**
     * 指定商品（SKU）
     */
    SKU,
    
    /**
     * 指定分类
     */
    CATEGORY
}
