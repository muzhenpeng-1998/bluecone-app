package com.bluecone.app.core.event.consume.spi;

/**
 * 事件消费指标扩展点。
 */
public interface ConsumeMetrics {

    void onAcquire();

    void onReplay();

    void onInProgress();

    void onSuccess();

    void onFailure();

    static ConsumeMetrics noop() {
        return new ConsumeMetrics() {
            @Override
            public void onAcquire() {
            }

            @Override
            public void onReplay() {
            }

            @Override
            public void onInProgress() {
            }

            @Override
            public void onSuccess() {
            }

            @Override
            public void onFailure() {
            }
        };
    }
}

