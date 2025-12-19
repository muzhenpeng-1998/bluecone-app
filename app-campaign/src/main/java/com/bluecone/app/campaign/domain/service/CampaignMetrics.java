package com.bluecone.app.campaign.domain.service;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 活动指标
 */
@Component
@RequiredArgsConstructor
public class CampaignMetrics {
    
    private final MeterRegistry meterRegistry;
    
    /**
     * 记录活动应用次数（计价阶段）
     */
    public void recordCampaignApplied(String campaignType, String campaignCode) {
        Counter.builder("campaign.applied.total")
                .tag("type", campaignType)
                .tag("code", campaignCode)
                .description("活动应用总次数")
                .register(meterRegistry)
                .increment();
    }
    
    /**
     * 记录活动执行成功
     */
    public void recordExecutionSuccess(String campaignType, String campaignCode) {
        Counter.builder("campaign.execution.success.total")
                .tag("type", campaignType)
                .tag("code", campaignCode)
                .description("活动执行成功总次数")
                .register(meterRegistry)
                .increment();
    }
    
    /**
     * 记录活动执行失败
     */
    public void recordExecutionFailure(String campaignType, String campaignCode, String reason) {
        Counter.builder("campaign.execution.failed.total")
                .tag("type", campaignType)
                .tag("code", campaignCode)
                .tag("reason", reason)
                .description("活动执行失败总次数")
                .register(meterRegistry)
                .increment();
    }
    
    /**
     * 记录活动跳过
     */
    public void recordExecutionSkipped(String campaignType, String campaignCode, String reason) {
        Counter.builder("campaign.execution.skipped.total")
                .tag("type", campaignType)
                .tag("code", campaignCode)
                .tag("reason", reason)
                .description("活动跳过总次数")
                .register(meterRegistry)
                .increment();
    }
}
