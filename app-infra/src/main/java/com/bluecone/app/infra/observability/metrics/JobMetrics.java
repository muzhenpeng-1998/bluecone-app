package com.bluecone.app.infra.observability.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Job Metrics
 * 
 * Provides custom metrics for scheduled jobs:
 * - Counter: job_executions_total (by job name and status)
 * - Counter: job_failures_total (by job name)
 * - Timer: job_execution_duration_seconds (execution latency by job name)
 */
@Slf4j
@Component
public class JobMetrics {

    private final MeterRegistry registry;

    public JobMetrics(MeterRegistry registry) {
        this.registry = registry;
    }

    /**
     * Record a successful job execution
     */
    public void recordExecutionSuccess(String jobName) {
        Counter.builder("job_executions_total")
                .description("Total number of job executions")
                .tag("job_name", jobName)
                .tag("status", "success")
                .register(registry)
                .increment();
    }

    /**
     * Record a failed job execution
     */
    public void recordExecutionFailure(String jobName) {
        Counter.builder("job_executions_total")
                .description("Total number of job executions")
                .tag("job_name", jobName)
                .tag("status", "failure")
                .register(registry)
                .increment();
        
        Counter.builder("job_failures_total")
                .description("Total number of job failures")
                .tag("job_name", jobName)
                .register(registry)
                .increment();
    }

    /**
     * Record job execution duration
     */
    public void recordExecutionDuration(String jobName, Runnable task) {
        Timer.builder("job_execution_duration_seconds")
                .description("Time taken to execute jobs")
                .tag("job_name", jobName)
                .register(registry)
                .record(task);
    }

    /**
     * Get timer for manual recording
     */
    public Timer.Sample startExecutionTimer() {
        return Timer.start(registry);
    }

    /**
     * Stop timer and record duration
     */
    public void stopExecutionTimer(Timer.Sample sample, String jobName) {
        sample.stop(Timer.builder("job_execution_duration_seconds")
                .description("Time taken to execute jobs")
                .tag("job_name", jobName)
                .register(registry));
    }

    /**
     * Record a consistency check result
     * Used by consistency jobs to track repair operations
     */
    public void recordConsistencyCheck(String jobName, int totalChecked, int missingCount, int repairedCount) {
        Counter.builder("job_consistency_checked_total")
                .description("Total number of records checked by consistency jobs")
                .tag("job_name", jobName)
                .register(registry)
                .increment(totalChecked);
        
        if (missingCount > 0) {
            Counter.builder("job_consistency_missing_total")
                    .description("Total number of missing records found by consistency jobs")
                    .tag("job_name", jobName)
                    .register(registry)
                    .increment(missingCount);
        }
        
        if (repairedCount > 0) {
            Counter.builder("job_consistency_repaired_total")
                    .description("Total number of records repaired by consistency jobs")
                    .tag("job_name", jobName)
                    .register(registry)
                    .increment(repairedCount);
        }
    }
}
