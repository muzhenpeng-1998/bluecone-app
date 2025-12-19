package com.bluecone.app.infra.event.consume;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * 事件消费日志服务
 * 提供消费日志的写入、查询等功能，保证消费者幂等性
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EventConsumeLogService {
    
    private final EventConsumeLogMapper consumeLogMapper;
    private final ObjectMapper objectMapper;
    
    /**
     * 检查事件是否已被消费（幂等性检查）
     * 
     * @param consumerName 消费者名称
     * @param eventId 事件ID
     * @return true=已消费，false=未消费
     */
    public boolean isConsumed(String consumerName, String eventId) {
        EventConsumeLogPO log = consumeLogMapper.selectByConsumerAndEvent(consumerName, eventId);
        return log != null && "SUCCESS".equals(log.getStatus());
    }
    
    /**
     * 检查幂等键是否已被消费
     * 
     * @param idempotencyKey 幂等键
     * @return true=已消费，false=未消费
     */
    public boolean isIdempotencyKeyConsumed(String idempotencyKey) {
        EventConsumeLogPO log = consumeLogMapper.selectByIdempotencyKey(idempotencyKey);
        return log != null;
    }
    
    /**
     * 记录消费成功
     * 
     * @param consumerName 消费者名称
     * @param eventId 事件ID
     * @param eventType 事件类型
     * @param tenantId 租户ID
     * @param idempotencyKey 幂等键
     * @param result 消费结果
     */
    @Transactional(rollbackFor = Exception.class)
    public void recordSuccess(String consumerName, String eventId, String eventType, 
                              Long tenantId, String idempotencyKey, Object result) {
        EventConsumeLogPO log = new EventConsumeLogPO();
        log.setConsumerName(consumerName);
        log.setEventId(eventId);
        log.setEventType(eventType);
        log.setTenantId(tenantId);
        log.setIdempotencyKey(idempotencyKey);
        log.setStatus("SUCCESS");
        
        if (result != null) {
            try {
                log.setConsumeResult(objectMapper.writeValueAsString(result));
            } catch (JsonProcessingException e) {
                log.setConsumeResult(result.toString());
            }
        }
        
        log.setConsumedAt(LocalDateTime.now());
        log.setCreatedAt(LocalDateTime.now());
        log.setUpdatedAt(LocalDateTime.now());
        
        consumeLogMapper.insert(log);
        
        this.log.info("Event consume success recorded: consumer={}, eventId={}, eventType={}, idempotencyKey={}", 
                consumerName, eventId, eventType, idempotencyKey);
    }
    
    /**
     * 记录消费失败
     * 
     * @param consumerName 消费者名称
     * @param eventId 事件ID
     * @param eventType 事件类型
     * @param tenantId 租户ID
     * @param idempotencyKey 幂等键
     * @param errorMsg 错误信息
     */
    @Transactional(rollbackFor = Exception.class)
    public void recordFailure(String consumerName, String eventId, String eventType, 
                              Long tenantId, String idempotencyKey, String errorMsg) {
        EventConsumeLogPO log = new EventConsumeLogPO();
        log.setConsumerName(consumerName);
        log.setEventId(eventId);
        log.setEventType(eventType);
        log.setTenantId(tenantId);
        log.setIdempotencyKey(idempotencyKey);
        log.setStatus("FAILED");
        log.setErrorMessage(errorMsg);
        log.setConsumedAt(LocalDateTime.now());
        log.setCreatedAt(LocalDateTime.now());
        log.setUpdatedAt(LocalDateTime.now());
        
        consumeLogMapper.insert(log);
        
        this.log.warn("Event consume failure recorded: consumer={}, eventId={}, eventType={}, error={}", 
                consumerName, eventId, eventType, errorMsg);
    }
    
    /**
     * 获取已消费的日志
     * 
     * @param consumerName 消费者名称
     * @param eventId 事件ID
     * @return 消费日志（如果存在）
     */
    public EventConsumeLogPO getConsumeLog(String consumerName, String eventId) {
        return consumeLogMapper.selectByConsumerAndEvent(consumerName, eventId);
    }
}
