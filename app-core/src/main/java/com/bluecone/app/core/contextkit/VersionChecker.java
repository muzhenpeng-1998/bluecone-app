package com.bluecone.app.core.contextkit;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 版本校验控制器：基于时间窗口与采样率决定是否触发轻量版本查询。
 */
public class VersionChecker {

    private final Duration window;
    private final double sampleRate;
    private final Map<CacheKey, Instant> lastChecked = new ConcurrentHashMap<>();

    public VersionChecker(Duration window, double sampleRate) {
        this.window = window != null ? window : Duration.ZERO;
        this.sampleRate = sampleRate;
    }

    public boolean shouldCheck(CacheKey key) {
        if (window.isZero() || sampleRate <= 0.0d) {
            return false;
        }
        Instant now = Instant.now();
        Instant last = lastChecked.get(key);
        if (last != null && now.isBefore(last.plus(window))) {
            return false;
        }
        return ThreadLocalRandom.current().nextDouble() < sampleRate;
    }

    public void markChecked(CacheKey key) {
        lastChecked.put(key, Instant.now());
    }
}

