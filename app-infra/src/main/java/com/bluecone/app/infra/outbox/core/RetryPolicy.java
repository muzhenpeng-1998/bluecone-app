// File: app-infra/src/main/java/com/bluecone/app/infra/outbox/core/RetryPolicy.java
package com.bluecone.app.infra.outbox.core;

import java.time.Duration;

/**
 * 重试策略抽象，用于计算下一次延迟与终止条件。
 */
public interface RetryPolicy {

    /**
     * 计算下一次重试的延迟。
     *
     * @param attemptCount 已尝试次数（从 1 开始计数更直观）
     * @return 延迟时间
     */
    Duration nextDelay(int attemptCount);

    /**
     * 是否应放弃重试，进入 DEAD。
     *
     * @param attemptCount 已尝试次数
     * @param lastError    最近一次错误
     * @return true 表示不再重试
     */
    boolean shouldGiveUp(int attemptCount, Throwable lastError);
}
