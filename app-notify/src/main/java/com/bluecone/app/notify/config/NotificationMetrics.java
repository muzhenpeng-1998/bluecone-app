package com.bluecone.app.notify.config;

import com.bluecone.app.notify.api.enums.NotificationChannel;
import com.bluecone.app.notify.api.enums.NotificationTaskStatus;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * 通知中心指标
 * 记录任务状态、发送成功率、延迟等指标
 */
@Component
@RequiredArgsConstructor
public class NotificationMetrics {
    
    private final MeterRegistry meterRegistry;
    private final ConcurrentHashMap<String, Long> taskCounters = new ConcurrentHashMap<>();
    
    /**
     * 记录任务创建
     */
    public void recordTaskCreated(String bizType, NotificationChannel channel) {
        Counter.builder("notify.task.created")
                .tag("biz_type", bizType)
                .tag("channel", channel.name())
                .register(meterRegistry)
                .increment();
    }
    
    /**
     * 记录任务发送成功
     */
    public void recordTaskSent(String bizType, NotificationChannel channel, long durationMs) {
        Counter.builder("notify.task.sent")
                .tag("biz_type", bizType)
                .tag("channel", channel.name())
                .register(meterRegistry)
                .increment();
        
        Timer.builder("notify.task.send.duration")
                .tag("biz_type", bizType)
                .tag("channel", channel.name())
                .register(meterRegistry)
                .record(durationMs, TimeUnit.MILLISECONDS);
    }
    
    /**
     * 记录任务发送失败
     */
    public void recordTaskFailed(String bizType, NotificationChannel channel, String errorCode) {
        Counter.builder("notify.task.failed")
                .tag("biz_type", bizType)
                .tag("channel", channel.name())
                .tag("error_code", errorCode != null ? errorCode : "unknown")
                .register(meterRegistry)
                .increment();
    }
    
    /**
     * 记录任务被频控限制
     */
    public void recordTaskRateLimited(String bizType, NotificationChannel channel) {
        Counter.builder("notify.task.rate_limited")
                .tag("biz_type", bizType)
                .tag("channel", channel.name())
                .register(meterRegistry)
                .increment();
    }
    
    /**
     * 注册任务状态计数器
     */
    public void registerTaskStatusGauge(NotificationTaskStatus status, Gauge.Builder<Long> gaugeBuilder) {
        String key = "status_" + status.name();
        taskCounters.putIfAbsent(key, 0L);
        
        Gauge.builder("notify.task.status.count", taskCounters, m -> m.getOrDefault(key, 0L))
                .tag("status", status.name())
                .register(meterRegistry);
    }
    
    /**
     * 更新任务状态计数
     */
    public void updateTaskStatusCount(NotificationTaskStatus status, long count) {
        String key = "status_" + status.name();
        taskCounters.put(key, count);
    }
}
