// File: app-infra/src/main/java/com/bluecone/app/infra/outbox/entity/OutboxMessageStatus.java
package com.bluecone.app.infra.outbox.entity;

/**
 * Outbox 消息状态机。
 */
/**
 * Simplified Outbox state machine: no intermediate persisted "processing" state to avoid hang.
 */
public enum OutboxMessageStatus {
    NEW,     // ready to be dispatched
    FAILED,  // failed but still retryable
    DONE,    // successfully processed
    DEAD     // permanently failed, no further retries
}
