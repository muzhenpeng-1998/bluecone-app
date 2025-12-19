package com.bluecone.app.campaign.api.enums;

/**
 * 活动类型枚举
 */
public enum CampaignType {
    
    /**
     * 订单满额立减（同步计价阶段）
     * 触发时机：订单计价时（PromoStage）
     * 执行通道：同步计价（无副作用）
     * 奖励类型：订单金额直接减免
     */
    ORDER_DISCOUNT("订单满额立减"),
    
    /**
     * 订单完成返券（异步结算）
     * 触发时机：订单支付成功（ORDER_PAID 事件）
     * 执行通道：异步消费者
     * 奖励类型：发放优惠券
     */
    ORDER_REBATE_COUPON("订单完成返券"),
    
    /**
     * 充值赠送（异步结算）
     * 触发时机：充值支付成功（RECHARGE_PAID 事件）
     * 执行通道：异步消费者
     * 奖励类型：赠送钱包余额
     */
    RECHARGE_BONUS("充值赠送");
    
    private final String description;
    
    CampaignType(String description) {
        this.description = description;
    }
    
    public String getDescription() {
        return description;
    }
    
    /**
     * 是否是同步计价类型（在计价阶段执行）
     */
    public boolean isSyncPricing() {
        return this == ORDER_DISCOUNT;
    }
    
    /**
     * 是否是异步结算类型（通过事件消费者执行）
     */
    public boolean isAsyncSettlement() {
        return this == ORDER_REBATE_COUPON || this == RECHARGE_BONUS;
    }
}
