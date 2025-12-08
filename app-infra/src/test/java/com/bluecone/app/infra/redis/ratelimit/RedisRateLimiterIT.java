package com.bluecone.app.infra.redis.ratelimit;

import static org.assertj.core.api.Assertions.assertThat;

import com.bluecone.app.infra.test.AbstractIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class RedisRateLimiterIT extends AbstractIntegrationTest {

    @Autowired
    private RedisRateLimiter rateLimiter;

    @BeforeEach
    void setUp() {
        flushRedis();
    }

    @Test
    void deniesRequestsBeyondWindowCapacity() {
        String key = "api:/orders/put";
        int limit = 3;
        int window = 1;

        assertThat(rateLimiter.tryAcquire(key, limit, window)).isTrue();
        assertThat(rateLimiter.tryAcquire(key, limit, window)).isTrue();
        assertThat(rateLimiter.tryAcquire(key, limit, window)).isTrue();
        assertThat(rateLimiter.tryAcquire(key, limit, window)).isFalse();
    }
}
