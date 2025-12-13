package com.bluecone.app.id.core;

/**
 * 系统时钟回拨超过阈值时抛出的异常。
 */
public class ClockRollbackException extends RuntimeException {

    private final long rollbackMs;
    private final long thresholdMs;

    /**
     * @param rollbackMs  实际检测到的回拨毫秒数
     * @param thresholdMs 配置的 FAIL_FAST 阈值毫秒数
     */
    public ClockRollbackException(long rollbackMs, long thresholdMs) {
        super("检测到系统时钟回拨 " + rollbackMs + " ms，超过阈值 " + thresholdMs + " ms，已按 FAIL_FAST 策略拒绝生成ID");
        this.rollbackMs = rollbackMs;
        this.thresholdMs = thresholdMs;
    }

    public long getRollbackMs() {
        return rollbackMs;
    }

    public long getThresholdMs() {
        return thresholdMs;
    }
}

