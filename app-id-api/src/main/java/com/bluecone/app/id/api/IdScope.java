package com.bluecone.app.id.api;

/**
 * ID 作用域枚举，用于 long 型 ID 的号段分配。
 * 
 * <p>每个作用域对应一个独立的号段序列，保证同一作用域内的 ID 单调递增且全局唯一。
 * 通常一个作用域对应一张业务表或一个业务领域。
 * </p>
 */
public enum IdScope {
    
    /**
     * 租户作用域，对应 bc_tenant 表
     */
    TENANT,
    
    /**
     * 门店作用域，对应 bc_store 表
     */
    STORE,
    
    /**
     * 订单作用域，对应 bc_order 表
     */
    ORDER,
    
    /**
     * 订单明细作用域，对应 bc_order_item 表
     */
    ORDER_ITEM,
    
    /**
     * 商品作用域，对应 bc_product 表
     */
    PRODUCT,
    
    /**
     * SKU 作用域，对应 bc_sku 表
     */
    SKU,
    
    /**
     * 用户作用域，对应 bc_user 表
     */
    USER,
    
    /**
     * 支付作用域，对应 bc_payment 表
     */
    PAYMENT,
    
    /**
     * 库存记录作用域，对应 bc_inventory_record 表
     */
    INVENTORY_RECORD,
    
    /**
     * 会员作用域，对应 bc_member 表
     */
    MEMBER,
    
    /**
     * 积分账户作用域，对应 bc_points_account 表
     */
    POINTS_ACCOUNT,
    
    /**
     * 积分流水作用域，对应 bc_points_ledger 表
     */
    POINTS_LEDGER,
    
    /**
     * 优惠券模板作用域，对应 bc_coupon_template 表
     */
    COUPON_TEMPLATE,
    
    /**
     * 优惠券作用域，对应 bc_coupon 表
     */
    COUPON,
    
    /**
     * 优惠券锁定记录作用域，对应 bc_coupon_lock 表
     */
    COUPON_LOCK,
    
    /**
     * 优惠券核销记录作用域，对应 bc_coupon_redemption 表
     */
    COUPON_REDEMPTION,
    
    /**
     * 优惠券发放日志作用域，对应 bc_coupon_grant_log 表
     */
    COUPON_GRANT_LOG,
    
    /**
     * 钱包账户作用域，对应 bc_wallet_account 表
     */
    WALLET_ACCOUNT,
    
    /**
     * 钱包账本流水作用域，对应 bc_wallet_ledger 表
     */
    WALLET_LEDGER,
    
    /**
     * 钱包冻结记录作用域，对应 bc_wallet_freeze 表
     */
    WALLET_FREEZE,
    
    /**
     * 钱包充值单作用域，对应 bc_wallet_recharge_order 表
     */
    WALLET_RECHARGE,
    
    /**
     * 增长引擎作用域，对应 bc_growth_* 相关表
     */
    GROWTH,
    
    /**
     * 活动编排：活动配置
     */
    CAMPAIGN,
    
    /**
     * 活动编排：执行日志
     */
    CAMPAIGN_EXECUTION_LOG;
    
    /**
     * 返回作用域名称（用于数据库 scope 字段）。
     * 
     * @return 作用域名称字符串
     */
    public String scopeName() {
        return this.name();
    }
}
