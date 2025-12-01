package com.bluecone.app.infra.notify.outbox;

import com.bluecone.app.core.event.DomainEventPublisher;
import com.bluecone.app.infra.notify.model.NotificationIntent;
import com.bluecone.app.infra.notify.model.NotificationPlan;
import com.bluecone.app.infra.notify.model.NotificationTask;
import com.bluecone.app.infra.notify.policy.NotificationPolicyEngine;
import com.bluecone.app.infra.notify.timeline.NotificationTimeline;
import com.bluecone.app.infra.notify.timeline.TimelineDecision;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;

/**
 * 通知 Outbox 发布器（Outbox 层）。
 *
 * <p>业务线程中只负责生成意图、评估策略、写入 Outbox，真正发送由 Outbox 调度异步执行。</p>
 */
public class NotifyOutboxPublisher {

    private static final Logger log = LoggerFactory.getLogger(NotifyOutboxPublisher.class);

    private final NotificationPolicyEngine policyEngine;
    private final NotificationTimeline notificationTimeline;
    private final DomainEventPublisher domainEventPublisher;

    public NotifyOutboxPublisher(NotificationPolicyEngine policyEngine,
                                 NotificationTimeline notificationTimeline,
                                 DomainEventPublisher domainEventPublisher) {
        this.policyEngine = Objects.requireNonNull(policyEngine, "policyEngine must not be null");
        this.notificationTimeline = Objects.requireNonNull(notificationTimeline, "notificationTimeline must not be null");
        this.domainEventPublisher = Objects.requireNonNull(domainEventPublisher, "domainEventPublisher must not be null");
    }

    public boolean publish(NotificationIntent intent) {
        NotificationPlan plan = policyEngine.evaluate(intent);
        if (plan.isEmpty()) {
            log.warn("[NotifyPublisher] plan empty scenario={} tenant={}", intent.getScenarioCode(), intent.getTenantId());
            return false;
        }
        boolean accepted = false;
        for (NotificationTask task : plan.getTasks()) {
            TimelineDecision decision = notificationTimeline.decide(task, intent);
            if (decision instanceof TimelineDecision.SendNow) {
                domainEventPublisher.publish(new NotifyOutboxEvent(intent, task));
                accepted = true;
            } else if (decision instanceof TimelineDecision.Aggregate aggregate) {
                log.info("[NotifyPublisher] aggregate bucket={} sendAt={} scenario={} channel={}",
                        aggregate.bucketKey(), aggregate.sendAt(), task.getScenarioCode(), task.getChannel());
                // TODO: 将聚合请求写入摘要存储，定时投递
                accepted = true;
            } else if (decision instanceof TimelineDecision.Drop drop) {
                log.info("[NotifyPublisher] drop reason={} scenario={} channel={}",
                        drop.reason(), task.getScenarioCode(), task.getChannel());
            }
        }
        return accepted;
    }
}
