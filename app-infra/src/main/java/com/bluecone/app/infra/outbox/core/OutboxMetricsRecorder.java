// File: app-infra/src/main/java/com/bluecone/app/infra/outbox/core/OutboxMetricsRecorder.java
package com.bluecone.app.infra.outbox.core;

import com.bluecone.app.infra.outbox.entity.OutboxMessageEntity;

/**
 * Outbox 观测指标钩子，可对接日志/Micrometer/Tracing。
 */
public interface OutboxMetricsRecorder {

    void onCreated(OutboxMessageEntity message);

    void onPublishedSuccess(OutboxMessageEntity message);

    void onPublishedFailure(OutboxMessageEntity message, Throwable error);

    void onDeadLetter(OutboxMessageEntity message, Throwable error);
}
