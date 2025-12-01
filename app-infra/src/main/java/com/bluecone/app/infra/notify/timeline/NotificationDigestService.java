package com.bluecone.app.infra.notify.timeline;

import com.bluecone.app.infra.notify.model.NotificationTask;

import java.time.Instant;

/**
 * 聚合摘要服务（预留）。
 *
 * <p>当前返回固定 60s 后的聚合计划，未来可按业务维度计算 bucket。</p>
 */
public class NotificationDigestService {

    public TimelineDecision.Aggregate planAggregate(NotificationTask task) {
        String bucketKey = task.getScenarioCode() + ":" + task.getChannel().getCode();
        Instant sendAt = Instant.now().plusSeconds(60);
        return new TimelineDecision.Aggregate(bucketKey, sendAt);
    }
}
