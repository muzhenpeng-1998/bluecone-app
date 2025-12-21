package com.bluecone.app.payment.scheduler;

import com.bluecone.app.infra.notify.AlertService;
import com.bluecone.app.infra.scheduler.annotation.BlueconeJob;
import com.bluecone.app.infra.scheduler.core.JobContext;
import com.bluecone.app.infra.scheduler.core.JobHandler;
import com.bluecone.app.payment.domain.enums.PaymentStatus;
import com.bluecone.app.payment.domain.repository.PaymentOrderRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 支付链路健康巡检 Job：
 * <p>
 * - 每 5 分钟统计最近 10 分钟的支付创建/成功量，粗略计算失败率；<br>
 * - 统计超过 30 分钟仍停留在 PENDING/INIT 的卡单数量；<br>
 * - 触发阈值时通过 AlertService 发送告警（降级日志）。</p>
 */
@Component
@BlueconeJob(code = "payment_health_check", name = "Payment Health Check", cron = "0 */5 * * * ?", timeoutSeconds = 30)
public class PaymentHealthCheckJob implements JobHandler {

    private static final Logger log = LoggerFactory.getLogger(PaymentHealthCheckJob.class);

    private final PaymentOrderRepository paymentOrderRepository;
    private final AlertService alertService;

    public PaymentHealthCheckJob(final PaymentOrderRepository paymentOrderRepository,
                                 final AlertService alertService) {
        this.paymentOrderRepository = paymentOrderRepository;
        this.alertService = alertService;
    }

    @Override
    public void handle(JobContext context) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime tenMinutesAgo = now.minusMinutes(10);
        LocalDateTime stuckBefore = now.minusMinutes(30);

        long created = paymentOrderRepository.countCreatedBetween(tenMinutesAgo, now);
        long succeeded = paymentOrderRepository.countSucceededBetween(tenMinutesAgo, now);
        long failed = created - succeeded;
        double failRate = created > 0 ? (double) failed / created : 0.0d;

        if (created >= 50 && failRate > 0.05d) {
            String title = "支付创建/回调失败率过高";
            String content = String.format("10分钟内创建=%d 成功=%d 失败=%d 失败率=%.2f%% traceId=%s",
                    created, succeeded, failed, failRate * 100, context.getTraceId());
            alertService.sendPaymentAlarm(title, content);
            log.warn("[payment-health] high failure rate created={} succeeded={} failed={} rate={}", created, succeeded, failed, failRate);
        } else {
//            log.info("[payment-health] window10m created={} succeeded={} failed={} rate={}", created, succeeded, failed, failRate);
        }

        long stuck = paymentOrderRepository.countStuckPayments(stuckBefore, List.of(PaymentStatus.PENDING, PaymentStatus.INIT));
        if (stuck > 20) {
            String title = "支付卡单告警";
            String content = String.format("超过30分钟仍未完成的支付单数量=%d traceId=%s", stuck, context.getTraceId());
            alertService.sendPaymentAlarm(title, content);
            log.warn("[payment-health] stuck payments detected count={}", stuck);
        }
    }
}
