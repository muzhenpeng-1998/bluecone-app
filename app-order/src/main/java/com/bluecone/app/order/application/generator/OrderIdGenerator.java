package com.bluecone.app.order.application.generator;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.concurrent.atomic.AtomicLong;
import org.springframework.stereotype.Component;

/**
 * 简易版的 Long 型订单 ID 生成器，模拟雪花算法。
 * TODO: 替换为统一的全局发号器实现。
 */
@Component
public class OrderIdGenerator {

    private static final long EPOCH_MS = ZonedDateTime.of(2025, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC)
            .toInstant()
            .toEpochMilli();
    private static final int SEQUENCE_BITS = 12;
    private static final long SEQUENCE_MASK = ~(-1L << SEQUENCE_BITS);

    private final AtomicLong sequence = new AtomicLong(0);

    public long nextId() {
        long timestamp = Instant.now().toEpochMilli() - EPOCH_MS;
        long seq = sequence.getAndIncrement() & SEQUENCE_MASK;
        return (timestamp << SEQUENCE_BITS) | seq;
    }
}
