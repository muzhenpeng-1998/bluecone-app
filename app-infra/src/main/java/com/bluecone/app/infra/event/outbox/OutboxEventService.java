package com.bluecone.app.infra.event.outbox;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.bluecone.app.core.event.outbox.OutboxEvent;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Outbox 事件服务
 * 提供事件的写入、查询、状态更新等功能
 */
@Slf4j
@Service
public class OutboxEventService {
    
    private final OutboxEventMapper outboxEventMapper;
    private final ObjectMapper objectMapper;

    public OutboxEventService(OutboxEventMapper outboxEventMapper,
                             @Qualifier("redisObjectMapper") ObjectMapper objectMapper) {
        this.outboxEventMapper = outboxEventMapper;
        this.objectMapper = objectMapper;
    }
    
    /**
     * 写入 Outbox 事件（同事务）
     * 
     * @param event Outbox 事件
     */
    @Transactional(rollbackFor = Exception.class)
    public void save(OutboxEvent event) {
        OutboxEventPO po = new OutboxEventPO();
        po.setTenantId(event.getTenantId());
        po.setStoreId(event.getStoreId());
        po.setAggregateType(event.getAggregateType().getCode());
        po.setAggregateId(event.getAggregateId());
        po.setEventType(event.getEventType().getCode());
        po.setEventId(event.getEventId());
        
        try {
            po.setEventPayload(objectMapper.writeValueAsString(event.getPayload()));
            po.setEventMetadata(objectMapper.writeValueAsString(event.getMetadata()));
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize event payload/metadata, eventId={}", event.getEventId(), e);
            throw new IllegalStateException("Failed to serialize event", e);
        }
        
        po.setStatus("NEW");
        po.setRetryCount(0);
        po.setMaxRetryCount(10);
        po.setNextRetryAt(event.getNextRetryAt() != null ? event.getNextRetryAt() : LocalDateTime.now());
        po.setCreatedAt(LocalDateTime.now());
        po.setUpdatedAt(LocalDateTime.now());
        
        outboxEventMapper.insert(po);
        
        log.info("Outbox event saved: eventId={}, eventType={}, aggregateType={}, aggregateId={}", 
                event.getEventId(), event.getEventType(), event.getAggregateType(), event.getAggregateId());
    }
    
    /**
     * 批量写入 Outbox 事件（同事务）
     * 
     * @param events Outbox 事件列表
     */
    @Transactional(rollbackFor = Exception.class)
    public void saveBatch(List<OutboxEvent> events) {
        if (events == null || events.isEmpty()) {
            return;
        }
        for (OutboxEvent event : events) {
            save(event);
        }
    }
    
    /**
     * 查询待投递的事件
     * 
     * @param limit 限制数量
     * @return 待投递事件列表
     */
    public List<OutboxEventPO> findPendingEvents(int limit) {
        return outboxEventMapper.selectPendingEvents(limit, LocalDateTime.now());
    }
    
    /**
     * 标记事件为已投递
     * 
     * @param eventId 事件ID
     */
    @Transactional(rollbackFor = Exception.class)
    public void markAsSent(String eventId) {
        LambdaQueryWrapper<OutboxEventPO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(OutboxEventPO::getEventId, eventId);
        
        OutboxEventPO po = outboxEventMapper.selectOne(wrapper);
        if (po == null) {
            log.warn("Outbox event not found: eventId={}", eventId);
            return;
        }
        
        po.setStatus("SENT");
        po.setSentAt(LocalDateTime.now());
        po.setUpdatedAt(LocalDateTime.now());
        
        outboxEventMapper.updateById(po);
        
        log.info("Outbox event marked as sent: eventId={}", eventId);
    }
    
    /**
     * 标记事件为失败（用于重试）
     * 
     * @param eventId 事件ID
     * @param errorMsg 错误信息
     */
    @Transactional(rollbackFor = Exception.class)
    public void markAsFailed(String eventId, String errorMsg) {
        LambdaQueryWrapper<OutboxEventPO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(OutboxEventPO::getEventId, eventId);
        
        OutboxEventPO po = outboxEventMapper.selectOne(wrapper);
        if (po == null) {
            log.warn("Outbox event not found: eventId={}", eventId);
            return;
        }
        
        int retryCount = po.getRetryCount() + 1;
        po.setRetryCount(retryCount);
        po.setLastError(errorMsg);
        
        // 指数退避：2^retryCount 秒
        int backoffSeconds = (int) Math.pow(2, retryCount);
        po.setNextRetryAt(LocalDateTime.now().plusSeconds(backoffSeconds));
        
        // 超过最大重试次数，标记为 DEAD
        if (retryCount >= po.getMaxRetryCount()) {
            po.setStatus("DEAD");
            log.warn("Outbox event marked as DEAD after {} retries: eventId={}, error={}", 
                    retryCount, eventId, errorMsg);
        } else {
            po.setStatus("FAILED");
            log.warn("Outbox event marked as FAILED (retry {}/{}): eventId={}, nextRetryAt={}, error={}", 
                    retryCount, po.getMaxRetryCount(), eventId, po.getNextRetryAt(), errorMsg);
        }
        
        po.setUpdatedAt(LocalDateTime.now());
        outboxEventMapper.updateById(po);
    }
    
    /**
     * 查询指定聚合根的所有事件
     * 
     * @param aggregateType 聚合根类型
     * @param aggregateId 聚合根ID
     * @return 事件列表
     */
    public List<OutboxEventPO> findByAggregate(String aggregateType, String aggregateId) {
        return outboxEventMapper.selectByAggregate(aggregateType, aggregateId);
    }
}
