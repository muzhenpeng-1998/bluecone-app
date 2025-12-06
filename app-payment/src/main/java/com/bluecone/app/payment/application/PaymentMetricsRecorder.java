package com.bluecone.app.payment.application;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

/**
 * 支付链路 Micrometer 指标收集器。
 *
 * <p>指标命名约定（用于 Grafana Dashboard）：<br>
 * - bluecone_payment_create_total：支付创建请求量，tags={channel,method,scene,result}；<br>
 * - bluecone_payment_create_duration：支付创建耗时分布（计时器），tags 同上；<br>
 * - bluecone_payment_callback_total：支付回调处理次数，tags={channel,method,result}；<br>
 * - bluecone_payment_callback_duration：支付回调耗时分布，tags 同上；<br>
 * - bluecone_outbox_publish_total：Outbox 写入/发布次数，tags={topic,result}，用于观测支付事件写库/失败。</p>
 */
@Component
public class PaymentMetricsRecorder {

    private final MeterRegistry meterRegistry;

    public PaymentMetricsRecorder(final MeterRegistry meterRegistry) {
        this.meterRegistry = Objects.requireNonNull(meterRegistry, "meterRegistry must not be null");
    }

    public void recordCreatePayment(final String channel,
                                    final String method,
                                    final String scene,
                                    final String result,
                                    final long startNanos) {
        long durationNanos = System.nanoTime() - startNanos;
        List<Tag> tags = List.of(
                Tag.of("channel", nullToUnknown(channel)),
                Tag.of("method", nullToUnknown(method)),
                Tag.of("scene", nullToUnknown(scene)),
                Tag.of("result", nullToUnknown(result))
        );
        Timer.builder("bluecone_payment_create_duration")
                .description("Create payment API duration (tags: channel/method/scene/result)")
                .tags(tags)
                .register(meterRegistry)
                .record(durationNanos, TimeUnit.NANOSECONDS);
        meterRegistry.counter("bluecone_payment_create_total", tags).increment();
    }

    public void recordCallback(final String channel,
                               final String method,
                               final String result,
                               final long startNanos) {
        long durationNanos = System.nanoTime() - startNanos;
        List<Tag> tags = List.of(
                Tag.of("channel", nullToUnknown(channel)),
                Tag.of("method", nullToUnknown(method)),
                Tag.of("result", nullToUnknown(result))
        );
        Timer.builder("bluecone_payment_callback_duration")
                .description("Payment callback processing duration (tags: channel/method/result)")
                .tags(tags)
                .register(meterRegistry)
                .record(durationNanos, TimeUnit.NANOSECONDS);
        meterRegistry.counter("bluecone_payment_callback_total", tags).increment();
    }

    public void recordEventPublish(final String topic, final String result) {
        List<Tag> tags = List.of(
                Tag.of("topic", nullToUnknown(topic)),
                Tag.of("result", nullToUnknown(result))
        );
        meterRegistry.counter("bluecone_outbox_publish_total", tags).increment();
    }

    private String nullToUnknown(final String value) {
        return value == null || value.isBlank() ? "UNKNOWN" : value;
    }
}
