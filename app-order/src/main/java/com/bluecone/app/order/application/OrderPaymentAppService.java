package com.bluecone.app.order.application;

import com.bluecone.app.order.application.dto.OrderPaymentResult;

/**
 * 订单支付联动服务：在支付成功时更新订单状态。
 */
public interface OrderPaymentAppService {

    OrderPaymentResult onPaymentSuccess(Long tenantId, Long orderId, Long payOrderId, Long paidAmount);
}
