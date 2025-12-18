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
    INVENTORY_RECORD;
    
    /**
     * 返回作用域名称（用于数据库 scope 字段）。
     * 
     * @return 作用域名称字符串
     */
    public String scopeName() {
        return this.name();
    }
}
