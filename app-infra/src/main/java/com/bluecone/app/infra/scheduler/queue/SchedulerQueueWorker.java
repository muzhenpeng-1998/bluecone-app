package com.bluecone.app.infra.scheduler.queue;

import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;

import com.bluecone.app.infra.redis.core.RedisOps;
import com.bluecone.app.infra.scheduler.config.SchedulerProperties;
import com.bluecone.app.infra.scheduler.runner.SchedulerExecutor;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * 队列消费者，使用 BRPOP 阻塞获取待执行任务，并提交到执行管线。
 */
public class SchedulerQueueWorker {

    private static final Logger log = LoggerFactory.getLogger(SchedulerQueueWorker.class);

    private final RedisOps redisOps;
    private final SchedulerExecutor executor;
    private final SchedulerProperties properties;
    private final ObjectMapper objectMapper;
    private ExecutorService consumerPool;

    public SchedulerQueueWorker(RedisOps redisOps,
                                SchedulerExecutor executor,
                                SchedulerProperties properties,
                                ObjectMapper objectMapper) {
        this.redisOps = redisOps;
        this.executor = executor;
        this.properties = properties;
        this.objectMapper = objectMapper;
    }

    @PostConstruct
    public void start() {
        if (!properties.isEnabled()) {
            log.info("[Scheduler] disabled, queue worker not started");
            return;
        }
        consumerPool = Executors.newFixedThreadPool(properties.getWorkerThreads(), new WorkerThreadFactory());
        for (int i = 0; i < properties.getWorkerThreads(); i++) {
            consumerPool.submit(this::pollLoop);
        }
        log.info("[Scheduler] queue worker started threads={}", properties.getWorkerThreads());
    }

    @PreDestroy
    public void stop() {
        if (consumerPool != null) {
            consumerPool.shutdownNow();
        }
    }

    private void pollLoop() {
        while (!Thread.currentThread().isInterrupted()) {
            try {
                String message = redisOps.brPop(properties.getQueueKey(), Duration.ofSeconds(5), String.class);
                if (!StringUtils.hasText(message)) {
                    continue;
                }
                SchedulerQueuePayload payload = objectMapper.readValue(message, SchedulerQueuePayload.class);
                if (!StringUtils.hasText(payload.getCode())) {
                    continue;
                }
                ensureTrace(payload);
                executor.execute(payload);
            } catch (Exception ex) {
                log.error("[Scheduler] queue worker error", ex);
            }
        }
    }

    private void ensureTrace(SchedulerQueuePayload payload) {
        if (!StringUtils.hasText(payload.getTraceId())) {
            payload.setTraceId(UUID.randomUUID().toString().replace("-", ""));
        }
    }

    private static final class WorkerThreadFactory implements ThreadFactory {
        private int counter = 0;

        @Override
        public Thread newThread(Runnable r) {
            Thread t = new Thread(r, "scheduler-worker-" + counter++);
            t.setDaemon(true);
            return t;
        }
    }
}
