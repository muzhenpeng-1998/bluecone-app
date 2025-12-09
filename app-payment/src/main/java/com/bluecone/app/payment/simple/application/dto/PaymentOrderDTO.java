package com.bluecone.app.payment.simple.application.dto;

import com.bluecone.app.payment.simple.domain.enums.PaymentChannel;
import com.bluecone.app.payment.simple.domain.enums.PaymentStatus;
import com.bluecone.app.payment.simple.domain.model.PaymentOrder;

/**
 * 供应用层与外部模块使用的支付单视图。
 */
public class PaymentOrderDTO {

    private Long id;
    private Long tenantId;
    private Long storeId;
    private Long userId;
    private Long orderId;
    private String payOrderNo;
    private Long totalAmount;
    private Long paidAmount;
    private String channel;
    private String status;

    public static PaymentOrderDTO from(PaymentOrder order) {
        if (order == null) {
            return null;
        }
        PaymentOrderDTO dto = new PaymentOrderDTO();
        dto.setId(order.getId());
        dto.setTenantId(order.getTenantId());
        dto.setStoreId(order.getStoreId());
        dto.setUserId(order.getUserId());
        dto.setOrderId(order.getOrderId());
        dto.setPayOrderNo(order.getPayOrderNo());
        dto.setTotalAmount(order.getTotalAmount());
        dto.setPaidAmount(order.getPaidAmount());
        dto.setChannel(order.getChannel() != null ? order.getChannel().getCode() : null);
        dto.setStatus(order.getStatus() != null ? order.getStatus().getCode() : null);
        return dto;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getTenantId() {
        return tenantId;
    }

    public void setTenantId(Long tenantId) {
        this.tenantId = tenantId;
    }

    public Long getStoreId() {
        return storeId;
    }

    public void setStoreId(Long storeId) {
        this.storeId = storeId;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public Long getOrderId() {
        return orderId;
    }

    public void setOrderId(Long orderId) {
        this.orderId = orderId;
    }

    public String getPayOrderNo() {
        return payOrderNo;
    }

    public void setPayOrderNo(String payOrderNo) {
        this.payOrderNo = payOrderNo;
    }

    public Long getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(Long totalAmount) {
        this.totalAmount = totalAmount;
    }

    public Long getPaidAmount() {
        return paidAmount;
    }

    public void setPaidAmount(Long paidAmount) {
        this.paidAmount = paidAmount;
    }

    public String getChannel() {
        return channel;
    }

    public void setChannel(String channel) {
        this.channel = channel;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
