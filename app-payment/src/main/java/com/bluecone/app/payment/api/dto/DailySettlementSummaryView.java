package com.bluecone.app.payment.api.dto;

import com.bluecone.app.payment.domain.reconcile.DailySettlementSummary;

import java.math.BigDecimal;
import java.time.LocalDate;

public class DailySettlementSummaryView {

    private Long tenantId;
    private Long storeId;
    private String channelCode;
    private LocalDate billDate;
    private BigDecimal totalPaidAmount;
    private BigDecimal totalRefundedAmount;
    private BigDecimal netAmount;
    private Integer payCount;
    private Integer refundCount;
    private BigDecimal totalFee;

    public static DailySettlementSummaryView fromDomain(DailySettlementSummary summary) {
        DailySettlementSummaryView view = new DailySettlementSummaryView();
        view.setTenantId(summary.getTenantId());
        view.setStoreId(summary.getStoreId());
        view.setChannelCode(summary.getChannelCode());
        view.setBillDate(summary.getBillDate());
        view.setTotalPaidAmount(summary.getTotalPaidAmount());
        view.setTotalRefundedAmount(summary.getTotalRefundedAmount());
        view.setNetAmount(summary.getNetAmount());
        view.setPayCount(summary.getPayCount());
        view.setRefundCount(summary.getRefundCount());
        view.setTotalFee(summary.getTotalFee());
        return view;
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

    public BigDecimal getTotalPaidAmount() {
        return totalPaidAmount;
    }

    public void setTotalPaidAmount(BigDecimal totalPaidAmount) {
        this.totalPaidAmount = totalPaidAmount;
    }

    public BigDecimal getTotalRefundedAmount() {
        return totalRefundedAmount;
    }

    public void setTotalRefundedAmount(BigDecimal totalRefundedAmount) {
        this.totalRefundedAmount = totalRefundedAmount;
    }

    public BigDecimal getNetAmount() {
        return netAmount;
    }

    public void setNetAmount(BigDecimal netAmount) {
        this.netAmount = netAmount;
    }

    public Integer getPayCount() {
        return payCount;
    }

    public void setPayCount(Integer payCount) {
        this.payCount = payCount;
    }

    public Integer getRefundCount() {
        return refundCount;
    }

    public void setRefundCount(Integer refundCount) {
        this.refundCount = refundCount;
    }

    public BigDecimal getTotalFee() {
        return totalFee;
    }

    public void setTotalFee(BigDecimal totalFee) {
        this.totalFee = totalFee;
    }
}
