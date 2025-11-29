package com.bluecone.app.infra.scheduler.dispatcher;

import java.time.LocalDateTime;
import java.time.Duration;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.bluecone.app.infra.redis.core.RedisKeyBuilder;
import com.bluecone.app.infra.redis.core.RedisKeyNamespace;
import com.bluecone.app.infra.redis.core.RedisOps;
import com.bluecone.app.infra.scheduler.config.SchedulerProperties;
import com.bluecone.app.infra.scheduler.core.CronUtils;
import com.bluecone.app.infra.scheduler.entity.JobDefinitionEntity;
import com.bluecone.app.infra.scheduler.queue.SchedulerQueuePayload;
import com.bluecone.app.infra.scheduler.queue.SchedulerQueuePublisher;
import com.bluecone.app.infra.scheduler.service.JobDefinitionService;

/**
 * 统一心跳：仅负责扫描到期任务并投递队列，不直接执行。
 */
public class SchedulerLoop {

    private static final Logger log = LoggerFactory.getLogger(SchedulerLoop.class);

    private final JobDefinitionService jobDefinitionService;
    private final SchedulerQueuePublisher publisher;
    private final SchedulerProperties properties;
    private final RedisOps redisOps;
    private final RedisKeyBuilder redisKeyBuilder;
    private final ScheduledExecutorService scheduler;
    private final String instanceId = UUID.randomUUID().toString();

    public SchedulerLoop(JobDefinitionService jobDefinitionService,
                         SchedulerQueuePublisher publisher,
                         SchedulerProperties properties,
                         RedisOps redisOps,
                         RedisKeyBuilder redisKeyBuilder) {
        this.jobDefinitionService = jobDefinitionService;
        this.publisher = publisher;
        this.properties = properties;
        this.redisOps = redisOps;
        this.redisKeyBuilder = redisKeyBuilder;
        this.scheduler = Executors.newSingleThreadScheduledExecutor(new LoopThreadFactory());
    }

    @PostConstruct
    public void start() {
        if (!properties.isEnabled()) {
            log.info("[Scheduler] disabled, scheduler loop not started");
            return;
        }
        scheduler.scheduleWithFixedDelay(this::beat,
                0,
                properties.getLoopIntervalSeconds(),
                TimeUnit.SECONDS);
        log.info("[Scheduler] loop started interval={}s", properties.getLoopIntervalSeconds());
    }

    @PreDestroy
    public void stop() {
        scheduler.shutdownNow();
    }

    private void beat() {
        LocalDateTime now = LocalDateTime.now();
        List<JobDefinitionEntity> enabled = jobDefinitionService.loadEnabledJobs();
        for (JobDefinitionEntity job : enabled) {
            if (!CronUtils.isDue(job.getNextRunAt(), now)) {
                continue;
            }
            if (!acquireLock(job.getCode())) {
                continue;
            }
            enqueue(job, now);
        }
    }

    private boolean acquireLock(String code) {
        String lockKey = redisKeyBuilder.buildForGlobal(RedisKeyNamespace.LOCK, "scheduler", code, "lock");
        Boolean locked = redisOps.setIfAbsent(lockKey, instanceId,
                Duration.ofSeconds(properties.getLoopIntervalSeconds() * 2L));
        return Boolean.TRUE.equals(locked);
    }

    private void enqueue(JobDefinitionEntity job, LocalDateTime now) {
        try {
            SchedulerQueuePayload payload = new SchedulerQueuePayload(
                    job.getCode(),
                    UUID.randomUUID().toString().replace("-", ""),
                    job.getTenantId(),
                    now);
            publisher.publish(properties.getQueueKey(), payload);
            LocalDateTime next = CronUtils.nextTime(job.getCronExpr(), now);
            jobDefinitionService.updateNextRun(job.getCode(), next);
        } catch (Exception ex) {
            log.error("[Scheduler] enqueue failed code={}", job.getCode(), ex);
        }
    }

    private static final class LoopThreadFactory implements ThreadFactory {
        @Override
        public Thread newThread(Runnable r) {
            Thread t = new Thread(r, "scheduler-loop");
            t.setDaemon(true);
            return t;
        }
    }
}
