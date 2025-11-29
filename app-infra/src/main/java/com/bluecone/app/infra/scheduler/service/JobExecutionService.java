package com.bluecone.app.infra.scheduler.service;

import java.time.LocalDateTime;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.bluecone.app.infra.scheduler.core.CronUtils;
import com.bluecone.app.infra.scheduler.core.JobContext;
import com.bluecone.app.infra.scheduler.entity.JobDefinitionEntity;
import com.bluecone.app.infra.scheduler.entity.JobExecutionLogEntity;
import com.bluecone.app.infra.scheduler.repository.JobDefinitionRepository;
import com.bluecone.app.infra.scheduler.repository.JobExecutionLogRepository;

/**
 * 执行结果落库及调度时间推进。
 */
@Service
public class JobExecutionService {

    private static final Logger log = LoggerFactory.getLogger(JobExecutionService.class);

    private final JobExecutionLogRepository logRepository;
    private final JobDefinitionRepository definitionRepository;

    public JobExecutionService(JobExecutionLogRepository logRepository,
                               JobDefinitionRepository definitionRepository) {
        this.logRepository = logRepository;
        this.definitionRepository = definitionRepository;
    }

    public void onSuccess(JobDefinitionEntity definition, JobContext context, long durationMs) {
        persistLog(definition, context, durationMs, "SUCCESS", null);
        pushClock(definition, context);
    }

    public void onFailure(JobDefinitionEntity definition, JobContext context, long durationMs, Exception ex) {
        persistLog(definition, context, durationMs, "FAILED", ex.getMessage());
        pushClock(definition, context);
    }

    private void persistLog(JobDefinitionEntity definition,
                            JobContext context,
                            long durationMs,
                            String status,
                            String error) {
        JobExecutionLogEntity entity = new JobExecutionLogEntity();
        entity.setJobCode(definition.getCode());
        entity.setTraceId(context.getTraceId());
        entity.setTenantId(resolveTenant(context.getTenantId()));
        entity.setStatus(status);
        entity.setStartedAt(context.getStartedAt());
        entity.setFinishedAt(LocalDateTime.now());
        entity.setDurationMs(durationMs);
        entity.setErrorMessage(truncate(error, 4000));
        entity.setCreatedAt(LocalDateTime.now());
        logRepository.save(entity);
    }

    private void pushClock(JobDefinitionEntity definition, JobContext context) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime next = CronUtils.nextTime(definition.getCronExpr(), now);
        definitionRepository.updateRunResult(definition.getCode(), now, next);
    }

    private String resolveTenant(String tenantId) {
        if (!StringUtils.hasText(tenantId)) {
            return "default";
        }
        return tenantId;
    }

    private String truncate(String text, int max) {
        if (text == null) {
            return null;
        }
        return text.length() <= max ? text : text.substring(0, max);
    }
}
