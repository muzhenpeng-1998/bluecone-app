// File: app-infra/src/main/java/com/bluecone/app/infra/outbox/service/OutboxRetryService.java
package com.bluecone.app.infra.outbox.service;

import com.bluecone.app.infra.outbox.config.OutboxProperties;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * 负责计算重试时间与死亡判定。
 */
@Service
public class OutboxRetryService {

    private final OutboxProperties properties;

    public OutboxRetryService(final OutboxProperties properties) {
        this.properties = properties;
    }

    public LocalDateTime nextRetryTime(final int retryCount) {
        long initial = properties.getInitialBackoffMillis();
        double multiplier = properties.getBackoffMultiplier();
        long backoff = (long) (initial * Math.pow(multiplier, Math.max(0, retryCount)));
        return LocalDateTime.now().plusNanos(backoff * 1_000_000);
    }

    public boolean shouldDeadLetter(final int retryCount) {
        return retryCount >= properties.getMaxRetries();
    }
}
