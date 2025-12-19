package com.bluecone.app.pricing.api.dto;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;

/**
 * 计价请求
 * 包含订单计价所需的所有输入信息
 */
public class PricingRequest implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    /**
     * 租户ID
     */
    private Long tenantId;
    
    /**
     * 门店ID
     */
    private Long storeId;
    
    /**
     * 用户ID
     */
    private Long userId;
    
    /**
     * 会员ID（可选）
     */
    private Long memberId;
    
    /**
     * 商品列表
     */
    private List<PricingItem> items;
    
    /**
     * 指定使用的优惠券ID（可选）
     */
    private Long couponId;
    
    /**
     * 使用积分数量（可选）
     */
    private Integer usePoints;
    
    /**
     * 配送方式（DELIVERY=配送, PICKUP=自提）
     */
    private String deliveryMode;
    
    /**
     * 配送距离（公里，用于计算配送费）
     */
    private BigDecimal deliveryDistance;
    
    /**
     * 订单类型（NORMAL=普通订单, GROUP=拼团订单等）
     */
    private String orderType;
    
    /**
     * 渠道（MINI_PROGRAM=小程序, APP=APP等）
     */
    private String channel;
    
    /**
     * 是否启用抹零
     */
    private Boolean enableRounding;
    
    public PricingRequest() {
    }
    
    public Long getTenantId() {
        return tenantId;
    }
    
    public void setTenantId(Long tenantId) {
        this.tenantId = tenantId;
    }
    
    public Long getStoreId() {
        return storeId;
    }
    
    public void setStoreId(Long storeId) {
        this.storeId = storeId;
    }
    
    public Long getUserId() {
        return userId;
    }
    
    public void setUserId(Long userId) {
        this.userId = userId;
    }
    
    public Long getMemberId() {
        return memberId;
    }
    
    public void setMemberId(Long memberId) {
        this.memberId = memberId;
    }
    
    public List<PricingItem> getItems() {
        return items;
    }
    
    public void setItems(List<PricingItem> items) {
        this.items = items;
    }
    
    public Long getCouponId() {
        return couponId;
    }
    
    public void setCouponId(Long couponId) {
        this.couponId = couponId;
    }
    
    public Integer getUsePoints() {
        return usePoints;
    }
    
    public void setUsePoints(Integer usePoints) {
        this.usePoints = usePoints;
    }
    
    public String getDeliveryMode() {
        return deliveryMode;
    }
    
    public void setDeliveryMode(String deliveryMode) {
        this.deliveryMode = deliveryMode;
    }
    
    public BigDecimal getDeliveryDistance() {
        return deliveryDistance;
    }
    
    public void setDeliveryDistance(BigDecimal deliveryDistance) {
        this.deliveryDistance = deliveryDistance;
    }
    
    public String getOrderType() {
        return orderType;
    }
    
    public void setOrderType(String orderType) {
        this.orderType = orderType;
    }
    
    public String getChannel() {
        return channel;
    }
    
    public void setChannel(String channel) {
        this.channel = channel;
    }
    
    public Boolean getEnableRounding() {
        return enableRounding;
    }
    
    public void setEnableRounding(Boolean enableRounding) {
        this.enableRounding = enableRounding;
    }
}
