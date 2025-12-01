package com.bluecone.app.infra.notify.timeline;

import java.time.Instant;

/**
 * Timeline 层决策模型（Send/Aggregate/Drop）。
 */
public sealed interface TimelineDecision permits TimelineDecision.SendNow, TimelineDecision.Aggregate, TimelineDecision.Drop {

    /**
     * 立即投递。
     */
    record SendNow() implements TimelineDecision {
    }

    /**
     * 聚合后再投递。
     *
     * @param bucketKey 聚合桶标识
     * @param sendAt    计划发送时间
     */
    record Aggregate(String bucketKey, Instant sendAt) implements TimelineDecision {
    }

    /**
     * 丢弃本次通知。
     *
     * @param reason 丢弃原因
     */
    record Drop(String reason) implements TimelineDecision {
    }
}
