package com.bluecone.app.order.domain.event;

import com.bluecone.app.core.event.DomainEvent;
import com.bluecone.app.core.event.EventMetadata;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * 商户拒单领域事件。
 * 
 * <h3>触发时机：</h3>
 * <p>商户在订单待接单（WAIT_ACCEPT）状态下拒绝接单，订单状态流转为 CANCELED。</p>
 * 
 * <h3>下游消费方：</h3>
 * <ul>
 *   <li>支付模块：触发自动退款流程，将用户已支付的款项退回</li>
 *   <li>通知模块：向用户发送拒单通知（短信、小程序消息、推送）</li>
 *   <li>商户端：更新订单列表，展示拒单记录</li>
 *   <li>数据分析：统计商户拒单率、拒单原因分布，用于运营优化</li>
 * </ul>
 */
public class OrderRejectedEvent extends DomainEvent {

    public static final String EVENT_TYPE = "ORDER_REJECTED";
    private static final String AGGREGATE_TYPE = "ORDER";

    private final Long tenantId;
    private final Long storeId;
    private final Long orderId;
    private final Long operatorId;
    private final String reasonCode;
    private final String reasonDesc;
    private final Long payOrderId;
    private final Long refundAmountCents;

    public OrderRejectedEvent(Long tenantId,
                              Long storeId,
                              Long orderId,
                              Long operatorId,
                              String reasonCode,
                              String reasonDesc,
                              Long payOrderId,
                              Long refundAmountCents) {
        super(EVENT_TYPE, buildMetadata(tenantId, storeId, orderId, payOrderId));
        this.tenantId = tenantId;
        this.storeId = storeId;
        this.orderId = orderId;
        this.operatorId = operatorId;
        this.reasonCode = reasonCode;
        this.reasonDesc = reasonDesc;
        this.payOrderId = payOrderId;
        this.refundAmountCents = refundAmountCents;
    }

    @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
    public OrderRejectedEvent(@JsonProperty("eventId") String eventId,
                              @JsonProperty("occurredAt") Instant occurredAt,
                              @JsonProperty("eventType") String eventType,
                              @JsonProperty("metadata") EventMetadata metadata,
                              @JsonProperty("tenantId") Long tenantId,
                              @JsonProperty("storeId") Long storeId,
                              @JsonProperty("orderId") Long orderId,
                              @JsonProperty("operatorId") Long operatorId,
                              @JsonProperty("reasonCode") String reasonCode,
                              @JsonProperty("reasonDesc") String reasonDesc,
                              @JsonProperty("payOrderId") Long payOrderId,
                              @JsonProperty("refundAmountCents") Long refundAmountCents) {
        super(eventId,
                occurredAt,
                eventType == null ? EVENT_TYPE : eventType,
                metadata == null ? buildMetadata(tenantId, storeId, orderId, payOrderId) : metadata);
        this.tenantId = tenantId;
        this.storeId = storeId;
        this.orderId = orderId;
        this.operatorId = operatorId;
        this.reasonCode = reasonCode;
        this.reasonDesc = reasonDesc;
        this.payOrderId = payOrderId;
        this.refundAmountCents = refundAmountCents;
    }

    public Long getTenantId() {
        return tenantId;
    }

    public Long getStoreId() {
        return storeId;
    }

    public Long getOrderId() {
        return orderId;
    }

    public Long getOperatorId() {
        return operatorId;
    }

    public String getReasonCode() {
        return reasonCode;
    }

    public String getReasonDesc() {
        return reasonDesc;
    }

    public Long getPayOrderId() {
        return payOrderId;
    }

    public Long getRefundAmountCents() {
        return refundAmountCents;
    }

    private static EventMetadata buildMetadata(Long tenantId,
                                               Long storeId,
                                               Long orderId,
                                               Long payOrderId) {
        Map<String, String> attributes = new HashMap<>();
        attributes.put("aggregateType", AGGREGATE_TYPE);
        if (orderId != null) {
            attributes.put("aggregateId", String.valueOf(orderId));
        }
        if (tenantId != null) {
            attributes.put("tenantId", String.valueOf(tenantId));
        }
        if (storeId != null) {
            attributes.put("storeId", String.valueOf(storeId));
        }
        if (payOrderId != null) {
            attributes.put("payOrderId", String.valueOf(payOrderId));
        }
        return attributes.isEmpty() ? EventMetadata.empty() : EventMetadata.of(attributes);
    }
}
