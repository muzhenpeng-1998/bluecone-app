package com.bluecone.app.application.payment.debug;

import com.bluecone.app.order.application.dto.OrderPaymentResult;
import com.bluecone.app.payment.simple.application.dto.PaymentOrderDTO;

/**
 * Response for mock payment success debug endpoint.
 */
public class MockPaySuccessResponse {

    private PaymentInfo payment;
    private OrderInfo order;

    public static MockPaySuccessResponse from(PaymentOrderDTO paymentOrder, OrderPaymentResult orderView) {
        MockPaySuccessResponse response = new MockPaySuccessResponse();
        
        PaymentInfo payment = new PaymentInfo();
        payment.setId(paymentOrder.getId());
        payment.setPayOrderNo(paymentOrder.getPayOrderNo());
        payment.setStatus(paymentOrder.getStatus());
        payment.setPaidAmount(paymentOrder.getPaidAmount());
        response.setPayment(payment);
        
        OrderInfo order = new OrderInfo();
        order.setOrderId(orderView.getOrderId());
        order.setStatus(orderView.getStatus());
        order.setPayStatus(orderView.getPayStatus());
        order.setPayOrderId(orderView.getPayOrderId());
        response.setOrder(order);
        
        return response;
    }

    public PaymentInfo getPayment() {
        return payment;
    }

    public void setPayment(PaymentInfo payment) {
        this.payment = payment;
    }

    public OrderInfo getOrder() {
        return order;
    }

    public void setOrder(OrderInfo order) {
        this.order = order;
    }

    public static class PaymentInfo {
        private Long id;
        private String payOrderNo;
        private String status;
        private Long paidAmount;

        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }

        public String getPayOrderNo() {
            return payOrderNo;
        }

        public void setPayOrderNo(String payOrderNo) {
            this.payOrderNo = payOrderNo;
        }

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }

        public Long getPaidAmount() {
            return paidAmount;
        }

        public void setPaidAmount(Long paidAmount) {
            this.paidAmount = paidAmount;
        }
    }

    public static class OrderInfo {
        private Long orderId;
        private String status;
        private String payStatus;
        private Long payOrderId;

        public Long getOrderId() {
            return orderId;
        }

        public void setOrderId(Long orderId) {
            this.orderId = orderId;
        }

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }

        public String getPayStatus() {
            return payStatus;
        }

        public void setPayStatus(String payStatus) {
            this.payStatus = payStatus;
        }

        public Long getPayOrderId() {
            return payOrderId;
        }

        public void setPayOrderId(Long payOrderId) {
            this.payOrderId = payOrderId;
        }
    }
}

