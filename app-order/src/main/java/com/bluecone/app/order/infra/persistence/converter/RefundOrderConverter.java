package com.bluecone.app.order.infra.persistence.converter;

import com.bluecone.app.order.domain.enums.RefundChannel;
import com.bluecone.app.order.domain.enums.RefundStatus;
import com.bluecone.app.order.domain.model.RefundOrder;
import com.bluecone.app.order.infra.persistence.po.RefundOrderPO;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.Map;

/**
 * 退款单领域模型与持久化对象转换器。
 */
@Slf4j
@Component
public class RefundOrderConverter {
    
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    
    /**
     * PO 转领域模型。
     * 
     * @param po 退款单PO
     * @return 退款单聚合根
     */
    public RefundOrder toDomain(RefundOrderPO po) {
        if (po == null) {
            return null;
        }
        
        Map<String, Object> ext = parseExtJson(po.getExtJson());
        
        return RefundOrder.builder()
                .id(po.getId())
                .tenantId(po.getTenantId())
                .storeId(po.getStoreId())
                .orderId(po.getOrderId())
                .publicOrderNo(po.getPublicOrderNo())
                .refundId(po.getRefundId())
                .channel(RefundChannel.fromCode(po.getChannel()))
                .refundAmount(po.getRefundAmount())
                .currency(po.getCurrency())
                .status(RefundStatus.fromCode(po.getStatus()))
                .refundNo(po.getRefundNo())
                .reasonCode(po.getReasonCode())
                .reasonDesc(po.getReasonDesc())
                .idemKey(po.getIdemKey())
                .payOrderId(po.getPayOrderId())
                .payNo(po.getPayNo())
                .refundRequestedAt(po.getRefundRequestedAt())
                .refundCompletedAt(po.getRefundCompletedAt())
                .ext(ext)
                .version(po.getVersion())
                .createdAt(po.getCreatedAt())
                .createdBy(po.getCreatedBy())
                .updatedAt(po.getUpdatedAt())
                .updatedBy(po.getUpdatedBy())
                .build();
    }
    
    /**
     * 领域模型转 PO。
     * 
     * @param domain 退款单聚合根
     * @return 退款单PO
     */
    public RefundOrderPO toPO(RefundOrder domain) {
        if (domain == null) {
            return null;
        }
        
        String extJson = serializeExtJson(domain.getExt());
        
        return RefundOrderPO.builder()
                .id(domain.getId())
                .tenantId(domain.getTenantId())
                .storeId(domain.getStoreId())
                .orderId(domain.getOrderId())
                .publicOrderNo(domain.getPublicOrderNo())
                .refundId(domain.getRefundId())
                .channel(domain.getChannel() != null ? domain.getChannel().getCode() : null)
                .refundAmount(domain.getRefundAmount())
                .currency(domain.getCurrency())
                .status(domain.getStatus() != null ? domain.getStatus().getCode() : null)
                .refundNo(domain.getRefundNo())
                .reasonCode(domain.getReasonCode())
                .reasonDesc(domain.getReasonDesc())
                .idemKey(domain.getIdemKey())
                .payOrderId(domain.getPayOrderId())
                .payNo(domain.getPayNo())
                .refundRequestedAt(domain.getRefundRequestedAt())
                .refundCompletedAt(domain.getRefundCompletedAt())
                .extJson(extJson)
                .version(domain.getVersion())
                .createdAt(domain.getCreatedAt())
                .createdBy(domain.getCreatedBy())
                .updatedAt(domain.getUpdatedAt())
                .updatedBy(domain.getUpdatedBy())
                .build();
    }
    
    /**
     * 解析扩展字段 JSON。
     * 
     * @param extJson 扩展字段 JSON 字符串
     * @return 扩展字段 Map
     */
    private Map<String, Object> parseExtJson(String extJson) {
        if (extJson == null || extJson.isBlank()) {
            return Collections.emptyMap();
        }
        try {
            return OBJECT_MAPPER.readValue(extJson, new TypeReference<Map<String, Object>>() {});
        } catch (Exception e) {
            log.warn("解析退款单扩展字段失败：extJson={}", extJson, e);
            return Collections.emptyMap();
        }
    }
    
    /**
     * 序列化扩展字段为 JSON。
     * 
     * @param ext 扩展字段 Map
     * @return 扩展字段 JSON 字符串
     */
    private String serializeExtJson(Map<String, Object> ext) {
        if (ext == null || ext.isEmpty()) {
            return null;
        }
        try {
            return OBJECT_MAPPER.writeValueAsString(ext);
        } catch (Exception e) {
            log.warn("序列化退款单扩展字段失败：ext={}", ext, e);
            return null;
        }
    }
}
