package com.bluecone.app.id.core;

import java.util.Objects;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.LongSupplier;

/**
 * 基于无锁 CAS 的单调 ULID 生成器。
 *
 * <p>特性：</p>
 * <ul>
 *     <li>使用 {@link ThreadLocalRandom} 作为随机源；</li>
 *     <li>在同一毫秒内保证严格单调（按 msb/lsb 比较）；</li>
 *     <li>时钟回拨时退化为使用上一次时间戳，保持单调性。</li>
 * </ul>
 */
public final class MonotonicUlidGenerator {

    private static final long TIMESTAMP_MASK = 0xFFFF_FFFFFFFFFFFFL; // 48 bits

    private final LongSupplier nowMillis;
    private final AtomicReference<Ulid128> last = new AtomicReference<>();

    /**
     * 使用系统当前时间的默认构造器。
     */
    public MonotonicUlidGenerator() {
        this(System::currentTimeMillis);
    }

    /**
     * 允许注入自定义时间源的构造器，便于测试或特殊场景。
     *
     * @param nowMillis 当前时间提供者（毫秒）
     */
    public MonotonicUlidGenerator(LongSupplier nowMillis) {
        this.nowMillis = Objects.requireNonNull(nowMillis, "nowMillis 不能为空");
    }

    /**
     * 生成下一个 ULID 值对象，在同一毫秒内保证严格单调递增。
     *
     * @return 下一个 ULID 值
     */
    public Ulid128 nextUlid() {
        while (true) {
            Ulid128 previous = last.get();
            long now = safeTimestamp(nowMillis.getAsLong());

            Ulid128 next;
            if (previous == null) {
                next = randomUlid(now);
            } else {
                long prevMsb = previous.msb();
                long prevLsb = previous.lsb();
                long prevTs = (prevMsb >>> 16) & TIMESTAMP_MASK;

                long ts = now;
                if (ts < prevTs) {
                    // 时钟回拨：继续使用上一次时间戳，保持单调性
                    ts = prevTs;
                }

                if (ts > prevTs) {
                    // 新毫秒，重新引导随机部分
                    next = randomUlid(ts);
                } else {
                    // 同一毫秒内递增随机部分
                    next = increment(prevMsb, prevLsb);
                }
            }

            if (last.compareAndSet(previous, next)) {
                return next;
            }
        }
    }

    /**
     * 生成下一个 ULID 字符串表示（26 位）。
     *
     * @return ULID 字符串
     */
    public String nextUlidString() {
        return nextUlid().toString();
    }

    private static long safeTimestamp(long epochMillis) {
        if (epochMillis < 0L) {
            return 0L;
        }
        return epochMillis & TIMESTAMP_MASK;
    }

    private static Ulid128 randomUlid(long timestamp) {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        long randomHigh = random.nextInt(1 << 16) & 0xFFFFL; // 16 bits
        long randomLow = random.nextLong(); // 64 bits

        long msb = ((timestamp & TIMESTAMP_MASK) << 16) | randomHigh;
        long lsb = randomLow;
        return new Ulid128(msb, lsb);
    }

    private static Ulid128 increment(long msb, long lsb) {
        long timestamp = (msb >>> 16) & TIMESTAMP_MASK;
        long randomHigh = msb & 0xFFFFL;
        long randomLow = lsb + 1L;

        if (randomLow == 0L) {
            randomHigh = (randomHigh + 1L) & 0xFFFFL;
            if (randomHigh == 0L) {
                // 随机部分全部溢出时，将时间戳推进 1ms，继续保持单调
                timestamp = (timestamp + 1L) & TIMESTAMP_MASK;
            }
        }

        long newMsb = ((timestamp & TIMESTAMP_MASK) << 16) | randomHigh;
        return new Ulid128(newMsb, randomLow);
    }
}

