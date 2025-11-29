// File: app-infra/src/main/java/com/bluecone/app/infra/outbox/core/SimpleExponentialBackoffRetryPolicy.java
package com.bluecone.app.infra.outbox.core;

import com.bluecone.app.infra.outbox.config.OutboxProperties;

import java.time.Duration;
import java.util.Objects;

/**
 * 指数退避重试策略：delay = min(maxDelay, baseDelay * 2^(attempt-1))。
 */
public class SimpleExponentialBackoffRetryPolicy implements RetryPolicy {

    private final OutboxProperties properties;

    public SimpleExponentialBackoffRetryPolicy(final OutboxProperties properties) {
        this.properties = Objects.requireNonNull(properties, "properties must not be null");
    }

    @Override
    public Duration nextDelay(final int attemptCount) {
        long base = properties.getBaseDelaySeconds();
        long max = properties.getMaxDelaySeconds();
        double delay = base * Math.pow(2, Math.max(0, attemptCount - 1));
        long clamped = Math.min(max, (long) delay);
        return Duration.ofSeconds(clamped);
    }

    @Override
    public boolean shouldGiveUp(final int attemptCount, final Throwable lastError) {
        return attemptCount >= properties.getMaxRetryCount();
    }
}
