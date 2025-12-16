package com.bluecone.app.id.internal.core;

import java.security.SecureRandom;
import java.time.Clock;
import java.util.concurrent.locks.ReentrantLock;

import com.bluecone.app.id.core.ClockRollbackException;
import com.bluecone.app.id.internal.config.BlueconeIdProperties;
import com.bluecone.app.id.internal.config.BlueconeIdProperties.Ulid.Rollback;
import com.bluecone.app.id.internal.metrics.UlidMetrics;

import de.huxhorn.sulky.ulid.ULID;

/**
 * Monotonic ULID generator designed for ordered inserts (e.g., MySQL primary keys).
 * <p>
 * 支持 STRICT（单锁全序）与 STRIPED（多分片）两种模式，通过分片降低锁竞争。
 */
public class UlidIdGenerator {

    private static final long TIMESTAMP_MASK = 0xFFFF_FFFFFFFFFFFFL; // 48 bits

    private static final int MAX_STRIPES = 1024;

    private final int stripes;
    private final int stripeBits;
    private final long stripeMask;
    private final State[] states;

    private final Clock clock;
    private final Rollback rollback;
    private final UlidMetrics metrics;

    /**
     * 单个分片的状态。
     */
    private static final class State {

        final SecureRandom random = new SecureRandom();
        final ReentrantLock lock = new ReentrantLock();

        long lastTimestamp = -1L;
        long lastMsb = 0L;
        long lastLsb = 0L;
    }

    /**
     * 默认构造器，保持兼容性：等价于 STRICT 模式（1 个分片）。
     */
    public UlidIdGenerator() {
        this(1, Clock.systemUTC(), new Rollback(), UlidMetrics.noop());
    }

    /**
     * 静态工厂方法，根据分片数量创建生成器实例。
     *
     * @param stripes 分片数量
     * @return ULID 生成器
     */
    public static UlidIdGenerator create(int stripes) {
        return new UlidIdGenerator(stripes, Clock.systemUTC(), new Rollback(), UlidMetrics.noop());
    }

    /**
     * 完整静态工厂方法，可注入 Clock、回拨策略与指标。
     *
     * @param stripes  分片数量
     * @param clock    时钟实现
     * @param rollback 回拨策略配置
     * @param metrics  指标封装
     * @return ULID 生成器
     */
    public static UlidIdGenerator create(int stripes,
                                         Clock clock,
                                         Rollback rollback,
                                         UlidMetrics metrics) {
        return new UlidIdGenerator(stripes, clock, rollback, metrics);
    }

    /**
     * 根据分片数量构造生成器。
     *
     * @param stripes 分片数量
     */
    UlidIdGenerator(int stripes) {
        this(stripes, Clock.systemUTC(), new Rollback(), UlidMetrics.noop());
    }

    /**
     * 根据分片数量及附加配置构造生成器。
     *
     * @param stripes  分片数量
     * @param clock    时钟实现
     * @param rollback 回拨策略配置
     * @param metrics  指标封装
     */
    UlidIdGenerator(int stripes, Clock clock, Rollback rollback, UlidMetrics metrics) {
        int fixedStripes = stripes;
        if (fixedStripes < 1) {
            fixedStripes = 1;
        } else if (fixedStripes > MAX_STRIPES) {
            fixedStripes = MAX_STRIPES;
        }
        this.stripes = fixedStripes;
        this.stripeBits = (this.stripes == 1)
                ? 0
                : Integer.SIZE - Integer.numberOfLeadingZeros(this.stripes - 1);
        this.stripeMask = (stripeBits == 0) ? 0L : (1L << stripeBits) - 1L;
        this.states = new State[this.stripes];
        for (int i = 0; i < this.stripes; i++) {
            this.states[i] = new State();
        }
        this.clock = (clock != null ? clock : Clock.systemUTC());
        this.rollback = (rollback != null ? rollback : new Rollback());
        this.metrics = (metrics != null ? metrics : UlidMetrics.noop());
    }

    /**
     * 生成下一个 ULID 值对象，在同一毫秒内保证单调递增。
     *
     * @return 下一个 ULID 值
     */
    public ULID.Value nextValue() {
        int idx = selectStripeIndex();
        State state = states[idx];
        long startNanos = System.nanoTime();

        while (true) {
            long now = clock.millis();
            boolean shouldWait = false;
            long rollbackMsForWait = 0L;

            state.lock.lock();
            try {
                long lastTs = state.lastTimestamp;
                long timestampToUse = now;

                if (lastTs >= 0L && now < lastTs) {
                    long rollbackMs = lastTs - now;
                    metrics.recordRollback(rollbackMs);

                    BlueconeIdProperties.Ulid.Policy policy = rollback.getPolicy();
                    switch (policy) {
                        case USE_LAST:
                            // 使用上一次时间戳，保证单调不中断
                            timestampToUse = lastTs;
                            break;
                        case FAIL_FAST:
                            long threshold = rollback.getFailFastThresholdMs();
                            if (threshold > 0 && rollbackMs > threshold) {
                                throw new ClockRollbackException(rollbackMs, threshold);
                            }
                            timestampToUse = lastTs;
                            break;
                        case WAIT:
                            // 不在锁内睡眠，记录信息后在锁外等待
                            shouldWait = true;
                            rollbackMsForWait = rollbackMs;
                            break;
                        default:
                            timestampToUse = lastTs;
                            break;
                    }
                }

                if (!shouldWait) {
                    // 正常生成路径：首次生成或已处理回拨
                    if (state.lastTimestamp < 0L || timestampToUse > state.lastTimestamp) {
                        bootstrapRandom(state, timestampToUse, idx);
                    } else {
                        incrementRandom(state, idx);
                    }
                    ULID.Value value = new ULID.Value(state.lastMsb, state.lastLsb);
                    metrics.recordGenerated(System.nanoTime() - startNanos);
                    return value;
                }
            } finally {
                state.lock.unlock();
            }

            // WAIT 策略下在锁外等待一小段时间后重试
            metrics.recordWait();
            long waitMax = rollback.getWaitMaxMs();
            long sleepMs = (waitMax > 0L)
                    ? Math.min(rollbackMsForWait, waitMax)
                    : rollbackMsForWait;
            if (sleepMs > 0L) {
                try {
                    Thread.sleep(sleepMs);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            } else {
                Thread.yield();
            }
        }
    }

    /**
     * 生成下一个 ULID 字符串，在同一毫秒内保证单调递增。
     *
     * @return ULID 字符串（26 位）
     */
    public String nextUlid() {
        ULID.Value value = nextValue();
        return value.toString();
    }

    /**
     * 根据当前线程选择分片索引，保证线程内稳定映射。
     *
     * @return 分片索引
     */
    private int selectStripeIndex() {
        if (stripes == 1) {
            return 0;
        }
        long tid = Thread.currentThread().getId();
        int idx = (int) (tid % stripes);
        if (idx < 0) {
            idx += stripes;
        }
        return idx;
    }

    /**
     * 初始化指定分片的随机状态。
     *
     * @param state     分片状态
     * @param timestamp 当前时间戳（毫秒）
     * @param idx       分片索引
     */
    private void bootstrapRandom(State state, long timestamp, int idx) {
        long randomLow = state.random.nextLong();
        if (stripeBits > 0) {
            // 将分片 ID 编码到低 stripeBits 位，降低跨分片碰撞风险
            randomLow = (randomLow & ~stripeMask) | (idx & stripeMask);
        }
        long randomHigh = state.random.nextInt(1 << 16) & 0xFFFFL;

        state.lastTimestamp = timestamp;
        state.lastMsb = ((timestamp & TIMESTAMP_MASK) << 16) | randomHigh;
        state.lastLsb = randomLow;

        // 防御性检查：保证低位始终等于分片 ID
        if (stripeBits > 0) {
            long expected = idx & stripeMask;
            long actual = state.lastLsb & stripeMask;
            if (actual != expected) {
                state.lastLsb = (state.lastLsb & ~stripeMask) | expected;
            }
        }
    }

    /**
     * 在同一毫秒内递增随机部分，保持单调性。
     *
     * @param state 分片状态
     * @param idx   分片索引
     */
    private void incrementRandom(State state, int idx) {
        if (stripeBits == 0) {
            // 单分片：沿用原有 randomLow + 1 逻辑
            long randomHigh = state.lastMsb & 0xFFFFL;
            long randomLow = state.lastLsb + 1;
            if (randomLow == 0L) {
                randomHigh = (randomHigh + 1) & 0xFFFFL;
                if (randomHigh == 0L) {
                    state.lastTimestamp += 1;
                }
            }
            state.lastMsb = ((state.lastTimestamp & TIMESTAMP_MASK) << 16) | randomHigh;
            state.lastLsb = randomLow;
        } else {
            // 多分片：低 stripeBits 位固定为分片 ID，高位部分按步长递增
            long step = 1L << stripeBits;
            long randomLow = state.lastLsb + step;
            long randomHigh = state.lastMsb & 0xFFFFL;

            // 当高位部分回绕为 0 时，进位到 randomHigh，再必要时进位到时间戳
            if ((randomLow & ~stripeMask) == 0L) {
                randomHigh = (randomHigh + 1) & 0xFFFFL;
                if (randomHigh == 0L) {
                    state.lastTimestamp += 1;
                }
            }

            state.lastMsb = ((state.lastTimestamp & TIMESTAMP_MASK) << 16) | randomHigh;
            state.lastLsb = randomLow;

            // 防御性检查：确保低位始终为分片 ID
            long expected = idx & stripeMask;
            long actual = state.lastLsb & stripeMask;
            if (actual != expected) {
                state.lastLsb = (state.lastLsb & ~stripeMask) | expected;
            }
        }
    }
}
