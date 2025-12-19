package com.bluecone.app.promo.api.dto;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;

/**
 * 优惠券查询上下文（用于查询可用券）
 */
public class CouponQueryContext implements Serializable {
    
    private Long tenantId;
    private Long userId;
    private Long storeId;
    private BigDecimal orderAmount;
    
    /**
     * 订单商品SKU ID列表（用于SKU级别优惠券匹配）
     */
    private List<Long> skuIds;
    
    /**
     * 订单商品分类ID列表（用于分类级别优惠券匹配）
     */
    private List<Long> categoryIds;

    public CouponQueryContext() {
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

    public Long getStoreId() {
        return storeId;
    }

    public void setStoreId(Long storeId) {
        this.storeId = storeId;
    }

    public BigDecimal getOrderAmount() {
        return orderAmount;
    }

    public void setOrderAmount(BigDecimal orderAmount) {
        this.orderAmount = orderAmount;
    }

    public List<Long> getSkuIds() {
        return skuIds;
    }

    public void setSkuIds(List<Long> skuIds) {
        this.skuIds = skuIds;
    }

    public List<Long> getCategoryIds() {
        return categoryIds;
    }

    public void setCategoryIds(List<Long> categoryIds) {
        this.categoryIds = categoryIds;
    }
}
