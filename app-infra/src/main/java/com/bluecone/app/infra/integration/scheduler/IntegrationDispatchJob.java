package com.bluecone.app.infra.integration.scheduler;

import com.bluecone.app.infra.integration.service.IntegrationDispatchService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Objects;

/**
 * 简化版调度任务：周期扫描待投递 Integration 任务。
 *
 * <p>未来可接入统一 Scheduler Center，当前以 @Scheduled 过渡。</p>
 */
@Component
@ConditionalOnProperty(prefix = "bluecone.integration", name = "enabled", havingValue = "true", matchIfMissing = true)
public class IntegrationDispatchJob {

    private static final Logger log = LoggerFactory.getLogger(IntegrationDispatchJob.class);

    private final IntegrationDispatchService dispatchService;

    public IntegrationDispatchJob(final IntegrationDispatchService dispatchService) {
        this.dispatchService = Objects.requireNonNull(dispatchService, "dispatchService must not be null");
    }

    @Scheduled(fixedDelayString = "${bluecone.integration.dispatch-interval-ms:5000}")
    public void dispatch() {
        try {
            dispatchService.dispatchDueDeliveries();
        } catch (Exception ex) {
            log.error("[Integration][Job] dispatch failed", ex);
        }
    }
}
