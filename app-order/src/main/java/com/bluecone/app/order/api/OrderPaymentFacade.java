package com.bluecone.app.order.api;

import java.time.LocalDateTime;

/**
 * 订单支付门面接口。
 * <p>对外暴露订单侧的支付相关能力，供支付模块调用。</p>
 * <p>隔离原则：支付模块不得直接操作订单表，只能通过此 Facade 调用订单应用服务。</p>
 */
public interface OrderPaymentFacade {

    /**
     * 标记订单为已支付。
     * <p>状态机约束：只允许从 WAIT_PAY 状态流转到 PAID 状态。</p>
     * <p>幂等性：如果订单已经是 PAID 状态，则直接返回成功，不抛异常（允许重复回调）。</p>
     *
     * @param tenantId   租户ID
     * @param orderId    订单ID
     * @param payOrderId 支付单ID
     * @param payChannel 支付渠道（如：WECHAT_JSAPI、ALIPAY_WAP）
     * @param payNo      渠道支付单号（如：微信transaction_id）
     * @param paidAt     支付完成时间
     * @throws com.bluecone.app.core.exception.BizException 如果订单不存在或状态不允许支付
     */
    void markOrderPaid(Long tenantId, Long orderId, Long payOrderId, String payChannel, String payNo, LocalDateTime paidAt);

    /**
     * 标记订单为已取消（带关单原因）。
     * <p>用于超时关单、用户取消、商户取消等场景。</p>
     * <p>幂等性：如果订单已经是 CANCELLED 状态，则直接返回成功。</p>
     *
     * @param tenantId    租户ID
     * @param orderId     订单ID
     * @param closeReason 关单原因（PAY_TIMEOUT、USER_CANCEL、MERCHANT_CANCEL等）
     * @throws com.bluecone.app.core.exception.BizException 如果订单不存在或状态不允许取消
     */
    void markOrderCancelled(Long tenantId, Long orderId, String closeReason);
}
