package com.bluecone.app.payment.api.dto;

import com.bluecone.app.payment.domain.reconcile.ReconcileResult;
import com.bluecone.app.payment.domain.reconcile.ReconcileDiffType;

import java.math.BigDecimal;
import java.time.LocalDate;

public class ReconcileResultView {

    private String channelCode;
    private LocalDate billDate;
    private String diffType;
    private String channelTradeNo;
    private String merchantOrderNo;
    private String merchantRefundNo;
    private Long paymentOrderId;
    private Long refundOrderId;
    private BigDecimal amountDiff;
    private String statusDiff;
    private String remark;

    public static ReconcileResultView fromDomain(ReconcileResult result) {
        ReconcileResultView view = new ReconcileResultView();
        view.setChannelCode(result.getChannelCode());
        view.setBillDate(result.getBillDate());
        view.setDiffType(result.getDiffType() == null ? null : result.getDiffType().name());
        view.setChannelTradeNo(result.getChannelTradeNo());
        view.setMerchantOrderNo(result.getMerchantOrderNo());
        view.setMerchantRefundNo(result.getMerchantRefundNo());
        view.setPaymentOrderId(result.getPaymentOrderId());
        view.setRefundOrderId(result.getRefundOrderId());
        view.setAmountDiff(result.getAmountDiff());
        view.setStatusDiff(result.getStatusDiff());
        view.setRemark(result.getRemark());
        return view;
    }

    // getters and setters

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

    public String getDiffType() {
        return diffType;
    }

    public void setDiffType(String diffType) {
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
}
