package com.bluecone.app.infra.redis.core;

import static org.assertj.core.api.Assertions.assertThat;

import com.bluecone.app.infra.test.AbstractIntegrationTest;
import java.time.Duration;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class RedisOpsIT extends AbstractIntegrationTest {

    @Autowired
    private RedisOps redisOps;

    @BeforeEach
    void setUp() {
        flushRedis();
    }

    @Test
    void stringAndObjectOperationsRoundTrip() {
        redisOps.setString("session:token", "abc", Duration.ofSeconds(30));
        assertThat(redisOps.getString("session:token")).isEqualTo("abc");

        SamplePayload payload = new SamplePayload("order", 3);
        redisOps.setObject("object:1", payload, null);
        SamplePayload reloaded = redisOps.getObject("object:1", SamplePayload.class);
        assertThat(reloaded).isEqualTo(payload);
    }

    @Test
    void hashOperationsSupportTypedValues() {
        redisOps.hSet("hash:order:1", "status", "PAID");
        redisOps.hSet("hash:order:1", "amount", 199);

        assertThat(redisOps.hGet("hash:order:1", "status", String.class)).isEqualTo("PAID");
        assertThat(redisOps.hGetAll("hash:order:1", Object.class))
                .containsEntry("amount", 199)
                .containsEntry("status", "PAID");
    }

    @Test
    void counterOperations() {
        redisOps.setString("counter", "0", null);
        redisOps.incr("counter", 2);
        redisOps.decr("counter", 1);

        assertThat(redisOps.getString("counter")).isEqualTo("1");
    }

    private record SamplePayload(String type, int count) {
    }
}
