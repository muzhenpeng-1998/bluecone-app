// File: app-infra/src/main/java/com/bluecone/app/infra/outbox/service/OutboxStoreService.java
package com.bluecone.app.infra.outbox.service;

import com.bluecone.app.core.event.DomainEvent;
import com.bluecone.app.infra.outbox.entity.OutboxMessageEntity;
import com.bluecone.app.infra.outbox.entity.OutboxMessageStatus;
import com.bluecone.app.infra.outbox.repository.OutboxMessageRepository;
import com.bluecone.app.infra.outbox.core.OutboxMetricsRecorder;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Objects;

/**
 * 负责将领域事件持久化到 Outbox 表（事务内）。
 */
@Service
public class OutboxStoreService {

    private static final Logger log = LoggerFactory.getLogger(OutboxStoreService.class);

    private final OutboxMessageRepository repository;
    private final ObjectMapper objectMapper;
    private final OutboxMetricsRecorder metricsRecorder;

    public OutboxStoreService(final OutboxMessageRepository repository,
                              final ObjectMapper objectMapper,
                              final OutboxMetricsRecorder metricsRecorder) {
        this.repository = repository;
        this.objectMapper = objectMapper;
        this.metricsRecorder = metricsRecorder;
    }

    public OutboxMessageEntity persist(final DomainEvent event,
                                       final String payload,
                                       final Map<String, String> headers) {
        Objects.requireNonNull(event, "event must not be null");
        OutboxMessageEntity entity = new OutboxMessageEntity();
        entity.setEventType(event.getEventType());
        String eventKey = headers.getOrDefault("eventKey", event.getEventId());
        entity.setEventKey(eventKey);
        entity.setPayload(payload);
        try {
            entity.setHeaders(objectMapper.writeValueAsString(headers));
        } catch (Exception e) {
            throw new IllegalStateException("Failed to serialize outbox headers", e);
        }
        String tenantId = headers.get("tenantId");
        if (tenantId != null) {
            entity.setTenantId(Long.parseLong(tenantId));
        }
        entity.setStatus(OutboxMessageStatus.NEW);
        entity.setRetryCount(0);
        entity.setNextRetryAt(LocalDateTime.now());
        repository.save(entity);
        log.info("[Outbox] persisted eventType={} eventKey={} outboxId={}", event.getEventType(), eventKey, entity.getId());
        metricsRecorder.onCreated(entity);
        return entity;
    }
}
