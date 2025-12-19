package com.bluecone.app.member.domain.enums;

/**
 * 积分变动方向枚举
 * 
 * @author bluecone
 * @since 2025-12-18
 */
public enum PointsDirection {
    
    /**
     * 获得积分（增加可用积分）
     * 例如：订单完成、签到、活动奖励
     */
    EARN("获得积分"),
    
    /**
     * 消费积分（减少可用积分或冻结积分）
     * 例如：积分兑换、积分抵扣订单
     */
    SPEND("消费积分"),
    
    /**
     * 冻结积分（将可用积分转为冻结积分）
     * 例如：下单时锁定积分，等待支付完成
     */
    FREEZE("冻结积分"),
    
    /**
     * 释放积分（将冻结积分转回可用积分）
     * 例如：订单取消、支付超时
     */
    RELEASE("释放积分"),
    
    /**
     * 回退积分（退款返还等反向操作）
     * 例如：订单退款，返还已消费的积分
     */
    REVERT("回退积分"),
    
    /**
     * 调整积分（管理员手动调整）
     * 例如：补偿、修正、赠送
     */
    ADJUST("调整积分");
    
    private final String description;
    
    PointsDirection(String description) {
        this.description = description;
    }
    
    public String getDescription() {
        return description;
    }
    
    /**
     * 判断是否增加可用积分
     */
    public boolean isIncreaseAvailable() {
        return this == EARN || this == RELEASE || this == REVERT;
    }
    
    /**
     * 判断是否减少可用积分
     */
    public boolean isDecreaseAvailable() {
        return this == FREEZE;
    }
    
    /**
     * 判断是否增加冻结积分
     */
    public boolean isIncreaseFrozen() {
        return this == FREEZE;
    }
    
    /**
     * 判断是否减少冻结积分
     */
    public boolean isDecreaseFrozen() {
        return this == SPEND || this == RELEASE;
    }
}
