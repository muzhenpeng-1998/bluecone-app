package com.bluecone.app.order.application;

import java.math.BigDecimal;

/**
 * 订单支付/退款状态变更应用服务：
 * - 支付成功/失败
 * - 超时关单
 * - 退款完成（全额/部分）
 *
 * 注意：
 * - 只负责订单与支付记录的状态修改，不直接调用微信/支付宝接口；
 * - 由支付模块/回调控制器/定时任务调用本服务。
 */
public interface OrderPaymentStatusAppService {

    /**
     * 支付成功回调。
     *
     * @param tenantId     租户ID
     * @param orderId      订单ID
     * @param payChannel   支付渠道
     * @param thirdTradeNo 第三方交易号
     * @param payAmount    实际支付金额
     */
    void onPaySuccess(Long tenantId, Long orderId, String payChannel, String thirdTradeNo, BigDecimal payAmount);

    /**
     * 支付失败/关闭。
     */
    void onPayFailed(Long tenantId, Long orderId, String payChannel, String thirdTradeNo);

    /**
     * 超时未支付自动取消。
     */
    void onPayTimeoutCancel(Long tenantId, Long orderId);

    /**
     * 退款完成（全额）。
     */
    void onFullRefundSuccess(Long tenantId, Long orderId, BigDecimal refundAmount, String refundTradeNo);

    /**
     * 退款完成（部分）。
     */
    void onPartialRefundSuccess(Long tenantId, Long orderId, BigDecimal refundAmount, String refundTradeNo);
}
