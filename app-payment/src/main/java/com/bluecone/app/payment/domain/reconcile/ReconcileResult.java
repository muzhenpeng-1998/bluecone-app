package com.bluecone.app.payment.domain.reconcile;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 渠道账单与本地支付/退款的对账差异记录。
 */
public class ReconcileResult {

    private Long id;
    private String channelCode;
    private LocalDate billDate;
    private ReconcileDiffType diffType;
    private String channelTradeNo;
    private String merchantOrderNo;
    private String merchantRefundNo;
    private Long paymentOrderId;
    private Long refundOrderId;
    private BigDecimal amountDiff;
    private String statusDiff;
    private String remark;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getChannelCode() {
        return channelCode;
    }

    public void setChannelCode(String channelCode) {
        this.channelCode = channelCode;
    }

    public LocalDate getBillDate() {
        return billDate;
    }

    public void setBillDate(LocalDate billDate) {
        this.billDate = billDate;
    }

    public ReconcileDiffType getDiffType() {
        return diffType;
    }

    public void setDiffType(ReconcileDiffType diffType) {
        this.diffType = diffType;
    }

    public String getChannelTradeNo() {
        return channelTradeNo;
    }

    public void setChannelTradeNo(String channelTradeNo) {
        this.channelTradeNo = channelTradeNo;
    }

    public String getMerchantOrderNo() {
        return merchantOrderNo;
    }

    public void setMerchantOrderNo(String merchantOrderNo) {
        this.merchantOrderNo = merchantOrderNo;
    }

    public String getMerchantRefundNo() {
        return merchantRefundNo;
    }

    public void setMerchantRefundNo(String merchantRefundNo) {
        this.merchantRefundNo = merchantRefundNo;
    }

    public Long getPaymentOrderId() {
        return paymentOrderId;
    }

    public void setPaymentOrderId(Long paymentOrderId) {
        this.paymentOrderId = paymentOrderId;
    }

    public Long getRefundOrderId() {
        return refundOrderId;
    }

    public void setRefundOrderId(Long refundOrderId) {
        this.refundOrderId = refundOrderId;
    }

    public BigDecimal getAmountDiff() {
        return amountDiff;
    }

    public void setAmountDiff(BigDecimal amountDiff) {
        this.amountDiff = amountDiff;
    }

    public String getStatusDiff() {
        return statusDiff;
    }

    public void setStatusDiff(String statusDiff) {
        this.statusDiff = statusDiff;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
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
}
