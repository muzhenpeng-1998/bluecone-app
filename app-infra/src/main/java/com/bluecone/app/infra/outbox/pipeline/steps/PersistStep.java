// File: app-infra/src/main/java/com/bluecone/app/infra/outbox/pipeline/steps/PersistStep.java
package com.bluecone.app.infra.outbox.pipeline.steps;

import com.bluecone.app.infra.outbox.pipeline.OutboxPipeline;
import com.bluecone.app.infra.outbox.pipeline.OutboxPublishContext;
import com.bluecone.app.infra.outbox.service.OutboxStoreService;
import org.springframework.stereotype.Component;

/**
 * 持久化步骤：将事件写入 Outbox 表（事务内）。
 */
@Component
public class PersistStep implements OutboxPipeline {

    private final OutboxStoreService storeService;

    public PersistStep(final OutboxStoreService storeService) {
        this.storeService = storeService;
    }

    @Override
    public void execute(final OutboxPublishContext context) {
        var entity = storeService.persist(context.getEvent(), context.getPayload(), context.getHeaders());
        context.setEntity(entity);
    }
}
