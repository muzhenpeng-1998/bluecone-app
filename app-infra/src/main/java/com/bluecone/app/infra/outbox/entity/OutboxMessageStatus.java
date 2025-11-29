// File: app-infra/src/main/java/com/bluecone/app/infra/outbox/entity/OutboxMessageStatus.java
package com.bluecone.app.infra.outbox.entity;

/**
 * Outbox 消息状态机。
 */
public enum OutboxMessageStatus {
    NEW,
    PUBLISHED,
    DONE,
    FAILED,
    DEAD
}
