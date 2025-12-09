package com.bluecone.app.order.infra.persistence.converter;

import com.bluecone.app.order.domain.enums.BizType;
import com.bluecone.app.order.domain.enums.OrderSource;
import com.bluecone.app.order.domain.enums.OrderStatus;
import com.bluecone.app.order.domain.enums.PayStatus;
import com.bluecone.app.order.domain.model.Order;
import com.bluecone.app.order.domain.model.OrderItem;
import com.bluecone.app.order.domain.model.OrderPayment;
import com.bluecone.app.order.domain.model.OrderSession;
import com.bluecone.app.order.infra.persistence.po.OrderItemPO;
import com.bluecone.app.order.infra.persistence.po.OrderPO;
import com.bluecone.app.order.infra.persistence.po.OrderPaymentPO;
import com.bluecone.app.order.infra.persistence.po.OrderSessionPO;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

public class OrderConverter {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private OrderConverter() {
    }

    public static Order toDomain(OrderPO orderPO, List<OrderItemPO> itemPOList) {
        if (orderPO == null) {
            return null;
        }
        List<OrderItem> items = itemPOList == null ? Collections.emptyList() : itemPOList.stream()
                .filter(Objects::nonNull)
                .map(OrderConverter::toDomain)
                .collect(Collectors.toList());
        return Order.builder()
                .id(orderPO.getId())
                .tenantId(orderPO.getTenantId())
                .storeId(orderPO.getStoreId())
                .userId(orderPO.getUserId())
                .sessionId(orderPO.getSessionId())
                .sessionVersion(orderPO.getSessionVersion())
                .orderNo(orderPO.getOrderNo())
                .clientOrderNo(orderPO.getClientOrderNo())
                .bizType(BizType.fromCode(orderPO.getBizType()))
                .orderSource(OrderSource.fromCode(orderPO.getOrderSource()))
                .channel(orderPO.getChannel())
                .totalAmount(orderPO.getTotalAmount())
                .discountAmount(orderPO.getDiscountAmount())
                .payableAmount(orderPO.getPayableAmount())
                .currency(orderPO.getCurrency())
                .status(OrderStatus.fromCode(orderPO.getStatus()))
                .payStatus(PayStatus.fromCode(orderPO.getPayStatus()))
                .remark(orderPO.getOrderRemark())
                .ext(parseJsonMap(orderPO.getExtJson()))
                .items(items)
                .version(orderPO.getVersion())
                .createdAt(orderPO.getCreatedAt())
                .createdBy(orderPO.getCreatedBy())
                .updatedAt(orderPO.getUpdatedAt())
                .updatedBy(orderPO.getUpdatedBy())
                .acceptOperatorId(orderPO.getAcceptOperatorId())
                .acceptedAt(orderPO.getAcceptedAt())
                .userDeleted(orderPO.getUserDeleted())
                .userDeletedAt(orderPO.getUserDeletedAt())
                .build();
    }

    public static OrderPO toPO(Order order) {
        if (order == null) {
            return null;
        }
        OrderPO po = new OrderPO();
        po.setId(order.getId());
        po.setTenantId(order.getTenantId());
        po.setStoreId(order.getStoreId());
        po.setUserId(order.getUserId());
        po.setSessionId(order.getSessionId());
        po.setSessionVersion(order.getSessionVersion());
        po.setOrderNo(order.getOrderNo());
        po.setClientOrderNo(order.getClientOrderNo());
        po.setBizType(order.getBizType() != null ? order.getBizType().getCode() : null);
        po.setOrderSource(order.getOrderSource() != null ? order.getOrderSource().getCode() : null);
        po.setChannel(order.getChannel());
        po.setTotalAmount(order.getTotalAmount());
        po.setDiscountAmount(order.getDiscountAmount());
        po.setPayableAmount(order.getPayableAmount());
        po.setCurrency(order.getCurrency());
        po.setStatus(order.getStatus() != null ? order.getStatus().getCode() : null);
        po.setPayStatus(order.getPayStatus() != null ? order.getPayStatus().getCode() : null);
        po.setOrderRemark(order.getRemark());
        po.setExtJson(toJson(order.getExt()));
        po.setVersion(order.getVersion());
        po.setCreatedAt(order.getCreatedAt());
        po.setUpdatedAt(order.getUpdatedAt());
        po.setCreatedBy(order.getCreatedBy());
        po.setUpdatedBy(order.getUpdatedBy());
        po.setAcceptOperatorId(order.getAcceptOperatorId());
        po.setAcceptedAt(order.getAcceptedAt());
        po.setUserDeleted(order.getUserDeleted());
        po.setUserDeletedAt(order.getUserDeletedAt());
        return po;
    }

    public static OrderItem toDomain(OrderItemPO po) {
        if (po == null) {
            return null;
        }
        return OrderItem.builder()
                .id(po.getId())
                .orderId(po.getOrderId())
                .tenantId(po.getTenantId())
                .storeId(po.getStoreId())
                .productId(po.getProductId())
                .skuId(po.getSkuId())
                .productName(po.getProductName())
                .skuName(po.getSkuName())
                .productCode(po.getProductCode())
                .quantity(po.getQuantity())
                .unitPrice(po.getUnitPrice())
                .discountAmount(po.getDiscountAmount())
                .payableAmount(po.getPayableAmount())
                .attrs(parseJsonMap(po.getAttrsJson()))
                .remark(po.getRemark())
                .createdAt(po.getCreatedAt())
                .createdBy(po.getCreatedBy())
                .updatedAt(po.getUpdatedAt())
                .updatedBy(po.getUpdatedBy())
                .build();
    }

    public static OrderItemPO toItemPO(Order order, OrderItem item) {
        if (item == null) {
            return null;
        }
        OrderItemPO po = new OrderItemPO();
        po.setId(item.getId());
        po.setTenantId(order != null ? order.getTenantId() : item.getTenantId());
        po.setStoreId(order != null ? order.getStoreId() : item.getStoreId());
        po.setOrderId(order != null ? order.getId() : item.getOrderId());
        po.setProductId(item.getProductId());
        po.setSkuId(item.getSkuId());
        po.setProductName(item.getProductName());
        po.setSkuName(item.getSkuName());
        po.setProductCode(item.getProductCode());
        po.setQuantity(item.getQuantity());
        po.setUnitPrice(item.getUnitPrice());
        po.setDiscountAmount(item.getDiscountAmount());
        po.setPayableAmount(item.getPayableAmount());
        po.setAttrsJson(toJson(item.getAttrs()));
        po.setRemark(item.getRemark());
        po.setCreatedAt(item.getCreatedAt());
        po.setCreatedBy(item.getCreatedBy());
        po.setUpdatedAt(item.getUpdatedAt());
        po.setUpdatedBy(item.getUpdatedBy());
        return po;
    }

    public static OrderPayment toDomain(OrderPaymentPO po) {
        if (po == null) {
            return null;
        }
        return OrderPayment.builder()
                .id(po.getId())
                .tenantId(po.getTenantId())
                .storeId(po.getStoreId())
                .orderId(po.getOrderId())
                .payChannel(po.getPayChannel())
                .payStatus(PayStatus.fromCode(po.getPayStatus()))
                .payAmount(po.getPayAmount())
                .currency(po.getCurrency())
                .thirdTradeNo(po.getThirdTradeNo())
                .payTime(po.getPayTime())
                .extra(parseJsonMap(po.getExtraJson()))
                .createdAt(po.getCreatedAt())
                .createdBy(po.getCreatedBy())
                .updatedAt(po.getUpdatedAt())
                .updatedBy(po.getUpdatedBy())
                .build();
    }

    public static OrderPaymentPO toPaymentPO(OrderPayment payment) {
        if (payment == null) {
            return null;
        }
        OrderPaymentPO po = new OrderPaymentPO();
        po.setId(payment.getId());
        po.setTenantId(payment.getTenantId());
        po.setStoreId(payment.getStoreId());
        po.setOrderId(payment.getOrderId());
        po.setPayChannel(payment.getPayChannel());
        po.setPayStatus(payment.getPayStatus() != null ? payment.getPayStatus().getCode() : null);
        po.setPayAmount(payment.getPayAmount());
        po.setCurrency(payment.getCurrency());
        po.setThirdTradeNo(payment.getThirdTradeNo());
        po.setPayTime(payment.getPayTime());
        po.setExtraJson(toJson(payment.getExtra()));
        po.setCreatedAt(payment.getCreatedAt());
        po.setCreatedBy(payment.getCreatedBy());
        po.setUpdatedAt(payment.getUpdatedAt());
        po.setUpdatedBy(payment.getUpdatedBy());
        return po;
    }

    public static OrderSession toDomain(OrderSessionPO po) {
        if (po == null) {
            return null;
        }
        return OrderSession.builder()
                .id(po.getId())
                .tenantId(po.getTenantId())
                .storeId(po.getStoreId())
                .sessionId(po.getSessionId())
                .tableId(po.getTableId())
                .status(po.getStatus())
                .version(po.getVersion())
                .lastSnapshot(po.getLastSnapshot())
                .extJson(po.getExtJson())
                .createdAt(po.getCreatedAt())
                .createdBy(po.getCreatedBy())
                .updatedAt(po.getUpdatedAt())
                .updatedBy(po.getUpdatedBy())
                .build();
    }

    public static OrderSessionPO toSessionPO(OrderSession session) {
        if (session == null) {
            return null;
        }
        OrderSessionPO po = new OrderSessionPO();
        po.setId(session.getId());
        po.setTenantId(session.getTenantId());
        po.setStoreId(session.getStoreId());
        po.setSessionId(session.getSessionId());
        po.setTableId(session.getTableId());
        po.setStatus(session.getStatus());
        po.setVersion(session.getVersion());
        po.setLastSnapshot(session.getLastSnapshot());
        po.setExtJson(session.getExtJson());
        po.setCreatedAt(session.getCreatedAt());
        po.setCreatedBy(session.getCreatedBy());
        po.setUpdatedAt(session.getUpdatedAt());
        po.setUpdatedBy(session.getUpdatedBy());
        return po;
    }

    private static Map<String, Object> parseJsonMap(String json) {
        if (json == null || json.isBlank()) {
            return Collections.emptyMap();
        }
        try {
            return OBJECT_MAPPER.readValue(json, new TypeReference<Map<String, Object>>() {
            });
        } catch (JsonProcessingException e) {
            return Collections.emptyMap();
        }
    }

    private static String toJson(Map<String, Object> map) {
        if (map == null || map.isEmpty()) {
            return null;
        }
        try {
            return OBJECT_MAPPER.writeValueAsString(map);
        } catch (JsonProcessingException e) {
            return null;
        }
    }
}
