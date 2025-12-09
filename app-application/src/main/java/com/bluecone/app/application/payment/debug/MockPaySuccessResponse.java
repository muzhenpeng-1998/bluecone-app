package com.bluecone.app.application.payment.debug;

import com.bluecone.app.order.application.dto.OrderPaymentResult;
import com.bluecone.app.payment.simple.application.dto.PaymentOrderDTO;

public class MockPaySuccessResponse {

    private PaymentOrderDTO paymentOrder;
    private OrderPaymentResult order;

    public static MockPaySuccessResponse from(PaymentOrderDTO paymentOrder, OrderPaymentResult order) {
        MockPaySuccessResponse resp = new MockPaySuccessResponse();
        resp.setPaymentOrder(paymentOrder);
        resp.setOrder(order);
        return resp;
    }

    public PaymentOrderDTO getPaymentOrder() {
        return paymentOrder;
    }

    public void setPaymentOrder(PaymentOrderDTO paymentOrder) {
        this.paymentOrder = paymentOrder;
    }

    public OrderPaymentResult getOrder() {
        return order;
    }

    public void setOrder(OrderPaymentResult order) {
        this.order = order;
    }
}
