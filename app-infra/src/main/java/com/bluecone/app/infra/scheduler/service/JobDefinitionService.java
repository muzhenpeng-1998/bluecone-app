package com.bluecone.app.infra.scheduler.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import com.bluecone.app.infra.scheduler.annotation.BlueconeJob;
import com.bluecone.app.infra.scheduler.core.CronUtils;
import com.bluecone.app.infra.scheduler.entity.JobDefinitionEntity;
import com.bluecone.app.infra.scheduler.queue.SchedulerQueuePayload;
import com.bluecone.app.infra.scheduler.queue.SchedulerQueuePublisher;
import com.bluecone.app.infra.scheduler.repository.JobDefinitionRepository;
import com.bluecone.app.infra.scheduler.config.SchedulerProperties;
import com.bluecone.app.core.tenant.TenantContext;

/**
 * Job 定义服务：注册、启停、触发。
 */
@Service
public class JobDefinitionService {

    private final JobDefinitionRepository repository;
    private final SchedulerQueuePublisher publisher;
    private final SchedulerProperties properties;

    public JobDefinitionService(JobDefinitionRepository repository,
                                SchedulerQueuePublisher publisher,
                                SchedulerProperties properties) {
        this.repository = repository;
        this.publisher = publisher;
        this.properties = properties;
    }

    public JobDefinitionEntity registerIfNeeded(BlueconeJob annotation) {
        String tenantId = resolveTenant();
        JobDefinitionEntity existing = repository.findByCode(annotation.code());
        if (existing == null) {
            JobDefinitionEntity created = new JobDefinitionEntity();
            created.setCode(annotation.code());
            created.setName(annotation.name());
            created.setCronExpr(annotation.cron());
            created.setEnabled(annotation.enabled());
            created.setTimeoutSeconds(annotation.timeoutSeconds());
            created.setTenantId(tenantId);
            created.setCreatedAt(LocalDateTime.now());
            created.setUpdatedAt(LocalDateTime.now());
            created.setNextRunAt(CronUtils.nextTime(annotation.cron(), LocalDateTime.now()));
            repository.insert(created);
            return created;
        }
        JobDefinitionEntity merged = new JobDefinitionEntity();
        merged.setId(existing.getId());
        merged.setCode(existing.getCode());
        merged.setName(existing.getName() != null ? existing.getName() : annotation.name());
        merged.setCronExpr(existing.getCronExpr() != null ? existing.getCronExpr() : annotation.cron());
        merged.setEnabled(existing.getEnabled() != null ? existing.getEnabled() : annotation.enabled());
        merged.setTimeoutSeconds(existing.getTimeoutSeconds() != null ? existing.getTimeoutSeconds() : annotation.timeoutSeconds());
        merged.setTenantId(existing.getTenantId() != null ? existing.getTenantId() : tenantId);
        merged.setLastRunAt(existing.getLastRunAt());
        merged.setNextRunAt(existing.getNextRunAt() != null
                ? existing.getNextRunAt()
                : CronUtils.nextTime(merged.getCronExpr(), LocalDateTime.now()));
        merged.setCreatedAt(existing.getCreatedAt());
        merged.setUpdatedAt(existing.getUpdatedAt());
        return merged;
    }

    public List<JobDefinitionEntity> loadEnabledJobs() {
        List<JobDefinitionEntity> enabled = repository.listEnabled();
        LocalDateTime now = LocalDateTime.now();
        for (JobDefinitionEntity job : enabled) {
            if (job.getNextRunAt() == null) {
                LocalDateTime next = CronUtils.nextTime(job.getCronExpr(), now);
                repository.updateNextRun(job.getCode(), next);
                job.setNextRunAt(next);
            }
        }
        return enabled;
    }

    public List<JobDefinitionEntity> listAll() {
        return repository.listAll();
    }

    public void updateNextRun(String code, LocalDateTime next) {
        repository.updateNextRun(code, next);
    }

    public void enable(String code) {
        repository.updateEnabled(code, true);
    }

    public void disable(String code) {
        repository.updateEnabled(code, false);
    }

    public void triggerRunNow(String code) {
        Assert.hasText(code, "code must not be blank");
        String tenantId = resolveTenant();
        SchedulerQueuePayload payload = new SchedulerQueuePayload(code,
                UUID.randomUUID().toString().replace("-", ""),
                tenantId,
                LocalDateTime.now());
        publisher.publish(properties.getQueueKey(), payload);
    }

    private String resolveTenant() {
        String tenantId = TenantContext.getTenantId();
        return (tenantId == null || tenantId.isBlank()) ? "default" : tenantId;
    }
}
