package com.bluecone.app.id.internal.metrics;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import com.bluecone.app.id.internal.config.BlueconeIdProperties;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.DistributionSummary;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
import io.micrometer.core.instrument.Timer;

/**
 * ULID 指标采集封装，避免在生成器内部散落 Micrometer 逻辑。
 */
public class UlidMetrics {

    private final Counter generatedCounter;
    private final Timer generateTimer;
    private final Counter rollbackCounter;
    private final DistributionSummary rollbackMsSummary;
    private final AtomicLong rollbackMaxMs;
    private final Counter waitCounter;

    private UlidMetrics(Counter generatedCounter,
                        Timer generateTimer,
                        Counter rollbackCounter,
                        DistributionSummary rollbackMsSummary,
                        AtomicLong rollbackMaxMs,
                        Counter waitCounter) {
        this.generatedCounter = generatedCounter;
        this.generateTimer = generateTimer;
        this.rollbackCounter = rollbackCounter;
        this.rollbackMsSummary = rollbackMsSummary;
        this.rollbackMaxMs = rollbackMaxMs;
        this.waitCounter = waitCounter;
    }

    /**
     * 返回一个空实现，所有记录操作均为 no-op。
     *
     * @return 无指标实现
     */
    public static UlidMetrics noop() {
        return new UlidMetrics(null, null, null, null, null, null);
    }

    /**
     * 基于给定的 MeterRegistry 创建指标封装。
     *
     * @param registry 指标注册中心
     * @param prefix   指标名前缀，例如 {@code bluecone.id.ulid}
     * @param stripes  条带数量
     * @param mode     生成模式（STRICT/STRIPED）
     * @return 指标封装
     */
    public static UlidMetrics from(MeterRegistry registry,
                                   String prefix,
                                   int stripes,
                                   BlueconeIdProperties.Mode mode) {
        if (registry == null) {
            return noop();
        }
        String modeTag = (mode != null ? mode.name() : "UNKNOWN");
        Tags tags = Tags.of("mode", modeTag);

        Counter generated = Counter.builder(prefix + ".generated.total")
                .description("ULID 生成总次数")
                .tags(tags)
                .register(registry);

        Timer generateTimer = Timer.builder(prefix + ".generate.latency")
                .description("ULID 生成延迟（纳秒）")
                .tags(tags)
                .register(registry);

        Counter rollback = Counter.builder(prefix + ".rollback.total")
                .description("ULID 检测到系统时钟回拨的总次数")
                .tags(tags)
                .register(registry);

        DistributionSummary rollbackSummary = DistributionSummary.builder(prefix + ".rollback.ms")
                .description("ULID 时钟回拨幅度（毫秒）分布")
                .baseUnit("ms")
                .tags(tags)
                .register(registry);

        AtomicLong rollbackMax = new AtomicLong(0L);
        Gauge.builder(prefix + ".rollback.max.ms", rollbackMax, AtomicLong::get)
                .description("ULID 时钟回拨的最大毫秒数")
                .baseUnit("ms")
                .tags(tags)
                .register(registry);

        // 条带数量 Gauge，便于观测配置情况
        Gauge.builder(prefix + ".stripes", () -> (double) stripes)
                .description("ULID 生成条带数量")
                .tags(tags)
                .register(registry);

        Counter wait = Counter.builder(prefix + ".wait.total")
                .description("ULID 因时钟回拨进行等待重试的总次数")
                .tags(tags)
                .register(registry);

        return new UlidMetrics(generated, generateTimer, rollback, rollbackSummary, rollbackMax, wait);
    }

    /**
     * 记录一次 ULID 生成完成以及耗时。
     *
     * @param durationNanos 生成耗时（纳秒）
     */
    public void recordGenerated(long durationNanos) {
        if (generatedCounter != null) {
            generatedCounter.increment();
        }
        if (generateTimer != null) {
            generateTimer.record(durationNanos, TimeUnit.NANOSECONDS);
        }
    }

    /**
     * 记录一次时钟回拨事件。
     *
     * @param rollbackMs 回拨毫秒数
     */
    public void recordRollback(long rollbackMs) {
        if (rollbackCounter != null) {
            rollbackCounter.increment();
        }
        if (rollbackMsSummary != null) {
            rollbackMsSummary.record(rollbackMs);
        }
        if (rollbackMaxMs != null) {
            rollbackMaxMs.updateAndGet(prev -> Math.max(prev, rollbackMs));
        }
    }

    /**
     * 记录一次因回拨而等待的事件。
     */
    public void recordWait() {
        if (waitCounter != null) {
            waitCounter.increment();
        }
    }
}

