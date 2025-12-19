package com.bluecone.app.billing.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * 订阅计费指标收集器
 * 记录提醒、续费、宽限期等关键业务指标
 */
@Slf4j
@Component
public class BillingMetrics {
    
    private final MeterRegistry meterRegistry;
    
    // 提醒任务指标
    private final Counter reminderTaskCreatedCounter;
    private final Counter reminderTaskSentCounter;
    private final Counter reminderTaskFailedCounter;
    
    // 续费指标
    private final Counter renewalInvoiceCreatedCounter;
    private final Counter renewalPaymentSuccessCounter;
    private final Counter renewalPaymentFailedCounter;
    
    // 宽限期指标
    private final Counter graceEnteredCounter;
    private final Counter graceExitedCounter;
    private final Counter graceExpiredCounter;
    
    // 订阅状态指标
    private final Counter subscriptionActivatedCounter;
    private final Counter subscriptionExpiredCounter;
    private final Counter subscriptionDowngradedCounter;
    
    // Dunning 指标
    private final Counter dunningLogCreatedCounter;
    private final Counter dunningSuccessCounter;
    private final Counter dunningFailedCounter;
    
    public BillingMetrics(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
        
        // 初始化提醒任务指标
        this.reminderTaskCreatedCounter = Counter.builder("billing.reminder.task.created")
                .description("提醒任务创建数量")
                .tag("module", "billing")
                .register(meterRegistry);
        
        this.reminderTaskSentCounter = Counter.builder("billing.reminder.task.sent")
                .description("提醒任务发送成功数量")
                .tag("module", "billing")
                .register(meterRegistry);
        
        this.reminderTaskFailedCounter = Counter.builder("billing.reminder.task.failed")
                .description("提醒任务发送失败数量")
                .tag("module", "billing")
                .register(meterRegistry);
        
        // 初始化续费指标
        this.renewalInvoiceCreatedCounter = Counter.builder("billing.renewal.invoice.created")
                .description("续费账单创建数量")
                .tag("module", "billing")
                .register(meterRegistry);
        
        this.renewalPaymentSuccessCounter = Counter.builder("billing.renewal.payment.success")
                .description("续费支付成功数量")
                .tag("module", "billing")
                .register(meterRegistry);
        
        this.renewalPaymentFailedCounter = Counter.builder("billing.renewal.payment.failed")
                .description("续费支付失败数量")
                .tag("module", "billing")
                .register(meterRegistry);
        
        // 初始化宽限期指标
        this.graceEnteredCounter = Counter.builder("billing.grace.entered")
                .description("进入宽限期数量")
                .tag("module", "billing")
                .register(meterRegistry);
        
        this.graceExitedCounter = Counter.builder("billing.grace.exited")
                .description("退出宽限期数量（续费成功）")
                .tag("module", "billing")
                .register(meterRegistry);
        
        this.graceExpiredCounter = Counter.builder("billing.grace.expired")
                .description("宽限期过期数量（降级）")
                .tag("module", "billing")
                .register(meterRegistry);
        
        // 初始化订阅状态指标
        this.subscriptionActivatedCounter = Counter.builder("billing.subscription.activated")
                .description("订阅激活数量")
                .tag("module", "billing")
                .register(meterRegistry);
        
        this.subscriptionExpiredCounter = Counter.builder("billing.subscription.expired")
                .description("订阅到期数量")
                .tag("module", "billing")
                .register(meterRegistry);
        
        this.subscriptionDowngradedCounter = Counter.builder("billing.subscription.downgraded")
                .description("订阅降级数量")
                .tag("module", "billing")
                .register(meterRegistry);
        
        // 初始化 Dunning 指标
        this.dunningLogCreatedCounter = Counter.builder("billing.dunning.log.created")
                .description("Dunning 日志创建数量")
                .tag("module", "billing")
                .register(meterRegistry);
        
        this.dunningSuccessCounter = Counter.builder("billing.dunning.success")
                .description("Dunning 发送成功数量")
                .tag("module", "billing")
                .register(meterRegistry);
        
        this.dunningFailedCounter = Counter.builder("billing.dunning.failed")
                .description("Dunning 发送失败数量")
                .tag("module", "billing")
                .register(meterRegistry);
    }
    
    // ==================== 提醒任务指标 ====================
    
    public void recordReminderTaskCreated(String reminderType) {
        reminderTaskCreatedCounter.increment();
        log.debug("[billing-metrics] 提醒任务创建，reminderType={}", reminderType);
    }
    
    public void recordReminderTaskSent(String reminderType) {
        reminderTaskSentCounter.increment();
        log.debug("[billing-metrics] 提醒任务发送成功，reminderType={}", reminderType);
    }
    
    public void recordReminderTaskFailed(String reminderType) {
        reminderTaskFailedCounter.increment();
        log.debug("[billing-metrics] 提醒任务发送失败，reminderType={}", reminderType);
    }
    
    // ==================== 续费指标 ====================
    
    public void recordRenewalInvoiceCreated(Long tenantId, String planCode) {
        renewalInvoiceCreatedCounter.increment();
        log.info("[billing-metrics] 续费账单创建，tenantId={}, planCode={}", tenantId, planCode);
    }
    
    public void recordRenewalPaymentSuccess(Long tenantId, String planCode) {
        renewalPaymentSuccessCounter.increment();
        log.info("[billing-metrics] 续费支付成功，tenantId={}, planCode={}", tenantId, planCode);
    }
    
    public void recordRenewalPaymentFailed(Long tenantId, String planCode) {
        renewalPaymentFailedCounter.increment();
        log.info("[billing-metrics] 续费支付失败，tenantId={}, planCode={}", tenantId, planCode);
    }
    
    // ==================== 宽限期指标 ====================
    
    public void recordGraceEntered(Long tenantId, String planCode) {
        graceEnteredCounter.increment();
        log.info("[billing-metrics] 进入宽限期，tenantId={}, planCode={}", tenantId, planCode);
    }
    
    public void recordGraceExited(Long tenantId, String planCode) {
        graceExitedCounter.increment();
        log.info("[billing-metrics] 退出宽限期（续费成功），tenantId={}, planCode={}", tenantId, planCode);
    }
    
    public void recordGraceExpired(Long tenantId, String planCode) {
        graceExpiredCounter.increment();
        log.info("[billing-metrics] 宽限期过期（降级），tenantId={}, planCode={}", tenantId, planCode);
    }
    
    // ==================== 订阅状态指标 ====================
    
    public void recordSubscriptionActivated(Long tenantId, String planCode) {
        subscriptionActivatedCounter.increment();
        log.info("[billing-metrics] 订阅激活，tenantId={}, planCode={}", tenantId, planCode);
    }
    
    public void recordSubscriptionExpired(Long tenantId, String planCode) {
        subscriptionExpiredCounter.increment();
        log.info("[billing-metrics] 订阅到期，tenantId={}, planCode={}", tenantId, planCode);
    }
    
    public void recordSubscriptionDowngraded(Long tenantId, String fromPlanCode, String toPlanCode) {
        subscriptionDowngradedCounter.increment();
        log.info("[billing-metrics] 订阅降级，tenantId={}, from={}, to={}", tenantId, fromPlanCode, toPlanCode);
    }
    
    // ==================== Dunning 指标 ====================
    
    public void recordDunningLogCreated(String channel) {
        dunningLogCreatedCounter.increment();
        log.debug("[billing-metrics] Dunning 日志创建，channel={}", channel);
    }
    
    public void recordDunningSuccess(String channel) {
        dunningSuccessCounter.increment();
        log.debug("[billing-metrics] Dunning 发送成功，channel={}", channel);
    }
    
    public void recordDunningFailed(String channel) {
        dunningFailedCounter.increment();
        log.debug("[billing-metrics] Dunning 发送失败，channel={}", channel);
    }
    
    // ==================== 计时器 ====================
    
    public Timer.Sample startTimer() {
        return Timer.start(meterRegistry);
    }
    
    public void recordTimer(Timer.Sample sample, String timerName, String... tags) {
        sample.stop(Timer.builder(timerName)
                .tags(tags)
                .register(meterRegistry));
    }
}
