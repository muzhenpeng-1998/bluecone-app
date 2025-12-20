package com.bluecone.app.infra.event.outbox;

import com.bluecone.app.core.event.DomainEvent;
import com.bluecone.app.core.event.DomainEventPublisher;
import com.bluecone.app.core.event.EventMetadata;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDateTime;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 事务内 Outbox 事件发布实现：将领域事件序列化后写入 bc_outbox_event，不做实际投递。
 * 
 * Note: 此实现为旧版本，仅在 dev/test 环境使用。生产环境使用新的 outbox.core 实现。
 */
@Slf4j 
@Component("legacyOutboxEventPublisher")
@Profile({"dev", "test"})
public class TransactionalOutboxEventPublisher implements DomainEventPublisher {

    private final OutboxEventRepository outboxEventRepository;
    private final ObjectMapper objectMapper;

    public TransactionalOutboxEventPublisher(OutboxEventRepository outboxEventRepository,
                                            @Qualifier("redisObjectMapper") ObjectMapper objectMapper) {
        this.outboxEventRepository = outboxEventRepository;
        this.objectMapper = objectMapper;
    }

    @Override
    @Transactional
    public void publish(DomainEvent event) {
        OutboxEventDO outbox = new OutboxEventDO();
        // tenantId 可从事件元数据中解析，当前若不存在则留空，后续可接入 ApiContext
        Long tenantId = event.getTenantId();
        outbox.setTenantId(tenantId);

        // 约定 aggregateType/aggregateId 从 metadata 中读取，找不到时回退到 eventType/eventId
        EventMetadata metadata = event.getMetadata();
        String aggregateType = metadata.get("aggregateType");
        if (aggregateType == null) {
            aggregateType = event.getEventType();
        }
        String aggregateId = metadata.get("aggregateId");
        if (aggregateId == null) {
            aggregateId = event.getEventId();
        }

        outbox.setAggregateType(aggregateType);
        outbox.setAggregateId(aggregateId);
        outbox.setEventType(event.getEventType());
        outbox.setStatus(0); // NEW
        outbox.setRetryCount(0);

        LocalDateTime now = LocalDateTime.now();
        outbox.setAvailableAt(now);
        outbox.setCreatedAt(now);
        outbox.setUpdatedAt(now);

        try {
            String body = objectMapper.writeValueAsString(event);
            outbox.setEventBody(body);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize domain event, type={}, id={}", event.getEventType(), event.getEventId(), e);
            // 保持简单：序列化失败直接让业务事务回滚，避免事件/数据不一致
            throw new IllegalStateException("Failed to serialize domain event " + event.getEventType(), e);
        }

        outboxEventRepository.save(outbox);
        log.debug("Outbox event persisted, type={}, id={}, aggregateType={}, aggregateId={}",
                event.getEventType(), event.getEventId(), aggregateType, aggregateId);
    }
}


