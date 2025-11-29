package com.bluecone.app.infra.scheduler.jobs;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.bluecone.app.infra.outbox.service.OutboxDispatchService;
import com.bluecone.app.infra.scheduler.annotation.BlueconeJob;
import com.bluecone.app.infra.scheduler.core.JobContext;
import com.bluecone.app.infra.scheduler.core.JobHandler;

/**
 * 示例：驱动 Outbox 投递。
 */
@Component
@BlueconeJob(code = "outbox_dispatch", name = "Outbox Dispatch", cron = "0/30 * * * * ?", timeoutSeconds = 30)
public class OutboxDispatchJob implements JobHandler {

    private static final Logger log = LoggerFactory.getLogger(OutboxDispatchJob.class);

    private final OutboxDispatchService dispatchService;

    public OutboxDispatchJob(OutboxDispatchService dispatchService) {
        this.dispatchService = dispatchService;
    }

    @Override
    public void handle(JobContext context) {
        log.info("[Scheduler] OutboxDispatch tick traceId={}", context.getTraceId());
        dispatchService.dispatchDueMessages();
    }
}
