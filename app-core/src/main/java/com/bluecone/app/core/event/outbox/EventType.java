package com.bluecone.app.core.event.outbox;

/**
 * 事件类型枚举
 * 定义系统中所有的领域事件类型
 */
public enum EventType {
    
    // ========== 订单事件 ==========
    
    /**
     * 订单结算锁定（Checkout 锁定优惠券/积分/钱包）
     * 触发时机：订单从 DRAFT 提交到 WAIT_PAY 状态
     * 资产操作：锁定优惠券、冻结钱包、冻结积分
     */
    ORDER_CHECKOUT_LOCKED("order.checkout_locked"),
    
    /**
     * 订单支付成功
     * 触发时机：订单从 WAIT_PAY 流转到 PAID 状态
     * 资产操作：提交核销优惠券、提交扣减钱包、提交扣减积分、赚取积分
     */
    ORDER_PAID("order.paid"),
    
    /**
     * 订单取消
     * 触发时机：订单流转到 CANCELED 状态（用户取消、商户拒单、超时取消）
     * 资产操作：释放优惠券、释放钱包冻结、释放积分冻结
     */
    ORDER_CANCELED("order.canceled"),
    
    /**
     * 订单退款成功
     * 触发时机：退款单状态流转到 SUCCESS
     * 资产操作：回退钱包、回退积分、回退优惠券（如果已核销）
     */
    ORDER_REFUNDED("order.refunded"),
    
    // ========== 支付事件 ==========
    
    /**
     * 支付成功
     * 触发时机：支付单状态流转到 SUCCESS
     * 资产操作：触发订单支付成功流程
     */
    PAYMENT_SUCCESS("payment.success"),
    
    /**
     * 支付失败
     * 触发时机：支付单状态流转到 FAILED
     * 资产操作：释放订单锁定的资产
     */
    PAYMENT_FAILED("payment.failed"),
    
    // ========== 退款事件 ==========
    
    /**
     * 退款成功
     * 触发时机：退款单状态流转到 SUCCESS
     * 资产操作：回退钱包、回退积分
     */
    REFUND_SUCCESS("refund.success"),
    
    /**
     * 退款失败
     * 触发时机：退款单状态流转到 FAILED
     * 资产操作：记录日志，人工介入
     */
    REFUND_FAILED("refund.failed"),
    
    // ========== 充值事件 ==========
    
    /**
     * 充值支付成功
     * 触发时机：充值单支付回调成功，状态流转到 PAID
     * 资产操作：写入钱包账本流水（CREDIT），增加可用余额
     */
    RECHARGE_PAID("recharge.paid"),
    
    // ========== 订阅计费事件 ==========
    
    /**
     * 订阅账单支付成功
     * 触发时机：订阅账单支付回调成功，状态流转到 PAID
     * 资产操作：更新租户订阅状态，切换套餐和配额
     */
    INVOICE_PAID("invoice.paid");
    
    private final String code;
    
    EventType(String code) {
        this.code = code;
    }
    
    public String getCode() {
        return code;
    }
    
    /**
     * 从 code 解析 EventType
     */
    public static EventType fromCode(String code) {
        if (code == null) {
            return null;
        }
        for (EventType type : values()) {
            if (type.code.equals(code)) {
                return type;
            }
        }
        return null;
    }
    
    @Override
    public String toString() {
        return code;
    }
}
