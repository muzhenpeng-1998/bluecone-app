package com.bluecone.app.infra.event.outbox;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * 进程内事件分发器
 * 将 Outbox 事件转换为 Spring ApplicationEvent 并发布到消费者
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class InProcessEventDispatcher {
    
    private final ApplicationEventPublisher eventPublisher;
    private final ObjectMapper objectMapper;
    
    /**
     * 分发事件到消费者
     * 
     * @param eventPO Outbox 事件持久化对象
     */
    public void dispatch(OutboxEventPO eventPO) {
        try {
            // 解析事件载荷和元数据
            Map<String, Object> payload = parsePayload(eventPO.getEventPayload());
            Map<String, Object> metadata = parseMetadata(eventPO.getEventMetadata());
            
            // 创建事件包装器
            DispatchedEvent event = DispatchedEvent.builder()
                    .eventId(eventPO.getEventId())
                    .eventType(eventPO.getEventType())
                    .aggregateType(eventPO.getAggregateType())
                    .aggregateId(eventPO.getAggregateId())
                    .tenantId(eventPO.getTenantId())
                    .storeId(eventPO.getStoreId())
                    .payload(payload)
                    .metadata(metadata)
                    .build();
            
            // 发布到 Spring 事件总线
            eventPublisher.publishEvent(event);
            
            log.info("Event dispatched: eventId={}, eventType={}, aggregateType={}, aggregateId={}", 
                    eventPO.getEventId(), eventPO.getEventType(), eventPO.getAggregateType(), eventPO.getAggregateId());
            
        } catch (Exception e) {
            log.error("Failed to dispatch event: eventId={}, eventType={}", 
                    eventPO.getEventId(), eventPO.getEventType(), e);
            throw new RuntimeException("Failed to dispatch event: " + eventPO.getEventId(), e);
        }
    }
    
    private Map<String, Object> parsePayload(String payloadJson) {
        if (payloadJson == null || payloadJson.isEmpty()) {
            return new HashMap<>();
        }
        try {
            return objectMapper.readValue(payloadJson, new TypeReference<Map<String, Object>>() {});
        } catch (Exception e) {
            log.warn("Failed to parse event payload, using empty map: {}", e.getMessage());
            return new HashMap<>();
        }
    }
    
    private Map<String, Object> parseMetadata(String metadataJson) {
        if (metadataJson == null || metadataJson.isEmpty()) {
            return new HashMap<>();
        }
        try {
            return objectMapper.readValue(metadataJson, new TypeReference<Map<String, Object>>() {});
        } catch (Exception e) {
            log.warn("Failed to parse event metadata, using empty map: {}", e.getMessage());
            return new HashMap<>();
        }
    }
}
