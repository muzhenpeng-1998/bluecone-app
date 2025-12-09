package com.bluecone.app.payment.simple.domain.model;

import com.bluecone.app.payment.simple.domain.enums.PaymentChannel;
import com.bluecone.app.payment.simple.domain.enums.PaymentStatus;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * 仅限于小程序调试流程的支付单聚合。
 */
public class PaymentOrder implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private String payOrderNo;
    private Long tenantId;
    private Long storeId;
    private Long userId;
    private Long orderId;
    private Long totalAmount;
    private Long paidAmount;
    private PaymentChannel channel;
    private PaymentStatus status;
    private String outTransactionNo;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime paidAt;

    public static PaymentOrder createForOrder(Long tenantId,
                                              Long storeId,
                                              Long userId,
                                              Long orderId,
                                              Long totalAmount) {
        if (tenantId == null || storeId == null || userId == null || orderId == null) {
            throw new IllegalArgumentException("tenantId/storeId/userId/orderId 必填");
        }
        if (totalAmount == null || totalAmount < 0) {
            throw new IllegalArgumentException("totalAmount 不能为空且不可为负");
        }
        LocalDateTime now = LocalDateTime.now();
        PaymentOrder order = new PaymentOrder();
        order.setTenantId(tenantId);
        order.setStoreId(storeId);
        order.setUserId(userId);
        order.setOrderId(orderId);
        order.setTotalAmount(totalAmount);
        order.setPaidAmount(0L);
        order.setChannel(PaymentChannel.INTERNAL_DEBUG);
        order.setStatus(PaymentStatus.WAIT_PAY);
        order.setCreatedAt(now);
        order.setUpdatedAt(now);
        order.setPayOrderNo(generatePayOrderNo(orderId, now));
        return order;
    }

    private static String generatePayOrderNo(Long orderId, LocalDateTime now) {
        return "PAY-" + orderId + "-" + now.toEpochSecond(java.time.ZoneOffset.UTC);
    }

    public void markSuccess(Long paidAmount, String outTransactionNo) {
        if (this.status != PaymentStatus.WAIT_PAY) {
            return;
        }
        if (paidAmount == null || paidAmount < 0) {
            throw new IllegalArgumentException("paidAmount 不合法");
        }
        this.paidAmount = paidAmount;
        this.outTransactionNo = outTransactionNo;
        this.status = PaymentStatus.SUCCESS;
        this.paidAt = LocalDateTime.now();
        this.updatedAt = this.paidAt;
    }

    // Getter / Setter
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

    public PaymentChannel getChannel() {
        return channel;
    }

    public void setChannel(PaymentChannel channel) {
        this.channel = channel;
    }

    public PaymentStatus getStatus() {
        return status;
    }

    public void setStatus(PaymentStatus status) {
        this.status = status;
    }

    public String getOutTransactionNo() {
        return outTransactionNo;
    }

    public void setOutTransactionNo(String outTransactionNo) {
        this.outTransactionNo = outTransactionNo;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public LocalDateTime getPaidAt() {
        return paidAt;
    }

    public void setPaidAt(LocalDateTime paidAt) {
        this.paidAt = paidAt;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, payOrderNo);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof PaymentOrder)) {
            return false;
        }
        PaymentOrder other = (PaymentOrder) obj;
        return Objects.equals(id, other.id);
    }
}
