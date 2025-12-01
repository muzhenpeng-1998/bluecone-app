package com.bluecone.app.core.notify;

/**
 * 通知优先级（API 层）。
 *
 * <p>用于在 Timeline 层做限流/降级决策，高优先级可突破部分限流。</p>
 */
public enum NotificationPriority {

    HIGH,

    NORMAL,

    LOW
}
