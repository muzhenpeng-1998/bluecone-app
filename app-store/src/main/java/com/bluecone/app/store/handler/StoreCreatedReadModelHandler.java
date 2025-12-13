package com.bluecone.app.store.handler;

import com.bluecone.app.core.event.EventHandler;
import com.bluecone.app.core.event.annotations.EventHandlerComponent;
import com.bluecone.app.core.event.consume.api.ConsumeOptions;
import com.bluecone.app.core.event.consume.api.EventEnvelope;
import com.bluecone.app.core.event.consume.api.EventHandlerTemplate;
import com.bluecone.app.id.core.Ulid128;
import com.bluecone.app.store.dao.entity.BcStoreReadModel;
import com.bluecone.app.store.dao.mapper.BcStoreReadModelMapper;
import com.bluecone.app.store.event.StoreCreatedEvent;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 门店创建事件消费端示例：将 STORE_CREATED 事件写入只读快照表。
 *
 * <p>通过 {@link EventHandlerTemplate} 实现消费幂等，保证同一事件只处理一次。</p>
 */
@EventHandlerComponent
public class StoreCreatedReadModelHandler implements EventHandler<StoreCreatedEvent> {

    private static final Logger log = LoggerFactory.getLogger(StoreCreatedReadModelHandler.class);

    private static final String CONSUMER_GROUP = "STORE_READMODEL";

    private final EventHandlerTemplate handlerTemplate;
    private final BcStoreReadModelMapper readModelMapper;
    private final ObjectMapper objectMapper;

    public StoreCreatedReadModelHandler(EventHandlerTemplate handlerTemplate,
                                        BcStoreReadModelMapper readModelMapper,
                                        ObjectMapper objectMapper) {
        this.handlerTemplate = handlerTemplate;
        this.readModelMapper = readModelMapper;
        this.objectMapper = objectMapper;
    }

    @Override
    public void handle(final StoreCreatedEvent event) {
        Ulid128 eventUlid = toUlid128(event.getEventId());
        long tenantId = event.getTenantId() != null ? event.getTenantId() : 0L;

        EventEnvelope envelope = new EventEnvelope(
                tenantId,
                eventUlid,
                event.getEventType(),
                serializePayload(event),
                null,
                event.getOccurredAt()
        );

        ConsumeOptions options = new ConsumeOptions(
                Duration.ofSeconds(30),
                false,
                Duration.ZERO,
                20,
                Duration.ofSeconds(1),
                Duration.ofMinutes(5)
        );

        com.bluecone.app.core.event.consume.api.EventHandler readModelHandler = env -> {
            BcStoreReadModel model = new BcStoreReadModel();
            model.setStoreInternalId(event.getStoreInternalId());
            model.setPublicId(event.getStorePublicId());
            model.setStoreNo(event.getStoreNo());
            model.setTenantId(event.getTenantId());
            model.setStoreName(event.getStoreName());
            model.setUpdatedAt(LocalDateTime.ofInstant(event.getOccurredAt(), ZoneOffset.UTC));
            readModelMapper.insert(model);
            log.info("[StoreCreatedReadModel] tenantId={} storeInternalId={} publicId={} storeNo={}",
                    event.getTenantId(),
                    event.getStoreInternalId(),
                    event.getStorePublicId(),
                    event.getStoreNo());
        };

        handlerTemplate.consume(CONSUMER_GROUP, envelope, readModelHandler, options);
    }

    private Ulid128 toUlid128(String eventId) {
        try {
            UUID uuid = UUID.fromString(eventId);
            return new Ulid128(uuid.getMostSignificantBits(), uuid.getLeastSignificantBits());
        } catch (IllegalArgumentException ex) {
            log.warn("Invalid eventId format for StoreCreatedEvent, fallback to zero ULID, eventId={}", eventId);
            return new Ulid128(0L, 0L);
        }
    }

    private String serializePayload(StoreCreatedEvent event) {
        try {
            return objectMapper.writeValueAsString(event);
        } catch (JsonProcessingException e) {
            log.warn("Failed to serialize StoreCreatedEvent payload, eventId={}", event.getEventId(), e);
            return null;
        }
    }
}

