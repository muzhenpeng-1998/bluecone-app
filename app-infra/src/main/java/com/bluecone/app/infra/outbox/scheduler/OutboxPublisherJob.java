// File: app-infra/src/main/java/com/bluecone/app/infra/outbox/scheduler/OutboxPublisherJob.java
package com.bluecone.app.infra.outbox.scheduler;

import com.bluecone.app.infra.observability.metrics.JobMetrics;
import com.bluecone.app.infra.outbox.config.OutboxProperties;
import com.bluecone.app.infra.outbox.service.OutboxDispatchService;
import io.micrometer.core.instrument.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 定时扫描 Outbox，发布待投递事件。
 */
@Component("legacyOutboxPublisherJob")
public class OutboxPublisherJob {

    private static final Logger log = LoggerFactory.getLogger(OutboxPublisherJob.class);
    private static final String JOB_NAME = "outbox_publisher";

    private final OutboxDispatchService dispatchService;
    private final OutboxProperties properties;
    private final JobMetrics jobMetrics;

    public OutboxPublisherJob(final OutboxDispatchService dispatchService,
                              final OutboxProperties properties,
                              final JobMetrics jobMetrics) {
        this.dispatchService = dispatchService;
        this.properties = properties;
        this.jobMetrics = jobMetrics;
    }

    @Scheduled(cron = "${bluecone.outbox.publish-cron:0/10 * * * * ?}")
    public void publish() {
        if (!properties.isEnabled()) {
            return;
        }
        
        Timer.Sample sample = jobMetrics.startExecutionTimer();
        try {
            log.debug("[OutboxJob] start dispatching due messages");
            dispatchService.dispatchDueMessages();
            jobMetrics.recordExecutionSuccess(JOB_NAME);
        } catch (Exception e) {
            jobMetrics.recordExecutionFailure(JOB_NAME);
            log.error("[OutboxJob] failed to dispatch messages", e);
            throw e;
        } finally {
            jobMetrics.stopExecutionTimer(sample, JOB_NAME);
        }
    }
}
