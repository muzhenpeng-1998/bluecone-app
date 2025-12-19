package com.bluecone.app.promo.domain.service;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * 优惠券发放指标收集
 */
@Slf4j
@Component
public class CouponGrantMetrics {

    private final Counter grantSuccessCounter;
    private final Counter grantFailureCounter;
    private final Counter quotaExceededCounter;
    private final Counter userQuotaExceededCounter;
    private final Counter idempotentReplayCounter;
    private final Timer grantTimer;

    public CouponGrantMetrics(MeterRegistry registry) {
        this.grantSuccessCounter = Counter.builder("coupon.grant.success")
                .description("优惠券发放成功次数")
                .tag("component", "promo")
                .register(registry);

        this.grantFailureCounter = Counter.builder("coupon.grant.failure")
                .description("优惠券发放失败次数")
                .tag("component", "promo")
                .register(registry);

        this.quotaExceededCounter = Counter.builder("coupon.grant.quota_exceeded")
                .description("总配额超限次数")
                .tag("component", "promo")
                .register(registry);

        this.userQuotaExceededCounter = Counter.builder("coupon.grant.user_quota_exceeded")
                .description("用户配额超限次数")
                .tag("component", "promo")
                .register(registry);

        this.idempotentReplayCounter = Counter.builder("coupon.grant.idempotent_replay")
                .description("幂等重放次数")
                .tag("component", "promo")
                .register(registry);

        this.grantTimer = Timer.builder("coupon.grant.duration")
                .description("优惠券发放耗时")
                .tag("component", "promo")
                .register(registry);
    }

    /**
     * 记录发放成功
     */
    public void recordSuccess() {
        grantSuccessCounter.increment();
    }

    /**
     * 记录发放失败
     */
    public void recordFailure(String reason) {
        grantFailureCounter.increment();
        log.warn("优惠券发放失败: reason={}", reason);
    }

    /**
     * 记录总配额超限
     */
    public void recordQuotaExceeded(Long templateId) {
        quotaExceededCounter.increment();
        log.warn("优惠券模板配额已用完: templateId={}", templateId);
    }

    /**
     * 记录用户配额超限
     */
    public void recordUserQuotaExceeded(Long userId, Long templateId) {
        userQuotaExceededCounter.increment();
        log.warn("用户优惠券配额已用完: userId={}, templateId={}", userId, templateId);
    }

    /**
     * 记录幂等重放
     */
    public void recordIdempotentReplay(String idempotencyKey) {
        idempotentReplayCounter.increment();
        log.debug("优惠券发放幂等重放: idempotencyKey={}", idempotencyKey);
    }

    /**
     * 记录发放耗时
     */
    public void recordDuration(long startTime) {
        long duration = System.currentTimeMillis() - startTime;
        grantTimer.record(duration, TimeUnit.MILLISECONDS);
    }
}
