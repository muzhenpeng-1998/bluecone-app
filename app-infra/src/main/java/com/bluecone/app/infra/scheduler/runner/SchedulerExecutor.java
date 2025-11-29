package com.bluecone.app.infra.scheduler.runner;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;

import com.bluecone.app.infra.scheduler.core.ExecutionPipeline;
import com.bluecone.app.infra.scheduler.core.ExecutionSandbox;
import com.bluecone.app.infra.scheduler.core.JobContext;
import com.bluecone.app.infra.scheduler.core.JobRegistry;
import com.bluecone.app.infra.scheduler.entity.JobDefinitionEntity;
import com.bluecone.app.infra.scheduler.queue.SchedulerQueuePayload;
import com.bluecone.app.infra.scheduler.service.JobExecutionService;

/**
 * 调度执行入口：包装 JobHandler 进入管线与沙箱。
 */
public class SchedulerExecutor {

    private static final Logger log = LoggerFactory.getLogger(SchedulerExecutor.class);

    private final JobRegistry registry;
    private final ExecutionPipeline pipeline;
    private final ExecutionSandbox sandbox;
    private final JobExecutionService executionService;

    public SchedulerExecutor(JobRegistry registry,
                             ExecutionPipeline pipeline,
                             ExecutionSandbox sandbox,
                             JobExecutionService executionService) {
        this.registry = registry;
        this.pipeline = pipeline;
        this.sandbox = sandbox;
        this.executionService = executionService;
    }

    public void execute(SchedulerQueuePayload payload) {
        registry.get(payload.getCode()).ifPresentOrElse(registered -> {
            JobDefinitionEntity definition = registered.getDefinition();
            String traceId = resolveTrace(payload);
            LocalDateTime scheduledAt = payload.getScheduledAt() == null ? LocalDateTime.now() : payload.getScheduledAt();
            JobContext context = new JobContext(
                    definition.getCode(),
                    definition.getName(),
                    definition.getCronExpr(),
                    definition.getTimeoutSeconds(),
                    payload.getTenantId(),
                    traceId,
                    scheduledAt,
                    buildAttributes(payload));
            context.markStarted(LocalDateTime.now());
            long start = System.currentTimeMillis();
            try {
                pipeline.execute(context, () -> {
                    try {
                        sandbox.run(context, () -> {
                            registered.getHandler().handle(context);
                            return null;
                        });
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                });
                long duration = System.currentTimeMillis() - start;
                executionService.onSuccess(definition, context, duration);
                log.info("[Scheduler] job success code={} traceId={} cost={}ms", definition.getCode(), traceId, duration);
            } catch (Exception ex) {
                long duration = System.currentTimeMillis() - start;
                executionService.onFailure(definition, context, duration, ex);
                log.error("[Scheduler] job failed code={} traceId={} cost={}ms", definition.getCode(), traceId, duration, ex);
            }
        }, () -> log.warn("[Scheduler] unknown job code={} dropped", payload.getCode()));
    }

    private String resolveTrace(SchedulerQueuePayload payload) {
        return StringUtils.hasText(payload.getTraceId())
                ? payload.getTraceId()
                : UUID.randomUUID().toString().replace("-", "");
    }

    private Map<String, Object> buildAttributes(SchedulerQueuePayload payload) {
        Map<String, Object> attributes = new HashMap<>();
        attributes.put("scheduledAt", payload.getScheduledAt());
        return attributes;
    }
}
