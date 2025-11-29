// File: app-infra/src/main/java/com/bluecone/app/infra/outbox/scheduler/OutboxPublisherJob.java
package com.bluecone.app.infra.outbox.scheduler;

import com.bluecone.app.infra.outbox.config.OutboxProperties;
import com.bluecone.app.infra.outbox.service.OutboxDispatchService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 定时扫描 Outbox，发布待投递事件。
 */
@Component
public class OutboxPublisherJob {

    private static final Logger log = LoggerFactory.getLogger(OutboxPublisherJob.class);

    private final OutboxDispatchService dispatchService;
    private final OutboxProperties properties;

    public OutboxPublisherJob(final OutboxDispatchService dispatchService,
                              final OutboxProperties properties) {
        this.dispatchService = dispatchService;
        this.properties = properties;
    }

    @Scheduled(cron = "${bluecone.outbox.publish-cron:0/10 * * * * ?}")
    public void publish() {
        if (!properties.isEnabled()) {
            return;
        }
        log.debug("[OutboxJob] start dispatching due messages");
        dispatchService.dispatchDueMessages();
    }
}
