package com.bluecone.app.id.core;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.LongSupplier;

/**
 * 基于 Snowflake 算法的 63 位 long 型 ID 生成器。
 *
 * <p>位布局：time(41) | node(10) | seq(12)</p>
 */
public final class SnowflakeLongIdGenerator {

    private static final int NODE_ID_BITS = 10;
    private static final int SEQUENCE_BITS = 12;

    private static final long MAX_NODE_ID = (1L << NODE_ID_BITS) - 1L;
    private static final long MAX_SEQUENCE = (1L << SEQUENCE_BITS) - 1L;
    private static final long MAX_TIMESTAMP = (1L << 41) - 1L;

    private final long epochMillis;
    private final long nodeId;
    private final LongSupplier nowMillis;

    /**
     * 高 52 位存储时间戳（41 bit），低 12 位存储序列号。
     */
    private final AtomicLong lastTimeAndSeq = new AtomicLong(-1L);

    /**
     * 使用系统时间的构造器。
     *
     * @param epochMillis 自定义纪元毫秒
     * @param nodeId      节点 ID，范围 [0, 1023]
     */
    public SnowflakeLongIdGenerator(long epochMillis, long nodeId) {
        this(epochMillis, nodeId, System::currentTimeMillis);
    }

    /**
     * 允许注入自定义时间源的构造器，便于测试。
     *
     * @param epochMillis 自定义纪元毫秒
     * @param nodeId      节点 ID，范围 [0, 1023]
     * @param nowMillis   时间提供者
     */
    public SnowflakeLongIdGenerator(long epochMillis, long nodeId, LongSupplier nowMillis) {
        if (nodeId < 0 || nodeId > MAX_NODE_ID) {
            throw new IllegalArgumentException(
                    "nodeId 必须在 [0," + MAX_NODE_ID + "] 之间，实际为 " + nodeId);
        }
        this.epochMillis = epochMillis;
        this.nodeId = nodeId;
        this.nowMillis = Objects.requireNonNull(nowMillis, "nowMillis 不能为空");
    }

    /**
     * 生成下一个全局唯一且单调递增的 long 型 ID。
     *
     * @return 63 位正整数 ID
     */
    public long nextId() {
        while (true) {
            long prev = lastTimeAndSeq.get();

            long now = nowMillis.getAsLong();
            long timestamp = Math.max(0L, now - epochMillis);
            if (timestamp > MAX_TIMESTAMP) {
                timestamp = timestamp % (MAX_TIMESTAMP + 1);
            }

            long prevTimestamp;
            long prevSeq;
            if (prev < 0L) {
                prevTimestamp = -1L;
                prevSeq = 0L;
            } else {
                prevTimestamp = prev >>> SEQUENCE_BITS;
                prevSeq = prev & MAX_SEQUENCE;
            }

            if (prevTimestamp >= 0L && timestamp < prevTimestamp) {
                // 时钟回拨：使用上一次时间戳，保证单调性
                timestamp = prevTimestamp;
            }

            long sequence;
            if (timestamp == prevTimestamp) {
                sequence = prevSeq + 1L;
                if (sequence > MAX_SEQUENCE) {
                    // 同一毫秒内序列号溢出，推进时间戳 1ms
                    timestamp = prevTimestamp + 1L;
                    sequence = 0L;
                }
            } else {
                sequence = 0L;
            }

            long nextPacked = (timestamp << SEQUENCE_BITS) | sequence;
            if (lastTimeAndSeq.compareAndSet(prev, nextPacked)) {
                return assembleId(timestamp, sequence);
            }
        }
    }

    private long assembleId(long timestamp, long sequence) {
        long tsPart = (timestamp & MAX_TIMESTAMP) << (NODE_ID_BITS + SEQUENCE_BITS);
        long nodePart = (nodeId & MAX_NODE_ID) << SEQUENCE_BITS;
        long seqPart = sequence & MAX_SEQUENCE;
        // 最高位始终为 0，得到 63 位正整数
        return tsPart | nodePart | seqPart;
    }
}

