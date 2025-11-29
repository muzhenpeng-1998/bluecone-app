// File: app-infra/src/main/java/com/bluecone/app/infra/outbox/scheduler/OutboxCleanerJob.java
package com.bluecone.app.infra.outbox.scheduler;

import com.bluecone.app.infra.outbox.config.OutboxProperties;
import com.bluecone.app.infra.outbox.entity.OutboxMessageStatus;
import com.bluecone.app.infra.outbox.repository.OutboxMessageRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 清理过期的 DONE / DEAD 消息，保持表规模可控。
 */
@Component
public class OutboxCleanerJob {

    private static final Logger log = LoggerFactory.getLogger(OutboxCleanerJob.class);

    private final OutboxMessageRepository repository;
    private final OutboxProperties properties;

    public OutboxCleanerJob(final OutboxMessageRepository repository,
                            final OutboxProperties properties) {
        this.repository = repository;
        this.properties = properties;
    }

    @Scheduled(cron = "${bluecone.outbox.clean-cron:0 0 3 * * ?}")
    public void clean() {
        if (!properties.isEnabled()) {
            return;
        }
        LocalDateTime before = LocalDateTime.now().minusDays(properties.getCleanRetentionDays());
        int deleted = repository.cleanOldMessages(List.of(OutboxMessageStatus.DONE, OutboxMessageStatus.DEAD), before, 500);
        if (deleted > 0) {
            log.info("[OutboxCleaner] deleted {} messages before {}", deleted, before);
        }
    }
}
