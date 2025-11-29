// File: app-infra/src/main/java/com/bluecone/app/infra/outbox/core/OutboxEventPublisher.java
package com.bluecone.app.infra.outbox.core;

import com.bluecone.app.core.event.DomainEvent;
import com.bluecone.app.infra.outbox.pipeline.OutboxPipeline;
import com.bluecone.app.infra.outbox.pipeline.OutboxPublishContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;

/**
 * 强一致的事件发布器：事务内写入 Outbox，提交后由调度器异步分发。
 *
 * <p>业务只感知 {@link com.bluecone.app.infra.outbox.core.DomainEventPublisher#publish(DomainEvent)}，
 * Outbox 表、序列化、重试等细节全部下沉到管道步骤。</p>
 */
@Component
@Profile("legacy-outbox")
public class OutboxEventPublisher implements DomainEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(OutboxEventPublisher.class);

    private final List<OutboxPipeline> pipelines;

    public OutboxEventPublisher(final List<OutboxPipeline> pipelines) {
        this.pipelines = Objects.requireNonNullElseGet(pipelines, List::of);
    }

    @Override
    @Transactional
    public void publish(final DomainEvent event) {
        Objects.requireNonNull(event, "event must not be null");
        OutboxPublishContext context = new OutboxPublishContext(event);
        for (OutboxPipeline pipeline : pipelines) {
            pipeline.execute(context);
        }
        log.debug("[Outbox] publish finished eventType={} outboxId={}",
                event.getEventType(),
                context.getEntity() != null ? context.getEntity().getId() : null);
    }
}
