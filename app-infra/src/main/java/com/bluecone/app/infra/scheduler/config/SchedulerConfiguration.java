package com.bluecone.app.infra.scheduler.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.bluecone.app.infra.redis.core.RedisKeyBuilder;
import com.bluecone.app.infra.redis.core.RedisOps;
import com.bluecone.app.infra.scheduler.core.ExecutionPipeline;
import com.bluecone.app.infra.scheduler.core.ExecutionSandbox;
import com.bluecone.app.infra.scheduler.core.JobRegistry;
import com.bluecone.app.infra.scheduler.dispatcher.SchedulerLoop;
import com.bluecone.app.infra.scheduler.queue.SchedulerQueueWorker;
import com.bluecone.app.infra.scheduler.runner.SchedulerExecutor;
import com.bluecone.app.infra.scheduler.service.JobDefinitionService;
import com.bluecone.app.infra.scheduler.service.JobExecutionService;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * 调度中心装配：注册心跳、执行器与消费者。
 */
@Configuration
@EnableConfigurationProperties(SchedulerProperties.class)
public class SchedulerConfiguration {

    @Bean
    public JobRegistry jobRegistry() {
        return new JobRegistry();
    }

    @Bean
    public ExecutionPipeline executionPipeline() {
        return new ExecutionPipeline();
    }

    @Bean
    public ExecutionSandbox executionSandbox() {
        return new ExecutionSandbox();
    }

    @Bean
    @ConditionalOnProperty(prefix = "bluecone.scheduler", name = "enabled", havingValue = "true", matchIfMissing = true)
    public SchedulerExecutor schedulerExecutor(JobRegistry registry,
                                               ExecutionPipeline pipeline,
                                               ExecutionSandbox sandbox,
                                               JobExecutionService executionService) {
        return new SchedulerExecutor(registry, pipeline, sandbox, executionService);
    }

    @Bean
    @ConditionalOnProperty(prefix = "bluecone.scheduler", name = "enabled", havingValue = "true", matchIfMissing = true)
    public SchedulerQueueWorker schedulerQueueWorker(RedisOps redisOps,
                                                     SchedulerExecutor schedulerExecutor,
                                                     SchedulerProperties properties,
                                                     ObjectMapper objectMapper) {
        return new SchedulerQueueWorker(redisOps, schedulerExecutor, properties, objectMapper);
    }

    @Bean
    @ConditionalOnProperty(prefix = "bluecone.scheduler", name = "enabled", havingValue = "true", matchIfMissing = true)
    public SchedulerLoop schedulerLoop(JobDefinitionService jobDefinitionService,
                                       com.bluecone.app.infra.scheduler.queue.SchedulerQueuePublisher publisher,
                                       SchedulerProperties properties,
                                       RedisOps redisOps,
                                       RedisKeyBuilder redisKeyBuilder) {
        return new SchedulerLoop(jobDefinitionService, publisher, properties, redisOps, redisKeyBuilder);
    }

    @Bean
    @ConditionalOnProperty(prefix = "bluecone.scheduler", name = "enabled", havingValue = "true", matchIfMissing = true)
    public SchedulerStarter schedulerStarter(ApplicationContext applicationContext,
                                             JobDefinitionService jobDefinitionService,
                                             JobRegistry jobRegistry) {
        return new SchedulerStarter(applicationContext, jobDefinitionService, jobRegistry);
    }
}
