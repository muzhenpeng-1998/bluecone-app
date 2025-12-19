package com.bluecone.app.member.domain.enums;

/**
 * 积分业务类型枚举
 * 
 * @author bluecone
 * @since 2025-12-18
 */
public enum PointsBizType {
    
    /**
     * 订单支付（下单冻结积分）
     */
    ORDER_PAY("订单支付"),
    
    /**
     * 订单完成（赚取积分）
     */
    ORDER_COMPLETE("订单完成"),
    
    /**
     * 订单取消（释放冻结积分）
     */
    ORDER_CANCEL("订单取消"),
    
    /**
     * 订单退款（返还积分）
     */
    REFUND("订单退款"),
    
    /**
     * 积分兑换（消费积分）
     */
    EXCHANGE("积分兑换"),
    
    /**
     * 签到奖励（赚取积分）
     */
    SIGN_IN("签到奖励"),
    
    /**
     * 活动奖励（赚取积分）
     */
    ACTIVITY_REWARD("活动奖励"),
    
    /**
     * 管理员调整（手动调整积分）
     */
    ADJUST("管理员调整"),
    
    /**
     * 积分过期（扣减积分）
     */
    EXPIRE("积分过期");
    
    private final String description;
    
    PointsBizType(String description) {
        this.description = description;
    }
    
    public String getDescription() {
        return description;
    }
}
