package com.bluecone.app.infra.scheduler.config;

import java.util.Map;

import jakarta.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;

import com.bluecone.app.infra.scheduler.annotation.BlueconeJob;
import com.bluecone.app.infra.scheduler.core.JobHandler;
import com.bluecone.app.infra.scheduler.core.JobRegistry;
import com.bluecone.app.infra.scheduler.entity.JobDefinitionEntity;
import com.bluecone.app.infra.scheduler.service.JobDefinitionService;

/**
 * 启动时扫描并注册所有声明式任务。
 */
public class SchedulerStarter {

    private static final Logger log = LoggerFactory.getLogger(SchedulerStarter.class);

    private final ApplicationContext applicationContext;
    private final JobDefinitionService jobDefinitionService;
    private final JobRegistry jobRegistry;

    public SchedulerStarter(ApplicationContext applicationContext,
                            JobDefinitionService jobDefinitionService,
                            JobRegistry jobRegistry) {
        this.applicationContext = applicationContext;
        this.jobDefinitionService = jobDefinitionService;
        this.jobRegistry = jobRegistry;
    }

    @PostConstruct
    public void register() {
        Map<String, JobHandler> handlers = applicationContext.getBeansOfType(JobHandler.class);
        for (JobHandler handler : handlers.values()) {
            BlueconeJob annotation = handler.getClass().getAnnotation(BlueconeJob.class);
            if (annotation == null) {
                continue;
            }
            JobDefinitionEntity definition = jobDefinitionService.registerIfNeeded(annotation);
            jobRegistry.register(definition, handler);
            log.info("[Scheduler] registered job code={} name={} cron={}",
                    definition.getCode(), definition.getName(), definition.getCronExpr());
        }
    }
}
