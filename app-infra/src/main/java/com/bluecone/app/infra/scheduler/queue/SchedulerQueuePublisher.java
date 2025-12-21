package com.bluecone.app.infra.scheduler.queue;

import java.time.Duration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import com.bluecone.app.infra.redis.core.RedisOps;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * 队列发布器：将待执行的任务投递到 Redis 列表。
 */
@Component
public class SchedulerQueuePublisher {

    private static final Logger log = LoggerFactory.getLogger(SchedulerQueuePublisher.class);

    private final RedisOps redisOps;
    private final ObjectMapper objectMapper;

    public SchedulerQueuePublisher(RedisOps redisOps, @Qualifier("redisObjectMapper") ObjectMapper objectMapper) {
        this.redisOps = redisOps;
        this.objectMapper = objectMapper;
    }

    public void publish(String queueKey, SchedulerQueuePayload payload) {
        try {
            String json = objectMapper.writeValueAsString(payload);
            redisOps.rPush(queueKey, json);
            redisOps.expire(queueKey, Duration.ofDays(7));
//            log.info("[Scheduler] queued job code={} traceId={}", payload.getCode(), payload.getTraceId());
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to publish job to queue: " + payload.getCode(), ex);
        }
    }
}
