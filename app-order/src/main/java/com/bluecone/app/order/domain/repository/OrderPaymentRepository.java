package com.bluecone.app.order.domain.repository;

import com.bluecone.app.order.domain.model.OrderPayment;

public interface OrderPaymentRepository {

    /**
     * 为订单创建支付记录。
     */
    void save(OrderPayment payment);

    /**
     * 根据订单ID查询支付记录。
     */
    OrderPayment findByOrderId(Long tenantId, Long orderId);

    /**
     * 根据第三方交易号查询，用于支付回调幂等。
     */
    OrderPayment findByChannelAndTradeNo(Long tenantId, String payChannel, String thirdTradeNo);

    /**
     * 更新支付状态。
     */
    void updateStatus(OrderPayment payment);
}
