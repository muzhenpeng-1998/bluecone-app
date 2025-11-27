package com.bluecone.app.core.log.error;

import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 异常限流器：防止相同异常在短时间内淹没日志。
 */
@Component
public class ExceptionLimiter {

    private static final int WINDOW_SECONDS = 10;
    private static final int MAX_PER_WINDOW = 100;
    private static final int MAX_SAME_ROOT_CAUSE = 500;

    private final ConcurrentHashMap<String, WindowCounter> ipBuckets = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, AtomicInteger> rootCauseCounter = new ConcurrentHashMap<>();

    public boolean allow(ExceptionEvent event) {
        if (event == null) {
            return false;
        }
        if (!ipWindowAllow(event)) {
            return false;
        }
        return rootCauseAllow(event);
    }

    private boolean ipWindowAllow(ExceptionEvent event) {
        String ip = Objects.requireNonNullElse(event.getClientIp(), "unknown");
        String errorCode = Objects.requireNonNullElse(event.getErrorCode(), "UNKNOWN");
        String key = ip + "|" + errorCode;
        long slot = Instant.now().getEpochSecond() / WINDOW_SECONDS;
        WindowCounter counter = ipBuckets.compute(key, (k, existing) -> {
            if (existing == null || existing.window != slot) {
                return new WindowCounter(slot, new AtomicInteger(0));
            }
            return existing;
        });
        int count = counter.counter.incrementAndGet();
        if (count > MAX_PER_WINDOW) {
            return false;
        }
        if (count == MAX_PER_WINDOW) {
            ipBuckets.remove(key);
        }
        return true;
    }

    private boolean rootCauseAllow(ExceptionEvent event) {
        String digest = Objects.requireNonNullElse(event.getExceptionType(), "unknown")
                + "|" + Objects.requireNonNullElse(event.getRootCause(), "unknown");
        int count = rootCauseCounter
                .computeIfAbsent(digest, k -> new AtomicInteger(0))
                .incrementAndGet();
        if (count > MAX_SAME_ROOT_CAUSE) {
            return false;
        }
        if (count == MAX_SAME_ROOT_CAUSE) {
            rootCauseCounter.remove(digest);
        }
        return true;
    }

    private record WindowCounter(long window, AtomicInteger counter) {
    }
}
