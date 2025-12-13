package com.bluecone.app.core.event.consume.api;

import java.time.Duration;

/**
 * 事件消费选项，用于控制锁租约、等待策略与退避参数。
 */
public record ConsumeOptions(
        Duration lockTtl,
        boolean waitIfInProgress,
        Duration waitMax,
        int maxRetry,
        Duration baseBackoff,
        Duration maxBackoff
) {
}

