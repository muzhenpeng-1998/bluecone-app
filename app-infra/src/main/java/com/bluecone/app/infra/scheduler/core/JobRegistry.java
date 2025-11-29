package com.bluecone.app.infra.scheduler.core;

import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import com.bluecone.app.infra.scheduler.entity.JobDefinitionEntity;

/**
 * Job 元数据与处理器的内存注册表。
 */
public class JobRegistry {

    private final Map<String, RegisteredJob> jobs = new ConcurrentHashMap<>();

    public void register(JobDefinitionEntity definition, JobHandler handler) {
        Objects.requireNonNull(definition, "definition must not be null");
        Objects.requireNonNull(handler, "handler must not be null");
        jobs.put(definition.getCode(), new RegisteredJob(definition, handler));
    }

    public Optional<RegisteredJob> get(String code) {
        return Optional.ofNullable(jobs.get(code));
    }

    public Collection<RegisteredJob> all() {
        return jobs.values();
    }

    public static class RegisteredJob {
        private final JobDefinitionEntity definition;
        private final JobHandler handler;

        public RegisteredJob(JobDefinitionEntity definition, JobHandler handler) {
            this.definition = definition;
            this.handler = handler;
        }

        public JobDefinitionEntity getDefinition() {
            return definition;
        }

        public JobHandler getHandler() {
            return handler;
        }
    }
}
