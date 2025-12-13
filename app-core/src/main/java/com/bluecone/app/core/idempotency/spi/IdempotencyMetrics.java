package com.bluecone.app.core.idempotency.spi;

import com.bluecone.app.core.idempotency.spi.IdempotencyRepository.AcquireState;

/**
 * 幂等相关指标接口，默认实现为 no-op，后续可接入 Micrometer。
 */
public interface IdempotencyMetrics {

    void recordAcquire(AcquireState state);

    void recordConflict();

    void recordReplay();

    void recordInProgress();

    static IdempotencyMetrics noop() {
        return new IdempotencyMetrics() {
            @Override
            public void recordAcquire(AcquireState state) {
            }

            @Override
            public void recordConflict() {
            }

            @Override
            public void recordReplay() {
            }

            @Override
            public void recordInProgress() {
            }
        };
    }
}

