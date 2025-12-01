package com.bluecone.app.infra.notify.timeline;

import com.bluecone.app.core.notify.NotificationPriority;
import com.bluecone.app.infra.notify.config.NotifyProperties;
import com.bluecone.app.infra.notify.model.NotificationIntent;
import com.bluecone.app.infra.notify.model.NotificationTask;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;

/**
 * Timeline 层默认实现：幂等 -> 限流 -> 聚合/丢弃。
 */
public class DefaultNotificationTimeline implements NotificationTimeline {

    private static final Logger log = LoggerFactory.getLogger(DefaultNotificationTimeline.class);

    private final NotifyRateLimiter rateLimiter;
    private final NotifyDedupService dedupService;
    private final NotificationDigestService digestService;
    private final NotifyProperties properties;

    public DefaultNotificationTimeline(NotifyRateLimiter rateLimiter,
                                       NotifyDedupService dedupService,
                                       NotificationDigestService digestService,
                                       NotifyProperties properties) {
        this.rateLimiter = Objects.requireNonNull(rateLimiter, "rateLimiter must not be null");
        this.dedupService = Objects.requireNonNull(dedupService, "dedupService must not be null");
        this.digestService = Objects.requireNonNull(digestService, "digestService must not be null");
        this.properties = Objects.requireNonNull(properties, "properties must not be null");
    }

    @Override
    public TimelineDecision decide(NotificationTask task, NotificationIntent intent) {
        // 1. 幂等
        boolean firstSeen = dedupService.tryMark(task.getIdempotentKey(), properties.getDefaultIdempotentMinutes());
        if (!firstSeen) {
            return new TimelineDecision.Drop("DUPLICATE");
        }

        // 2. 限流
        int max = task.getMaxPerMinute() == null ? properties.getDefaultMaxPerMinute() : task.getMaxPerMinute();
        boolean allowed = rateLimiter.tryConsume(task, max);
        if (!allowed) {
            if (task.getPriority() == NotificationPriority.HIGH) {
                log.warn("[NotifyTimeline] high priority over limit, fallback aggregate scenario={} channel={} tenant={}",
                        task.getScenarioCode(), task.getChannel(), task.getTenantId());
                return digestService.planAggregate(task);
            }
            return new TimelineDecision.Drop("RATE_LIMIT");
        }

        return new TimelineDecision.SendNow();
    }
}
