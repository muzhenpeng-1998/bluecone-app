package com.bluecone.app.billing.event;

import com.bluecone.app.core.event.DomainEvent;
import com.bluecone.app.core.event.EventMetadata;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * 账单支付成功领域事件
 * 当订阅账单支付成功后发出，用于触发订阅激活
 */
public class InvoicePaidEvent extends DomainEvent {

    public static final String EVENT_TYPE = "invoice.paid";

    private final Long invoiceId;
    private final String invoiceNo;
    private final Long tenantId;
    private final Long planSkuId;
    private final String planCode;
    private final String planName;
    private final String billingPeriod;
    private final Integer periodMonths;
    private final Long paidAmountFen;
    private final LocalDateTime paidAt;
    private final LocalDateTime effectiveStartAt;
    private final LocalDateTime effectiveEndAt;

    public InvoicePaidEvent(final Long invoiceId,
                            final String invoiceNo,
                            final Long tenantId,
                            final Long planSkuId,
                            final String planCode,
                            final String planName,
                            final String billingPeriod,
                            final Integer periodMonths,
                            final Long paidAmountFen,
                            final LocalDateTime paidAt,
                            final LocalDateTime effectiveStartAt,
                            final LocalDateTime effectiveEndAt,
                            final String traceId) {
        this(null,
                null,
                buildMetadata(traceId, tenantId),
                invoiceId,
                invoiceNo,
                tenantId,
                planSkuId,
                planCode,
                planName,
                billingPeriod,
                periodMonths,
                paidAmountFen,
                paidAt,
                effectiveStartAt,
                effectiveEndAt);
    }

    @JsonCreator
    public InvoicePaidEvent(@JsonProperty("eventId") final String eventId,
                            @JsonProperty("occurredAt") final Instant occurredAt,
                            @JsonProperty("metadata") final EventMetadata metadata,
                            @JsonProperty("invoiceId") final Long invoiceId,
                            @JsonProperty("invoiceNo") final String invoiceNo,
                            @JsonProperty("tenantId") final Long tenantId,
                            @JsonProperty("planSkuId") final Long planSkuId,
                            @JsonProperty("planCode") final String planCode,
                            @JsonProperty("planName") final String planName,
                            @JsonProperty("billingPeriod") final String billingPeriod,
                            @JsonProperty("periodMonths") final Integer periodMonths,
                            @JsonProperty("paidAmountFen") final Long paidAmountFen,
                            @JsonProperty("paidAt") final LocalDateTime paidAt,
                            @JsonProperty("effectiveStartAt") final LocalDateTime effectiveStartAt,
                            @JsonProperty("effectiveEndAt") final LocalDateTime effectiveEndAt) {
        super(eventId, occurredAt, EVENT_TYPE, metadata);
        this.invoiceId = invoiceId;
        this.invoiceNo = invoiceNo;
        this.tenantId = tenantId;
        this.planSkuId = planSkuId;
        this.planCode = planCode;
        this.planName = planName;
        this.billingPeriod = billingPeriod;
        this.periodMonths = periodMonths;
        this.paidAmountFen = paidAmountFen;
        this.paidAt = paidAt;
        this.effectiveStartAt = effectiveStartAt;
        this.effectiveEndAt = effectiveEndAt;
    }

    private static EventMetadata buildMetadata(String traceId, Long tenantId) {
        Map<String, String> attrs = new HashMap<>();
        if (traceId != null) {
            attrs.put("traceId", traceId);
        }
        if (tenantId != null) {
            attrs.put("tenantId", String.valueOf(tenantId));
        }
        return EventMetadata.of(attrs);
    }

    public Long getInvoiceId() {
        return invoiceId;
    }

    public String getInvoiceNo() {
        return invoiceNo;
    }

    public Long getTenantId() {
        return tenantId;
    }

    public Long getPlanSkuId() {
        return planSkuId;
    }

    public String getPlanCode() {
        return planCode;
    }

    public String getPlanName() {
        return planName;
    }

    public String getBillingPeriod() {
        return billingPeriod;
    }

    public Integer getPeriodMonths() {
        return periodMonths;
    }

    public Long getPaidAmountFen() {
        return paidAmountFen;
    }

    public LocalDateTime getPaidAt() {
        return paidAt;
    }

    public LocalDateTime getEffectiveStartAt() {
        return effectiveStartAt;
    }

    public LocalDateTime getEffectiveEndAt() {
        return effectiveEndAt;
    }

    @Override
    public String toString() {
        return "InvoicePaidEvent{" +
                "invoiceId=" + invoiceId +
                ", invoiceNo='" + invoiceNo + '\'' +
                ", tenantId=" + tenantId +
                ", planSkuId=" + planSkuId +
                ", planCode='" + planCode + '\'' +
                ", planName='" + planName + '\'' +
                ", billingPeriod='" + billingPeriod + '\'' +
                ", periodMonths=" + periodMonths +
                ", paidAmountFen=" + paidAmountFen +
                ", paidAt=" + paidAt +
                ", effectiveStartAt=" + effectiveStartAt +
                ", effectiveEndAt=" + effectiveEndAt +
                ", eventId='" + getEventId() + '\'' +
                ", eventType='" + getEventType() + '\'' +
                ", occurredAt=" + getOccurredAt() +
                '}';
    }
}
