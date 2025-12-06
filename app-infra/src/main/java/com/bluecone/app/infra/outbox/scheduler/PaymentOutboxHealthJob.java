package com.bluecone.app.infra.outbox.scheduler;

import com.bluecone.app.infra.notify.AlertService;
import com.bluecone.app.infra.outbox.entity.OutboxMessageStatus;
import com.bluecone.app.infra.outbox.repository.OutboxMessageRepository;
import com.bluecone.app.infra.scheduler.annotation.BlueconeJob;
import com.bluecone.app.infra.scheduler.core.JobContext;
import com.bluecone.app.infra.scheduler.core.JobHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * 监控支付相关 Outbox 堆积与失败率。
 */
@Component
@BlueconeJob(code = "payment_outbox_health", name = "Payment Outbox Health", cron = "0 */2 * * * ?", timeoutSeconds = 30)
public class PaymentOutboxHealthJob implements JobHandler {

    private static final Logger log = LoggerFactory.getLogger(PaymentOutboxHealthJob.class);

    private final OutboxMessageRepository outboxMessageRepository;
    private final AlertService alertService;

    public PaymentOutboxHealthJob(final OutboxMessageRepository outboxMessageRepository,
                                  final AlertService alertService) {
        this.outboxMessageRepository = outboxMessageRepository;
        this.alertService = alertService;
    }

    @Override
    public void handle(JobContext context) {
        LocalDateTime fiveMinutesAgo = LocalDateTime.now().minusMinutes(5);

        long pending = outboxMessageRepository.countByStatusAndPrefix(OutboxMessageStatus.NEW, "payment.", fiveMinutesAgo);
        long failed = outboxMessageRepository.countByStatusAndPrefix(OutboxMessageStatus.FAILED, "payment.", null);

        if (pending > 100) {
            String title = "Outbox 堆积告警";
            String content = String.format("payment.* NEW>5min 堆积数量=%d traceId=%s", pending, context.getTraceId());
            alertService.sendPaymentAlarm(title, content);
            log.warn("[outbox-health] pending payment events backlog={}", pending);
        }
        if (failed > 20) {
            String title = "Outbox 投递失败告警";
            String content = String.format("payment.* FAILED 数量=%d traceId=%s", failed, context.getTraceId());
            alertService.sendPaymentAlarm(title, content);
            log.warn("[outbox-health] failed payment events count={}", failed);
        }
        log.info("[outbox-health] payment backlog pending={} failed={}", pending, failed);
    }
}
