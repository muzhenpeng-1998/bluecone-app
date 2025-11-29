package com.bluecone.app.infra.scheduler.core;

import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import com.bluecone.app.infra.tenant.TenantContext;

/**
 * 隔离执行器：负责超时、MDC 与租户上下文的防护，避免任务互相污染。
 */
public class ExecutionSandbox {

    private static final Logger log = LoggerFactory.getLogger(ExecutionSandbox.class);

    private final ExecutorService executor;

    public ExecutionSandbox() {
        this.executor = Executors.newCachedThreadPool(new SandboxThreadFactory());
    }

    public void run(JobContext context, Callable<Void> callable) throws Exception {
        String traceId = context.getTraceId() != null ? context.getTraceId() : UUID.randomUUID().toString().replace("-", "");
        MDC.put("traceId", traceId);
        if (context.getTenantId() != null) {
            TenantContext.setTenantId(context.getTenantId());
        }
        try {
            Future<Void> future = executor.submit(callable);
            long timeout = Math.max(context.getTimeoutSeconds(), 1);
            future.get(timeout, TimeUnit.SECONDS);
        } catch (TimeoutException te) {
            log.error("[Scheduler] job timeout code={} timeoutSeconds={}", context.getCode(), context.getTimeoutSeconds());
            throw new IllegalStateException("Job timeout: " + context.getCode(), te);
        } catch (ExecutionException ee) {
            Throwable cause = ee.getCause() == null ? ee : ee.getCause();
            throw (cause instanceof Exception) ? (Exception) cause : new RuntimeException(cause);
        } finally {
            TenantContext.clear();
            MDC.clear();
        }
    }

    public void shutdown(Duration timeout) {
        executor.shutdown();
        try {
            executor.awaitTermination(timeout.toMillis(), TimeUnit.MILLISECONDS);
        } catch (InterruptedException ignored) {
            Thread.currentThread().interrupt();
        } finally {
            executor.shutdownNow();
        }
    }

    private static final class SandboxThreadFactory implements ThreadFactory {
        private int counter = 0;

        @Override
        public Thread newThread(Runnable r) {
            Thread t = new Thread(r, "scheduler-sandbox-" + counter++);
            t.setDaemon(true);
            return t;
        }
    }
}
