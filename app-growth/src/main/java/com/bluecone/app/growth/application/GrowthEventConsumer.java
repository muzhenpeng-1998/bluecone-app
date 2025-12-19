package com.bluecone.app.growth.application;

import com.bluecone.app.core.event.outbox.EventType;
import com.bluecone.app.infra.event.outbox.OutboxEventDO;
import com.bluecone.app.infra.event.outbox.handler.OutboxEventHandler;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 增长引擎事件消费者
 * 消费 PAYMENT_SUCCESS 事件，触发首单奖励发放
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class GrowthEventConsumer implements OutboxEventHandler {
    
    private final GrowthApplicationService growthApplicationService;
    private final ObjectMapper objectMapper;
    
    @Override
    public boolean supports(OutboxEventDO event) {
        return EventType.PAYMENT_SUCCESS.getCode().equals(event.getEventType());
    }
    
    @Override
    public void handle(OutboxEventDO event) throws Exception {
        log.info("[growth-consumer] 消费 PAYMENT_SUCCESS 事件，eventId={}, aggregateId={}", 
                event.getId(), event.getAggregateId());
        
        try {
            // 解析事件载荷
            Map<String, Object> payload = objectMapper.readValue(event.getEventBody(), Map.class);
            Long tenantId = getLongValue(payload, "tenantId");
            Long userId = getLongValue(payload, "userId");
            Long orderId = getLongValue(payload, "orderId");
            
            if (tenantId == null || userId == null || orderId == null) {
                log.error("[growth-consumer] 事件载荷缺少必要字段，eventId={}, payload={}", 
                        event.getId(), payload);
                return;
            }
            
            // 处理首单完成（会检查是否首单，并触发奖励发放）
            growthApplicationService.handleFirstOrderCompleted(tenantId, userId, orderId);
            
            log.info("[growth-consumer] 首单处理完成，eventId={}, userId={}, orderId={}", 
                    event.getId(), userId, orderId);
            
        } catch (Exception e) {
            log.error("[growth-consumer] 消费 PAYMENT_SUCCESS 事件失败，eventId={}", event.getId(), e);
            throw e;
        }
    }
    
    private Long getLongValue(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value == null) {
            return null;
        }
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        try {
            return Long.valueOf(value.toString());
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
