package com.bluecone.app.order.infra.persistence.converter;

import com.bluecone.app.order.domain.model.OrderActionLog;
import com.bluecone.app.order.infra.persistence.po.OrderActionLogPO;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Collections;
import java.util.Map;

/**
 * 订单动作日志领域模型与 PO 互转。
 */
public class OrderActionLogConverter {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private OrderActionLogConverter() {
    }

    /**
     * PO 转领域模型。
     */
    public static OrderActionLog toDomain(OrderActionLogPO po) {
        if (po == null) {
            return null;
        }
        return OrderActionLog.builder()
                .id(po.getId())
                .tenantId(po.getTenantId())
                .storeId(po.getStoreId())
                .orderId(po.getOrderId())
                .actionType(po.getActionType())
                .actionKey(po.getActionKey())
                .operatorId(po.getOperatorId())
                .operatorName(po.getOperatorName())
                .status(po.getStatus())
                .resultJson(po.getResultJson())
                .errorCode(po.getErrorCode())
                .errorMsg(po.getErrorMsg())
                .ext(parseJsonMap(po.getExtJson()))
                .createdAt(po.getCreatedAt())
                .updatedAt(po.getUpdatedAt())
                .build();
    }

    /**
     * 领域模型转 PO。
     */
    public static OrderActionLogPO toPO(OrderActionLog log) {
        if (log == null) {
            return null;
        }
        OrderActionLogPO po = new OrderActionLogPO();
        po.setId(log.getId());
        po.setTenantId(log.getTenantId());
        po.setStoreId(log.getStoreId());
        po.setOrderId(log.getOrderId());
        po.setActionType(log.getActionType());
        po.setActionKey(log.getActionKey());
        po.setOperatorId(log.getOperatorId());
        po.setOperatorName(log.getOperatorName());
        po.setStatus(log.getStatus());
        po.setResultJson(log.getResultJson());
        po.setErrorCode(log.getErrorCode());
        po.setErrorMsg(log.getErrorMsg());
        po.setExtJson(toJson(log.getExt()));
        po.setCreatedAt(log.getCreatedAt());
        po.setUpdatedAt(log.getUpdatedAt());
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
