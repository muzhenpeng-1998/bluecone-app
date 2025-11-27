package com.bluecone.app.core.log.pipeline;

import com.bluecone.app.core.log.ApiEvent;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 事件限流器：限制同一 IP 的错误事件并抵御重复洪峰。
 */
@Component
public class EventLimiter {

    private static final int ERROR_LIMIT_PER_SECOND = 5;
    private static final int REPEAT_ERROR_LIMIT = 20;

    private final ConcurrentHashMap<String, WindowCounter> errorBuckets = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, AtomicInteger> exceptionCounter = new ConcurrentHashMap<>();

    public boolean allow(ApiEvent event) {
        if (event == null) {
            return false;
        }
        if (event.getEventType() != ApiEvent.EventType.API_ERROR) {
            return true;
        }
        if (!checkIpWindow(event)) {
            return false;
        }
        return checkExceptionBurst(event);
    }

    private boolean checkIpWindow(ApiEvent event) {
        String ip = Objects.requireNonNullElse(event.getIp(), "unknown");
        long currentSecond = Instant.now().getEpochSecond();
        WindowCounter bucket = errorBuckets.compute(ip, (key, existing) -> {
            if (existing == null || existing.second != currentSecond) {
                return new WindowCounter(currentSecond, new AtomicInteger(0));
            }
            return existing;
        });
        int count = bucket.counter.incrementAndGet();
        return count <= ERROR_LIMIT_PER_SECOND;
    }

    private boolean checkExceptionBurst(ApiEvent event) {
        String digest = Objects.requireNonNullElse(event.getExceptionDigest(), "unknown");
        int count = exceptionCounter
                .computeIfAbsent(digest, key -> new AtomicInteger(0))
                .incrementAndGet();
        if (count > REPEAT_ERROR_LIMIT) {
            return false;
        }
        // 简单的过期机制：超出窗口后清理计数，避免内存累积
        if (count == REPEAT_ERROR_LIMIT) {
            exceptionCounter.remove(digest);
        }
        return true;
    }

    private record WindowCounter(long second, AtomicInteger counter) {
    }
}
