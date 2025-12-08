package com.bluecone.app.infra.redis.idempotent;

import static org.assertj.core.api.Assertions.assertThat;

import com.bluecone.app.infra.test.AbstractIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class RedisIdempotentExecutorIT extends AbstractIntegrationTest {

    @Autowired
    private RedisIdempotentExecutor executor;

    @BeforeEach
    void setUp() {
        flushRedis();
    }

    @Test
    void preventsDuplicateExecutionWithinTtl() {
        String bizKey = "checkout:order-1";
        assertThat(executor.tryEnter(bizKey, IdempotentScene.API, 5)).isTrue();
        assertThat(executor.tryEnter(bizKey, IdempotentScene.API, 5)).isFalse();

        executor.onSuccess(bizKey, IdempotentScene.API, 5);
    }
}
