package com.bluecone.app.infra.event.outbox;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * 已分发的事件
 * 用于在 Spring 事件总线中传递
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DispatchedEvent {
    
    /**
     * 事件唯一ID
     */
    private String eventId;
    
    /**
     * 事件类型
     */
    private String eventType;
    
    /**
     * 聚合根类型
     */
    private String aggregateType;
    
    /**
     * 聚合根ID
     */
    private String aggregateId;
    
    /**
     * 租户ID
     */
    private Long tenantId;
    
    /**
     * 门店ID
     */
    private Long storeId;
    
    /**
     * 事件载荷
     */
    private Map<String, Object> payload;
    
    /**
     * 事件元数据
     */
    private Map<String, Object> metadata;
    
    /**
     * 便捷方法：获取载荷中的字段
     */
    public Object getPayloadField(String key) {
        return payload != null ? payload.get(key) : null;
    }
    
    /**
     * 便捷方法：获取元数据中的字段
     */
    public Object getMetadataField(String key) {
        return metadata != null ? metadata.get(key) : null;
    }
    
    /**
     * 便捷方法：获取 traceId
     */
    public String getTraceId() {
        Object traceId = getMetadataField("traceId");
        return traceId != null ? traceId.toString() : null;
    }
    
    /**
     * 便捷方法：获取 requestId
     */
    public String getRequestId() {
        Object requestId = getMetadataField("requestId");
        return requestId != null ? requestId.toString() : null;
    }
    
    /**
     * 便捷方法：获取 userId
     */
    public Long getUserId() {
        Object userId = getMetadataField("userId");
        if (userId instanceof Number) {
            return ((Number) userId).longValue();
        }
        if (userId instanceof String) {
            try {
                return Long.parseLong((String) userId);
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return null;
    }
}
