package com.bluecone.app.growth.domain.service;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 增长引擎指标监控
 */
@Slf4j
@Component
public class GrowthMetrics {
    
    private final MeterRegistry registry;
    private final Counter bindCounter;
    private final Counter rewardIssuedCounter;
    private final Counter rewardFailedCounter;
    private final Timer rewardIssueDuration;
    
    public GrowthMetrics(MeterRegistry registry) {
        this.registry = registry;
        this.bindCounter = Counter.builder("growth.bind.total")
                .description("Total number of invite bindings")
                .register(registry);
                
        this.rewardIssuedCounter = Counter.builder("growth.reward.issued.total")
                .description("Total number of rewards issued")
                .tag("kind", "all")
                .register(registry);
                
        this.rewardFailedCounter = Counter.builder("growth.reward.failed.total")
                .description("Total number of reward issuance failures")
                .register(registry);
                
        this.rewardIssueDuration = Timer.builder("growth.reward.issue.duration")
                .description("Duration of reward issuance")
                .register(registry);
    }
    
    /**
     * 记录绑定事件
     */
    public void recordBind() {
        bindCounter.increment();
    }
    
    /**
     * 记录奖励发放成功
     */
    public void recordRewardIssued(String rewardType) {
        rewardIssuedCounter.increment();
        Counter.builder("growth.reward.issued.total")
                .description("Total number of rewards issued by type")
                .tag("kind", rewardType.toLowerCase())
                .register(registry)
                .increment();
    }
    
    /**
     * 记录奖励发放失败
     */
    public void recordRewardFailed(String errorCode) {
        rewardFailedCounter.increment();
        Counter.builder("growth.reward.failed.total")
                .description("Total number of reward issuance failures by error code")
                .tag("error_code", errorCode)
                .register(registry)
                .increment();
    }
    
    /**
     * 记录奖励发放耗时
     */
    public void recordRewardDuration(long startTimeMillis) {
        long duration = System.currentTimeMillis() - startTimeMillis;
        rewardIssueDuration.record(java.time.Duration.ofMillis(duration));
    }
}
