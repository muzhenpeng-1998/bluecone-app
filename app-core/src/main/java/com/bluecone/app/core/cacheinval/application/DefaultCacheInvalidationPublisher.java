package com.bluecone.app.core.cacheinval.application;

import com.bluecone.app.core.cacheinval.api.CacheInvalidationEvent;
import com.bluecone.app.core.cacheinval.api.CacheInvalidationPublisher;
import com.bluecone.app.core.cacheinval.transport.CacheInvalidationBus;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.Objects;

/**
 * Default publisher that executes invalidation locally and broadcasts
 * after the surrounding transaction commits.
 */
public class DefaultCacheInvalidationPublisher implements CacheInvalidationPublisher {

    private final CacheInvalidationExecutor executor;
    private final CacheInvalidationBus bus;

    public DefaultCacheInvalidationPublisher(CacheInvalidationExecutor executor,
                                             CacheInvalidationBus bus) {
        this.executor = Objects.requireNonNull(executor, "executor");
        this.bus = Objects.requireNonNull(bus, "bus");
    }

    @Override
    public void publishAfterCommit(CacheInvalidationEvent event) {
        if (event == null) {
            return;
        }
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    perform(event);
                }
            });
        } else {
            // No active transaction: execute immediately as best-effort.
            perform(event);
        }
    }

    private void perform(CacheInvalidationEvent event) {
        executor.execute(event);
        bus.broadcast(event);
    }
}

