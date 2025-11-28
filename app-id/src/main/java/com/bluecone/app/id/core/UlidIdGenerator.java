package com.bluecone.app.id.core;

import java.security.SecureRandom;
import java.util.concurrent.locks.ReentrantLock;

import de.huxhorn.sulky.ulid.ULID;

/**
 * Monotonic ULID generator designed for ordered inserts (e.g., MySQL primary keys).
 * Implements monotonicity manually to avoid API differences across ULID versions.
 */
public class UlidIdGenerator {

    private static final long TIMESTAMP_MASK = 0xFFFF_FFFFFFFFFFFFL; // 48 bits

    private final SecureRandom random = new SecureRandom();
    private final ReentrantLock lock = new ReentrantLock();

    private long lastTimestamp = -1L;
    private long lastMsb = 0L;
    private long lastLsb = 0L;

    /**
     * Generates the next ULID string, monotonic within the same millisecond.
     */
    public String nextUlid() {
        lock.lock();
        try {
            long now = System.currentTimeMillis();
            if (now > lastTimestamp) {
                bootstrapRandom(now);
            } else {
                incrementRandom();
            }
            ULID.Value value = new ULID.Value(lastMsb, lastLsb);
            return value.toString();
        } finally {
            lock.unlock();
        }
    }

    private void bootstrapRandom(long timestamp) {
        byte[] entropy = new byte[10];
        random.nextBytes(entropy);

        long randomHigh = ((entropy[0] & 0xFFL) << 8) | (entropy[1] & 0xFFL);
        long randomLow = 0L;
        for (int i = 2; i < 10; i++) {
            randomLow = (randomLow << 8) | (entropy[i] & 0xFFL);
        }

        lastTimestamp = timestamp;
        lastMsb = ((timestamp & TIMESTAMP_MASK) << 16) | randomHigh;
        lastLsb = randomLow;
    }

    private void incrementRandom() {
        long randomHigh = lastMsb & 0xFFFFL;
        long randomLow = lastLsb + 1;
        if (randomLow == 0L) {
            randomHigh = (randomHigh + 1) & 0xFFFFL;
            if (randomHigh == 0L) {
                lastTimestamp += 1;
            }
        }
        lastMsb = ((lastTimestamp & TIMESTAMP_MASK) << 16) | randomHigh;
        lastLsb = randomLow;
    }
}
