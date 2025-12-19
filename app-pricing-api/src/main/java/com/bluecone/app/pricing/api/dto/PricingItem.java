package com.bluecone.app.pricing.api.dto;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Map;

/**
 * 计价商品项
 * 包含商品的基本信息和计价所需的属性
 */
public class PricingItem implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    /**
     * 商品SKU ID
     */
    private Long skuId;
    
    /**
     * 商品名称
     */
    private String skuName;
    
    /**
     * 商品分类ID
     */
    private Long categoryId;
    
    /**
     * 数量
     */
    private Integer quantity;
    
    /**
     * 商品基价（单价）
     */
    private BigDecimal basePrice;
    
    /**
     * 规格加价（如大杯+2元）
     */
    private BigDecimal specSurcharge;
    
    /**
     * 商品标签（用于活动匹配）
     */
    private String tags;
    
    /**
     * 扩展属性（用于特殊计价逻辑）
     */
    private Map<String, Object> attributes;
    
    public PricingItem() {
    }
    
    public Long getSkuId() {
        return skuId;
    }
    
    public void setSkuId(Long skuId) {
        this.skuId = skuId;
    }
    
    public String getSkuName() {
        return skuName;
    }
    
    public void setSkuName(String skuName) {
        this.skuName = skuName;
    }
    
    public Long getCategoryId() {
        return categoryId;
    }
    
    public void setCategoryId(Long categoryId) {
        this.categoryId = categoryId;
    }
    
    public Integer getQuantity() {
        return quantity;
    }
    
    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }
    
    public BigDecimal getBasePrice() {
        return basePrice;
    }
    
    public void setBasePrice(BigDecimal basePrice) {
        this.basePrice = basePrice;
    }
    
    public BigDecimal getSpecSurcharge() {
        return specSurcharge;
    }
    
    public void setSpecSurcharge(BigDecimal specSurcharge) {
        this.specSurcharge = specSurcharge;
    }
    
    public String getTags() {
        return tags;
    }
    
    public void setTags(String tags) {
        this.tags = tags;
    }
    
    public Map<String, Object> getAttributes() {
        return attributes;
    }
    
    public void setAttributes(Map<String, Object> attributes) {
        this.attributes = attributes;
    }
}
