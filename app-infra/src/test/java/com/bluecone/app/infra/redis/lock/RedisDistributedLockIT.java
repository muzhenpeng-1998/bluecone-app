package com.bluecone.app.infra.redis.lock;

import static org.assertj.core.api.Assertions.assertThat;

import com.bluecone.app.infra.redis.core.RedisOps;
import com.bluecone.app.infra.test.AbstractIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class RedisDistributedLockIT extends AbstractIntegrationTest {

    @Autowired
    private RedisDistributedLock distributedLock;

    @Autowired
    private RedisOps redisOps;

    @BeforeEach
    void setUp() {
        flushRedis();
    }

    @Test
    void lockIsExclusivePerBizKey() {
        boolean acquired = distributedLock.tryLock("inventory:sku-1", "worker-A", 0, 2000);
        assertThat(acquired).isTrue();

        boolean competing = distributedLock.tryLock("inventory:sku-1", "worker-B", 200, 2000);
        assertThat(competing).isFalse();

        distributedLock.unlock("inventory:sku-1", "worker-A");

        boolean second = distributedLock.tryLock("inventory:sku-1", "worker-B", 0, 2000);
        assertThat(second).isTrue();
    }
}
