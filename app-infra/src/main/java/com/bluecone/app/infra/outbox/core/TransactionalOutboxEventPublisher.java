// File: app-infra/src/main/java/com/bluecone/app/infra/outbox/core/TransactionalOutboxEventPublisher.java
package com.bluecone.app.infra.outbox.core;

import com.bluecone.app.core.event.DomainEvent;
import com.bluecone.app.core.event.DomainEventPublisher;
import com.bluecone.app.infra.outbox.entity.OutboxMessageEntity;
import com.bluecone.app.infra.outbox.service.OutboxStoreService;
import com.bluecone.app.core.tenant.TenantContext;
import com.bluecone.app.infra.outbox.core.DefaultEventSerializer;
import com.bluecone.app.infra.outbox.core.EventSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.Objects;

/**
 * 事务内 Outbox 发布器：将 DomainEvent 与业务更新同一事务提交到 Outbox 表。
 *
+ * <p>业务侧只依赖 {@link DomainEventPublisher}，无需感知 Outbox/重试/投递细节。
 * 数据提交后由调度器异步分发，实现高可靠事件发布。</p>
 */
@Component
@Primary
@Profile({"!dev & !test"})
public class TransactionalOutboxEventPublisher implements DomainEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(TransactionalOutboxEventPublisher.class);

    private final OutboxStoreService outboxStoreService;
    private final EventSerializer eventSerializer;

    public TransactionalOutboxEventPublisher(final OutboxStoreService outboxStoreService,
                                             final EventSerializer eventSerializer) {
        this.outboxStoreService = outboxStoreService;
        this.eventSerializer = eventSerializer;
    }

    @Override
    @Transactional
    public void publish(final DomainEvent event) {
        Objects.requireNonNull(event, "event must not be null");
        Map<String, String> headers = buildHeaders(event);
        String payload = eventSerializer.serializePayload(event);
        headers.putIfAbsent(DefaultEventSerializer.HEADER_EVENT_CLASS, event.getClass().getName());
        headers.putIfAbsent(DefaultEventSerializer.HEADER_EVENT_TYPE, event.getEventType());
        headers.putIfAbsent("eventKey", event.getEventType() + ":" + event.getEventId());
        OutboxMessageEntity entity = outboxStoreService.persist(event, payload, headers);
        log.info("[OutboxTx] staged eventType={} eventId={} eventKey={} tenantId={}",
                event.getEventType(), event.getEventId(), entity.getEventKey(), entity.getTenantId());
    }

    private Map<String, String> buildHeaders(final DomainEvent event) {
        Map<String, String> headers = eventSerializer.serializeHeaders(event);
        headers.putIfAbsent("traceId", MDC.get("traceId"));
        if (TenantContext.getTenantId() != null) {
            headers.putIfAbsent("tenantId", TenantContext.getTenantId());
        }
        headers.putIfAbsent("eventId", event.getEventId());
        return headers;
    }
}
