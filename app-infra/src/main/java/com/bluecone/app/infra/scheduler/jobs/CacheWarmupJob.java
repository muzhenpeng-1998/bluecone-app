package com.bluecone.app.infra.scheduler.jobs;

import java.time.Duration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.bluecone.app.infra.redis.core.RedisKeyBuilder;
import com.bluecone.app.infra.redis.core.RedisKeyNamespace;
import com.bluecone.app.infra.redis.core.RedisOps;
import com.bluecone.app.infra.scheduler.annotation.BlueconeJob;
import com.bluecone.app.infra.scheduler.core.JobContext;
import com.bluecone.app.infra.scheduler.core.JobHandler;

/**
 * 示例：基础缓存预热。
 */
@Component
@BlueconeJob(code = "cache_warmup", name = "Cache Warmup", cron = "0 0/5 * * * ?", timeoutSeconds = 60)
public class CacheWarmupJob implements JobHandler {

    private static final Logger log = LoggerFactory.getLogger(CacheWarmupJob.class);

    private final RedisOps redisOps;
    private final RedisKeyBuilder redisKeyBuilder;

    public CacheWarmupJob(RedisOps redisOps, RedisKeyBuilder redisKeyBuilder) {
        this.redisOps = redisOps;
        this.redisKeyBuilder = redisKeyBuilder;
    }

    @Override
    public void handle(JobContext context) {
        String key = redisKeyBuilder.buildForGlobal(RedisKeyNamespace.CACHE, "scheduler", "warmup");
        redisOps.setString(key, "ok", Duration.ofMinutes(10));
//        log.info("[Scheduler] cache warmup heartbeat traceId={}", context.getTraceId());
    }
}
