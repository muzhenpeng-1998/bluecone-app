package com.bluecone.app.infra.scheduler;

import static org.assertj.core.api.Assertions.assertThat;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.bluecone.app.infra.scheduler.annotation.BlueconeJob;
import com.bluecone.app.infra.scheduler.core.JobContext;
import com.bluecone.app.infra.scheduler.core.JobHandler;
import com.bluecone.app.infra.scheduler.entity.JobDefinitionEntity;
import com.bluecone.app.infra.scheduler.entity.JobExecutionLogEntity;
import com.bluecone.app.infra.scheduler.mapper.JobExecutionLogMapper;
import com.bluecone.app.infra.scheduler.queue.SchedulerQueuePayload;
import com.bluecone.app.infra.scheduler.repository.JobDefinitionRepository;
import com.bluecone.app.infra.scheduler.runner.SchedulerExecutor;
import com.bluecone.app.infra.test.AbstractIntegrationTest;
import java.time.LocalDateTime;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.TestPropertySource;

@TestPropertySource(properties = "bluecone.scheduler.enabled=true")
class SchedulerExecutorIT extends AbstractIntegrationTest {

    @Autowired
    private SchedulerExecutor schedulerExecutor;

    @Autowired
    private JobDefinitionRepository jobDefinitionRepository;

    @Autowired
    private JobExecutionLogMapper jobExecutionLogMapper;

    @Autowired
    private com.bluecone.app.infra.scheduler.core.JobRegistry jobRegistry;

    @Autowired
    private RecordingJobHandler recordingJobHandler;

    @BeforeEach
    void setUp() {
        cleanDatabase();
        JobDefinitionEntity definition = new JobDefinitionEntity();
        definition.setCode("test.job.execution");
        definition.setName("Test Job");
        definition.setCronExpr("0/5 * * * * ?");
        definition.setEnabled(true);
        definition.setTimeoutSeconds(30);
        definition.setTenantId("default");
        definition.setCreatedAt(LocalDateTime.now());
        definition.setUpdatedAt(LocalDateTime.now());
        definition.setNextRunAt(LocalDateTime.now());
        jobDefinitionRepository.insert(definition);
        jobRegistry.register(definition, recordingJobHandler);
    }

    @Test
    void executesRegisteredJobAndPersistsLog() {
        JobDefinitionEntity definition = jobDefinitionRepository.findByCode("test.job.execution");
        assertThat(definition).isNotNull();

        SchedulerQueuePayload payload = new SchedulerQueuePayload(
                definition.getCode(),
                "trace-it-" + System.currentTimeMillis(),
                "tenant-1",
                LocalDateTime.now());

        schedulerExecutor.execute(payload);

        JobContext captured = recordingJobHandler.lastContext.get();
        assertThat(captured).isNotNull();
        assertThat(captured.getTraceId()).isEqualTo(payload.getTraceId());
        assertThat(captured.getTenantId()).isEqualTo("tenant-1");

        JobExecutionLogEntity logEntity = jobExecutionLogMapper.selectOne(new LambdaQueryWrapper<>());
        assertThat(logEntity).isNotNull();
        assertThat(logEntity.getJobCode()).isEqualTo(definition.getCode());
        assertThat(logEntity.getTraceId()).isEqualTo(payload.getTraceId());
        assertThat(logEntity.getStatus()).isEqualTo("SUCCESS");
    }

    @Configuration
    static class SchedulerTestConfig {

        @Bean
        RecordingJobHandler recordingJobHandler() {
            return new RecordingJobHandler();
        }
    }

    @BlueconeJob(code = "test.job.execution", name = "Test Job", cron = "0/5 * * * * ?", timeoutSeconds = 30)
    static class RecordingJobHandler implements JobHandler {

        private final AtomicReference<JobContext> lastContext = new AtomicReference<>();

        @Override
        public void handle(JobContext context) {
            lastContext.set(context);
        }
    }
}
